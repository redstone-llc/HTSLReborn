package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer

// Used primarily for testing the tokenizer, preprocessor, and parser
fun main(args: Array<String>) {
    val input = """
define MACRO stat Kills
MACRO inc 10
loop 2 index {
    MACRO mult 10
    MACRO inc index
}

""".trimIndent()
    println("---- Tokenization ----")
    val tokens = Tokenizer.tokenize(input)
    for (token in tokens) {
        println("${token.tokenType} -> '${token.string}'")
    }
    println("---- Preprocessing ----")
    val preparsed = PreProcess.preProcess(tokens)
    for (token in preparsed) {
        println("${token.tokenType} -> '${token.string}'")
    }
}