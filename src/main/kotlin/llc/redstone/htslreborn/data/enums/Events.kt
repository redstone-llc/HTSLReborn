package llc.redstone.htslreborn.data.enums

/**
 * Available events that actions may be placed in.
 *
 * @param label Housing's string label for the event.
 */
enum class Events(val label: String) {
    /** Event fired when a player joins the house. */
    PLAYER_JOIN("Player Join"),
    /** Event fired when a player leaves the house. */
    PLAYER_QUIT("Player Quit"),
    /** Event fired when a player dies. */
    PLAYER_DEATH("Player Death"),
    /** Event fired when a player kills another player. */
    PLAYER_KILL("Player Kill"),
    /** Event fired when a player respawns. */
    PLAYER_RESPAWN("Player Respawn"),
    /** Event fired when a player's group changes. */
    GROUP_CHANGE("Group Change"),
    /** Event fired when a player's PvP state changes. */
    PVP_STATE_CHANGE("PvP State Change"),
    /** Event fired when a player catches a fish. */
    FISH_CATCH("Fish Caught"),
    /** Event fired when a player enters a portal. */
    PLAYER_ENTER_PORTAL("Player Enter Portal"),
    /** Event fired when a player takes damage. */
    PLAYER_DAMAGE("Player Damage"),
    /** Event fired when a player breaks a block. */
    PLAYER_BLOCK_BREAK("Player Block Break"),
    /** Event fired when a player starts a parkour course. */
    START_PARKOUR("Start Parkour"),
    /** Event fired when a player completes a parkour course. */
    COMPLETE_PARKOUR("Complete Parkour"),
    /** Event fired when a player drops an item. */
    PLAYER_DROP_ITEM("Player Drop Item"),
    /** Event fired when a player picks up an item. */
    PLAYER_PICK_UP_ITEM("Player Pick Up Item"),
    /** Event fired when a player changes their held item. */
    PLAYER_CHANGE_HELD_ITEM("Player Change Held Item"),
    /** Event fired when a player toggles sneaking. */
    PLAYER_TOGGLE_SNEAK("Player Toggle Sneak"),
    /** Event fired when a player toggles flight. */
    PLAYER_TOGGLE_FLIGHT("Player Toggle Flight")
}