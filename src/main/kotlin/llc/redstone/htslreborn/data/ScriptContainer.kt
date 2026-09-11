package llc.redstone.htslreborn.data

data class ScriptContainer(
    val context: ImportContext,
    val target: ContextTarget = ContextTarget(),
    val actions: List<Action> = emptyList(),
)

enum class ImportContext {
    DEFAULT,
    FUNCTION,
    EVENT,
    COMMAND,
    REGION,
    NPC,
    BUTTON,
    PAD,
    CUSTOMMENU,
    ;

    companion object {
        fun fromKeyword(keyword: String): ImportContext? = when (keyword.lowercase()) {
            "function" -> FUNCTION
            "event" -> EVENT
            "command" -> COMMAND
            "region" -> REGION
            "npc" -> NPC
            "button" -> BUTTON
            "pad" -> PAD
            "gui", "custommenu" -> CUSTOMMENU
            else -> null
        }
    }
}

data class ContextTarget(
    val name: String? = null,
    val trigger: String? = null,
)
