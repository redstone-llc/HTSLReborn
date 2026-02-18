package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.LocationParser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.tokenizer.Tokens

// Used primarily for testing the tokenizer, preprocessor, and parser
fun main(args: Array<String>) {
    val input = """
        stat kills inc 1
    """.trimIndent()

    val tokens = Tokenizer.tokenize(input)
    println("Tokens:")
    tokens.forEach { println("${it.tokenType} -> ${it.string}") }

    val preProcessedTokens = PreProcess.preProcess(tokens)
    println("\nPre-Processed Tokens:")
    preProcessedTokens.forEach { println("${it.tokenType} -> ${it.string}") }
}