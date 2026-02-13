package llc.redstone.htslreborn.parser

import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import llc.redstone.systemsapi.data.Location

object LocationParser {
    fun parse(str: String, iterator: ListIterator<TokenWithPosition>): Location =
        when (str.lowercase().replace("_", " ")) {
            "house spawn location", "house spawn" -> Location.HouseSpawn
            "current location" -> Location.CurrentLocation
            "invokers location" -> Location.InvokersLocation
            "custom coordinates" -> {
                val token = iterator.next().string
                val parts = token.split(" ")
                var xPart: String
                var yPart: String
                var zPart: String
                var pitch: String? = null
                var yaw: String? = null

                if (parts.size == 1) {
                    xPart = parts[0]
                    yPart = iterator.next().string
                    zPart = iterator.next().string
                    val pitchToken = if (iterator.hasNext()) iterator.next() else null
                    pitch = if (pitchToken != null && pitchToken.tokenType != Tokens.BOOLEAN && pitchToken.tokenType != Tokens.NEWLINE) {
                        pitchToken.string
                    } else {
                        iterator.previous()
                        null
                    }
                    val yawToken = if (iterator.hasNext()) iterator.next() else null
                    yaw = if (yawToken != null && yawToken.tokenType != Tokens.BOOLEAN && yawToken.tokenType != Tokens.NEWLINE) {
                        yawToken.string
                    } else {
                        iterator.previous()
                        null
                    }

                } else {
                    if (parts.size !in 3..5) error("Custom coordinates must have 3 to 5 parts: $token")
                    xPart = parts[0]
                    yPart = parts[1]
                    zPart = parts[2]
                    pitch = parts.getOrNull(3)
                    yaw = parts.getOrNull(4)
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