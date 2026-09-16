package llc.redstone.htslreborn.queue.exporter

import llc.redstone.htslreborn.data.*
import llc.redstone.htslreborn.queue.Operation
import llc.redstone.htslreborn.queue.Operation.OpenMenu
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.Status
import llc.redstone.htslreborn.utils.InputUtils
import llc.redstone.htslreborn.utils.ItemStackUtils.getCurrentValue
import llc.redstone.htslreborn.utils.ItemStackUtils.getProperties
import llc.redstone.htslreborn.utils.ItemStackUtils.loreLines
import llc.redstone.htslreborn.utils.MenuUtils
import llc.redstone.htslreborn.utils.MenuUtils.ACTION_SLOTS
import llc.redstone.htslreborn.utils.PredicateUtils.ItemMatch.ItemExact
import llc.redstone.htslreborn.utils.PredicateUtils.ItemSelector
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameContains
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameExact
import llc.redstone.htslreborn.utils.TextUtils
import net.minecraft.world.item.Items
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField

object Exporter {
    val actions = mutableListOf<Action>()
    val conditions = mutableListOf<Condition>()
    val args = mutableMapOf<KParameter, Any?>()

    fun process() {
        ExportSession.begin()
        Queue.session = ExportSession
        Queue.addAll(buildOps(startIndex = 0))
    }

    fun buildOps(startIndex: Int): List<Operation> {
        val perPage = ACTION_SLOTS.size
        val startPage = startIndex / perPage
        return listOf(
            Operation.ResetExport(keep = startIndex),
            OpenMenu(NameContains("Actions"), checkIfOpened = true),
            Operation.Callback {
                MenuUtils.goToFirstPage()

                val queue = ArrayDeque<Operation>()
                queue.add(Operation.GotoPage(startPage))
                queue.addAll(handleActions(skip = startIndex % perPage, checkpointFrom = startIndex))
                var index = (startPage + 1) * perPage
                while (MenuUtils.nextPage()) {
                    queue.add(Operation.NextPage)
                    queue.addAll(handleActions(checkpointFrom = index))
                    index += perPage
                }
                MenuUtils.goToFirstPage()
                Queue.addAll(queue, 0)
                Status.Success
            },
        )
    }

    suspend fun handleActions(skip: Int = 0, checkpointFrom: Int? = null): ArrayDeque<Operation> {
        if (MenuUtils.findSlots(MenuItems.NO_ACTIONS).firstOrNull() != null) {
            return ArrayDeque()
        }

        val queue = ArrayDeque<Operation>()

        val slots = MenuUtils.actionSlotsOnPage().drop(skip)
        for ((offset, slot) in slots.withIndex()) {
            if (checkpointFrom != null) queue.add(Operation.ExportCheckpoint(checkpointFrom + offset))
            val itemProperties = slot.item.getProperties(true).toMutableList()
            val name = TextUtils.convertTextToString(slot.item.hoverName, false)

            var actionClass = Action::class.sealedSubclasses.firstOrNull {
                it.findAnnotations(ActionDefinition::class).any { ann -> ann.displayName == name }
            } ?: throw IllegalStateException("No action class found for action name: $name")

            if (itemProperties.firstOrNull() != null && itemProperties.first().first == "Holder") {
                val holderName = itemProperties.first().second
                actionClass = when (holderName) {
                    "Player" -> Action.PlayerVariable::class
                    "Global" -> Action.GlobalVariable::class
                    "Team" -> Action.TeamVariable::class
                    else -> throw IllegalStateException("Unknown holder type: $holderName")
                }
                itemProperties.removeAt(0)
            }

            val constructor = actionClass.primaryConstructor
                ?: throw IllegalStateException("No primary constructor found for action class: ${actionClass.simpleName}")
            val properties = constructor.parameters.mapNotNull { param ->
                val prop = actionClass.memberProperties.find { it.name == param.name } as? KProperty1<Action, *>
                prop?.let { it to param }
            }

            val shouldOpenActionSettings =
                properties.any {
                    it.first.returnType.classifier == ItemStack::class
                            || it.first.returnType.classifier == List::class
                } || itemProperties.any {
                    it.second == "0.0" || it.second.endsWith("...")
                }

            if (shouldOpenActionSettings) {
                queue.add(Operation.Click(slot.index))
                queue.add(OpenMenu(NameExact("Action Settings")))
            }

            for ((index, pair) in properties.withIndex()) {
                val (prop, param) = pair
                val colorValue = itemProperties.getOrNull(index)?.second
                val value = colorValue?.replace(Regex("&[0-9a-fk-or]"), "")

                if (prop.returnType.classifier == ItemStack::class) {
                    queue.add(Operation.ItemProperty(param, ACTION_SLOTS[index]))
                    queue.add(OpenMenu(NameContains("Action Settings"), checkIfOpened = true))
                } else if (value == "0.0" || value?.endsWith("...") == true) {
                    queue.add(Operation.LongProperty(param, prop, colorValue, ACTION_SLOTS[index]))
                } else if (prop.returnType.classifier == List::class) {
                    val field = prop.javaField?.genericType as? ParameterizedType
                        ?: error("Could not get parameterized type for List property ${prop.name}")
                    val listType = field.actualTypeArguments[0]
                    if (listType == Action::class.java) {
                        queue.add(Operation.Click(ACTION_SLOTS[index]))
                        queue.add(OpenMenu(NameContains("Actions")))
                        queue.add(Operation.ExportActions)
                    } else if (listType == Condition::class.java) {
                        queue.add(Operation.Click(ACTION_SLOTS[index]))
                        queue.add(OpenMenu(NameContains("Conditions")))
                        queue.add(Operation.ExportConditions)
                    }
                    queue.add(Operation.ClickItem(MenuItems.BACK))
                    queue.add(OpenMenu(NameContains("Action Settings"), checkIfOpened = true))
                } else {
                    queue.add(Operation.SimpleProperty(param, prop, colorValue ?: ""))
                }
            }

            if (shouldOpenActionSettings) {
                queue.add(Operation.ClickItem(MenuItems.BACK))
                queue.add(OpenMenu(NameContains("Actions")))
            }
            queue.add(Operation.CompileAction(actionClass))
        }
        return queue
    }

    suspend fun handleConditions(): ArrayDeque<Operation> {
        if (MenuUtils.findSlots(MenuItems.NO_CONDITIONS).firstOrNull() != null) {
            return ArrayDeque()
        }

        val queue = ArrayDeque<Operation>()

        val slots = MenuUtils.actionSlotsOnPage()
        for (slot in slots) {
            val itemProperties = slot.item.getProperties(true).toMutableList()
            val inverted = slot.item.loreLines(true).any { it.contains("Inverted", ignoreCase = true) }
            val name = TextUtils.convertTextToString(slot.item.hoverName, false)

            var conditionClass = Condition::class.sealedSubclasses.firstOrNull {
                it.findAnnotations(DisplayName::class).any { ann -> ann.value == name }
            } ?: throw IllegalStateException("No condition class found for action name: $name")

            if (itemProperties.firstOrNull() != null && itemProperties.first().first == "Holder") {
                val holderName = itemProperties.first().second
                conditionClass = when (holderName) {
                    "Player" -> Condition.PlayerVariableRequirement::class
                    "Global" -> Condition.GlobalVariableRequirement::class
                    "Team" -> Condition.TeamVariableRequirement::class
                    else -> throw IllegalStateException("Unknown holder type: $holderName")
                }
                itemProperties.removeAt(0)
            }

            val constructor = conditionClass.primaryConstructor
                ?: throw IllegalStateException("No primary constructor found for condition class: ${conditionClass.simpleName}")
            val properties = constructor.parameters.mapNotNull { param ->
                val prop = conditionClass.memberProperties.find { it.name == param.name } as? KProperty1<Action, *>
                prop?.let { it to param }
            }

            val shouldOpenConditionSettings =
                properties.any {
                    it.first.returnType.classifier == ItemStack::class
                } || itemProperties.any {
                    it.second == "0.0" || it.second.endsWith("...")
                }

            if (shouldOpenConditionSettings) {
                queue.add(Operation.Click(slot.index))
                queue.add(OpenMenu(NameExact("Settings")))
            }

            for ((index, pair) in properties.withIndex()) {
                val (prop, param) = pair
                val colorValue = itemProperties[index].second
                val value = colorValue.replace(Regex("&[0-9a-fk-or]"), "")
                val index = index + 1 // Adjust the index to account for the "Inverted" property at index 0


                if (prop.returnType.classifier == ItemStack::class) {
                    queue.add(Operation.ItemProperty(param, ACTION_SLOTS[index]))
                    queue.add(OpenMenu(NameContains("Settings"), checkIfOpened = true))
                } else if (value == "0.0" || value.endsWith("...")) {
                    queue.add(Operation.LongProperty(param, prop, colorValue, ACTION_SLOTS[index]))
                } else {
                    queue.add(Operation.SimpleProperty(param, prop, colorValue))
                }
            }

            if (shouldOpenConditionSettings) {
                queue.add(Operation.ClickItem(MenuItems.BACK))
                queue.add(OpenMenu(NameExact("Edit Conditions")))
            }
            queue.add(Operation.CompileCondition(conditionClass, inverted))
        }
        return queue
    }

    fun handleSimpleProperty(prop: KProperty1<Action, *>, colorValue: String): Any? {
        var value = colorValue.replace(Regex("&[0-9a-fk-or]"), "")

        if (value == "Not Set") {
            return null
        }

        value = when (prop.returnType.classifier) {
            Int::class, Long::class, Double::class -> value.replace(",", "")
            else -> value
        }

        val result = when (prop.returnType.classifier) {
            String::class -> colorValue
            Int::class -> value.toIntOrNull()
            Long::class -> value.toLongOrNull()
            Double::class -> value.toDoubleOrNull()
            Boolean::class -> value.equals("Enabled", ignoreCase = true)
            InventorySlot::class -> {
                InventorySlot.fromKey(value)
            }

            else -> null
        }

        if (result != null) {
            return result
        }

        if (prop.returnType.isSubtypeOf(Keyed::class.starProjectedType.withNullability(true))) {
            val companion = prop.returnType.classifier
                .let { it as? KClass<*> }
                ?.companionObjectInstance
                ?: error("No companion object for keyed enum: ${prop.returnType}")

            val getByKeyMethod = companion::class.members.find { it.name == "fromKey" }
                ?: error("No getByKey method for keyed enum: ${prop.returnType}")

            return getByKeyMethod.call(companion, value)
        }

        return null
    }

    suspend fun handleItemProperty(
        propertySlotIndex: Int
    ): ItemStack {
        val stack = MenuUtils.getSlot(propertySlotIndex).item

        MenuUtils.packetClick(propertySlotIndex)
        MenuUtils.onOpen(NameExact("Select an Item"))
            ?: error("Failed to open the item selection menu")

        val received = InputUtils.getItemFromMenu(null, stack) {
            MenuUtils.packetClick(13)
        }
        received.restore()

        MenuUtils.clickItems(MenuItems.BACK)
        MenuUtils.onOpen(null)

        return ItemStack(
            stack = received.stack,
            relativeFileLocation = "",
        )
    }

    suspend fun handleLongProperty(
        prop: KProperty1<Action, *>,
        colorValue: String,
        propertySlotIndex: Int
    ): Any? {
        var colorValue = colorValue

        when (prop.returnType.classifier) {
            Location::class -> {
                MenuUtils.getSlot(propertySlotIndex).item.getCurrentValue(false)?.let {
                    colorValue = it
                }
            }

            ItemStack::class -> {}
            else -> {
                colorValue = InputUtils.getPreviousInput {
                    MenuUtils.packetClick(propertySlotIndex)
                }.also {
                    MenuUtils.onOpen(null)
                }
            }
        }

        return handleSimpleProperty(prop, colorValue)
    }

    object MenuItems {
        val BACK = ItemSelector(
            name = NameExact("Go Back"),
            item = ItemExact(Items.ARROW)
        )
        val NO_ACTIONS = ItemSelector(
            name = NameExact("No Actions!"),
            item = ItemExact(Items.BEDROCK)
        )
        val NO_CONDITIONS = ItemSelector(
            name = NameExact("No Conditions!"),
            item = ItemExact(Items.BEDROCK)
        )
    }
}