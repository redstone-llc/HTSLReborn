package llc.redstone.htslreborn.parser

import com.strumenta.antlrkotlin.parsers.generated.HTSLParser
import com.strumenta.antlrkotlin.parsers.generated.HTSLParser.ActionStatementContext
import com.strumenta.antlrkotlin.parsers.generated.HTSLParser.ArgumentContext
import llc.redstone.htslreborn.data.Action
import llc.redstone.htslreborn.data.Action.*
import llc.redstone.htslreborn.data.InventorySlot
import llc.redstone.htslreborn.data.ItemStack
import llc.redstone.htslreborn.data.Keyed
import llc.redstone.htslreborn.data.Location
import llc.redstone.htslreborn.data.Operator
import llc.redstone.htslreborn.utils.ErrorUtils
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import llc.redstone.htslreborn.utils.ItemUtils
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

object ActionParser {
    val keywords = linkedMapOf(
        "applyLayout" to ApplyInventoryLayout::class,
        "applyPotion" to ApplyPotionEffect::class,
        "cancelEvent" to CancelEvent::class,
        "globalvar" to GlobalVariable::class,
        "globalstat" to GlobalVariable::class,
        "changeHealth" to ChangeHealth::class,
        "hungerLevel" to ChangeHunger::class,
        "maxHealth" to ChangeMaxHealth::class,
        "changeGroup" to ChangePlayerGroup::class,
        "changePlayerGroup" to ChangePlayerGroup::class,
        "var" to PlayerVariable::class,
        "stat" to PlayerVariable::class,
        "teamvar" to TeamVariable::class,
        "teamstat" to TeamVariable::class,
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

    fun handleSwaps(parameters: MutableList<KParameter>, clazz: KClass<out Action>) {
        fun swapParams(name: String, name2: String) {
            val index1 = parameters.indexOfFirst { it.name == name }
            val index2 = parameters.indexOfFirst { it.name == name2 }
            if (index1 != -1 && index2 != -1) {
                val temp = parameters[index1]
                parameters[index1] = parameters[index2]
                parameters[index2] = temp
            }
        }

        if (clazz == ChangeHunger::class || clazz == ChangeMaxHealth::class || clazz == ChangeHealth::class) swapParams("amount", "op")
        if (clazz == TeamVariable::class) swapParams("teamName", "variable")
        if (clazz == DropItem::class) {
            swapParams("despawnDurationTicks", "prioritizePlayer")
            swapParams("pickupDelayTicks", "inventoryFallback")
        }
    }

    fun parse(keyword: String, statement: ActionStatementContext, statementArgs: List<ArgumentContext>, path: Path): Action {
        val actionClass = keywords[keyword]
            ?: htslCompileError("Unknown action: $keyword", statement)
        val constructor = actionClass.primaryConstructor
            ?: htslCompileError("No primary constructor found for action: ${actionClass.simpleName}", statement)

        val args: MutableMap<KParameter, Any?> = mutableMapOf()
        val parameters = constructor.parameters.toMutableList()

        handleSwaps(parameters, actionClass)

        try {
            val iterator = statementArgs.listIterator()
            for (param in parameters) {
                val prop = actionClass.memberProperties.find { it.name == param.name }!!
                val arg = if (iterator.hasNext()) iterator.next() else continue

                val parsedValue = PropertyParser.parse(prop, param, arg, iterator, path)
                args[param] = parsedValue
            }
        } catch (e: ErrorUtils.HTSLCompileException) {
            throw e
        } catch (e: Exception) {
            htslCompileError("Error parsing arguments for action: ${actionClass.simpleName}. ${e.message}", statement)
        }

        val newArgs = args.filterValues { it != null }.toMutableMap()

        if (newArgs.size != constructor.parameters.size) {
            actionClass.constructors.forEach { newCon ->
                if (newArgs.size == newCon.parameters.size) {
                    return newCon.callBy(newArgs)
                }
            }
        }
        return try {
            constructor.callBy(args)
        } catch (_: Exception) {
            constructor.callBy(newArgs)
        }
    }
}