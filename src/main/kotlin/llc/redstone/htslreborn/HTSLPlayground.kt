package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import kotlin.io.path.Path

// Used primarily for testing the tokenizer and preprocessor
fun main(args: Array<String>) {
    val input = """
        chat type=join;name=%var.global/join/name%;playerid=%var.global/join/playerid%
    """.split("\n").joinToString("\n") { it.trim() }
    val tokens = Tokenizer.tokenize(input)
    println("Tokens:")
    tokens.forEach { println("${it.tokenType} -> ${it.string}") }

    val preProcessedTokens = PreProcess.preProcess(tokens)
    println("\nPre-Processed Tokens:")
    preProcessedTokens.forEach { println("${it.tokenType} -> ${it.string} (${it.startsAt}-${it.endsAt})") }

    val parser = Parser.parse(preProcessedTokens, Path("test.htsl"))
    println("\nParsed Actions:")
    println(parser)
}