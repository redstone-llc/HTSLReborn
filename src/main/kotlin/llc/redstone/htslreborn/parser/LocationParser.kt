package llc.redstone.htslreborn.parser

import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import llc.redstone.systemsapi.data.Location

object LocationParser {
    fun parse(str: String, iterator: Iterator<TokenWithPosition>): Location =
        when (str.lowercase().replace("_", " ")) {
            "house spawn location", "house spawn" -> Location.HouseSpawn
            "current location" -> Location.CurrentLocation
            "invokers location" -> Location.InvokersLocation
            "custom coordinates" -> {
                val xPart = iterator.next().string
                val yPart = iterator.next().string
                val zPart = iterator.next().string
                var pitch: String? = null
                var yaw: String? = null
                if (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next.tokenType != Tokens.NEWLINE) {
                        pitch = next.string
                        if (iterator.hasNext()) {
                            val next2 = iterator.next()
                            if (next2.tokenType != Tokens.NEWLINE) {
                                yaw = next2.string
                            }
                        }
                    }

                }

                fun parsePart(part: String?): Location.Custom.Coordinate? {
                    if (part == null) return null
                    return Location.Custom.Coordinate(
                        value = part.removePrefix("~").removePrefix("^").toDoubleOrNull() ?: 0.0,
                        type = when {
                            part.startsWith("~") -> Location.Custom.Type.RELATIVE
                            part.startsWith("^") -> Location.Custom.Type.CARET
                            else -> Location.Custom.Type.NORMAL
                        }
                    )
                }

                Location.Custom(
                    x = parsePart(xPart) ?: error("Invalid X coordinate: $xPart"),
                    y = parsePart(yPart) ?: error("Invalid Y coordinate: $yPart"),
                    z = parsePart(zPart) ?: error("Invalid Z coordinate: $zPart"),
                    pitch = parsePart(pitch),
                    yaw = parsePart(yaw)
                )
            }

            else -> error("Unknown location type: $str")
        }
}