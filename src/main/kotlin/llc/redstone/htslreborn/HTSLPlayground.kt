package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import kotlin.io.path.Path

// Used primarily for testing the tokenizer and preprocessor
fun main(args: Array<String>) {
    val input = """
chat "hi"
var "Kills" -= 10 false
applyLayout "test"
applyPotion "Strength" 10 10 true true
globalvar "Kills" = 2 false
changeHealth += 20
changePlayerGroup "test" true
clearEffects
actionBar "message hehe he ha"
title "Hello World!" "This is a subtitle!" 1 2 3
failParkour "This is a reason!"
fullHeal
xpLevel 10
kill
parkCheck
sound "Anvil Land" 0.7 1.0 "custom_coordinates" "~ ~ ~"
resetInventory
chat "Hello there! This is a message"
lobby null
compassTarget null
gamemode "Creative"
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