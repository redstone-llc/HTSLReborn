package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import java.io.File

// Used primarily for testing the tokenizer, preprocessor, and parser
fun main(args: Array<String>) {
    val input = File("./run/htsl/imports/test.htsl")
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