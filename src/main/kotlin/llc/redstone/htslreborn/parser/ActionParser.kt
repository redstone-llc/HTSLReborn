package llc.redstone.htslreborn.parser

import dev.wekend.housingtoolbox.feature.data.Action
import dev.wekend.housingtoolbox.feature.data.Action.*
import dev.wekend.housingtoolbox.feature.data.ItemStack
import dev.wekend.housingtoolbox.feature.data.Keyed
import dev.wekend.housingtoolbox.feature.data.Location
import dev.wekend.housingtoolbox.feature.data.StatOp
import dev.wekend.housingtoolbox.feature.data.StatValue
import guru.zoroark.tegral.niwen.lexer.Token
import llc.redstone.htslreborn.tokenizer.Operators
import llc.redstone.htslreborn.tokenizer.Tokens
import kotlin.reflect.KParameter
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType

object ActionParser {
    val keywords = mapOf(
        "applyLayout" to ApplyInventoryLayout::class,
        "applyPotion" to ApplyPotionEffect::class,
        "balanceTeam" to BalancePlayerTeam::class,
        "cancelEvent" to CancelEvent::class,
        "globalstat" to GlobalVariable::class,
        "globalvar" to GlobalVariable::class,
        "changeHealth" to ChangeHealth::class,
        "hungerLevel" to ChangeHunger::class,
        "maxHealth" to ChangeMaxHealth::class,
        "changeGroup" to ChangePlayerGroup::class,
        "stat" to PlayerVariable::class,
        "var" to PlayerVariable::class,
        "teamstat" to TeamVariable::class,
        "teamvar" to TeamVariable::class,
        "clearEffects" to ClearAllPotionEffects::class,
        "closeMenu" to CloseMenu::class,
        "actionBar" to DisplayActionBar::class,
        "displayMenu" to DisplayMenu::class,
        "title" to DisplayTitle::class,
        "enchant" to EnchantHeldItem::class,
        "exit" to Exit::class,
        "failParkour" to FailParkour::class,
        "fullHeal" to FullHeal::class,
        "xpLevel" to GiveExperienceLevels::class,
        "giveItem" to GiveItem::class,
        "kill" to KillPlayer::class,
        "parkCheck" to ParkourCheckpoint::class,
        "pause" to PauseExecution::class,
        "sound" to PlaySound::class,
        "removeItem" to RemoveItem::class,
        "resetInventory" to ResetInventory::class,
        "chat" to SendMessage::class,
        "lobby" to SendToLobby::class,
        "compassTarget" to SetCompassTarget::class,
        "gamemode" to SetGameMode::class,
        "setTeam" to SetPlayerTeam::class,
        "tp" to TeleportPlayer::class,
        "function" to ExecuteFunction::class,
        "consumeItem" to UseHeldItem::class,
        "dropItem" to DropItem::class,
        "changeVelocity" to ChangeVelocity::class,
        "launchTarget" to LaunchToTarget::class,
        "playerWeather" to SetPlayerWeather::class,
        "playerTime" to SetPlayerTime::class,
        "displayNametag" to ToggleNametagDisplay::class,
    )

    fun createAction(keyword: String, iterator: Iterator<Token>): Action? {
        //Get the action class
        val clazz = keywords[keyword] ?: return null

        val constructor = clazz.primaryConstructor ?: return null

        val args: MutableMap<KParameter, Any?> = mutableMapOf()

        for (param in constructor.parameters) {
            val prop = clazz.memberProperties.find { it.name == param.name }!!

            val token = iterator.next()
            //End of action
            if (token.tokenType == Tokens.NEWLINE) continue
            println(token.tokenType)

            args[param] = when (prop.returnType.classifier) {
                String::class -> token.string
                Int::class -> token.string.toInt()
                Long::class -> token.string.removeSuffix("L").toLong()
                Double::class -> token.string.toDouble()
                Boolean::class -> token.string.toBoolean()
                //Stat Values
                StatValue::class -> {
                    when (token.tokenType) {
                        Tokens.STRING -> StatValue.Str(token.string)
                        Tokens.INT -> StatValue.I32(token.string.toInt())
                        Tokens.LONG -> StatValue.Lng(token.string.removeSuffix("L").toLong())
                        Tokens.DOUBLE -> StatValue.Dbl(token.string.removeSuffix("D").toDouble())
                        else -> error("Unknown StatValue token: ${token.string}")
                    }
                }

                Location::class -> LocationParser.parse(token.string, iterator)

                ItemStack::class -> {
//                        val relativeFileLocation = token.string
//                        val nbtString = File(relativeFileLocation).readText()
//
//                        ItemStack(
//                            nbt = nbt,
//                            relativeFileLocation = relativeFileLocation,
//                        )
                }

                StatOp::class -> when (token.tokenType) {
                    Operators.SET -> StatOp.Set
                    Operators.INCREMENT -> StatOp.Inc
                    Operators.DECREMENT -> StatOp.Dec
                    Operators.MULTIPLY -> StatOp.Mul
                    Operators.DIVIDE -> StatOp.Div
                    Operators.BITWISE_AND -> StatOp.BitAnd
                    Operators.BITWISE_OR -> StatOp.BitOr
                    Operators.BITWISE_XOR -> StatOp.BitXor
                    Operators.LEFT_SHIFT -> StatOp.LS
                    Operators.LOGICAL_RIGHT_SHIFT -> StatOp.LRS
                    Operators.ARITHMETIC_RIGHT_SHIFT -> StatOp.ARS
                    else -> error("Unknown StatOp: ${token.string}")
                }


                else -> null
            }

            if (args.containsKey(param) && args[param] != null) {
                continue
            }

            if (prop.returnType.isSubtypeOf(Keyed::class.starProjectedType)) {
                val companion = prop.returnType.classifier
                    .let { it as? kotlin.reflect.KClass<*> }
                    ?.companionObjectInstance
                    ?: error("No companion object for keyed enum: ${prop.returnType}")

                val getByKeyMethod = companion::class.members.find { it.name == "fromKey" }
                    ?: error("No getByKey method for keyed enum: ${prop.returnType}")

                args[param] = getByKeyMethod.call(companion, token.string)
            }
        }

        println(args.values.joinToString(","))
        if (args.size != constructor.parameters.size) {
            clazz.constructors.forEach { newCon ->
                if (constructor.parameters.size == newCon.parameters.size) {
                    return newCon.callBy(args)
                }
            }
        }

        return constructor.callBy(args)
    }
}