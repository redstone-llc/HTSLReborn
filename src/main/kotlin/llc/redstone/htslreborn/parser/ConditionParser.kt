package llc.redstone.htslreborn.parser

import com.strumenta.antlrkotlin.parsers.generated.HTSLParser.ArgumentContext
import com.strumenta.antlrkotlin.parsers.generated.HTSLParser.ConditionStatementContext
import llc.redstone.htslreborn.data.Condition
import llc.redstone.htslreborn.data.Condition.*
import llc.redstone.htslreborn.utils.ErrorUtils
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import java.nio.file.Path
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

object ConditionParser {
    val keywords = linkedMapOf(
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


    fun parse(
        keyword: String,
        condition: ConditionStatementContext,
        statementArgs: List<ArgumentContext>,
        path: Path
    ): Condition? {
        val conditionClass = keywords[keyword]
            ?: htslCompileError("Unknown condition: $keyword", condition)
        val constructor = conditionClass.primaryConstructor
            ?: htslCompileError("No primary constructor found for condition: ${conditionClass.simpleName}", condition)

        val args: MutableMap<KParameter, Any?> = mutableMapOf()
        val parameters = constructor.parameters.toMutableList()

        try {
            val iterator = statementArgs.listIterator()
            for (param in parameters) {
                val prop = conditionClass.memberProperties.find { it.name == param.name }!!
                val arg = if (iterator.hasNext()) iterator.next() else continue

                val parsedValue = PropertyParser.parse(prop, param, arg, iterator, path)
                args[param] = parsedValue
            }
        } catch (e: ErrorUtils.HTSLCompileException) {
            throw e
        } catch (e: Exception) {
            htslCompileError(
                "Error parsing arguments for condition: ${conditionClass.simpleName}. ${e.message}",
                condition
            )
        }

        val newArgs = args.filterValues { it != null }.toMutableMap()

        if (newArgs.size != constructor.parameters.size) {
            conditionClass.constructors.forEach { newCon ->
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