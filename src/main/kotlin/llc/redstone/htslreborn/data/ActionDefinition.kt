package llc.redstone.htslreborn.data

import llc.redstone.htslreborn.data.enums.Events

@Target(AnnotationTarget.CLASS)
annotation class ActionDefinition(
    val displayName: String,
    val defaultLimit: Int,
    val scopes: Array<Scope> = []
)

@Repeatable
annotation class Scope(
    val scope: ScopeType,
    val limit: Int = -1,
    val events: Array<Events> = []
)

enum class ScopeType {
    FUNCTION,
    REGION,
    COMMAND,
    MENU,
    ITEM,
    CONDITIONAL,
    EVENT,
    RANDOM
}