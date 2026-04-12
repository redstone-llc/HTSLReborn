package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import kotlin.io.path.Path

// Used primarily for testing the tokenizer and preprocessor
fun main(args: Array<String>) {
    val input = """
        var x = teamvar red score
        var x = globalvar score
        var x = var "score"
        var x = randomint 1 10
        var x = randomdouble 1.0 10.0
        var x = health
        var x = maxhealth
        var x = hunger
        var x = locX
        var x = locY
        var x = locZ
        var x = unix
        
    """.split("\n").joinToString("\n") { it.trim() }
    val tokens = Tokenizer.tokenize(input)
    println("Tokens:")
    tokens.forEach { println("${it.tokenType} -> ${it.string}") }

    val preProcessedTokens = PreProcess.preProcess(tokens)
    println("\nPre-Processed Tokens:")
    preProcessedTokens.forEach { println("${it.tokenType} -> ${it.string}") }

    val parser = Parser.parse(preProcessedTokens, Path("test.htsl"))
    println("\nParsed Actions:")
    println(parser)
}