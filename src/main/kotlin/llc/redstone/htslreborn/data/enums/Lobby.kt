package llc.redstone.htslreborn.data.enums

import llc.redstone.htslreborn.data.Keyed

enum class Lobby(override val key: String) : Keyed {
    MainLobby("Main Lobby"),
    TournamentHall("Tournament Hall"),
    BlitzSG("Blitz SG"),
    TNTGames("The TNT Games"),
    MegaWalls("Mega Walls"),
    ArcadeGames("Arcade Games"),
    CopsAndCrims("Cops and Crims"),
    UHCChampions("UHC Champions"),
    Warlords("Warlords"),
    SmashHeroes("Smash Heroes"),
    Housing("Housing"),
    SkyWars("SkyWars"),
    SpeedUHC("Speed UHC"),
    ClassicGames("Classic Games"),
    Prototype("Prototype"),
    BedWars("Bed Wars"),
    MurderMystery("Murder Mystery"),
    BuildBattle("Build Battle"),
    Duels("Duels"),
    WoolGames("Wool Games");

    companion object {
        fun fromKey(key: String): Lobby? = entries.find { it.key.equals(key, true) }
    }
}