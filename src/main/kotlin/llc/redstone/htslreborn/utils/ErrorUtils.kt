package llc.redstone.htslreborn.utils

import org.antlr.v4.kotlinruntime.*


object ErrorUtils {
    fun htslCompileError(message: String, token: ParserRuleContext): Nothing {
        val errorMessage =
            "HTSL Compile Error at line ${token.position?.start?.line}, column ${token.position?.start?.column}\n- $message"
        throw HTSLCompileException(errorMessage)
    }

    class HTSLCompileException(message: String) : Exception(message) {
        override fun printStackTrace() {
            // Don't print stack trace for compile errors to avoid spamming the console with irrelevant information
        }
    }

    /**
     * ANTLR's default listener just prints to stderr and lets the parser recover.
     * This one throws instead, so a syntax error aborts compilation immediately.
     */
    object ThrowingErrorListener : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ): Nothing {
            throw HTSLCompileException("HTSL Syntax Error at line $line, column $charPositionInLine\n- $msg")
        }
    }

    /** Swap the default console listeners on a lexer/parser for [ThrowingErrorListener]. */
    fun <T : Lexer> T.throwOnError(): T = apply {
        removeErrorListeners()
        addErrorListener(ThrowingErrorListener)
    }

    fun <T : Parser> T.throwOnError(): T = apply {
        removeErrorListeners()
        addErrorListener(ThrowingErrorListener)
    }
}
