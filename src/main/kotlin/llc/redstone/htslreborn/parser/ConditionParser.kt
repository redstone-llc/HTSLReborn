package llc.redstone.htslreborn.parser

import llc.redstone.htslreborn.tokenizer.Comparators
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import llc.redstone.systemsapi.data.*
import llc.redstone.systemsapi.data.Condition.*
import llc.redstone.systemsapi.data.Condition.DamageCause
import llc.redstone.systemsapi.data.Condition.FishingEnvironment
import llc.redstone.systemsapi.data.Condition.PortalType
import net.minecraft.nbt.StringNbtReader
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

object ConditionParser {
    val keywords = mapOf(
        "blockType" to BlockType::class,
        "damageAmount" to RequiredDamageAmount::class,
        "damageCause" to DamageCause::class,
        "doingParkour" to InParkour::class,
        "fishingEnv" to FishingEnvironment::class,
        "globalvar" to GlobalVariableRequirement::class,
        "globalstat" to GlobalVariableRequirement::class,
        "hasItem" to HasItem::class,
        "hasPotion" to RequiredEffect::class,
        "isItem" to IsItem::class,
        "isSneaking" to PlayerSneaking::class,
        "maxHealth" to RequiredMaxHealth::class,
        "placeholder" to RequiredPlaceholderNumber::class,
        "isFlying" to PlayerFlying::class,
        "health" to RequiredHealth::class,
        "hunger" to RequiredHungerLevel::class,
        "var" to PlayerVariableRequirement::class,
        "stat" to PlayerVariableRequirement::class,
        "portal" to PortalType::class,
        "canPvp" to PvpEnabled::class,
        "gamemode" to RequiredGameMode::class,
        "hasGroup" to RequiredGroup::class,
        "inGroup" to RequiredGroup::class,
        "hasPermission" to HasPermission::class,
        "hasTeam" to RequiredTeam::class,
        "inTeam" to RequiredTeam::class,
        "teamvar" to TeamVariableRequirement::class,
        "teamstat" to TeamVariableRequirement::class,
        "inRegion" to InRegion::class,
    )

    fun createCondition(keyword: String, iterator: Iterator<TokenWithPosition>, path: Path?, inverted: Boolean = false): Condition? {
        val clazz = keywords[keyword] ?: return null

        val constructor = clazz.primaryConstructor ?: return null

        val args: MutableMap<KParameter, Any?> = mutableMapOf()

        for (param in constructor.parameters) {
            val prop = clazz.memberProperties.find { it.name == param.name }!!

            val token = iterator.next()
            if (token.tokenType == Tokens.COMMA) break

            args[param] = when (prop.returnType.classifier) {
                String::class -> token.string
                Int::class -> token.string.toInt()
                Long::class -> token.string.removeSuffix("L").toLong()
                Double::class -> token.string.removeSuffix("D").toDouble()
                Boolean::class -> token.string.toBoolean()
                StatValue::class -> {
                    when (token.tokenType) {
                        Tokens.STRING -> StatValue.Str(token.string)
                        Tokens.INT -> {
                            if (token.string.toIntOrNull() == null) {
                                StatValue.Lng(token.string.removeSuffix("L").toLong())
                            } else {
                                StatValue.I32(token.string.toInt())
                            }
                        }
                        Tokens.LONG -> StatValue.Lng(token.string.removeSuffix("L").toLong())
                        Tokens.DOUBLE -> StatValue.Dbl(token.string.removeSuffix("D").toDouble())
                        Tokens.NULL -> null
                        else -> StatValue.Str(token.string)
                    }
                }

                ItemStack::class -> {
                    if (path == null) {
                        htslCompileError("Cannot load ItemStack from file when file is null", token)
                    }
                    val relativeFileLocation = token.string
                    val parent = if (path.isDirectory()) path else path.parent
                    val nbtString = parent.resolve(relativeFileLocation).readText()

                    ItemStack(
                        nbt = StringNbtReader.readCompound(nbtString),
                        relativeFileLocation = relativeFileLocation,
                    )
                }

                Comparison::class -> when (token.tokenType) {
                    Comparators.EQUALS -> Comparison.Eq
                    Comparators.GREATER_THAN -> Comparison.Gt
                    Comparators.LESS_THAN -> Comparison.Lt
                    Comparators.LESS_THAN_OR_EQUAL -> Comparison.Le
                    Comparators.GREATER_THAN_OR_EQUAL -> Comparison.Ge
                    else -> null
                }
                else -> null
            }

            if (token.tokenType == Tokens.NULL) {
                args[param] = null
            }

            if (args.containsKey(param) && args[param] != null) {
                continue
            }

            if (prop.returnType.isSubtypeOf(Keyed::class.starProjectedType)) {
                val companion = prop.returnType.classifier
                    .let { it as? kotlin.reflect.KClass<*> }
                    ?.companionObjectInstance
                    ?: htslCompileError("No companion object for keyed enum: ${prop.returnType}", token)

                val getByKeyMethod = companion::class.members.find { it.name == "fromKey" }
                    ?: htslCompileError("No getByKey method for keyed enum: ${prop.returnType}", token)

                args[param] = getByKeyMethod.call(companion, token)
            }
        }

        val con = constructor.callBy(args)
        con.inverted = inverted
        return con
    }
}