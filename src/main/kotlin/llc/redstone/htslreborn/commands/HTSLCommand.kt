package llc.redstone.htslreborn.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.htslreborn.utils.ItemUtils.giveItem
import llc.redstone.htslreborn.utils.ItemUtils.saveItem
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text
import kotlin.io.path.*

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
                .then(literal("item")
                    .then(literal("give").then(
                        argument("file", StringArgumentType.greedyString())
                            .executes(::giveItem)
                    ))
                    .then(literal("save").then(
                        argument("file", StringArgumentType.greedyString())
                            .executes(::saveItem)
                    ))
                    .then(literal("delete").then(
                        argument("file", StringArgumentType.greedyString())
                            .executes(::deleteItem)
                    ))
                )
        )
    }

    fun import(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file") ?: return -1

        val file = Path(fileArg)

        HTSLImporter.importFile(file, supportsBase = false)

        return 1
    }

    fun giveItem(context: CommandContext<FabricClientCommandSource>): Int {
        var fileArg = StringArgumentType.getString(context, "file").trim()
        if(!fileArg.endsWith(".nbt")) fileArg += ".nbt"
        val file = FileHandler.baseDir.resolve(fileArg)

        try {
            val item = context.source.player.giveItem(file)
            context.source.sendFeedback(Text.translatable(
                "htslreborn.command.item.give.success",
                item.name
            ))
            return 1
        } catch (e: IllegalStateException) {
            context.source.sendError(Text.translatable(
                "htslreborn.command.item.give.fail",
                file.pathString
            ))
            e.printStackTrace()
            return -1
        }
    }

    fun saveItem(context: CommandContext<FabricClientCommandSource>): Int {
        var fileArg = StringArgumentType.getString(context, "file").trim()
        if(!fileArg.endsWith(".nbt")) fileArg += ".nbt"
        val file = FileHandler.baseDir.resolve(fileArg)

        try {
            val item = context.source.player.saveItem(file)
            context.source.sendFeedback(Text.translatable(
                "htslreborn.command.item.save.success",
                item.name, file.pathString
            ))
            return 1
        } catch (e: IllegalStateException) {
            context.source.sendError(Text.translatable(
                "htslreborn.command.item.save.fail",
                file.pathString
            ))
            e.printStackTrace()
            return -1
        }
    }

    fun deleteItem(context: CommandContext<FabricClientCommandSource>): Int {
        var fileArg = StringArgumentType.getString(context, "file").trim()
        if(!fileArg.endsWith(".nbt")) fileArg += ".nbt"
        val file = FileHandler.baseDir.resolve(fileArg)

        try {
            file.deleteExisting()
            context.source.sendFeedback(Text.translatable(
                "htslreborn.command.item.delete.success",
                file.pathString
            ))
            return 1
        } catch (e: IllegalStateException) {
            context.source.sendError(Text.translatable(
                "htslreborn.command.item.delete.fail",
                file.pathString
            ))
            e.printStackTrace()
            return -1
        }
    }
}