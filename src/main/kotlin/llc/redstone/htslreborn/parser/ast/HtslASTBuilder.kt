package llc.redstone.htslreborn.parser.ast

import com.strumenta.antlrkotlin.parsers.generated.HTSLBaseVisitor
import com.strumenta.antlrkotlin.parsers.generated.HTSLLexer
import com.strumenta.antlrkotlin.parsers.generated.HTSLParser
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.parser.PreProcessor
import llc.redstone.htslreborn.utils.ErrorUtils.throwOnError
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import java.nio.file.Files
import java.nio.file.Path

class HtslAstBuilder(
    val path: Path,
    val includedFiles: MutableSet<Path> = mutableSetOf(),
) : HTSLBaseVisitor<Any?>() {
    val preProcessor = PreProcessor(includedFiles)

    init {
        if (Files.isRegularFile(path)) {
            includedFiles.add(path.toAbsolutePath().normalize())
        }
    }

    override fun visitProgram(ctx: HTSLParser.ProgramContext): List<ScriptContainer> {
        return preProcessor.preprocessProgram(ctx.statement(), path)
    }

    override fun defaultResult(): Any {
        return emptyList<Any>()
    }

    companion object {
        fun parseFile(file: Path, includedFiles: MutableSet<Path> = mutableSetOf()): List<ScriptContainer> {
            val charStream = CharStreams.fromPath(file)
            val lexer = HTSLLexer(charStream).throwOnError()
            val tokens = CommonTokenStream(lexer)
            val parser = HTSLParser(tokens).throwOnError()
            return HtslAstBuilder(file, includedFiles).visitProgram(parser.program())
        }
    }
}
