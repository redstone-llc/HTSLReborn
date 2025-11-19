package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.ActionParser
import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.tokenizer.Tokenizer
import java.io.File

fun main() {
    val file = File("test.htsl")
    val tokens = Tokenizer.tokenize(file)
//    for (token in tokens) {
//        println("${token.tokenType} -> ${token.string}")
//    }

    val actions = Parser.parse(tokens)
    for (action in actions) {
        println(action)
    }
}