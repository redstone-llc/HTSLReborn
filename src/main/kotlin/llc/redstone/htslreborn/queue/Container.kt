package llc.redstone.htslreborn.queue

import llc.redstone.htslreborn.data.ImportContext
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.queue.Operation.*
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameContains
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameExact

object Container {
    fun OperationBuilder.enterContext(container: ScriptContainer) {
        when (container.context) {
            ImportContext.FUNCTION -> {
                +Chat("/function edit ${container.target.name}")
                +OpenMenu(NameContains("Actions: "))
            }

            ImportContext.EVENT -> {
                +Chat("/eventactions")
                +OpenMenu(NameExact("Event Actions"))
                +Option(container.target.name ?: throw IllegalStateException("Event container has no target name"))
                +OpenMenu(NameContains("Edit Actions"))
            }

            ImportContext.COMMAND -> {
                +Chat("/customcommands edit ${container.target.name}")
                +OpenMenu(NameContains("Actions: "))
            }

            ImportContext.NPC -> {
                +Chat("/menu")
                +OpenMenu(NameExact("Housing Menu"))
                +Option("Systems")
                +OpenMenu(NameExact("Systems"))
                +Option("NPCs")
                +OpenMenu(NameExact("NPCs"))
                +Option(container.target.name ?: throw IllegalStateException("NPC container has no target name"))
                +OpenMenu(NameExact("Edit Actions"))
                if (container.target.trigger?.contains("Left Click") == true) {
                    +Click(11)
                } else if (container.target.trigger?.contains("Right Click") == true) {
                    +Click(12)
                }
                +OpenMenu(NameExact("Edit Actions"))
            }

            ImportContext.REGION -> {
                +Chat("/region edit ${container.target.name}")
                +OpenMenu(NameContains("Actions: "))
                +Option(container.target.trigger ?: throw IllegalStateException("Region container has no trigger"))
                +OpenMenu(NameContains("Edit Actions"))
            }

            ImportContext.CUSTOMMENU -> {
                +Chat("/custommenus edit ${container.target.name}")
                +OpenMenu(NameContains("Edit Menu"))
                +Click(15)
                +OpenMenu(NameContains("Edit Elements"))
                +Click(container.target.trigger?.toIntOrNull() ?: throw IllegalStateException("Custom menu container has no trigger"))
                +OpenMenu(NameContains("Edit Actions"))
            }

            else -> {}
        }
    }
}