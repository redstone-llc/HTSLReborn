package llc.redstone.htslreborn.parser

import com.strumenta.antlrkotlin.parsers.generated.HTSLParser
import llc.redstone.htslreborn.data.*
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import llc.redstone.htslreborn.utils.ItemUtils
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

object PropertyParser {
    fun parse(
        prop: KProperty1<out PropertyHolder, *>,
        param: KParameter,
        arg: HTSLParser.ArgumentContext,
        iterator: ListIterator<HTSLParser.ArgumentContext>,
        path: Path
    ): Any? {
        if (arg.text == "null") {
            return null
        }

        if (prop.returnType.isSubtypeOf(Keyed::class.starProjectedType.withNullability(true))) {
            val companion = prop.returnType.classifier
                .let { it as? KClass<*> }
                ?.companionObjectInstance
                ?: error("No companion object for keyed enum: ${prop.returnType}")

            val getByKeyMethod = companion::class.members.find { it.name == "fromKey" }

            val result = getByKeyMethod?.call(companion, arg.text.replace("\"", ""))
            if (result != null) {
                return result
            }
        }
        return when (prop.returnType.classifier) {
            String::class -> arg.text
            Int::class -> (arg.text.toIntOrNull()
                ?: htslCompileError("Missing numeric value for parameter: ${param.name}", arg))

            Double::class -> (arg.text.toDoubleOrNull()
                ?: htslCompileError("Missing numeric value for parameter: ${param.name}", arg))

            Boolean::class -> (arg.text.toBooleanStrictOrNull()
                ?: htslCompileError("Invalid boolean argument for parameter: ${param.name}", arg))

            Operator::class -> (arg.text.let { Operator.fromSymbol(it) }
                ?: htslCompileError("Invalid operator argument for parameter: ${param.name}", arg))

            Comparator::class -> (arg.text.let { Comparator.fromSymbol(it) })
                ?: htslCompileError("Invalid comparator argument for parameter: ${param.name}", arg)

            ItemStack::class -> {
                parseItemStack(arg, path)
            }

            Location::class -> {
                parseLocation(arg, iterator)
                    ?: htslCompileError("Invalid location argument for parameter: ${param.name}", arg)
            }

            InventorySlot::class -> {
                val slotIndex = arg.text.replace("\"", "")
                InventorySlot.fromKey(slotIndex)
                    ?: htslCompileError("Invalid inventory slot: $slotIndex", arg)
            }

            else -> htslCompileError("Unsupported parameter type: ${prop.returnType}", arg)
        }
    }

    fun parseItemStack(
        arg: HTSLParser.ArgumentContext,
        path: Path
    ): ItemStack {
        var argValue = arg.text
        if (argValue.startsWith("slot_")) {
            val slot = argValue.removePrefix("slot_").toIntOrNull()
                ?: throw IllegalArgumentException("Invalid slot index: $argValue")
            return ItemStack(
                slot = slot,
                relativeFileLocation = argValue,
            )
        }
        val nbt = try {
            val parent = if (path.isDirectory()) path else path.parent
            if (!argValue.endsWith(".nbt")) {
                argValue += ".nbt"
            }
            val file = parent.resolve(argValue)
            ItemUtils.fileToNbtCompound(file)
        } catch (_: Exception) {
            htslCompileError("Error reading NBT file: $argValue", arg)
        }

        return ItemStack(
            nbt = nbt,
            relativeFileLocation = argValue,
        )
    }

    fun parseLocation(
        arg: HTSLParser.ArgumentContext,
        iterator: ListIterator<HTSLParser.ArgumentContext>
    ): Location? {
        val str = arg.text.replace("\"", "")
        return when (str.lowercase().replace("_", " ")) {
            "null" -> null
            "house spawn location", "house spawn" -> Location.HouseSpawn
            "current location" -> Location.CurrentLocation
            "invokers location" -> Location.InvokersLocation
            "custom coordinates" -> {
                var xPart: String
                var yPart: String
                var zPart: String
                var pitch: String? = null
                var yaw: String? = null
                if (iterator.hasNext()) {
                    val nextArg = iterator.next()
                    if (nextArg.STRING() != null) {
                        val split = nextArg.text.replace("\"", "").split(" ").map { it.trim() }
                        if (split.size !in 3..5) {
                            htslCompileError(
                                "Invalid number of coordinates for custom coordinates: ${split.size}. Expected 3 to 5.",
                                nextArg
                            )
                        }
                        xPart = if (checkCoordinate(split[0])) split[0] else htslCompileError(
                            "Invalid X coordinate for custom coordinates: ${split[0]}",
                            nextArg
                        )
                        yPart = if (checkCoordinate(split[1])) split[1] else htslCompileError(
                            "Invalid Y coordinate for custom coordinates: ${split[1]}",
                            nextArg
                        )
                        zPart = if (checkCoordinate(split[2])) split[2] else htslCompileError(
                            "Invalid Z coordinate for custom coordinates: ${split[2]}",
                            nextArg
                        )
                        if (split.size >= 4) {
                            pitch = split[3]
                        }
                        if (split.size >= 5) {
                            yaw = split[4]
                        }
                        Location.Custom(xPart, yPart, zPart, pitch, yaw)
                    } else {
                        xPart = nextArg.text.replace("\"", "")
                        // Move to Y coordinate
                        val yArg = if (iterator.hasNext()) iterator.next() else null
                        yPart = yArg?.text?.replace("\"", "")
                            ?: htslCompileError("Missing Y coordinate for custom coordinates", arg)
                        // Move to Z coordinate
                        val zArg = if (iterator.hasNext()) iterator.next() else null
                        zPart = zArg?.text?.replace("\"", "")
                            ?: htslCompileError("Missing Z coordinate for custom coordinates", arg)
                        // Move to optional pitch coordinate
                        if (iterator.hasNext()) {
                            val pitchArg = iterator.next()
                            pitch = pitchArg.text.replace("\"", "")
                            if (iterator.hasNext()) {
                                // Move to Yaw coordinate
                                val yawArg = iterator.next()
                                yaw = yawArg.text.replace("\"", "")
                            }
                        }
                        Location.Custom(xPart, yPart, zPart, pitch, yaw)
                    }
                } else {
                    htslCompileError("Missing coordinates for custom coordinates", arg)
                }
            }

            else -> htslCompileError("Invalid location: $str", arg)
        }
    }

    fun checkCoordinate(coord: String): Boolean {
        val regex = Regex("^[~^]?[+-]?\\d+(\\.\\d+)?$")
        return regex.matches(coord)
    }
}