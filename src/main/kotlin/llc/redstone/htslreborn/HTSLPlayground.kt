package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import kotlin.io.path.Path

// Used primarily for testing the tokenizer and preprocessor
fun main(args: Array<String>) {
    val input = """
    tp "custom_coordinates" "~%var.global/x% ~%var.global/y% ~%var.global/z%" false
    """.trimIndent()
    val tokens = Tokenizer.tokenize(input)
    println("Tokens:")
    tokens.forEach { println("${it.tokenType} -> ${it.string}") }

    val preProcessedTokens = PreProcess.preProcess(tokens)
    println("\nPre-Processed Tokens:")
    preProcessedTokens.forEach { println("${it.tokenType} -> ${it.string}") }

    val parser = Parser.parse(preProcessedTokens, Path("test.htsl"))
        println("\nParsed Actions:")
        parser["base"]?.forEach {
            println(it)
        }
}