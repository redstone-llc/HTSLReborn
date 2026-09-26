package llc.redstone.htslreborn.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.runBlocking
//? if >=26.2 {
/*import llc.redstone.htslreborn.screen
*///?}
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.config.HTSLConfig
import llc.redstone.htslreborn.config.HTSLConfigScreen
import llc.redstone.htslreborn.parser.ast.HtslAstBuilder
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.importer.Importer
import llc.redstone.htslreborn.ui.browser.FileHandler
import llc.redstone.htslreborn.utils.ItemUtils.giveItem
import llc.redstone.htslreborn.utils.ItemUtils.saveItem
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

object HTSLCommand {
    private fun resolveBaseFile(fileArg: String, extension: String): Path {
        val trimmed = fileArg.trim()
        val pathWithExtension = if (trimmed.endsWith(".$extension", ignoreCase = true)) trimmed else "$trimmed.$extension"
        val path = Path(pathWithExtension)
        return if (path.isAbsolute) path else FileHandler.baseDir.resolve(path)
    }

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
                .then(literal("resume").executes(::resume))
                .then(literal("config").executes(::openConfig))
                .then(literal("item")
                    .then(literal("give").then(
                        argument("file", StringArgumentType.greedyString())
                            .executes(::giveItem)
                    ))
                    .then(literal("save").then(
                        argument("file", StringArgumentType.greedyString())
                            .executes(::saveItem)
                    ))
                    .then(literal("delete")
                        .then(
                            argument("file", StringArgumentType.greedyString())
                                .executes(::deleteItem)
                        )
                    )
                )
        )
    }

    fun import(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file") ?: return -1
        val file = resolveBaseFile(fileArg, "htsl")

        val ast = HtslAstBuilder.parseFile(file)
        Importer.process(ast, file)

        return 1
    }

    fun resume(context: CommandContext<FabricClientCommandSource>): Int {
        Queue.resume()
        return 1
    }

    fun giveItem(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file")
        val file = resolveBaseFile(fileArg, "nbt")

        try {
            runBlocking {
                val item = context.source.player.giveItem(file)
                context.source.sendFeedback(
                    Component.translatable(
                        "htslreborn.command.item.give.success",
                        item.hoverName
                    )
                )
            }
            return 1
        } catch (e: IllegalStateException) {
            context.source.sendError(Component.translatable(
                "htslreborn.command.item.give.fail",
                file.pathString
            ))
            e.printStackTrace()
            return -1
        }
    }

    fun saveItem(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file")
        val file = resolveBaseFile(fileArg, "nbt")

        try {
            val item = context.source.player.saveItem(file)
            context.source.sendFeedback(Component.translatable(
                "htslreborn.command.item.save.success",
                item.hoverName, file.pathString
            ))
            return 1
        } catch (e: IllegalStateException) {
            context.source.sendError(Component.translatable(
                "htslreborn.command.item.save.fail",
                file.pathString
            ))
            e.printStackTrace()
            return -1
        }
    }

    fun openConfig(context: CommandContext<FabricClientCommandSource>): Int {
        HTSLConfigScreen.open(HTSLReborn.MC.screen)
        return 1
    }

    fun deleteItem(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file")
        if (fileArg == "confirm") {
            return confirmDelete(context)
        }

        val file = resolveBaseFile(fileArg, "nbt")
        if (HTSLConfig.data.fileDeletionConfirmation) {
            HTSLConfig.pendingDelete = file
            context.source.sendFeedback(
                Component.translatable("htslreborn.command.item.delete.confirm", file.pathString)
                    .withStyle(
                        Style.EMPTY.withColor(ChatFormatting.YELLOW)
                            .withClickEvent(ClickEvent.RunCommand("/htsl item delete confirm"))
                    )
            )
            return 1
        }
        return performDelete(context, file)
    }

    fun confirmDelete(context: CommandContext<FabricClientCommandSource>): Int {
        val file = HTSLConfig.pendingDelete ?: run {
            context.source.sendError(Component.translatable("htslreborn.command.item.delete.none"))
            return -1
        }
        HTSLConfig.pendingDelete = null
        return performDelete(context, file)
    }

    private fun performDelete(context: CommandContext<FabricClientCommandSource>, file: Path): Int {
        return if (HTSLConfig.performDelete(file)) {
            context.source.sendFeedback(Component.translatable(
                "htslreborn.command.item.delete.success",
                file.pathString
            ))
            1
        } else {
            context.source.sendError(Component.translatable(
                "htslreborn.command.item.delete.fail",
                file.pathString
            ))
            -1
        }
    }
}