package llc.redstone.htslreborn.parser

import com.strumenta.antlrkotlin.parsers.generated.HTSLLexer
import com.strumenta.antlrkotlin.parsers.generated.HTSLParser
import com.strumenta.antlrkotlin.parsers.generated.HTSLParser.*
import llc.redstone.htslreborn.data.*
import llc.redstone.htslreborn.parser.ast.HtslAstBuilder
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import llc.redstone.htslreborn.utils.ErrorUtils.throwOnError
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.mozilla.javascript.Context
import java.nio.file.Files
import java.nio.file.Path

class PreProcessor(
    private val includedFiles: MutableSet<Path> = mutableSetOf(),
) {
    val placeholderShortcuts = mapOf(
        "var $" to "|var.player/%s|",
        "stat $" to "|var.player/%s|",
        "globalvar $" to "|var.global/%s|",
        "globalstat $" to "|var.global/%s|",
        "teamvar $ $" to "|var.team/%s %s|",
        "teamstat $ $" to "|var.team/%s %s|",
        "randomint $ $" to "|random.int/%s %s|",
        "health" to "|player.health|",
        "maxhealth" to "|player.maxhealth|",
        "hunger" to "|player.hunger|",
        "locX" to "|player.pos.x|",
        "locY" to "|player.pos.y|",
        "locZ" to "|player.pos.z|",
        "unix" to "|date.unix%"
    )
    val definedReplacements = mutableMapOf<String, List<ArgumentContext>>()
    var loopVarName: String? = null
    var loopIndex: Int? = null
    val context = Context.enter()!!
    val scope = context.initSafeStandardObjects()!!

    private fun processIfStatement(ifStatement: HTSLParser.IfStatementContext, path: Path): Action.Conditional {
        val conditions = preprocessConditions(ifStatement.conditionStatement(), path)
        val logicOperator = listOf("or", "true").contains(ifStatement.logicOperator()?.text)
        val ifBlock = ifStatement.block(0)!!.statement().flatMap { requireNestedActions(it, path) }
        val elseBlock = ifStatement.block(1)?.statement()?.flatMap { requireNestedActions(it, path) }
        return Action.Conditional(conditions, logicOperator, ifBlock, elseBlock ?: emptyList())
    }

    fun preprocessProgram(statements: List<StatementContext>, path: Path): List<ScriptContainer> {
        val containers = mutableListOf<ScriptContainer>()
        var context = ImportContext.DEFAULT
        var target = ContextTarget()
        val actions = mutableListOf<Action>()

        fun flush() {
            // Files that start with `goto` would otherwise emit an empty DEFAULT container.
            if (context == ImportContext.DEFAULT && actions.isEmpty()) return
            containers.add(ScriptContainer(context, target, actions.toList()))
            actions.clear()
        }

        for (unit in statements.flatMap { preprocessStatement(it, path) }) {
            when (unit) {
                is PreprocessedUnit.ActionUnit -> actions.add(unit.action)
                is PreprocessedUnit.GotoUnit -> {
                    flush()
                    context = unit.context
                    target = unit.target
                }

                is PreprocessedUnit.IncludedContainers -> {
                    flush()
                    containers.addAll(unit.containers)
                }
            }
        }
        flush()
        return containers
    }

    fun preprocessConditions(conditions: List<ConditionStatementContext>, path: Path): List<Condition> {
        return conditions.map { condition ->
            val inverted = condition.text.startsWith("!")
            var keyword = condition.IDENTIFIER().text
            var args = processArguments(condition.argument())
            if (definedReplacements[keyword] != null) {
                val replacement = definedReplacements[keyword]!!
                keyword = replacement.firstOrNull()?.text ?: keyword
                args = replacement.drop(1) + args
            }
            ConditionParser.parse(keyword, condition, args, inverted, path) ?: htslCompileError(
                "Unknown condition: $keyword",
                condition
            )
        }
    }

    private fun preprocessStatement(statement: StatementContext, path: Path): List<PreprocessedUnit> {
        if (statement.actionStatement() != null) {
            val actionStatement = statement.actionStatement()!!
            var keyword = actionStatement.IDENTIFIER().text
            var args = processArguments(actionStatement.argument())
            if (definedReplacements[keyword] != null) {
                val replacement = definedReplacements[keyword]!!
                keyword = replacement.firstOrNull()?.text ?: keyword
                args = replacement.drop(1) + args
            }
            return listOf(PreprocessedUnit.ActionUnit(ActionParser.parse(keyword, actionStatement, args, path)))
        }

        if (statement.ifStatement() != null) {
            return listOf(PreprocessedUnit.ActionUnit(processIfStatement(statement.ifStatement()!!, path)))
        }

        if (statement.jsCodeStatement() != null) {
            val jsCode = statement.jsCodeStatement()!!.text
            handleJsCode(jsCode, statement.jsCodeStatement()!!)
            return emptyList()
        }

        if (statement.randomStatement() != null) {
            val actions = statement.randomStatement()!!.block().statement().flatMap { requireNestedActions(it, path) }
            return listOf(PreprocessedUnit.ActionUnit(Action.RandomAction(actions)))
        }

        if (statement.defineStatment() != null) {
            val defineStatement = statement.defineStatment()!!
            val name = defineStatement.IDENTIFIER().text
            val value = defineStatement.argument()
            definedReplacements[name] = value
            return emptyList()
        }

        if (statement.loopStatement() != null) {
            val loopStatement = statement.loopStatement()!!
            val loopCount = loopStatement.NUMBER().text.toInt()
            val loopVar = loopStatement.IDENTIFIER().text

            val units = mutableListOf<PreprocessedUnit>()
            for (i in 0 until loopCount) {
                loopVarName = loopVar
                loopIndex = i
                units.addAll(loopStatement.block().statement().flatMap { preprocessStatement(it, path) })
            }
            loopVarName = null
            loopIndex = null

            return units
        }

        if (statement.gotoStatement() != null) {
            return listOf(parseGoto(statement.gotoStatement()!!, path))
        }

        htslCompileError("Unknown statement type", statement)
    }

    private fun requireNestedActions(statement: StatementContext, path: Path): List<Action> {
        return preprocessStatement(statement, path).map { unit ->
            when (unit) {
                is PreprocessedUnit.ActionUnit -> unit.action
                else -> htslCompileError("goto cannot be used inside if or random blocks", statement)
            }
        }
    }

    private fun parseGoto(gotoStatement: GotoStatementContext, path: Path): PreprocessedUnit {
        val contextKeyword = gotoStatement.IDENTIFIER().text
        val context = ImportContext.fromKeyword(contextKeyword)
            ?: htslCompileError(
                "Unknown goto context: $contextKeyword. Expected function, event, command, region, npc, gui, menu, or custommenu",
                gotoStatement
            )

        val rawArgs = processArguments(gotoStatement.argument())
        val asIndex = rawArgs.indexOfLast { isAsKeyword(it) }
        val fileArg: ArgumentContext?
        val args: List<ArgumentContext>
        if (asIndex >= 0 && asIndex == rawArgs.lastIndex - 1) {
            fileArg = rawArgs.last()
            args = rawArgs.dropLast(2)
        } else {
            fileArg = null
            args = rawArgs
        }

        val name = args.getOrNull(0)?.let { unquoteArgument(it) }
            ?: htslCompileError("goto $contextKeyword requires a name", gotoStatement)
        if (name.isBlank()) {
            htslCompileError("goto $contextKeyword requires a name", gotoStatement)
        }
        if (args.size > 2) {
            htslCompileError("Too many arguments for goto $contextKeyword", gotoStatement)
        }
        val trigger = args.getOrNull(1)?.let { unquoteArgument(it) }
        val target = ContextTarget(name = name, trigger = trigger)

        if (fileArg != null) {
            val fileName = unquoteArgument(fileArg)
            if (fileName.isBlank()) {
                htslCompileError("goto ... as <file> requires a file name", gotoStatement)
            }
            val nested = includeHtslFile(fileName, gotoStatement, path).toMutableList()
            if (nested.isNotEmpty()) {
                nested[0] = nested[0].copy(context = context, target = target)
            }
            return PreprocessedUnit.IncludedContainers(nested)
        }

        return PreprocessedUnit.GotoUnit(context, target)
    }

    private fun includeHtslFile(fileName: String, statement: ParserRuleContext, path: Path): List<ScriptContainer> {
        val directory = if (Files.isDirectory(path)) path else path.parent
            ?: htslCompileError("Couldn't find the file \"$fileName\", please make sure it exists!", statement)
        val resolved = resolveHtslFile(directory, fileName)
            ?: htslCompileError("Couldn't find the file \"$fileName\", please make sure it exists!", statement)
        val normalized = resolved.toAbsolutePath().normalize()
        if (normalized in includedFiles) {
            htslCompileError("Nested file calls detected", statement)
        }
        return HtslAstBuilder.parseFile(normalized, includedFiles)
    }

    private fun resolveHtslFile(directory: Path, fileName: String): Path? {
        val withExtension = if (fileName.endsWith(".htsl", ignoreCase = true)) fileName else "$fileName.htsl"
        val candidates = listOf(
            directory.resolve(withExtension),
            directory.resolve(fileName),
        )
        return candidates.firstOrNull { Files.isRegularFile(it) }
    }

    private fun unquoteArgument(arg: ArgumentContext): String {
        val raw = arg.text
        return if (raw.length >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            raw.substring(1, raw.length - 1)
        } else {
            raw
        }
    }

    private fun isAsKeyword(arg: ArgumentContext): Boolean {
        return arg.IDENTIFIER() != null && arg.text.equals("as", ignoreCase = true)
    }

    private sealed interface PreprocessedUnit {
        data class ActionUnit(val action: Action) : PreprocessedUnit
        data class GotoUnit(val context: ImportContext, val target: ContextTarget) : PreprocessedUnit
        data class IncludedContainers(val containers: List<ScriptContainer>) : PreprocessedUnit
    }

    private fun processArguments(arguments: List<ArgumentContext>): List<ArgumentContext> {
        val newArguments = mutableListOf<ArgumentContext>()
        val iterator = arguments.listIterator()
        while (iterator.hasNext()) {
            val arg = iterator.next()
            when {
                (Operator.fromSymbol(arg.text) != null) -> {
                    newArguments.add(arg)
                    handlePlaceholderShortcuts(iterator, newArguments)
                    continue
                }

                (Comparator.fromSymbol(arg.text) != null) -> {
                    newArguments.add(arg)
                    handlePlaceholderShortcuts(iterator, newArguments)
                    continue
                }

                arg.IDENTIFIER() != null -> {
                    val replacement = definedReplacements[arg.text]
                    if (replacement != null) {
                        newArguments.addAll(replacement)
                    } else {
                        newArguments.add(arg)
                    }
                }

                arg.jsCodeStatement() != null -> {
                    val jsCode = arg.jsCodeStatement()?.text ?: ""
                    val result = handleJsCode(jsCode, arg.jsCodeStatement()!!)
                    if (result != null) {
                        newArguments.add(result)
                    } else {
                        newArguments.add(arg)
                    }
                }

                else -> {
                    newArguments.add(arg)
                }
            }
        }

        return newArguments
    }

    private fun handlePlaceholderShortcuts(
        iterator: ListIterator<ArgumentContext>,
        newArguments: MutableList<ArgumentContext>
    ) {
        //Placeholder shortcuts
        if (iterator.hasNext()) {
            val nextArg = iterator.next()
            if (isPlaceholderShortcut(nextArg)) {
                val shortcut = placeholderShortcuts.entries.first { it.key.startsWith(nextArg.text) }


                val argCount = shortcut.key.count { it == '$' }
                val args = mutableListOf<ArgumentContext>()
                for (i in 1..argCount) {
                    if (iterator.hasNext()) {
                        val nextArg = iterator.next()
                        args.add(nextArg)
                    }
                }

                if (args.size < argCount) {
                    newArguments.add(nextArg)
                    newArguments.addAll(args)
                } else {
                    val preProcessed = processArguments(args)
                    String.format(shortcut.value, *preProcessed.map { it.text }.toTypedArray()).let { formatted ->
                        newArguments.add(parseTextToArgument(formatted.replace("|", "%")))
                    }
                }
            } else {
                iterator.previous()
            }
        }
    }

    private fun parseTextToArgument(text: String): ArgumentContext {
        val lexer = HTSLLexer(CharStreams.fromString(text)).throwOnError()
        val tokens = CommonTokenStream(lexer)
        val parser = HTSLParser(tokens).throwOnError()
        val argumentContext = parser.argument()

        return argumentContext
    }

    private fun isPlaceholderShortcut(arg: ArgumentContext): Boolean {
        if (arg.IDENTIFIER() == null) return false
        return placeholderShortcuts.keys.any { shortcut -> shortcut.startsWith(arg.text) }
    }

    private fun handleJsCode(code: String, statement: JsCodeStatementContext): ArgumentContext? {
        var code = code

        for ((key, value) in definedReplacements) {
            val regex = Regex("(?<!\")\\b$key\\b(?!\")") // Match whole words not inside quotes
            code = code.replace(regex, value.joinToString(" ") { it.text })
        }
        if (loopVarName != null && loopIndex != null) {
            val regex = Regex("(?<!\")\\b$loopVarName\\b(?!\")") // Match whole words not inside quotes
            code = code.replace(regex, loopIndex.toString())
        }
        try {
            var result = context.evaluateString(scope, code, "script", 1, null)

            if (result is String && result.contains(" ") && !result.contains("%")) {
                result = "\"$result\""
            }
            return parseTextToArgument(result.toString())
        } catch (e: Exception) {
            htslCompileError("Error evaluating JS code: ${e.message}", statement)
        }
    }
}