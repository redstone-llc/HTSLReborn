package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.LocationParser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.tokenizer.Tokens

// Used primarily for testing the tokenizer, preprocessor, and parser
fun main(args: Array<String>) {
    val input = """
        "custom_coordinates" "1.0 5.0 10.0"
        "custom_coordinates" "1.0 5.0 10.0 0 0" false
        "custom_coordinates" "1.0 5.0 10.0 50 50" true
        custom_coordinates 1.0 5.0 10.0
        custom_coordinates 1.0 5.0 10.0 0 0 true
        custom_coordinates 1.0 5.0 10.0 50 50 false
    """.trimIndent()

    val tokens = Tokenizer.tokenize(input)
    println("Tokens:")
    tokens.forEach { println("${it.tokenType} -> ${it.string}") }

    println("\nParsed Locations:")
    val iterator = tokens.listIterator()
    while (iterator.hasNext()) {
        val token = iterator.next()
        if (token.tokenType == Tokens.STRING && token.string.lowercase() == "custom_coordinates") {
            val location = LocationParser.parse(token.string, iterator)
            println(location)
            val next = if (iterator.hasNext()) iterator.next() else null
            println("Next token after location: ${next?.tokenType} -> ${next?.string}")
        }
    }
}