package llc.redstone.htslreborn.tokenizer

import guru.zoroark.tegral.niwen.lexer.StateBuilder
import guru.zoroark.tegral.niwen.lexer.StateLabel
import guru.zoroark.tegral.niwen.lexer.TokenType
import guru.zoroark.tegral.niwen.lexer.matchers.TokenRecognizer
import guru.zoroark.tegral.niwen.lexer.matchers.anyOf
import guru.zoroark.tegral.niwen.lexer.matchers.matches

enum class Tokens: TokenType {
    ACTION_KEYWORD,
    CONDITION_KEYWORD,
    INVERTED,
    INT,
    LONG,
    DOUBLE,
    STRING,
    PLACEHOLDER_STRING,
    QUOTE,
    PLACEHOLDER,
    COMMENT,
    NEWLINE,
    BOOLEAN,
    IF_AND_CONDITION_START,
    IF_OR_CONDITION_START,
    RANDOM_KEYWORD,
    GOTO_KEYWORD,
    IF_CONDITION_END,
    DEPTH_ADD,
    DEPTH_SUBTRACT,
    ELSE_KEYWORD,
    COMMA,
    BRACE_OPEN,
    BRACE_CLOSE,
    JS_CODE,
    LOOP_KEYWORD,
    DEFINE_KEYWORD,
    DEFINE_VALUE,
    NULL
    ;
}

enum class Operators: TokenType {
    UNSET,
    INCREMENT,
    DECREMENT,
    SET,
    MULTIPLY,
    DIVIDE,
    BITWISE_AND,
    BITWISE_OR,
    BITWISE_XOR,
    LEFT_SHIFT,
    ARITHMETIC_RIGHT_SHIFT,
    LOGICAL_RIGHT_SHIFT
}

fun StateBuilder.operatorTokens() {
    word("unset") isToken Operators.UNSET
    "increment" isToken Operators.INCREMENT
    "decrement" isToken Operators.DECREMENT
    "multiply" isToken Operators.MULTIPLY
    "divide" isToken Operators.DIVIDE
    word("inc") isToken Operators.INCREMENT
    "+=" isToken Operators.INCREMENT
    word("dec") isToken Operators.DECREMENT
    "-=" isToken Operators.DECREMENT
    word("set") isToken Operators.SET
    "=" isToken Operators.SET
    word("mult") isToken Operators.MULTIPLY
    "*=" isToken Operators.MULTIPLY
    word("div") isToken Operators.DIVIDE
    "/=" isToken Operators.DIVIDE
    word("and") isToken Operators.BITWISE_AND
    "&=" isToken Operators.BITWISE_AND
    word("xor") isToken Operators.BITWISE_XOR
    "^=" isToken Operators.BITWISE_XOR
    word("or") isToken Operators.BITWISE_OR
    "|=" isToken Operators.BITWISE_OR
    word("shl") isToken Operators.LEFT_SHIFT
    word("shr") isToken Operators.ARITHMETIC_RIGHT_SHIFT
    word("lshr") isToken Operators.LOGICAL_RIGHT_SHIFT
    anyOf("leftShift", "<<=") isToken Operators.LEFT_SHIFT
    anyOf("arithmeticRightShift", ">>=") isToken Operators.ARITHMETIC_RIGHT_SHIFT
    anyOf("logicalRightShift", ">>>=") isToken Operators.LOGICAL_RIGHT_SHIFT
}

enum class Comparators: TokenType {
    EQUALS,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
}

fun StateBuilder.comparatorTokens() {
    anyOf("==", "equals") isToken Comparators.EQUALS
    anyOf("<=", "lessThanOrEquals") isToken Comparators.LESS_THAN_OR_EQUAL
    anyOf("<", "lessThan") isToken Comparators.LESS_THAN
    anyOf(">=", "greaterThanOrEquals") isToken Comparators.GREATER_THAN_OR_EQUAL
    anyOf(">", "greaterThan") isToken Comparators.GREATER_THAN
}

enum class States: StateLabel {
    IN_STRING,
    IN_CONDITION_STRING,
    PLACEHOLDER,
    PLACEHOLDER_CONDITION,
    IF_CONDITION,
    JS_INTERPRETER,
    JS_INTERPRETER_CONDITION,
    DEFINE,
    IN_MULTI_LINE_COMMENT
}

fun StateBuilder.word(word: String): TokenRecognizer {
    return matches("\b$word\b")
}