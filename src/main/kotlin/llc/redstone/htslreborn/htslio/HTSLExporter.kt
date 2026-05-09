package llc.redstone.htslreborn.htslio

import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.exporting
import llc.redstone.htslreborn.HTSLReborn.exportingFile
import llc.redstone.htslreborn.parser.ActionParser
import llc.redstone.htslreborn.parser.ActionParser.handleSwaps
import llc.redstone.htslreborn.parser.ConditionParser
import llc.redstone.htslreborn.utils.UIErrorToast
import llc.redstone.htslreborn.utils.UISuccessToast
import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsdata.*
import net.minecraft.sound.SoundEvents
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

object HTSLExporter {
    private val actionKeywords = ActionParser.keywords.entries.associate { (keyword, actionClass) -> actionClass to keyword }
    private val conditionKeywords = ConditionParser.keywords.entries.associate { (keyword, conditionClass) -> conditionClass to keyword }
    private val actionPropertyCache = mutableMapOf<KClass<out Action>, List<KProperty1<Action, *>>>()
    private val conditionPropertyCache = mutableMapOf<KClass<out Condition>, List<KProperty1<Condition, *>>>()

    @Suppress("UNCHECKED_CAST")
    private fun orderedActionProperties(actionClass: KClass<out Action>): List<KProperty1<Action, *>> {
        return actionPropertyCache.getOrPut(actionClass) {
            val constructor = actionClass.primaryConstructor ?: return@getOrPut emptyList()
            val parameters = constructor.parameters.toMutableList()
            handleSwaps(parameters, actionClass)
            val propertiesByName = actionClass.memberProperties.associateBy { it.name }

            parameters.mapNotNull { parameter ->
                propertiesByName[parameter.name] as? KProperty1<Action, *>
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun orderedConditionProperties(conditionClass: KClass<out Condition>): List<KProperty1<Condition, *>> {
        return conditionPropertyCache.getOrPut(conditionClass) {
            val constructor = conditionClass.primaryConstructor ?: return@getOrPut emptyList()
            val propertiesByName = conditionClass.memberProperties.associateBy { it.name }

            constructor.parameters.mapNotNull { parameter ->
                propertiesByName[parameter.name] as? KProperty1<Condition, *>
            }
        }
    }

    private fun quoteIfNeeded(value: String): String {
        val escaped = value.replace("\"", "\\\"")
        return if (escaped.isEmpty() || escaped.any { it.isWhitespace() } || escaped == "null" || escaped.contains("\\\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    fun exportFile(path: Path, onComplete: (Boolean) -> Unit = {}) {
        SystemsAPI.launch {
            exportingFile = path
            exporting = true

            try {
                val actionContainer = SystemsAPI.getHousingImporter().getOpenActionContainer()
                if (actionContainer == null) {
                    UIErrorToast.report("You must have an action gui open to export HTSL code.")
                    onComplete(false)
                    return@launch
                }

                val actions = actionContainer.getActions()
                val lines = export(actions)
                path.parent?.let {
                    if (!it.exists()) {
                        it.createDirectories()
                    }
                }
                path.writeText(lines.joinToString("\n"))

                if (HTSLReborn.CONFIG.playCompleteSound) MC.player?.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    1.0f,
                    1.0f
                )
                UISuccessToast.report("Successfully exported HTSL code to ${path.name}")
                onComplete(true)
            } catch (e: CancellationException) {
                onComplete(false)
            } catch (e: Exception) {
                if (e.cause is CancellationException) {
                    onComplete(false)
                    return@launch
                }

                if (HTSLReborn.CONFIG.playCompleteSound) MC.player?.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(),
                    1.0f,
                    0.8f
                )
                UIErrorToast.report(e)
                e.printStackTrace()
                onComplete(false)
            } finally {
                exporting = false
                exportingFile = null
            }
        }
    }

    //This class is a little gross :)
    fun handleProperty(property: KProperty1<PropertyHolder, *>, value: Any?): List<String> {
        val properties = mutableListOf<String>()

        if (value == null) {
            properties.add("null")
            return properties
        }

        when (property.returnType.classifier) {
            String::class -> {
                if (value == "Not Set") {
                    properties.add("null")
                    return properties
                }
                properties.add(quoteIfNeeded(value as String))
            }

            StatValue::class -> {
                when (value) {
                    is StatValue.Str -> properties.add("\"${value.value.replace("\"", "\\\"")}\"")
                    is StatValue.UnquotedStr -> properties.add(quoteIfNeeded(value.value))
                    else -> properties.add(value.toString())
                }
            }

            Int::class, Double::class, Long::class, Boolean::class -> {
                properties.add(value.toString())
            }

            StatOp::class -> {
                val statOp = value as StatOp
                when (statOp) {
                    StatOp.Inc -> properties.add("+=")
                    StatOp.Dec -> properties.add("-=")
                    StatOp.Set -> properties.add("=")
                    StatOp.Mul -> properties.add("*=")
                    StatOp.Div -> properties.add("/=")
                    StatOp.BitAnd -> properties.add("&=")
                    StatOp.BitOr -> properties.add("|=")
                    StatOp.BitXor -> properties.add("^=")
                    StatOp.LS -> properties.add("<<=")
                    StatOp.ARS -> properties.add(">>=")
                    StatOp.LRS -> properties.add(">>>=")
                    StatOp.UnSet -> properties.add("unset")
                }
            }

            Location::class -> {
                val location = value as Location
                if (location !is Location.Custom) {
                    properties.add("\"${location.key}\"")
                } else {
                    properties.add("\"custom_coordinates\" \"$location\"")
                }
            }

            Comparison::class -> {
                val comparison = value as Comparison
                when (comparison) {
                    Comparison.Eq -> properties.add("==")
                    Comparison.Gt -> properties.add(">")
                    Comparison.Lt -> properties.add("<")
                    Comparison.Ge -> properties.add(">=")
                    Comparison.Le -> properties.add("<=")
                }
            }

            InventorySlot::class -> {
                val inventorySlot = value as InventorySlot
                properties.add("\"${inventorySlot.key}\"")
            }

            ItemStack::class -> {
                val itemStack = value as ItemStack
                properties.add(
                    "\"${itemStack.nbt.toString().replace("\"", "\\\"")}\""
                )
            }

            else -> {
                if (property.returnType.isSubtypeOf(Keyed::class.starProjectedType.withNullability(true))) {
                    val keyed = value as Keyed
                    if (keyed::class.hasAnnotation<CustomKey>()) {
                        properties.add(keyed.toString())
                    } else if (keyed is KeyedLabeled) {
                        properties.add("\"${keyed.label}\"")
                    } else {
                        properties.add("\"${keyed.key}\"")
                    }
                } else {
                    properties.add(value.toString()) //More than likely null
                }
            }
        }

        return properties
    }

    @Suppress("UNCHECKED_CAST")
    fun export(actions: List<Action>): List<String> {
        val lines = mutableListOf<String>()
        for (action in actions) {
            if (action is Action.Conditional) {
                val exportedConditions = exportConditions(action.conditions)
                lines.add("if${if (action.matchAnyCondition) " or" else ""} (${exportedConditions.joinToString(", ")}) {")
                val exportedActions = export(action.ifActions)
                lines.addAll(exportedActions.map { "    $it" })
                if (action.elseActions.isNotEmpty()) {
                    lines.add("} else {")
                    val exportedElseActions = export(action.elseActions)
                    lines.addAll(exportedElseActions.map { "    $it" })
                }
                lines.add("}")
                continue
            }

            if (action is Action.RandomAction) {
                lines.add("random {")
                val exportedActions = export(action.actions)
                lines.addAll(exportedActions.map { "    $it" })
                lines.add("}")
                continue
            }

            val actionClass = action::class
            val newActionProperties = orderedActionProperties(actionClass)
            val keyword = actionKeywords[actionClass] ?: continue
            val properties = mutableListOf<String>()

            for (property in newActionProperties) {
                if (property.name == "actionName") continue
                val value = property.get(action)
                properties.addAll(handleProperty(property as KProperty1<PropertyHolder, *>, value))
                if (value == StatOp.UnSet) break
            }

            val line = if (properties.isNotEmpty()) {
                "$keyword ${properties.joinToString(" ")}"
            } else {
                keyword
            }
            lines.add(line)
        }
        return lines
    }

    @Suppress("UNCHECKED_CAST")
    fun exportConditions(conditions: List<Condition>): List<String> {
        val conditionStrings = mutableListOf<String>()
        for (condition in conditions) {
            val conditionClass = condition::class
            val newConditionProperties = orderedConditionProperties(conditionClass)
            val keyword = conditionKeywords[conditionClass] ?: continue
            val properties = mutableListOf<String>()

            for (property in newConditionProperties) {
                if (property.name == "conditionName" || property.name == "inverted") continue
                val value = property.get(condition)
                properties.addAll(handleProperty(property as KProperty1<PropertyHolder, *>, value))
            }

            val conditionString = "${if (condition.inverted) "!" else ""}${
                if (properties.isNotEmpty()) {
                    "$keyword ${properties.joinToString(" ")}"
                } else {
                    keyword
                }
            }"
            conditionStrings.add(conditionString)
        }
        return conditionStrings
    }
}
