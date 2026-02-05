package llc.redstone.htslreborn.htslio

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.importing
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
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.world.GameMode
import java.nio.file.Path
import kotlin.collections.contains
import kotlin.collections.isNotEmpty
import kotlin.io.path.name

object HTSLImporter {
    fun importFile(path: Path, method: suspend (ActionContainer, List<Action>) -> Unit = ActionContainer::addActions, supportsBase: Boolean = true, onComplete: () -> Unit = {}) {
        val compiledCode: MutableMap<String, List<Action>>
        try {
            var tokens = Tokenizer.tokenize(path)
            tokens = PreProcess.preProcess(tokens)
            compiledCode = Parser.parse(tokens, path)
        }  catch (e: Exception) {
            UIErrorToast.report(e)
            e.printStackTrace()
            onComplete()
            return
        }

        import(path, compiledCode, method, supportsBase, onComplete)
    }

    fun import(path: Path, compiledCode: MutableMap<String, List<Action>>, method: suspend (ActionContainer, List<Action>) -> Unit = ActionContainer::addActions, supportsBase: Boolean = true, onComplete: () -> Unit = {}) {
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
        var errored = false
        SystemsAPI.launch {
            try {
                importing = true
                for ((goto, actions) in compiledCode) {
                    val split = goto.split(" ")
                    when (split.first()) {
                        "base" -> {
                            SystemsAPI.getHousingImporter().getOpenActionContainer()
                                ?.let { method(it, actions) }
                        }

                        "function" -> {
                            val name = split.getOrNull(1) ?: continue
                            SystemsAPI.getHousingImporter().getFunction(name)?.getActionContainer()
                                ?.let { method(it, actions) }
                        }

                        "command" -> {
                            val name = split.getOrNull(1) ?: continue
                            SystemsAPI.getHousingImporter().getCommand(name)?.getActionContainer()
                                ?.let { method(it, actions) }
                        }

                        "event" -> {
                            val name = split.getOrNull(1) ?: continue
                            SystemsAPI.getHousingImporter().getEvent(Event.Events.valueOf(name))
                                .let { method(it, actions) }
                        }

                        "gui" -> {
                            val name = split.getOrNull(1) ?: continue
                            val slot = split.getOrNull(2)?.toIntOrNull() ?: continue
                            SystemsAPI.getHousingImporter().getMenu(name)?.getMenuElement(slot)?.getActionContainer()
                                ?.let { method(it, actions) }
                        }
                    }
                }
            } catch (e: Exception) {
                errored = true
                onComplete()
                UIErrorToast.report(e)
                e.printStackTrace()
                importing = false
            }
            if (errored) return@launch
            UISuccessToast.report("Successfully imported HTSL code from ${path.name}")
            onComplete()
            importing = false
        }
    }
}