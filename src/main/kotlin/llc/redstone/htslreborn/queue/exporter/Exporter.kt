package llc.redstone.htslreborn.queue.exporter

import llc.redstone.htslreborn.data.*
import llc.redstone.htslreborn.parser.ActionParser
import llc.redstone.htslreborn.parser.ActionParser.handleSwaps
import llc.redstone.htslreborn.parser.ConditionParser
import llc.redstone.htslreborn.queue.*
import llc.redstone.htslreborn.queue.Container.enterContext
import llc.redstone.htslreborn.queue.Operation.OpenMenu
import llc.redstone.htslreborn.ui.working.ContainerQueueEntry
import llc.redstone.htslreborn.utils.*
import llc.redstone.htslreborn.utils.ItemStackUtils.getCurrentValue
import llc.redstone.htslreborn.utils.ItemStackUtils.getProperties
import llc.redstone.htslreborn.utils.ItemStackUtils.loreLines
import llc.redstone.htslreborn.utils.MenuUtils.ACTION_SLOTS
import llc.redstone.htslreborn.utils.PredicateUtils.ItemMatch.ItemExact
import llc.redstone.htslreborn.utils.PredicateUtils.ItemSelector
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameContains
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameExact
import net.minecraft.world.item.Items
import java.lang.reflect.ParameterizedType
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField

object Exporter : BuildableContainer {
    val actions = mutableListOf<Action>()
    val conditions = mutableListOf<Condition>()
    val args = mutableMapOf<KParameter, Any?>()

    fun process(container: ScriptContainer, path: Path) {
        Queue.containers.add(ContainerQueueEntry(container = container, context = Exporter, source = path))
    }

    override fun build(container: ScriptContainer?, exportFrom: Int, path: Path?): List<Operation> {
        if (container == null) error("No container to diff")
        val builder = OperationBuilder()
        if (container.context == ImportContext.DEFAULT && !MenuUtils.isActionContainerOpen()) {
            ToastUtils.skippingClosedContainer(container.context)
            return emptyList()
        }

        builder.apply {
            enterContext(container)
            buildOps(startIndex = exportFrom)
            +Operation.Callback {
                if (path == null) error("No path provided for export")
                val lines = export(actions, path)
                path.parent?.let {
                    if (!it.exists()) {
                        it.createDirectories()
                    }
                }
                path.writeText(lines.joinToString("\n"))
                Status.Success
            }
        }

        return builder.ops
    }

    fun OperationBuilder.buildOps(startIndex: Int) {
        val perPage = ACTION_SLOTS.size
        val startPage = startIndex / perPage
        +Operation.ResetExport(keep = startIndex)
        +OpenMenu(NameContains("Actions"), checkIfOpened = true)
        +Operation.Callback {
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
        }
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
            ?: error(TextUtils.translate("htslreborn.error.item_menu"))

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

    private fun quoteIfNeeded(value: String): String {
        val escaped = value.replace("\"", "\\\"")
        return if (escaped.isEmpty() || escaped.any { it.isWhitespace() } || escaped == "null" || escaped.contains("\\\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    //This class is a little gross :)
    fun handleProperty(property: KProperty1<PropertyHolder, *>, value: Any?, path: Path): List<String> {
        val properties = mutableListOf<String>()

        if (value == null) {
            properties.add("null")
            return properties
        }

        when (property.returnType.classifier) {
            String::class -> {
                if (value == "Not Set") {
                    properties.add("null")
                    return properties
                }
                if (property.name == "amount" || property.name == "variable") {
                    properties.add(quoteIfNeeded(value as String))
                } else {
                    properties.add("\"${value as String}\"")
                }
            }

            Int::class, Double::class, Long::class, Boolean::class -> {
                properties.add(value.toString())
            }

            Operator::class -> {
                val operator = value as Operator
                when (operator) {
                    Operator.INCREMENT -> properties.add("+=")
                    Operator.DECREMENT -> properties.add("-=")
                    Operator.SET -> properties.add("=")
                    Operator.MULTIPLY -> properties.add("*=")
                    Operator.DIVIDE -> properties.add("/=")
                    Operator.BITWISE_AND -> properties.add("&=")
                    Operator.BITWISE_OR -> properties.add("|=")
                    Operator.BITWISE_XOR -> properties.add("^=")
                    Operator.LEFT_SHIFT -> properties.add("<<=")
                    Operator.LOGICAL_RIGHT_SHIFT -> properties.add(">>=")
                    Operator.ARITHMETIC_RIGHT_SHIFT -> properties.add(">>>=")
                    Operator.UNSET -> properties.add("unset")
                }
            }

            Location::class -> {
                val location = value as Location
                if (location !is Location.Custom) {
                    properties.add("\"${location.key}\"")
                } else {
                    properties.add("\"custom_coordinates\" \"$location\"")
                }
            }

            Comparator::class -> {
                val comparison = value as Comparator
                when (comparison) {
                    Comparator.EQUALS -> properties.add("==")
                    Comparator.GREATER_THAN -> properties.add(">")
                    Comparator.LESS_THAN -> properties.add("<")
                    Comparator.GREATER_THAN_OR_EQUAL -> properties.add(">=")
                    Comparator.LESS_THAN_OR_EQUAL -> properties.add("<=")
                    Comparator.NOT_EQUALS -> properties.add("!=")
                }
            }

            InventorySlot::class -> {
                val inventorySlot = value as InventorySlot
                properties.add("\"${inventorySlot.key}\"")
            }

            ItemStack::class -> {
                val itemStack = value as ItemStack
                val stack = itemStack.stack ?: error("ItemStack is null for property ${property.name}")
                val nbt = NbtHelper.serializeItemStack(stack).getOrNull() ?: return properties
                val nbtString = nbt.toString()
                val itemName = stack.hoverName.string.replace(" ", "_")
                if (path.parent.resolve("$itemName.nbt").exists()) {
                    properties.add("\"$itemName.nbt\"")
                } else {
                    path.parent.resolve("$itemName.nbt").writeText(nbtString)
                    properties.add("\"$itemName.nbt\"")
                }
            }

            else -> {
                if (property.returnType.isSubtypeOf(Keyed::class.starProjectedType.withNullability(true))) {
                    val keyed = value as Keyed
                    if (keyed::class.hasAnnotation<CustomKey>()) {
                        properties.add(keyed.toString())
                    } else if (keyed is KeyedLabeled) {
                        properties.add("\"${keyed.label}\"")
                    } else {
                        properties.add("\"${keyed.key}\"")
                    }
                } else {
                    properties.add(value.toString()) //More than likely null
                }
            }
        }

        return properties
    }

    fun export(actions: List<Action>, path: Path): List<String> {
        val lines = mutableListOf<String>()
        for (action in actions) {
            if (action is Action.Conditional) {
                val exportedConditions = exportConditions(action.conditions, path)
                lines.add("if${if (action.matchAnyCondition) " or" else ""} (${exportedConditions.joinToString(", ")}) {")
                val exportedActions = export(action.ifActions, path)
                lines.addAll(exportedActions.map { "    $it" })
                if (action.elseActions.isNotEmpty()) {
                    lines.add("} else {")
                    val exportedElseActions = export(action.elseActions, path)
                    lines.addAll(exportedElseActions.map { "    $it" })
                }
                lines.add("}")
                continue
            }

            if (action is Action.RandomAction) {
                lines.add("random {")
                val exportedActions = export(action.actions, path)
                lines.addAll(exportedActions.map { "    $it" })
                lines.add("}")
                continue
            }

            val actionClass = action::class
            val constructor = actionClass.primaryConstructor!!
            val parameters = constructor.parameters.toMutableList()

            handleSwaps(parameters, actionClass)

            val actionProperties = actionClass.memberProperties
            val newActionProperties = mutableListOf<KProperty1<Action, *>>()

            for (parm in parameters) {
                newActionProperties.add(actionProperties.find { it.name == parm.name } as KProperty1<Action, *>)
            }

            val keyword = ActionParser.keywords.entries.find { it.value == action::class }?.key ?: continue
            val properties = mutableListOf<String>()

            for (property in newActionProperties) {
                if (property.name == "actionName") continue
                val value = property.getter.call(action)
                // Add only the first string, because only conditionals and random actions should have lists
                properties.add(handleProperty(property as KProperty1<PropertyHolder, *>, value, path).first())
                if (value == Operator.UNSET) break
            }

            val line = if (properties.isNotEmpty()) {
                "$keyword ${properties.joinToString(" ")}"
            } else {
                keyword
            }
            lines.add(line)
        }
        return lines
    }

    fun exportConditions(conditions: List<Condition>, path: Path): List<String> {
        val conditionStrings = mutableListOf<String>()
        for (condition in conditions) {
            val conditionClass = condition::class
            val constructor = conditionClass.primaryConstructor!!
            val parameters = constructor.parameters.toMutableList()

            val conditionProperties = conditionClass.memberProperties
            val newConditionProperties = mutableListOf<KProperty1<Condition, *>>()

            for (parm in parameters) {
                newConditionProperties.add(conditionProperties.find { it.name == parm.name } as KProperty1<Condition, *>)
            }
            val keyword = ConditionParser.keywords.entries.find { it.value == condition::class }?.key ?: continue
            val properties = mutableListOf<String>()

            for (property in newConditionProperties) {
                if (property.name == "conditionName" || property.name == "inverted") continue
                val value = property.getter.call(condition)
                // Add only the first string, because only conditionals and random actions should have lists
                properties.add(handleProperty(property as KProperty1<PropertyHolder, *>, value, path).first())
            }

            val conditionString = "${if (condition.inverted) "!" else ""}${
                if (properties.isNotEmpty()) {
                    "$keyword ${properties.joinToString(" ")}"
                } else {
                    keyword
                }
            }"
            conditionStrings.add(conditionString)
        }
        return conditionStrings
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