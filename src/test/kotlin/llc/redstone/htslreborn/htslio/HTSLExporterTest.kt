package llc.redstone.htslreborn.htslio

import llc.redstone.systemsdata.Action
import llc.redstone.systemsdata.Location
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
            )
        )

        assertEquals(
            listOf(
                "chat \"hello world\"",
                "chat \"\"",
                "chat \"null\"",
            ),
            exported
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
