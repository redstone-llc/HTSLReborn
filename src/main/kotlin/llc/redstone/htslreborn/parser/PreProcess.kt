package llc.redstone.htslreborn.parser

import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import org.mozilla.javascript.Context
import kotlin.toString

object PreProcess {
    fun preProcess(tokens: List<TokenWithPosition>, loopVarName: String? = null, loopIndex: Int? = null): List<TokenWithPosition> {
        val processedTokens = mutableListOf<TokenWithPosition>()

        val context = Context.enter();
        val scope = context.initStandardObjects()
        if (loopVarName != null) {
            val index = loopIndex ?: 0
            scope.put(loopVarName, scope, index)
        }

        val iterator = tokens.iterator()

        var loopAmount = 0
        var loopVarName: String? = null
        while (iterator.hasNext()) {
            val token = iterator.next()
            when (token.tokenType) {
                Tokens.JS_CODE -> {
                    val result = context.evaluateString(scope, token.string, "HTSL_JS_EVAL", 1, null)
                    val resultString = when (result) {
                        is String -> "\"$result\"" // Wrap strings in quotes
                        else -> result.toString()
                    }

                    processedTokens.addAll(Tokenizer.tokenize(resultString))
                }

                Tokens.LOOP_KEYWORD -> {
                    var nextToken = iterator.takeIf { it.hasNext() }?.next()
                    if (nextToken?.tokenType != Tokens.INT) continue
                    loopAmount = nextToken.string.toIntOrNull() ?: 1

                    nextToken = iterator.takeIf { it.hasNext() }?.next()
                    if (nextToken?.tokenType != Tokens.STRING) continue
                    loopVarName = nextToken.string
                    nextToken = iterator.takeIf { it.hasNext() }?.next()
                    if (nextToken?.tokenType != Tokens.DEPTH_ADD) continue
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
                        processedTokens.addAll(preProcess(tokens, loopVarName, it))
                    }
                }

                else -> processedTokens.add(token)
            }
        }

        return processedTokens
    }
}