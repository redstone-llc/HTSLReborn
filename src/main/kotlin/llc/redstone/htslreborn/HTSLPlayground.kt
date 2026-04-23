package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import kotlin.io.path.Path

// Used primarily for testing the tokenizer and preprocessor
fun main(args: Array<String>) {
    val input = """
        goto function "Dynamic Hats 1"

if (var route/selected >= 2, !hasItem "slot_29" "Metadata" "Armor") {
    giveItem "slot_29" false "39" true
    var hat/dynamic = 1
    exit
}


goto function "Chestplates 1"

if (var route/selected >= 2, !hasItem "slot_29" "Metadata" "Armor") {
    removeItem "slot_29"
    giveItem "slot_29" false "38" true
    exit
}

goto function "Remove Hats 2"

if (!var route/selected >= 2, hasItem "slot_29" "Metadata") {
    removeItem "slot_29"
}

if (!var route/selected >= 2, hasItem "slot_29" "Metadata") {
    removeItem "slot_29"
}
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