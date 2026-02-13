package llc.redstone.htslreborn.parser

import guru.zoroark.tegral.niwen.lexer.Token
import llc.redstone.htslreborn.HTSLReborn.CONFIG
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject

object PreProcess {
    fun preProcess(
        tokens: List<TokenWithPosition>,
        loopVarName: String? = null,
        loopIndex: Int? = null,
        defineList: MutableMap<String, String> = mutableMapOf(),
        reusedContext: Context? = null,
        reusedScope: ScriptableObject? = null
    ): List<TokenWithPosition> {
        val processedTokens = mutableListOf<TokenWithPosition>()

        val context = reusedContext ?: Context.enter()
        val scope = reusedScope ?: if (CONFIG.disablesJSSandboxing) context.initStandardObjects() else context.initSafeStandardObjects()
        if (loopVarName != null) {
            val index = loopIndex ?: 0
            scope.put(loopVarName, scope, index)
        }

        val iterator = tokens.iterator()

        var loopAmount: Int
        var loopVarName: String? = loopVarName
        while (iterator.hasNext()) {
            val token = iterator.next()
            when (token.tokenType) {
                Tokens.PLACEHOLDER -> {
                    val valueToken = iterator.takeIf { it.hasNext() }?.next()
                    if (valueToken?.tokenType != Tokens.STRING) htslCompileError(
                        "Expected placeholder name after '%'",
                        valueToken ?: token
                    )
                    val placeholder = valueToken.string

                    val closingToken = iterator.takeIf { it.hasNext() }?.next()
                    if (closingToken?.tokenType != Tokens.PLACEHOLDER) htslCompileError(
                        "Expected closing '%' for placeholder '$placeholder'",
                        closingToken ?: token
                    )

                    processedTokens.add(
                        TokenWithPosition(
                            Token(
                                "%$placeholder%",
                                token.startsAt,
                                token.endsAt,
                                Tokens.STRING,
                            ),
                            token.line,
                            token.column
                        )
                    )
                }
                Tokens.JS_CODE -> {
                    // Replace defined variables
                    var processedString = token.string
                    for ((key, value) in defineList) {
                        val regex = Regex("(?<!\")\\b$key\\b(?!\")") // Match whole words not inside quotes
                        processedString = processedString.replace(regex, value)
                    }

                    val result = context.evaluateString(scope, processedString, "HTSL_JS_EVAL", token.line, null)
                    val resultString = when (result) {
                        is String -> "\"$result\"" // Wrap strings in quotes
                        else -> result.toString()
                    }

                    processedTokens.addAll(Tokenizer.tokenize(resultString))
                }

                Tokens.DEFINE_KEYWORD -> {
                    val valueToken = iterator.takeIf { it.hasNext() }?.next()
                    if (valueToken?.tokenType != Tokens.DEFINE_VALUE) htslCompileError(
                        "Expected value after variable name",
                        valueToken ?: token
                    )
                    val varName = valueToken.string.split(" ")[0]
                    if (listOf("goto", "//", "/*", "*/", "loop").contains(varName)) {
                        htslCompileError("Invalid variable name '$varName'", token)
                    }
                    val value = valueToken.string.substringAfter(" ")

                    defineList[varName] = value
                }

                Tokens.LOOP_KEYWORD -> {
                    var nextToken = iterator.takeIf { it.hasNext() }?.next()
                    if (nextToken?.tokenType != Tokens.INT) htslCompileError(
                        "Expected integer after 'loop' keyword",
                        nextToken ?: token
                    )
                    loopAmount = nextToken.string.toIntOrNull() ?: 1

                    nextToken = iterator.takeIf { it.hasNext() }?.next()
                    if (nextToken?.tokenType != Tokens.STRING) htslCompileError(
                        "Expected loop variable name after loop amount",
                        nextToken ?: token
                    )
                    loopVarName = nextToken.string
                    nextToken = iterator.takeIf { it.hasNext() }?.next()
                    if (nextToken?.tokenType != Tokens.DEPTH_ADD) htslCompileError(
                        "Expected '{' after loop variable name",
                        nextToken ?: token
                    )
                    if (loopAmount <= 0) continue

                    var depth = 1
                    val tokens = mutableListOf<TokenWithPosition>()

                    while (iterator.hasNext()) {
                        val loopToken = iterator.next()
                        when (loopToken.tokenType) {
                            Tokens.DEPTH_ADD -> depth++
                            Tokens.DEPTH_SUBTRACT -> if (--depth == 0) break
                        }
                        tokens.add(loopToken)
                    }

                    repeat(loopAmount) {
                        processedTokens.addAll(preProcess(tokens, loopVarName, it, defineList, context, scope))
                    }
                }

                Tokens.STRING -> {
                    var processedString = token.string
                    if (processedString == "\"\"") {
                        processedTokens.add(
                            TokenWithPosition(
                                Token(
                                    "",
                                    token.startsAt,
                                    token.endsAt,
                                    Tokens.STRING,
                                ),
                                token.line,
                                token.column
                            )
                        )
                        continue
                    }

                    for ((key, value) in defineList) {
                        val regex = Regex("(?<!\")\\b$key\\b(?!\")") // Match whole words not inside quotes
                        processedString = processedString.replace(regex, value)
                    }
                    val loopVarNameRegex = loopVarName?.let { Regex("(?<!\")\\b$it\\b(?!\")") }
                    if (loopVarNameRegex != null) {
                        processedString = processedString.replace(loopVarNameRegex, loopIndex.toString())
                    }

                    if (processedString == token.string) {
                        processedTokens.add(token)
                        continue
                    }

                    processedTokens.addAll(Tokenizer.tokenize(processedString))
                }

                else -> {
                    processedTokens.add(token)
                }
            }
        }

        return processedTokens
    }
}