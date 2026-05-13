package llc.redstone.htslreborn.htslio

import llc.redstone.systemsdata.Action
import llc.redstone.systemsdata.Comparison
import llc.redstone.systemsdata.Condition
import llc.redstone.systemsdata.Location
import llc.redstone.systemsdata.StatOp
import llc.redstone.systemsdata.StatValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HTSLExporterTest {
    @Test
    fun testStringValuesAreQuotedWhenRequired() {
        val exported = HTSLExporter.export(
            listOf(
                Action.SendMessage("hello world"),
                Action.SendMessage(""),
                Action.SendMessage("null"),
                Action.SendMessage("type=join;name=%var.global/join/name%;playerid=%var.global/join/playerid%"),
            )
        )

        assertEquals(
            listOf(
                "chat \"hello world\"",
                "chat \"\"",
                "chat \"null\"",
                "chat type=join;name=%var.global/join/name%;playerid=%var.global/join/playerid%",
            ),
            exported
        )
    }

    @Test
    fun testVariableAliasesExportAsVarAndPreservePlaceholders() {
        val exportedActions = HTSLExporter.export(
            listOf(
                Action.PlayerVariable("Kills", StatOp.Set, StatValue.UnquotedStr("%stat.player/Kills%")),
                Action.GlobalVariable("Total", StatOp.Inc, StatValue.I32(1)),
            )
        )

        assertEquals(
            listOf(
                "var Kills = %stat.player/Kills% false",
                "globalvar Total += 1 false",
            ),
            exportedActions
        )

        val exportedConditions = HTSLExporter.exportConditions(
            listOf(
                Condition.PlayerVariableRequirement("Kills", Comparison.Ge, StatValue.UnquotedStr("%stat.player/Kills%")),
                Condition.GlobalVariableRequirement("Total", Comparison.Eq, StatValue.I32(1)),
            )
        )

        assertEquals(
            listOf(
                "var Kills >= %stat.player/Kills% null",
                "globalvar Total == 1 null",
            ),
            exportedConditions
        )
    }

    @Test
    fun testReadableConditionAliasesArePreferredOnExport() {
        val exportedConditions = HTSLExporter.exportConditions(
            listOf(
                Condition.RequiredGroup("default", true),
                Condition.RequiredTeam("red"),
                Condition.InRegion("spawn"),
            )
        )

        assertEquals(
            listOf(
                "hasGroup default true",
                "hasTeam red",
                "inRegion spawn",
            ),
            exportedConditions
        )
    }

    @Test
    fun testCustomLocationsExportAllCoordinates() {
        val exported = HTSLExporter.export(
            listOf(
                Action.TeleportPlayer(
                    Location.Custom(
                        Location.Custom.Coordinate("1"),
                        Location.Custom.Coordinate("2"),
                        Location.Custom.Coordinate("3"),
                    )
                )
            )
        )

        assertEquals(
            listOf("tp \"custom_coordinates\" \"1 2 3\" false"),
            exported
        )
    }
}