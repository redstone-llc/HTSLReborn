package llc.redstone.htslreborn.htslio

import kotlinx.coroutines.CancellationException
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.importing
import llc.redstone.htslreborn.HTSLReborn.importingFile
import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.utils.UIErrorToast
import llc.redstone.htslreborn.utils.UISuccessToast
import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsapi.api.Event
import llc.redstone.systemsapi.data.Action
import llc.redstone.systemsapi.importer.ActionContainer
import llc.redstone.systemsapi.util.CommandUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.world.GameMode
import java.nio.file.Path
import kotlin.io.path.name

object HTSLImporter {
    fun importFile(
        path: Path,
        method: suspend (ActionContainer, List<Action>) -> Unit = ActionContainer::addActions,
        supportsBase: Boolean = true,
        onComplete: () -> Unit = {}
    ) {
        val compiledCode: MutableMap<String, List<Action>>
        try {
            var tokens = Tokenizer.tokenize(path)
            tokens = PreProcess.preProcess(tokens)
            compiledCode = Parser.parse(tokens, path)
        } catch (e: Exception) {
            UIErrorToast.report(e)
            e.printStackTrace()
            onComplete()
            return
        }

        import(path, compiledCode, method, supportsBase, onComplete)
    }

    fun import(
        path: Path,
        compiledCode: MutableMap<String, List<Action>>,
        method: suspend (ActionContainer, List<Action>) -> Unit = ActionContainer::addActions,
        supportsBase: Boolean = true,
        onComplete: () -> Unit = {}
    ) {
        if (compiledCode.contains("base") && compiledCode["base"]?.isNotEmpty() == true && !supportsBase) {
            MinecraftClient.getInstance().player?.sendMessage(
                Text.of("Couldn't use actions before a goto call.").copy().withColor(Colors.RED), false
            )
        }

        if (MC.currentScreen?.title?.string?.contains(Regex("Edit Actions|Actions: ")) == false) {
            MinecraftClient.getInstance().player?.sendMessage(
                Text.of("You must have an action gui open to import HTSL code.").copy().withColor(Colors.RED), false
            )
            return
        }

        if (MC.player?.gameMode != GameMode.CREATIVE) CommandUtils.runCommand("gmc")

        //TODO: go through the compiled code and look for anything that doesnt exist yet and prompt the user to create it first
        SystemsAPI.launch {
            try {
                importingFile = path
                importing = true

                for ((goto, actions) in compiledCode) {
                    val type = goto.split(" ").first()
                    val args = goto.substringAfter(" ")
                    when (type) {
                        "base" -> {
                            SystemsAPI.getHousingImporter().getOpenActionContainer()
                                ?.let { method(it, actions) }
                        }

                        "function" -> {
                            val function = SystemsAPI.getHousingImporter().getFunction(args)
                                ?: SystemsAPI.getHousingImporter().createFunction(args)

                            method(function.getActionContainer(), actions)
                        }

                        "command" -> {
                            val command = SystemsAPI.getHousingImporter().getCommand(args)
                                ?: SystemsAPI.getHousingImporter().createCommand(args)

                            method(command.getActionContainer(), actions)
                        }

                        "event" -> {
                            SystemsAPI.getHousingImporter().getEvent(Event.Events.valueOf(args))
                                .let { method(it, actions) }
                        }

                        "gui" -> {
                            val name = args.substringBeforeLast(" ")
                            val slot =
                                args.substringAfterLast(" ").toIntOrNull() ?: error("Invalid slot number in goto $goto")
                            val menu = SystemsAPI.getHousingImporter().getMenu(name)
                                ?: SystemsAPI.getHousingImporter().createMenu(name)

                            menu.getMenuElement(slot).getActionContainer()
                                ?.let { method(it, actions) } ?: error("Slot $slot does not exist in menu $name")
                        }
                    }
                }

                if (HTSLReborn.CONFIG.playCompleteSound) MC.player?.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    1.0f,
                    1.0f
                )
                UISuccessToast.report("Successfully imported HTSL code from ${path.name}")
                onComplete()
            } catch (_: CancellationException) {

                if (HTSLReborn.CONFIG.playCompleteSound) MC.player?.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(),
                    1.0f,
                    0.8f
                )

                UIErrorToast.report("Import cancelled.")
                SystemsAPI.getHousingImporter().setImporting(false)
                onComplete()
            } catch (e: Exception) {
                if (HTSLReborn.CONFIG.playCompleteSound) MC.player?.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(),
                    1.0f,
                    0.8f
                )
                UIErrorToast.report(e)
                SystemsAPI.getHousingImporter().setImporting(false)
                e.printStackTrace()
                onComplete()
            } finally {
                importing = false
                importingFile = null
            }
        }
    }
}