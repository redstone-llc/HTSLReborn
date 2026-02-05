package llc.redstone.htslreborn.tokenizer

import llc.redstone.htslreborn.tokenizer.States.*
import guru.zoroark.tegral.niwen.lexer.StateLabel
import guru.zoroark.tegral.niwen.lexer.Token
import guru.zoroark.tegral.niwen.lexer.matchers.anyOf
import guru.zoroark.tegral.niwen.lexer.matchers.matches
import guru.zoroark.tegral.niwen.lexer.niwenLexer
import llc.redstone.htslreborn.parser.ActionParser
import llc.redstone.htslreborn.parser.ConditionParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readLines
import kotlin.io.path.readText

object Tokenizer {
    fun tokenize(text: String): List<TokenWithPosition> {
        val actionKeywords = ActionParser.keywords.keys
        val conditionKeywords = ConditionParser.keywords.keys

        val lexer = niwenLexer {
            default state {
                anyOf(*actionKeywords.toTypedArray()) isToken Tokens.ACTION_KEYWORD

                "{\n" isToken Tokens.DEPTH_ADD

                anyOf("} else {", "}else{", "}else {", "} else{") isToken Tokens.ELSE_KEYWORD
                "loop" isToken Tokens.LOOP_KEYWORD
                "}" isToken Tokens.DEPTH_SUBTRACT
                "random" isToken Tokens.RANDOM_KEYWORD
                "goto" isToken Tokens.GOTO_KEYWORD

                matches("if( (and|false))?\\s*\\(") isToken Tokens.IF_AND_CONDITION_START thenState IF_CONDITION
                matches("if (or|true)\\s*\\(") isToken Tokens.IF_OR_CONDITION_START thenState IF_CONDITION

                comparatorTokens()
                operatorTokens()

                anyOf("true", "false") isToken Tokens.BOOLEAN

                '\n' isToken Tokens.NEWLINE

                matches("//.*") isToken Tokens.COMMENT
                "/*" isToken Tokens.COMMENT thenState IN_MULTI_LINE_COMMENT
                matches("-?\\d+\\.\\d+") isToken Tokens.DOUBLE
                matches("-?\\d+?L") isToken Tokens.LONG
                matches("-?\\d+") isToken Tokens.INT
                matches("\\s+").ignore

                '{' isToken Tokens.BRACE_OPEN thenState JS_INTERPRETER
                '\"' isToken Tokens.QUOTE thenState IN_STRING
                '%' isToken Tokens.PLACEHOLDER thenState PLACEHOLDER

                matches("""define """) isToken Tokens.DEFINE_KEYWORD thenState DEFINE

                matches("[^\\s(){}%\",]+") isToken Tokens.STRING

            }

            IN_MULTI_LINE_COMMENT state {
                matches("([^*]|(\\*+[^*/]))+") isToken Tokens.COMMENT
                "*/" isToken Tokens.COMMENT thenState default
            }

            IF_CONDITION state {
                anyOf(*conditionKeywords.toTypedArray()) isToken Tokens.CONDITION_KEYWORD

                comparatorTokens()
                operatorTokens()

                anyOf("true", "false") isToken Tokens.BOOLEAN
                anyOf(",", ", ") isToken Tokens.COMMA

                matches("//.*") isToken Tokens.COMMENT
                matches("-?\\d+\\.\\d+") isToken Tokens.DOUBLE
                matches("-?\\d+?L") isToken Tokens.LONG
                matches("-?\\d+") isToken Tokens.INT
                matches("\\s+").ignore

                '!' isToken Tokens.INVERTED

                '\"' isToken Tokens.QUOTE thenState IN_CONDITION_STRING
                '%' isToken Tokens.PLACEHOLDER thenState PLACEHOLDER_CONDITION
                '{' isToken Tokens.BRACE_OPEN thenState JS_INTERPRETER_CONDITION

                matches("[^\\s(){}%\",]+") isToken Tokens.STRING

                ')' isToken Tokens.IF_CONDITION_END thenState default
            }

            JS_INTERPRETER state {
                matches("[^}]+") isToken Tokens.JS_CODE
                '}' isToken Tokens.BRACE_CLOSE thenState default
            }

            JS_INTERPRETER_CONDITION state {
                matches("[^}]+") isToken Tokens.JS_CODE
                '}' isToken Tokens.BRACE_CLOSE thenState IF_CONDITION
            }

            DEFINE state {
                matches(".+(?=\\n)") isToken Tokens.DEFINE_VALUE
                '\n' isToken Tokens.NEWLINE thenState default
            }

            fun stringState(state: StateLabel, nextState: StateLabel?) {
                state state {
                    matches("""(\\"|[^\\"])+""") isToken Tokens.STRING
                    if (nextState != null) {
                        '\"' isToken Tokens.QUOTE thenState nextState
                    } else {
                        '\"' isToken Tokens.QUOTE thenState default
                    }
                }
            }

            fun placeholderStringState(state: StateLabel, nextState: StateLabel?) {
                state state {
                    matches("""[\w./ ]+""") isToken Tokens.STRING
                    if (nextState != null) {
                        "%" isToken Tokens.PLACEHOLDER thenState nextState
                    } else {
                        "%" isToken Tokens.PLACEHOLDER thenState default
                    }
                }
            }

            stringState(IN_STRING, null)
            stringState(IN_CONDITION_STRING, IF_CONDITION)

            placeholderStringState(PLACEHOLDER, null)
            placeholderStringState(PLACEHOLDER_CONDITION, IF_CONDITION)
        }
        return lexer.tokenize(text)
            .filter {
                it.tokenType != Tokens.QUOTE &&
                        it.tokenType != Tokens.BRACE_OPEN &&
                        it.tokenType != Tokens.BRACE_CLOSE &&
                        it.tokenType != Tokens.COMMENT
            } //Filter out unused and wasted tokens
//            .filter { it.tokenType != Tokens.NEWLINE }
            .map { token ->
                TokenWithPosition(
                    token,
                    text.take(token.startsAt).count { it == '\n' } + 1,
                    token.startsAt - text.lastIndexOf('\n', token.startsAt - 1)
                )
            }
    }


    fun tokenize(path: Path): List<TokenWithPosition> {
        val input = path.readLines().joinToString("\n")
        return tokenize(input)
    }

    class TokenWithPosition(
        val token: Token,
        val line: Int,
        val column: Int
    ) {
        val string = token.string
        val endsAt = token.endsAt
        val startsAt = token.startsAt
        val tokenType = token.tokenType
    }
}