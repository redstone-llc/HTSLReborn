package llc.redstone.htslreborn.importer

import llc.redstone.htslreborn.data.*
import llc.redstone.htslreborn.data.enums.Sound
import llc.redstone.htslreborn.importer.Operation.*
import llc.redstone.htslreborn.utils.MenuUtils
import llc.redstone.htslreborn.utils.PredicateUtils
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameContains
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameExact
import llc.redstone.htslreborn.utils.PropertyReflection
import llc.redstone.htslreborn.utils.ToastUtils
import net.minecraft.world.item.Items
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

object ParserToQueue {
    private val slots = mutableMapOf(
        0 to 10,
        1 to 11,
        2 to 12,
        3 to 13,
        4 to 14,
        5 to 15,
        6 to 16,
        7 to 19,
        8 to 20,
        9 to 21,
        10 to 22,
        11 to 23,
        12 to 24,
        13 to 25,
        14 to 28,
        15 to 29,
        16 to 30,
        17 to 31,
        18 to 32,
        19 to 33,
        20 to 34,
    )

    fun process(containers: List<ScriptContainer>) {
        for (container in containers) {
            if (container.context == ImportContext.DEFAULT && !MenuUtils.isActionContainerOpen()) {
                ToastUtils.send("§cSkipping ${container.context.name}", "§7No action container is open.")
                continue
            }
            Queue.enqueue {
                when (container.context) {
                    ImportContext.FUNCTION -> {
                        +Chat("/function edit ${container.target.name}")
                        +OpenMenu(NameContains("Actions: "))
                    }

                    else -> {}
                }

                handleActions(container.actions)
            }
        }
    }

    fun OperationBuilder.handleActions(actions: List<Action>) {
        for (action in actions) {
            val displayName =
                (action::class.annotations.find { it is ActionDefinition } as ActionDefinition).displayName
            val defaultInstance = PropertyReflection.defaultInstance(action::class) as? Action
                ?: throw IllegalStateException("No default instance found for ${action::class.simpleName}")
            val properties = PropertyReflection.propertiesOf(action)

            //Start in the action container, if not already there
            +OpenMenu(NameContains("Actions"))
            +OpenMenu(NameExact("Add Action"), slot = 50)
            +Option(displayName)

            if (action is Action.ChangeVariable) {
                +OpenMenu(NameExact("Action Settings"))
                if (action.holder == VariableHolder.Global) {
                    +Click(slots[0] ?: 0)
                    +OpenMenu(NameExact("Action Settings"), checkIfOpened = false)
                }
                if (action.holder == VariableHolder.Team) {
                    +Click(slots[0] ?: 0)
                    +OpenMenu(NameExact("Action Settings"), checkIfOpened = false)
                    +Click(slots[0] ?: 0)
                    +OpenMenu(NameExact("Action Settings"), checkIfOpened = false)
                }
            }

            for ((index, property) in properties.withIndex()) {
                val value = property.get(action)
                val defaultValue = property.get(defaultInstance)
                if (value == defaultValue) continue
                val slot = slots[index] ?: continue

                +OpenMenu(NameExact("Action Settings"))

                handleProperty(property, value, defaultValue, slot)
            }

            if (properties.isNotEmpty()) {
                +OpenMenu(NameExact("Action Settings"), checkIfOpened = false)
                +ClickItem(MenuItems.BACK)
            }
        }
    }

    fun OperationBuilder.handleConditions(conditions: List<Condition>) {
        for (condition in conditions) {
            val displayName = (condition::class.annotations.find { it is DisplayName } as DisplayName).value
            val defaultInstance = PropertyReflection.defaultInstance(condition::class) as? Condition
                ?: throw IllegalStateException("No default instance found for ${condition::class.simpleName}")
            val properties = PropertyReflection.propertiesOf(condition)

            //Start in the condition container, if not already there
            +OpenMenu(NameContains("Edit Conditions"))
            +OpenMenu(NameExact("Add Condition"), slot = 50)
            +Option(displayName)

            for ((index, property) in properties.withIndex()) {
                val value = property.get(condition)
                val defaultValue = property.get(defaultInstance)
                if (value == defaultValue) continue
                val slot = slots[index] ?: continue

                +OpenMenu(NameExact("Settings"))

                handleProperty(property, value, defaultValue, slot)
            }

            if (properties.isNotEmpty()) {
                +OpenMenu(NameExact("Settings"), checkIfOpened = false)
                +ClickItem(MenuItems.BACK)
            }
        }
    }

    fun OperationBuilder.handleProperty(property: KProperty1<*, *>, value: Any?, defaultValue: Any?, slot: Int) {

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
                    handleActions(actions)
                    +OpenMenu(NameExact("Edit Actions"))
                    +ClickItem(MenuItems.BACK)
                } else if (value.first() is Condition) {
                    val conditions = value.filterIsInstance<Condition>()
                    if (conditions.size != value.size) error("List contains non-condition entries")
                    +Click(slot)
                    handleConditions(conditions)
                    +OpenMenu(NameExact("Edit Conditions"))
                    +ClickItem(MenuItems.BACK)
                    +OpenMenu(NameExact("Action Settings"))
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