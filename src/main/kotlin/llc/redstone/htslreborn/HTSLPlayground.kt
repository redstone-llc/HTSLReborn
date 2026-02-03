package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import org.mozilla.javascript.EvaluatorException
import java.io.File

// Used primarily for testing the tokenizer, preprocessor, and parser
fun main(args: Array<String>) {
    val files = File("./testfiles/").walkTopDown().filter { it.isFile && it.extension == "htsl" }.toList()
    var errorCount = 0
    var fileCount = 0
    var successCount = 0
    var errorTypes = mutableSetOf<String>()
    for (file in files) {
        fileCount++
        try {
            val tokens = Tokenizer.tokenize(file)
            val preparsed = PreProcess.preProcess(tokens)
            println("File: ${file.name}")
            println("Tokens: ${tokens.size}, Preprocessed Tokens: ${preparsed.size}")
            successCount++
        } catch (e: Exception) {
            println("Error processing file ${file.name}: ${e.message}")
            errorCount++
            errorTypes.add(e.message ?: "Unknown error")
            if (e is EvaluatorException) {
                println("JavaScript Error at line ${e.lineNumber()}, column ${e.columnNumber()}")
            }
            continue
        }
    }
    println("Processed $fileCount files: $successCount successful, $errorCount errors.")
    println("Error types:")
    for (errorType in errorTypes) {
        println("- $errorType")
    }
}