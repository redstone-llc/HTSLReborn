package llc.redstone.htslreborn.queue.differ

import llc.redstone.htslreborn.HTSLReborn.JAVERS
import llc.redstone.htslreborn.data.*
import llc.redstone.htslreborn.queue.*
import llc.redstone.htslreborn.queue.exporter.Exporter
import llc.redstone.htslreborn.queue.importer.Importer
import llc.redstone.htslreborn.queue.importer.Importer.handleActions
import llc.redstone.htslreborn.queue.importer.Importer.handleConditions
import llc.redstone.htslreborn.queue.importer.Importer.handleProperty
import llc.redstone.htslreborn.ui.working.ContainerQueueEntry
import llc.redstone.htslreborn.utils.MenuUtils
import llc.redstone.htslreborn.utils.MenuUtils.ACTION_SLOTS
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameContains
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameExact
import llc.redstone.htslreborn.utils.ToastUtils
import net.minecraft.world.inventory.ContainerInput
import org.javers.core.diff.changetype.container.ElementValueChange
import org.javers.core.diff.changetype.container.ListChange
import org.javers.core.diff.changetype.container.ValueAdded
import org.javers.core.diff.changetype.container.ValueRemoved
import java.nio.file.Path
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

object Differ : BuildableContainer {
    fun process(containers: List<ScriptContainer>, path: Path, context: ImportContext? = null, target: ContextTarget? = null) {
        Queue.containers.addAll(containers.mapIndexed { index, container ->
            if (index == 0 && context != null && target != null && container.context == ImportContext.DEFAULT) {
                container.context = context
                container.target = target
            }
            ContainerQueueEntry(container, Differ, path)
        })
    }

    override fun build(container: ScriptContainer?, exportFrom: Int, path: Path?): List<Operation> {
        if (container == null) error("No container to diff")
        val ops = mutableListOf<Operation>()
        if (container.context == ImportContext.DEFAULT && !MenuUtils.isActionContainerOpen()) {
            ToastUtils.skippingClosedContainer(container.context)
            return ops
        }
        ops += buildContainer(container,  exportFrom)
        return ops
    }

    private fun buildContainer(container: ScriptContainer, exportFrom: Int): List<Operation> {
        val ops = mutableListOf<Operation>()
        ops += Operation.DiffPhase(DiffSession.Phase.EXPORT)
        ops += Exporter.build(container, exportFrom = exportFrom)
        ops += Operation.DiffPhase(DiffSession.Phase.EDIT)
        ops += Operation.Callback {
            Queue.addAll(handleActions(Exporter.actions, container.actions), 0)
            Status.Success
        }
        return ops
    }

    fun handleActions(oldActions: List<Action>, newActions: List<Action>): List<Operation> {
        val diff = JAVERS.compare(oldActions, newActions)
        val changes = diff.getChangesByType(ListChange::class.java).getOrNull(0)?.changes ?: return emptyList()

        var newIndex = oldActions.size - 1

        val builder = OperationBuilder()
        builder.apply {
            for (change in changes) {
                val index = change.index
                +Operation.OpenMenu(NameContains("Actions"), checkIfOpened = true)

                fun handleAdd(action: Action) {
                    newIndex += 1
                    if (index >= oldActions.size) {
                        handleActions(listOf(action))
                        +Operation.OpenMenu(NameContains("Actions"))
                    } else {
                        val (page, slot) = MenuUtils.getSlotAndPage(newIndex)
                        val (indexPage, indexSlot) = MenuUtils.getSlotAndPage(index)
                        +Operation.GotoPage(page)
                        handleActions(listOf(action))
                        +Operation.OpenMenu(NameContains("Actions"))
                        var counter = 0
                        var (currentPage, currentSlot) = MenuUtils.getSlotAndPage(newIndex)
                        while (currentSlot != indexSlot || currentPage != indexPage) {
                            counter += 1
                            +Operation.Click(ACTION_SLOTS[currentSlot], 0, ContainerInput.QUICK_MOVE)
                            +Operation.OpenMenu(NameContains("Actions"))
                            val (newPage, newSlot) = MenuUtils.getSlotAndPage(newIndex - counter)
                            currentPage = newPage
                            currentSlot = newSlot
                            +Operation.GotoPage(currentPage)
                        }
                    }
                }

                fun handleRemove() {
                    val (page, slot) = MenuUtils.getSlotAndPage(index)
                    newIndex -= 1
                    +Operation.GotoPage(page)
                    +Operation.Click(ACTION_SLOTS[slot], 1)
                    +Operation.OpenMenu(NameContains("Actions"))
                }

                when (change) {
                    is ValueAdded -> {
                        val action = change.addedValue as? Action ?: continue
                        handleAdd(action)
                    }

                    is ValueRemoved -> {
                        handleRemove()
                    }

                    is ElementValueChange -> {
                        val oldValue = change.leftValue as? Action ?: continue
                        val newValue = change.rightValue as? Action ?: continue

                        val (page, slot) = MenuUtils.getSlotAndPage(index)

                        +Operation.GotoPage(page)
                        if (oldValue::class != newValue::class) {
//                            if (oldValue is Action.ChangeVariable && newValue is Action.ChangeVariable) {
//                                continue
//                            } //TODO: Handle ChangeVariable type change
                            handleRemove()
                            handleAdd(newValue)
                        } else {
                            updateAction(ACTION_SLOTS[slot], oldValue, newValue)

                        }
                    }
                }
            }
        }
        return builder.ops
    }

    fun handleConditions(oldActions: List<Condition>, newActions: List<Condition>): List<Operation> {
        val diff = JAVERS.compare(oldActions, newActions)
        val changes = diff.getChangesByType(ListChange::class.java).getOrNull(0)?.changes ?: return emptyList()

        var newIndex = oldActions.size - 1

        val builder = OperationBuilder()
        builder.apply {
            for (change in changes) {
                val index = change.index
                +Operation.OpenMenu(NameExact("Edit Conditions"), checkIfOpened = true)
                when (change) {
                    is ValueAdded -> {
                        val action = change.addedValue as? Condition ?: continue
                        newIndex += 1
                        if (index >= oldActions.size) {
                            handleConditions(listOf(action))
                        } else {
                            val (page, slot) = MenuUtils.getSlotAndPage(newIndex)
                            val (indexPage, indexSlot) = MenuUtils.getSlotAndPage(index)
                            +Operation.GotoPage(page)
                            handleConditions(listOf(action))
                            var counter = 1
                            var (currentPage, currentSlot) = MenuUtils.getSlotAndPage(newIndex - counter)
                            while (currentSlot != indexSlot || currentPage != indexPage) {
                                counter += 1
                                +Operation.Click(ACTION_SLOTS[currentSlot], 0, ContainerInput.QUICK_MOVE)
                                +Operation.OpenMenu(NameExact("Edit Conditions"))
                                val (newPage, newSlot) = MenuUtils.getSlotAndPage(oldActions.size - counter)
                                currentPage = newPage
                                currentSlot = newSlot
                                +Operation.GotoPage(currentPage)
                            }
                        }
                    }

                    is ValueRemoved -> {
                        val (page, slot) = MenuUtils.getSlotAndPage(index)
                        newIndex -= 1
                        +Operation.GotoPage(page)
                        +Operation.Click(ACTION_SLOTS[slot], 1)
                        +Operation.OpenMenu(NameContains("Edit Conditions"))
                    }

                    is ElementValueChange -> {
                        val oldValue = change.leftValue as? Condition ?: continue
                        val newValue = change.rightValue as? Condition ?: continue

                        val (page, slot) = MenuUtils.getSlotAndPage(index)

                        +Operation.GotoPage(page)
                        if (oldValue::class != newValue::class) {
                            println("I am pretty certain this cannot happen")
                        } else {
                            updateCondition(ACTION_SLOTS[slot], oldValue, newValue)
                        }
                    }
                }
            }
        }
        return builder.ops
    }

    fun OperationBuilder.updateCondition(slotId: Int, oldValue: Condition, newValue: Condition) {
        val parameters = oldValue::class.primaryConstructor!!.parameters.toMutableList()
        val conditionProperties = oldValue.javaClass.kotlin.memberProperties

        if (parameters.isEmpty()) return

        +Operation.Click(slotId)

        val properties = mutableListOf<KProperty1<Condition, *>>()
        for (parm in parameters) {
            properties.add(conditionProperties.find { it.name == parm.name } ?: continue)
        }

        for ((index, property) in properties.withIndex()) {
            val oldPropValue = property.get(oldValue)
            val newPropValue = property.get(newValue)
            if (oldPropValue != newPropValue) {
                +Operation.OpenMenu(NameExact("Settings"), checkIfOpened = true)
                val slot = ACTION_SLOTS[index]
                handleProperty(property, newPropValue, oldPropValue, slot, 0)
                +Operation.OpenMenu(NameExact("Settings"))
            }
        }
        +Operation.OpenMenu(NameExact("Settings"), checkIfOpened = true)
        +Operation.ClickItem(Importer.MenuItems.BACK)

    }

    fun OperationBuilder.updateAction(slotId: Int, oldValue: Action, newValue: Action) {
        val parameters = oldValue::class.primaryConstructor!!.parameters.toMutableList()
        val actionProperties = oldValue.javaClass.kotlin.memberProperties

        if (parameters.isEmpty()) return

        +Operation.Click(slotId)
        if (oldValue is Action.Conditional && newValue is Action.Conditional) {
            handleConditional(oldValue, newValue)
            return
        }

        if (oldValue is Action.RandomAction && newValue is Action.RandomAction) {
            val actionsDiff = JAVERS.compare(oldValue.actions, newValue.actions)
            if (actionsDiff.changes.isNotEmpty()) {
                +Operation.OpenMenu(NameExact("Action Settings"), checkIfOpened = true)
                +Operation.Click(10)
                +Operation.OpenMenu(NameExact("Edit Actions"), checkIfOpened = true)
                handleActions(oldValue.actions, newValue.actions).forEach { +it }
                +Operation.OpenMenu(NameExact("Edit Actions"), checkIfOpened = true)
                +Operation.ClickItem(Importer.MenuItems.BACK)
            }
            return
        }

        val properties = mutableListOf<KProperty1<Action, *>>()
        for (parm in parameters) {
            properties.add(actionProperties.find { it.name == parm.name } ?: continue)
        }

        if (newValue is Action.ChangeVariable) {
            properties.add(0, actionProperties.find { it.name == "holder" } ?: return)
        }

        for ((index, property) in properties.withIndex()) {
            val oldPropValue = property.get(oldValue)
            val newPropValue = property.get(newValue)
            if (oldPropValue != newPropValue) {
                +Operation.OpenMenu(NameExact("Action Settings"), checkIfOpened = true)
                val slot = ACTION_SLOTS[index]
                handleProperty(property, newPropValue, oldPropValue, slot, 0)
                +Operation.OpenMenu(NameExact("Action Settings"))

            }
        }
        +Operation.OpenMenu(NameExact("Action Settings"), checkIfOpened = true)
        +Operation.ClickItem(Importer.MenuItems.BACK)
    }

    fun OperationBuilder.handleConditional(oldConditional: Action.Conditional, newConditional: Action.Conditional) {
        val condDiff = JAVERS.compare(oldConditional.conditions, newConditional.conditions)
        if (condDiff.changes.isNotEmpty()) {
            +Operation.OpenMenu(NameExact("Action Settings"), checkIfOpened = true)
            +Operation.Click(10)
            +Operation.OpenMenu(NameExact("Edit Conditions"))
            handleConditions(oldConditional.conditions, newConditional.conditions).forEach { +it }
            +Operation.OpenMenu(NameExact("Edit Conditions"), checkIfOpened = true)
            +Operation.ClickItem(Importer.MenuItems.BACK)
        }
        if (oldConditional.matchAnyCondition != newConditional.matchAnyCondition) {
            +Operation.OpenMenu(NameExact("Action Settings"), checkIfOpened = true)
            +Operation.Click(11)
            +Operation.OpenMenu(NameExact("Action Settings"))
        }

        val ifDiff = JAVERS.compare(oldConditional.ifActions, newConditional.ifActions)
        if (ifDiff.changes.isNotEmpty()) {
            +Operation.OpenMenu(NameExact("Action Settings"), checkIfOpened = true)
            +Operation.Click(12)
            +Operation.OpenMenu(NameExact("Edit Actions"))
            handleActions(oldConditional.ifActions, newConditional.ifActions).forEach { +it }
            +Operation.OpenMenu(NameExact("Edit Actions"), checkIfOpened = true)
            +Operation.ClickItem(Importer.MenuItems.BACK)
        }

        val elseDif = JAVERS.compare(oldConditional.elseActions, newConditional.elseActions)
        if (elseDif.changes.isNotEmpty()) {
            +Operation.OpenMenu(NameExact("Action Settings"), checkIfOpened = true)
            +Operation.Click(13)
            +Operation.OpenMenu(NameExact("Edit Actions"))
            handleActions(oldConditional.elseActions, newConditional.elseActions).forEach { +it }
            +Operation.OpenMenu(NameExact("Edit Actions"), checkIfOpened = true)
            +Operation.ClickItem(Importer.MenuItems.BACK)
        }

        +Operation.OpenMenu(NameExact("Action Settings"), checkIfOpened = true)
        +Operation.ClickItem(Importer.MenuItems.BACK)
    }
}