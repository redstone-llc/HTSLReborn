package llc.redstone.htslreborn.data

import llc.redstone.htslreborn.data.enums.Events
import llc.redstone.htslreborn.utils.CommandUtils

data class ScriptContainer(
    var context: ImportContext,
    var target: ContextTarget = ContextTarget(),
    val actions: List<Action> = emptyList(),
)

enum class ImportContext {
    DEFAULT,
    FUNCTION,
    EVENT,
    COMMAND,
    REGION,
    NPC,
    CUSTOMMENU,
    ;

    fun translationKey(): String = "htslreborn.context.${name.lowercase()}"

    suspend fun getContexts(): List<String> {
        return when (this) {
            FUNCTION -> CommandUtils.getTabCompletions("function edit ")
            EVENT -> Events.entries.map { it.label }
            COMMAND -> CommandUtils.getTabCompletions("command edit ")
            REGION -> CommandUtils.getTabCompletions("region edit ")
            NPC -> emptyList() //TODO: Implement NPC context retrieval
            CUSTOMMENU -> CommandUtils.getTabCompletions("custommenu edit ")
            else -> emptyList()
        }
    }

    companion object {
        fun fromKeyword(keyword: String): ImportContext? = when (keyword.lowercase()) {
            "function" -> FUNCTION
            "event" -> EVENT
            "command" -> COMMAND
            "region" -> REGION
            "npc" -> NPC
            "gui", "menu", "custommenu" -> CUSTOMMENU
            else -> null
        }


    }
}

data class ContextTarget(
    val name: String? = null,
    val trigger: String? = null,
)
