@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package llc.redstone.htslreborn.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import llc.redstone.htslreborn.data.ScopeType.*
import llc.redstone.htslreborn.data.enums.*

sealed class Action(
    @Transient private val actionName: String = ""
) : PropertyHolder {
    @ActionDefinition(
        displayName = "Conditional",
        defaultLimit = 25,
        scopes = [
            Scope(FUNCTION),
            Scope(MENU),
            Scope(
                EVENT, 40, [
                    Events.PLAYER_JOIN, Events.PLAYER_QUIT, Events.PLAYER_DEATH, Events.PLAYER_KILL,
                    Events.PLAYER_RESPAWN, Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH,
                    Events.PLAYER_ENTER_PORTAL, Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR,
                    Events.COMPLETE_PARKOUR, Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM,
                    Events.PLAYER_TOGGLE_SNEAK, Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class Conditional(
        val conditions: List<Condition> = emptyList(),
        val matchAnyCondition: Boolean = false,
        val ifActions: List<Action> = emptyList(),
        val elseActions: List<Action> = emptyList(),
    ) : Action("CONDITIONAL")

    @ActionDefinition(
        displayName = "Cancel Event",
        defaultLimit = 1,
        scopes = [
            Scope(
                EVENT, events = [
                    Events.PLAYER_DEATH, Events.FISH_CATCH, Events.PLAYER_DAMAGE, Events.PLAYER_DROP_ITEM,
                    Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    class CancelEvent : Action("CANCEL_EVENT")

    @ActionDefinition(
        displayName = "Change Player's Group",
        defaultLimit = 1,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL, Events.PLAYER_DAMAGE,
                    Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR, Events.PLAYER_DROP_ITEM,
                    Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK, Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class ChangePlayerGroup(
        @property:Pagination val newGroup: String? = null,
        val includeHigherGroups: Boolean = true,
    ) : Action("CHANGE_PLAYER_GROUP")

    @ActionDefinition(
        displayName = "Kill Player",
        defaultLimit = 1,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM)
        ]
    )
    class KillPlayer : Action("KILL")

    @ActionDefinition(
        displayName = "Full Heal",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    class FullHeal : Action("FULL_HEAL")

    @ActionDefinition(
        displayName = "Display Title",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class DisplayTitle(
        val title: String = "Hello World!",
        val subtitle: String = "",
        val fadeIn: Int = 1,
        val stay: Int = 5,
        val fadeOut: Int = 1,
    ) : Action("TITLE")

    @ActionDefinition(
        displayName = "Display Action Bar",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class DisplayActionBar(
        val message: String = "Hello World!"
    ) : Action("ACTION_BAR")

    @ActionDefinition(
        displayName = "Reset Inventory",
        defaultLimit = 1,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    class ResetInventory : Action("RESET_INVENTORY")

    @ActionDefinition(
        displayName = "Change Max Health",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class ChangeMaxHealth(
        val amount: String = "20",
        val op: Operator = Operator.SET,
        val healOnChange: Boolean = true,
    ) : Action("CHANGE_MAX_HEALTH")

    @ActionDefinition(
        displayName = "Parkour Checkpoint",
        defaultLimit = 1,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    class ParkourCheckpoint : Action("PARKOUR_CHECKPOINT")

    @ActionDefinition(
        displayName = "Give Item",
        defaultLimit = 40,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class GiveItem(
        val item: ItemStack? = null,
        val allowMultiple: Boolean = false,
        val inventorySlot: InventorySlot? = null,
        val replaceExistingItem: Boolean = false,
    ) : Action("GIVE_ITEM")

    @ActionDefinition(
        displayName = "Remove Item",
        defaultLimit = 40,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class RemoveItem(
        val item: ItemStack? = null,
    ) : Action("REMOVE_ITEM")

    @ActionDefinition(
        displayName = "Send a Chat Message",
        defaultLimit = 20,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class SendMessage(
        val message: String = "Hello!",
    ) : Action("SEND_MESSAGE")

    @ActionDefinition(
        displayName = "Apply Potion Effect",
        defaultLimit = 22,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class ApplyPotionEffect(
        val effect: PotionEffect? = null,
        val duration: Int = 60,
        val level: Int = 1,
        @SerialName("override_existing_effects")
        val override: Boolean = false,
        @SerialName("show_potion_icon")
        val showIcon: Boolean = true,
    ) : Action("POTION_EFFECT")

    @ActionDefinition(
        displayName = "Clear All Potion Effects",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    class ClearAllPotionEffects : Action("CLEAR_EFFECTS")

    @ActionDefinition(
        displayName = "Give Experience Levels",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class GiveExperienceLevels(
        val levels: Int = 1,
    ) : Action("GIVE_EXP_LEVELS")

    @ActionDefinition(
        displayName = "Send to Lobby",
        defaultLimit = 1,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM)
        ]
    )
    data class SendToLobby(
        val location: Lobby? = null,
    ) : Action("SEND_TO_LOBBY")

    @ActionDefinition(
        displayName = "Change Variable",
        defaultLimit = 25,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_QUIT, Events.PLAYER_DEATH, Events.PLAYER_KILL,
                    Events.PLAYER_RESPAWN, Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH,
                    Events.PLAYER_ENTER_PORTAL, Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR,
                    Events.COMPLETE_PARKOUR, Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM,
                    Events.PLAYER_TOGGLE_SNEAK, Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    sealed class ChangeVariable(
        val holder: VariableHolder
    ) : Action("CHANGE_VARIABLE")

    @ActionDefinition(
        displayName = "Change Variable",
        defaultLimit = 25,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_QUIT, Events.PLAYER_DEATH, Events.PLAYER_KILL,
                    Events.PLAYER_RESPAWN, Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH,
                    Events.PLAYER_ENTER_PORTAL, Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR,
                    Events.COMPLETE_PARKOUR, Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM,
                    Events.PLAYER_TOGGLE_SNEAK, Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class PlayerVariable(
        val variable: String = "Kills",
        val op: Operator = Operator.INCREMENT,
        val amount: String = "1",
        val unset: Boolean = false
    ) : ChangeVariable(VariableHolder.Player)

    @ActionDefinition(
        displayName = "Change Variable",
        defaultLimit = 25,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_QUIT, Events.PLAYER_DEATH, Events.PLAYER_KILL,
                    Events.PLAYER_RESPAWN, Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH,
                    Events.PLAYER_ENTER_PORTAL, Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR,
                    Events.COMPLETE_PARKOUR, Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM,
                    Events.PLAYER_TOGGLE_SNEAK, Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class TeamVariable(
        @property:Pagination val teamName: String? = null,
        val variable: String = "Kills",
        val op: Operator = Operator.INCREMENT,
        val amount: String = "1",
        val unset: Boolean = false
    ) : ChangeVariable(VariableHolder.Team)

    @ActionDefinition(
        displayName = "Change Variable",
        defaultLimit = 25,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_QUIT, Events.PLAYER_DEATH, Events.PLAYER_KILL,
                    Events.PLAYER_RESPAWN, Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH,
                    Events.PLAYER_ENTER_PORTAL, Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR,
                    Events.COMPLETE_PARKOUR, Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM,
                    Events.PLAYER_TOGGLE_SNEAK, Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class GlobalVariable(
        val variable: String = "Kills",
        val op: Operator = Operator.INCREMENT,
        val amount: String = "1",
        val unset: Boolean = false
    ) : ChangeVariable(VariableHolder.Global)

    @ActionDefinition(
        displayName = "Teleport Player",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class TeleportPlayer(
        val location: Location? = null,
        val preventInsideBlocks: Boolean = false,
    ) : Action("TELEPORT_PLAYER")

    @ActionDefinition(
        displayName = "Fail Parkour",
        defaultLimit = 1,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class FailParkour(
        val reason: String = "Failed!"
    ) : Action("FAIL_PARKOUR")

    @ActionDefinition(
        displayName = "Play Sound",
        defaultLimit = 25,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class PlaySound(
        val sound: Sound? = null,
        val volume: Double = 0.7,
        val pitch: Double = 1.0,
        val location: Location? = null,
    ) : Action("PLAY_SOUND")

    @ActionDefinition(
        displayName = "Set Compass Target",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class SetCompassTarget(
        val location: Location? = null
    ) : Action("SET_COMPASS_TARGET")

    @ActionDefinition(
        displayName = "Set Gamemode",
        defaultLimit = 1,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class SetGameMode(
        val gamemode: GameMode? = null
    ) : Action("SET_GAMEMODE")

    @ActionDefinition(
        displayName = "Change Health",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class ChangeHealth(
        val amount: String = "20",
        val op: Operator = Operator.SET,
    ) : Action("CHANGE_HEALTH")

    @ActionDefinition(
        displayName = "Change Hunger Level",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class ChangeHunger(
        val amount: String = "20",
        val op: Operator = Operator.SET,
    ) : Action("CHANGE_HUNGER")

    @ActionDefinition(
        displayName = "Use/Remove Held Item",
        defaultLimit = 1,
        scopes = [
            Scope(ITEM)
        ]
    )
    class UseHeldItem : Action("USE_HELD_ITEM")

    @ActionDefinition(
        displayName = "Random Action",
        defaultLimit = 25,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class RandomAction(
        val actions: List<Action> = emptyList(),
    ) : Action("RANDOM_ACTION")

    @ActionDefinition(
        displayName = "Trigger Function",
        defaultLimit = 10,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class ExecuteFunction(
        @property:Pagination val name: String? = null,
        val global: Boolean = false
    ) : Action("TRIGGER_FUNCTION")

    @ActionDefinition(
        displayName = "Apply Inventory Layout",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class ApplyInventoryLayout(
        @property:Pagination val layout: String? = null
    ) : Action("APPLY_LAYOUT")

    @ActionDefinition(
        displayName = "Exit",
        defaultLimit = 1,
        scopes = [
            Scope(CONDITIONAL)
        ]
    )
    class Exit : Action("EXIT")

    @ActionDefinition(
        displayName = "Enchant Held Item",
        defaultLimit = 25,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class EnchantHeldItem(
        val enchantment: Enchantment? = null,
        val level: Int = 1,
    ) : Action("ENCHANT_HELD_ITEM")

    @ActionDefinition(
        displayName = "Pause Execution",
        defaultLimit = 30,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class PauseExecution(
        val ticks: Int = 20
    ) : Action("PAUSE")

    @ActionDefinition(
        displayName = "Set Player Team",
        defaultLimit = 1,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class SetPlayerTeam(
        @property:Pagination val team: String? = null
    ) : Action("SET_PLAYER_TEAM")

    @ActionDefinition(
        displayName = "Display Menu",
        defaultLimit = 10,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class DisplayMenu(
        @property:Pagination val menu: String? = null
    ) : Action("DISPLAY_MENU")

    @ActionDefinition(
        displayName = "Close Menu",
        defaultLimit = 1,
        scopes = [
            Scope(MENU)
        ]
    )
    class CloseMenu : Action("CLOSE_MENU")

    @ActionDefinition(
        displayName = "Drop Item",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class DropItem(
        val item: ItemStack? = null,
        val location: Location? = null,
        val dropNaturally: Boolean = true,
        val disableMerging: Boolean = false,
        val despawnDurationTicks: Int = 6000,
        val pickupDelayTicks: Int = 10,
        val prioritizePlayer: Boolean = false,
        val inventoryFallback: Boolean = false,
    ) : Action("DROP_ITEM")

    @ActionDefinition(
        displayName = "Change Velocity",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class ChangeVelocity(
        val x: String = "10.0",
        val y: String = "10.0",
        val z: String = "10.0",
    ) : Action("CHANGE_VELOCITY")

    @ActionDefinition(
        displayName = "Launch to Target",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class LaunchToTarget(
        val location: Location? = null,
        val strength: Double = 2.0
    ) : Action("LAUNCH_TO_TARGET")

    @ActionDefinition(
        displayName = "Set Player Weather",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class SetPlayerWeather(
        val weather: Weather? = null
    ) : Action("SET_PLAYER_WEATHER")

    @ActionDefinition(
        displayName = "Set Player Time",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class SetPlayerTime(
        val time: Time = Time.ResetToWorldTime
    ) : Action("SET_PLAYER_TIME")

    @ActionDefinition(
        displayName = "Toggle Nametag Display",
        defaultLimit = 5,
        scopes = [
            Scope(FUNCTION),
            Scope(REGION),
            Scope(COMMAND),
            Scope(MENU),
            Scope(ITEM),
            Scope(
                EVENT, events = [
                    Events.PLAYER_JOIN, Events.PLAYER_DEATH, Events.PLAYER_KILL, Events.PLAYER_RESPAWN,
                    Events.GROUP_CHANGE, Events.PVP_STATE_CHANGE, Events.FISH_CATCH, Events.PLAYER_ENTER_PORTAL,
                    Events.PLAYER_DAMAGE, Events.PLAYER_BLOCK_BREAK, Events.START_PARKOUR, Events.COMPLETE_PARKOUR,
                    Events.PLAYER_DROP_ITEM, Events.PLAYER_PICK_UP_ITEM, Events.PLAYER_CHANGE_HELD_ITEM, Events.PLAYER_TOGGLE_SNEAK,
                    Events.PLAYER_TOGGLE_FLIGHT
                ]
            )
        ]
    )
    data class ToggleNametagDisplay(
        val displayNametag: Boolean = true
    ) : Action("TOGGLE_NAMETAG_DISPLAY")

    class CustomAction(
        val customKey: String,
        val parameters: Map<String, Any> = emptyMap(),
        val function: (Map<String, Any>) -> Unit = { _ -> }
    ) : Action(customKey)
}

interface Keyed {
    val key: String

    fun entries(): List<Keyed> {
        return this::class.java.enumConstants?.toList() ?: emptyList()
    }
}

annotation class CustomKey
annotation class Pagination

interface KeyedCycle : Keyed {
    fun getOrdinal(): Int
}

interface KeyedLabeled : Keyed {
    val label: String
}

data class ItemStack(
    val stack: net.minecraft.world.item.ItemStack? = null,
    val slot: Int? = null,
    val relativeFileLocation: String,
)

@Serializable
sealed class Location(override val key: String) : Keyed {
    @CustomKey
    class Custom(
        val x: String,
        val y: String,
        val z: String,
        val yaw: String? = null,
        val pitch: String? = null,
    ) : Location("Custom Coordinates") {

        override fun toString(): String {
            return if (yaw != null && pitch != null) {
                "$x $y $z $yaw $pitch"
            } else {
                "$x $y $z"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Custom) return false

            return other.toString() == toString()
        }
    }

    object HouseSpawn : Location("House Spawn Location")

    object InvokersLocation : Location("Invokers Location")

    object CurrentLocation : Location("Current Location")

    companion object {
        fun fromKey(key: String): Location? = when (key) {
            HouseSpawn.key -> HouseSpawn
            InvokersLocation.key -> InvokersLocation
            CurrentLocation.key -> CurrentLocation
            else -> null
        }
    }
}

enum class GameMode(override val key: String) : KeyedCycle {
    Adventure("Adventure"),
    Survival("Survival"),
    Creative("Creative");

    override fun getOrdinal(): Int {
        return ordinal
    }

    companion object {
        fun fromKey(key: String): GameMode? = entries.find { it.key.equals(key, true) }
    }
}

enum class Operator(vararg val symbol: String, val advanced: Boolean = false, override val key: String): Keyed {
    UNSET("unset", key = "Unset"),
    SET("=", "set", key = "Set"),
    INCREMENT("+=", "increment", "inc", key = "Increment"),
    DECREMENT("-=", "decrement", "dec", key = "Decrement"),
    MULTIPLY("*=", "multiply", "mult", key = "Multiply"),
    DIVIDE("/=", "divide", "div", key = "Divide"),
    BITWISE_AND("&=", "and", advanced = true, key = "Bitwise AND"),
    BITWISE_OR("|=", "or", advanced = true, key = "Bitwise OR"),
    BITWISE_XOR("^=", "xor", advanced = true, key = "Bitwise XOR"),
    LEFT_SHIFT("<<=", "leftShift", "shl", advanced = true, key = "Left Shift"),
    ARITHMETIC_RIGHT_SHIFT(">>=", "arithmeticRightShift", "shr", advanced = true, key = "Arithmetic Right Shift"),
    LOGICAL_RIGHT_SHIFT(">>>=", "logicalRightShift", "lshr", advanced = true, key = "Logic Right Shift");

    companion object {
        fun fromSymbol(symbol: String): Operator? {
            return entries.find {
                it.symbol.any { s ->
                    s.equals(symbol, true)
                }
            }
        }

        fun fromKey(key: String): Operator? {
            return entries.find { it.key.equals(key, true) }
        }
    }
}

sealed class InventorySlot(override val key: String, val slot: Int) : Keyed {
    @CustomKey
    data class ManualInput(val inputSlot: Int) : InventorySlot("Manual Input", inputSlot)
    class HandSlot : InventorySlot("Hand Slot", -2)
    class FirstAvailableSlot : InventorySlot("First Available Slot", -1)
    class HotbarSlot(slot: Int) : InventorySlot("Hotbar Slot $slot", slot - 1)
    class PlayerInventorySlot(slot: Int) : InventorySlot("Inventory Slot $slot", slot + 8)
    class HelmetSlot : InventorySlot("Helmet", 39)
    class ChestplateSlot : InventorySlot("Chestplate", 38)
    class LeggingsSlot : InventorySlot("Leggings", 37)
    class BootsSlot : InventorySlot("Boots", 36)

    override fun toString(): String {
        return "$slot"
    }

    companion object {
        fun fromKey(key: String): InventorySlot? {
            if (key.equals("Hand Slot", true)) return HandSlot()
            if (key.equals("First Available Slot", true)) return FirstAvailableSlot()
            if (key.equals("Helmet", true)) return HelmetSlot()
            if (key.equals("Chestplate", true)) return ChestplateSlot()
            if (key.equals("Leggings", true)) return LeggingsSlot()
            if (key.equals("Boots", true)) return BootsSlot()

            val hotbarMatch = Regex("""Hotbar Slot (\d+)""").find(key)
            if (hotbarMatch != null) {
                val slot = hotbarMatch.groupValues[1].toInt()
                return HotbarSlot(slot)
            }

            val inventoryMatch = Regex("""Inventory Slot (\d+)""").find(key)
            if (inventoryMatch != null) {
                val slot = inventoryMatch.groupValues[1].toInt()
                return PlayerInventorySlot(slot)
            }

            val slot = key.toIntOrNull() ?: return null
            return ManualInput(slot)
        }
    }
}

enum class Weather(override val key: String) : KeyedCycle {
    SUNNY("Sunny"),
    RAINING("Raining");

    override fun getOrdinal(): Int {
        return ordinal
    }

    companion object {
        fun fromKey(key: String): Weather? = entries.find { it.key.equals(key, true) }
    }
}

sealed class Time(override val key: String) : Keyed {
    @CustomKey
    class Custom(
        val time: Long
    ) : Time("Custom Time") {
        override fun toString(): String {
            return time.toString()
        }
    }

    object ResetToWorldTime : Time("Reset to World Time")
    object Sunrise : Time("Sunrise (0)")
    object Noon : Time("Noon (6,000)")
    object Sunset : Time("Sunset (12,000)")
    object Midnight : Time("Midnight (18,000)")

    companion object {
        fun fromKey(key: String): Time {
            if (key.contains("Reset to World Time", true)) return ResetToWorldTime
            if (key.contains("Sunrise", true)) return Sunrise
            if (key.contains("Noon", true)) return Noon
            if (key.contains("Sunset", true)) return Sunset
            if (key.contains("Midnight", true)) return Midnight
            return Custom(key.replace(",", "").toLong())
        }
    }
}

enum class VariableHolder(override val key: String) : KeyedCycle {
    Player("Player"),
    Global("Global"),
    Team("Team");

    override fun getOrdinal(): Int {
        return ordinal
    }

    companion object {
        fun fromKey(key: String): VariableHolder? = entries.find { it.key.equals(key, true) }
    }
}