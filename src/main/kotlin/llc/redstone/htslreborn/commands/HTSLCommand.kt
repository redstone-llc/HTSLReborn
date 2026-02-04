package llc.redstone.htslreborn.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import java.io.File

object HTSLCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            literal("htsl")
                .then(
                    literal("import")
                        .then(
                            argument("file", StringArgumentType.greedyString())
                                .executes(::import)
                        )
                )
                .then(literal("test").executes(::test))
        )
    }

    fun import(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file") ?: return -1

        val file = File(fileArg)

        HTSLImporter.importFile(file, supportsBase = false)

        return 1
    }

    fun test(context: CommandContext<FabricClientCommandSource>): Int {
        val imports = File("./htsl/imports")
        imports.walkTopDown().flatMap { it.walkTopDown() }.filter { it.extension == "htsl" }.forEach {
            var tokens = Tokenizer.tokenize(it)
            tokens = PreProcess.preProcess(tokens)
            val compiledCode = Parser.parse(tokens, it)
        }

        return 1
    }
}