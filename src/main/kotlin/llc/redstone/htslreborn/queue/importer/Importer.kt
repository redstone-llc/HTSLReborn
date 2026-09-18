package llc.redstone.htslreborn.queue.importer

import llc.redstone.htslreborn.data.*
import llc.redstone.htslreborn.data.enums.Sound
import llc.redstone.htslreborn.queue.BuildableContainer
import llc.redstone.htslreborn.queue.Container.enterContext
import llc.redstone.htslreborn.queue.Operation
import llc.redstone.htslreborn.queue.Operation.*
import llc.redstone.htslreborn.queue.OperationBuilder
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.ui.working.ContainerQueueEntry
import llc.redstone.htslreborn.utils.MenuUtils
import llc.redstone.htslreborn.utils.MenuUtils.ACTION_SLOTS
import llc.redstone.htslreborn.utils.PredicateUtils
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameContains
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameExact
import llc.redstone.htslreborn.utils.PropertyReflection
import llc.redstone.htslreborn.utils.ToastUtils
import net.minecraft.world.item.Items
import java.nio.file.Path
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

object Importer: BuildableContainer {
    fun process(containers: List<ScriptContainer>, path: Path) {
        Queue.containers.addAll(containers.map { ContainerQueueEntry(it, Importer, path) })
    }

    override fun build(container: ScriptContainer?, exportFrom: Int, path: Path?): List<Operation> {
        if (container == null) return emptyList()
        val builder = OperationBuilder()
        if (container.context == ImportContext.DEFAULT && !MenuUtils.isActionContainerOpen()) {
            ToastUtils.send("§cSkipping ${container.context.name}", "§7No action container is open.")
            return emptyList()
        }
        builder.apply {
            enterContext(container)
            +CountActions
            handleActions(container.actions)
        }
        return builder.ops
    }

    fun buildResume(container: ScriptContainer, checkpoint: Checkpoint, baseCount: Int): List<Operation> {
        if (container.context == ImportContext.DEFAULT && !MenuUtils.isActionContainerOpen()) {
            error("Open the action container you were importing into first")
        }
        val all = build(container)
        val start = all.indexOf(checkpoint)
        if (start < 0) error("Checkpoint $checkpoint not found in regenerated queue")

        val path = checkpoint.path
        val preamble = OperationBuilder().apply {
            enterContext(container)
            +OpenMenu(NameContains("Actions"), checkIfOpened = true)
            for (i in 0 until path.size - 1 step 2) {
                val action = path[i]
                val property = path[i + 1]
                +GotoPage(action / ACTION_SLOTS.size)
                +OpenMenu(NameExact("Action Settings"), slot = ACTION_SLOTS[action % ACTION_SLOTS.size])
                +Click(ACTION_SLOTS[property])
                +OpenMenu(NameExact("Edit Actions"))
            }
            val base = if (path.size == 1) baseCount else 0
            +TrimActions(base + path.last())
        }.ops

        return preamble + all.drop(start)
    }

    fun OperationBuilder.handleActions(actions: List<Action>) {
        for ((actionIndex, action) in actions.withIndex()) {
            val displayName =
                (action::class.annotations.find { it is ActionDefinition } as ActionDefinition).displayName
            val defaultInstance = PropertyReflection.defaultInstance(action::class) as? Action
                ?: throw IllegalStateException("No default instance found for ${action::class.simpleName}")
            val properties = PropertyReflection.propertiesOf(action)

            path += actionIndex
            +Checkpoint(path.toList())

            //Start in the action container, if not already there
            +OpenMenu(NameContains("Actions"), checkIfOpened = true)
            +OpenMenu(NameExact("Add Action"), slot = 50)
            +Option(displayName)

            if (action is Action.ChangeVariable) {
                +OpenMenu(NameExact("Action Settings"))
                if (action.holder == VariableHolder.Global) {
                    +Click(ACTION_SLOTS[0])
                    +OpenMenu(NameExact("Action Settings"))
                }
                if (action.holder == VariableHolder.Team) {
                    +Click(ACTION_SLOTS[0])
                    +OpenMenu(NameExact("Action Settings"))
                    +Click(ACTION_SLOTS[0])
                    +OpenMenu(NameExact("Action Settings"))
                }
            }

            for ((index, property) in properties.withIndex()) {
                val value = property.get(action)
                val defaultValue = property.get(defaultInstance)
                if (value == defaultValue) continue
                val slot = ACTION_SLOTS[index]

                +OpenMenu(NameExact("Action Settings"), checkIfOpened = true)

                handleProperty(property, value, defaultValue, slot, index)
                +OpenMenu(NameExact("Action Settings"))
            }

            if (properties.isNotEmpty()) {
                +OpenMenu(NameExact("Action Settings"), checkIfOpened = true)
                +ClickItem(MenuItems.BACK)
            }
            path.removeLast()
        }
    }

    fun OperationBuilder.handleConditions(conditions: List<Condition>) {
        for (condition in conditions) {
            val displayName = (condition::class.annotations.find { it is DisplayName } as DisplayName).value
            val defaultInstance = PropertyReflection.defaultInstance(condition::class) as? Condition
                ?: throw IllegalStateException("No default instance found for ${condition::class.simpleName}")
            val properties = PropertyReflection.propertiesOf(condition)

            //Start in the condition container, if not already there
            +OpenMenu(NameContains("Edit Conditions"), checkIfOpened = true)
            +OpenMenu(NameExact("Add Condition"), slot = 50)
            +Option(displayName)

            for ((index, property) in properties.withIndex()) {
                val value = property.get(condition)
                val defaultValue = property.get(defaultInstance)
                if (value == defaultValue) continue
                val slot = ACTION_SLOTS[index] ?: continue

                +OpenMenu(NameExact("Settings"), checkIfOpened = true)

                handleProperty(property, value, defaultValue, slot, index)
                +OpenMenu(NameExact("Settings"))
            }

            if (properties.isNotEmpty()) {
                +OpenMenu(NameExact("Settings"), checkIfOpened = true)
                +ClickItem(MenuItems.BACK)
            }
        }
    }

    fun OperationBuilder.handleProperty(
        property: KProperty1<*, *>,
        value: Any?,
        defaultValue: Any?,
        slot: Int,
        index: Int
    ) {

        when (property.returnType.classifier) {
            String::class -> {
                if (property.annotations.find { it is Pagination } != null) {
                    +Click(slot)
                    +OpenMenu(NameExact("Select Option"))
                    +Option(value as String)
                    return
                }
                val value = value as String
                +Click(slot)
                +Input(value)
            }

            Int::class, Double::class -> {
                val value = value
                +Click(slot)
                +Input(value.toString())
            }

            List::class -> {
                val value = value as List<*>
                if (value.isEmpty()) return
                //if the first entry is an action then we assume they all are actions
                if (value.first() is Action) {
                    val actions = value.filterIsInstance<Action>()
                    if (actions.size != value.size) error("List contains non-action entries")
                    +Click(slot)
                    path += index
                    handleActions(actions)
                    path.removeLast()
                    +OpenMenu(NameExact("Edit Actions"), checkIfOpened = true)
                    +ClickItem(MenuItems.BACK)
                } else if (value.first() is Condition) {
                    val conditions = value.filterIsInstance<Condition>()
                    if (conditions.size != value.size) error("List contains non-condition entries")
                    +Click(slot)
                    handleConditions(conditions)
                    +OpenMenu(NameExact("Edit Conditions"), checkIfOpened = true)
                    +ClickItem(MenuItems.BACK)
                }
            }

            ItemStack::class -> {
                val value = value as ItemStack
                +Click(slot)
                +OpenMenu(NameExact("Select an Item"))
                +Item(value.stack, value.slot)
            }

            Boolean::class -> {
                +Click(slot) //Essentially because we know they don't equal each other we just click it :)
            }

            InventorySlot::class -> {
                val value = value as InventorySlot
                +Click(slot)
                +OpenMenu(NameExact("Select Inventory Slot"))
                +Option(value.key)

                if (value::class.annotations.find { it is CustomKey } != null) {
                    +Input(value.toString())
                }
            }

            Sound::class -> {
                val value = value as Sound
                +Click(slot)
                +OpenMenu(NameExact("Select Option"))
                +Click(48)
                +Input(value.key)
            }

            else -> {
                if (property.returnType.isSubtypeOf(Keyed::class.starProjectedType.withNullability(true))) {
                    val keyed = value as Keyed

                    if (keyed is KeyedCycle) {
                        val defaultOrdinal = (defaultValue as? KeyedCycle)?.getOrdinal() ?: -1
                        val currentOrdinal = keyed.getOrdinal()
                        val difference = currentOrdinal - defaultOrdinal

                        repeat(difference) {
                            +Click(slot)
                        }
                        return
                    }

                    +Click(slot)
                    +OpenMenu(NameExact("Select Option"))
                    if (keyed is KeyedLabeled) {
                        +Option(keyed.label)
                    } else {
                        +Option(keyed.key)
                    }

                    if (keyed::class.annotations.find { it is CustomKey } != null) {
                        +Input(keyed.toString())
                    }
                }
            }
        }
    }

    object MenuItems {
        val BACK = PredicateUtils.ItemSelector(
            NameExact("Go Back"),
            PredicateUtils.ItemMatch.ItemExact(Items.ARROW)
        )
    }
}