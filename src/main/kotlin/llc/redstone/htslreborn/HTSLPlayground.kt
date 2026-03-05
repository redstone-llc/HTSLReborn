package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer

// Used primarily for testing the tokenizer and preprocessor
fun main(args: Array<String>) {
    val input = """
        kill
     """.trimIndent()

    val tokens = Tokenizer.tokenize(input)
    println("Tokens:")
    tokens.forEach { println("${it.tokenType} -> ${it.string}") }

    val preProcessedTokens = PreProcess.preProcess(tokens)
    println("\nPre-Processed Tokens:")
    preProcessedTokens.forEach { println("${it.tokenType} -> ${it.string}") }
}