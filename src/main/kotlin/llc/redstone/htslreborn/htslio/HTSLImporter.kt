package llc.redstone.htslreborn.htslio

import kotlinx.coroutines.CancellationException
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.importing
import llc.redstone.htslreborn.HTSLReborn.importingFile
import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.htslio.ActionMenuRecovery.isActionMenuTimeout
import llc.redstone.htslreborn.htslio.ActionMenuRecovery.isActionSettingsTimeout
import llc.redstone.htslreborn.htslio.ActionMenuRecovery.isSettingsTimeout
import llc.redstone.htslreborn.utils.UIErrorToast
import llc.redstone.htslreborn.utils.UISuccessToast
import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsapi.api.Event
import llc.redstone.systemsapi.api.Command as HousingCommand
import llc.redstone.systemsapi.api.Function as HousingFunction
import llc.redstone.systemsapi.api.Menu as HousingMenu
import llc.redstone.systemsdata.Action
import llc.redstone.systemsdata.Condition
import llc.redstone.systemsdata.Pagination
import llc.redstone.systemsapi.importer.ActionContainer
import llc.redstone.systemsapi.util.CommandUtils
import llc.redstone.systemsapi.util.MenuUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.world.GameMode
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

object HTSLImporter {
    val APPEND_ACTIONS: suspend (ActionContainer, List<Action>) -> Unit = { actionContainer, actions ->
        appendActionsSafely(actionContainer, actions)
    }

    val REPLACE_ACTIONS: suspend (ActionContainer, List<Action>) -> Unit = { actionContainer, actions ->
        replaceActionsSafely(actionContainer, actions)
    }

    private fun mergeCompiledCode(compiledCode: List<Pair<String, List<Action>>>): List<Pair<String, List<Action>>> {
        val merged = linkedMapOf<String, MutableList<Action>>()
        for ((goto, actions) in compiledCode) {
            merged.getOrPut(goto) { mutableListOf() }.addAll(actions)
        }
        return merged.map { (goto, actions) -> goto to actions.toList() }
    }

    fun importFile(
        path: Path,
        method: suspend (ActionContainer, List<Action>) -> Unit = APPEND_ACTIONS,
        supportsBase: Boolean = true,
        onComplete: () -> Unit = {}
    ) {
        val compiledCode: MutableList<Pair<String, List<Action>>>
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
        compiledCode: MutableList<Pair<String, List<Action>>>,
        method: suspend (ActionContainer, List<Action>) -> Unit = APPEND_ACTIONS,
        supportsBase: Boolean = true,
        onComplete: () -> Unit = {}
    ) {
        val preparedCode = mergeCompiledCode(compiledCode)

        if (preparedCode.any { it.first == "base" && it.second.isNotEmpty() } && !supportsBase) {
            MinecraftClient.getInstance().player?.sendMessage(
                Text.of("Couldn't use actions before a goto call.").copy().withColor(Colors.RED), false
            )
            onComplete()
            return
        }

        if (supportsBase) {
            if (MC.currentScreen?.title?.string?.contains(Regex("Edit Actions|Actions: ")) != true) {
                MinecraftClient.getInstance().player?.sendMessage(
                    Text.of("You must have an action gui open to import HTSL code.").copy().withColor(Colors.RED), false
                )
                onComplete()
                return
            }
        }

        val player = MC.player ?: run {
            UIErrorToast.report("Cannot import without a player.")
            onComplete()
            return
        }

        if (player.gameMode != GameMode.CREATIVE) CommandUtils.runCommand("gmc")

        //TODO: go through the compiled code and look for anything that doesnt exist yet and prompt the user to create it first
        SystemsAPI.launch {
            val importTuning = SystemsApiImportTuning.apply()
            try {
                importingFile = path
                importing = true

                val housingImporter = SystemsAPI.getHousingImporter()
                val functions = mutableMapOf<String, HousingFunction>()
                val createdFunctions = mutableSetOf<String>()
                val commands = mutableMapOf<String, HousingCommand>()
                val createdCommands = mutableSetOf<String>()
                val menus = mutableMapOf<String, HousingMenu>()

                for ((goto, actions) in preparedCode) {
                    val type = goto.split(" ").first()
                    val args = goto.substringAfter(" ")
                    when (type) {
                        "base" -> {
                            if (actions.isNotEmpty()) {
                                housingImporter.getOpenActionContainer()
                                    ?.let { method(it, actions) }
                                    ?: error("No action GUI is open for base actions")
                            }
                        }

                        "function" -> {
                            val function = functions.getOrPut(args) {
                                housingImporter.getFunction(args)
                                    ?: housingImporter.createFunction(args).also { createdFunctions.add(args) }
                            }
                            if (actions.isNotEmpty()) {
                                if (args !in createdFunctions) MC.player?.closeScreen()
                                val actionContainer = function.getActionContainer()
                                method(actionContainer, actions)
                            }
                        }

                        "command" -> {
                            val command = commands.getOrPut(args) {
                                housingImporter.getCommand(args)
                                    ?: housingImporter.createCommand(args).also { createdCommands.add(args) }
                            }

                            if (actions.isNotEmpty()) {
                                if (args !in createdCommands) MC.player?.closeScreen()
                                val actionContainer = command.getActionContainer()
                                method(actionContainer, actions)
                            }
                        }

                        "event" -> {
                            if (actions.isNotEmpty()) {
                                housingImporter.getEvent(
                                    Event.Events.entries.find {
                                        it.name.equals(args, false) ||
                                                it.label.equals(args, false)
                                    } ?: error("Event $args does not exist")
                                ).let { method(it, actions) }
                            }
                        }

                        "gui" -> {
                            val name = args.substringBeforeLast(" ")
                            val slot =
                                args.substringAfterLast(" ").toIntOrNull() ?: error("Invalid slot number in goto $goto")
                            val menu = menus.getOrPut(name) {
                                housingImporter.getMenu(name)
                                    ?: housingImporter.createMenu(name)
                            }

                            if (actions.isNotEmpty()) {
                                menu.getMenuElement(slot).getActionContainer()
                                    ?.let { method(it, actions) } ?: error("Slot $slot does not exist in menu $name")
                            }
                        }

                        else -> error("Unknown goto type '$type'")
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
                importTuning.restore()
                importing = false
                importingFile = null
            }
        }
    }

    suspend fun appendActionsSafely(actionContainer: ActionContainer, actions: List<Action>) {
        if (actions.isEmpty()) return

        MenuUtils.onOpen(actionContainer.title)
        for (action in actions) {
            try {
                MenuUtils.onOpen(actionContainer.title)
                actionContainer.addActions(listOf(action))
            } catch (e: Exception) {
                if (!e.isActionMenuTimeout(actionContainer.title)) {
                    if (e.isActionSettingsTimeout() && recoverFromSettingsTimeout(actionContainer.title, action, "Action Settings")) {
                        continue
                    }
                    if (e.isSettingsTimeout() && recoverFromSettingsTimeout(actionContainer.title, action, "Settings")) {
                        continue
                    }
                    throw e
                }

                HTSLReborn.LOGGER.warn("Import hit a transient action menu return timeout for '${actionContainer.title}'; recovering and continuing.")
                if (!ActionMenuRecovery.recover(actionContainer.title)) {
                    throw e
                }
            }
        }
    }

    suspend fun replaceActionsSafely(actionContainer: ActionContainer, actions: List<Action>) {
        try {
            actionContainer.setActions(emptyList())
        } finally {
            SystemsAPI.getHousingImporter().setImporting(false)
        }

        appendActionsSafely(actionContainer, actions)
    }

    private suspend fun recoverFromSettingsTimeout(actionContainerTitle: String, action: Action, menuName: String): Boolean {
        HTSLReborn.LOGGER.warn("Import hit a transient $menuName timeout while configuring ${action::class.simpleName}; recovering and continuing.")

        completePaginatedSelectionIfStillOpen(paginatedSelections(action))

        return ActionMenuRecovery.recover(actionContainerTitle)
    }

    private suspend fun completePaginatedSelectionIfStillOpen(selections: List<String>) {
        val currentTitle = MC.currentScreen?.title?.string ?: return
        if (!currentTitle.contains("Select Option")) return

        val candidates = selections.distinct()
        if (candidates.isEmpty()) return

        var visibleSelection: String? = null
        for (candidate in candidates) {
            if (runCatching { MenuUtils.findSlots(candidate).isNotEmpty() }.getOrDefault(false)) {
                visibleSelection = candidate
                break
            }
        }

        val selection = visibleSelection ?: candidates.singleOrNull() ?: return

        runCatching {
            MenuUtils.clickItems(selection, paginated = visibleSelection == null)
            SystemsAPI.scaledDelay(4.0)
        }.onFailure {
            HTSLReborn.LOGGER.warn("Could not finish selecting paginated option '$selection' during import recovery.")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun paginatedSelections(value: Any?): List<String> {
        val selections = mutableListOf<String>()

        fun collect(current: Any?) {
            when (current) {
                is Action, is Condition -> {
                    for (property in current::class.memberProperties) {
                        val propertyValue = (property as KProperty1<Any, *>).get(current)
                        if (property.hasAnnotation<Pagination>() && propertyValue is String) {
                            selections.add(propertyValue)
                        }

                        if (propertyValue is Iterable<*>) {
                            propertyValue.forEach(::collect)
                        }
                    }
                }
            }
        }

        collect(value)
        return selections
    }
}
