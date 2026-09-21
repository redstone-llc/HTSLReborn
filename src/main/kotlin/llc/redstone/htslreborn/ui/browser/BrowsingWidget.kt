package llc.redstone.htslreborn.ui.browser

import kotlinx.coroutines.launch
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.data.ContextTarget
import llc.redstone.htslreborn.data.ImportContext
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.parser.ast.HtslAstBuilder
import llc.redstone.htslreborn.queue.differ.Differ
import llc.redstone.htslreborn.queue.exporter.Exporter
import llc.redstone.htslreborn.queue.importer.Importer
import llc.redstone.htslreborn.ui.HTSLScreen
import llc.redstone.htslreborn.ui.HTSLScrollWidget
import llc.redstone.htslreborn.ui.browser.FileExplorerHandler.setWatchedDir
import llc.redstone.htslreborn.ui.browser.FileHandler.baseDir
import llc.redstone.htslreborn.utils.MenuUtils
import llc.redstone.htslreborn.utils.TextUtils
import llc.redstone.htslreborn.utils.TextUtils.drawEllipsis
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.nio.file.Path

class BrowsingWidget(x: Int, y: Int) :
    AbstractWidget(x, y, 223, 222, Component.literal("Browsing")) {
    companion object {
        val IMPORTING = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/browser/importing.png")
        val EXPORTING = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/browser/exporting.png")
        var scrollHeight = 0.0

        val ACTIVE = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/browser/active.png")
        val INACTIVE = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/browser/inactive.png")

        val COMMAND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/command.png")
        val EVENT = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/event.png")
        val FUNCTION = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/function.png")
        val NPC = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/npc.png")
        val REGION = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/region.png")
        val CUSTOMMENU = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/custommenu.png")

        val ADD = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/add.png")
        val UPDATE = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/update.png")
        val EXPORT = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/export.png")

        var filePath: Path? = null
        var container: ScriptContainer? = null

        val cachedContexts = mutableMapOf<ImportContext, List<ScriptContainer>>()
        var openContext: ImportContext? = null

        var fileScroll: HTSLScrollWidget? = null
        var contextScroll: HTSLScrollWidget? = null

        var searchBox: EditBox? = null
    }

    init {
        if (MenuUtils.isActionContainerOpen()) {
            container = ScriptContainer(ImportContext.DEFAULT)
        }

        HTSLReborn.SCOPE.launch {
            for (context in ImportContext.entries) {
                cachedContexts[context] = context.getContexts().map {
                    ScriptContainer(
                        context, target = ContextTarget(
                            name = it
                        )
                    )
                }
            }
        }
    }

    fun isLeftBrowsing(): Boolean {
        return when (HTSLScreen.browsingType) {
            HTSLScreen.BrowsingType.IMPORT_LEFT, HTSLScreen.BrowsingType.EXPORT_LEFT -> true
            HTSLScreen.BrowsingType.IMPORT_RIGHT, HTSLScreen.BrowsingType.EXPORT_RIGHT -> false
            else -> false
        }
    }

    fun isImporting(): Boolean {
        return when (HTSLScreen.browsingType) {
            HTSLScreen.BrowsingType.IMPORT_LEFT, HTSLScreen.BrowsingType.IMPORT_RIGHT -> true
            HTSLScreen.BrowsingType.EXPORT_LEFT, HTSLScreen.BrowsingType.EXPORT_RIGHT -> false
            else -> false
        }
    }

    fun contexts(): List<String> {
        val context = openContext
        return if (context != null) {
            listOf("Contexts", TextUtils.titleCase(context.name))
        } else {
            listOf("Contexts")
        }
    }

    init {
        fileScroll = HTSLScrollWidget(0, 0, 204, 145, FileLayout(0, 0, 204, 145), scrollHeight)
        contextScroll = HTSLScrollWidget(0, 0, 204, 145, ContextLayout(0, 0, 204, 145), scrollHeight)

        searchBox = EditBox(MC.font, 0, 0, 135, 13, Component.literal("Search")).apply {
            setHint(Component.literal("Search").withColor(0x3F3F3F).withoutShadow())
            isBordered = false
            setTextShadow(false)
            setTextColor(0xFF3F3F3F.toInt())
            setMaxLength(64)
            value = FileHandler.search
            setResponder { query ->
                FileHandler.search = query
                FileHandler.refreshFiles()
                fileScroll?.setScrollAmount(0.0)
                contextScroll?.setScrollAmount(0.0)
            }
        }
    }

    fun getNames(): List<String> {
        return if (isImporting()) {
            if (isLeftBrowsing()) {
                val subDir = FileHandler.currentDir
                (baseDir.nameCount - 1 until subDir.nameCount).map { subDir.getName(it).toString() }
            } else {
                contexts()
            }
        } else {
            if (isLeftBrowsing()) {
                contexts()
            } else {
                val subDir = FileHandler.currentDir
                (baseDir.nameCount - 1 until subDir.nameCount).map { subDir.getName(it).toString() }
            }
        }
    }

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta)

        val left = isLeftBrowsing()

        guiGraphics.text(
            MC.font,
            Component.literal(if (isImporting()) "Importing" else "Exporting"),
            x + 8,
            y + 8,
            0xFF3F3F3F.toInt(),
            false
        )


        val names = getNames()
        for ((i, name) in names.withIndex()) {
            val nameX = x + 9 + MC.font.width(names.subList(0, i).joinToString(" > ")) + if (i > 0) MC.font.width(" > ") else 0
            val nameY = y + 41
            guiGraphics.text(
                MC.font,
                Component.literal(name),
                nameX,
                nameY,
                if (mouseX in nameX..(nameX + MC.font.width(name)) && mouseY in nameY..(nameY + MC.font.lineHeight)) 0xFF595959.toInt() else 0xFF3F3F3F.toInt(),
                false
            )
            if (i < names.size - 1) {
                val separatorX = nameX + MC.font.width(name)
                val separatorY = nameY
                guiGraphics.text(
                    MC.font,
                    Component.literal(" > "),
                    separatorX,
                    separatorY,
                    0xFF3F3F3F.toInt(),
                    false
                )
            }
        }

        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            if (left) ACTIVE else INACTIVE,
            x + 8,
            y + 19,
            0.0f,
            0.0f,
            97,
            13,
            97,
            13
        )
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            if (left) INACTIVE else ACTIVE,
            x + 118,
            y + 19,
            0.0f,
            0.0f,
            97,
            13,
            97,
            13
        )


        val input =
            if (isImporting()) {
                filePath?.fileName?.toString() ?: "Not Set"
            } else {
                if (container?.context == ImportContext.DEFAULT) "Current Container"
                else container?.target?.name ?: "Not Set"
            }

        val output = if (isImporting()) {
            if (container?.context == ImportContext.DEFAULT) "Current Container"
            else container?.target?.name ?: "Not Set"
        } else {
            filePath?.fileName?.toString() ?: "Not Set"
        }

        val badgeTexture = when (container?.context) {
            ImportContext.COMMAND -> COMMAND
            ImportContext.EVENT -> EVENT
            ImportContext.FUNCTION -> FUNCTION
            ImportContext.NPC -> NPC
            ImportContext.REGION -> REGION
            ImportContext.CUSTOMMENU -> CUSTOMMENU
            else -> null
        }

        val badgeX = if (!isImporting()) x + 11 else x + 121
        val badgeInput = isImporting()
        guiGraphics.drawEllipsis(
            MC.font,
            Component.literal(input),
            if (badgeInput || badgeTexture == null) x + 11 else x + 20,
            y + 22,
            if (badgeInput || badgeTexture == null) 92 else 83,
            0xFF3F3F3F.toInt(),
            false
        )

        guiGraphics.drawEllipsis(
            MC.font,
            Component.literal(output),
            if (!badgeInput || badgeTexture == null) x + 121 else x + 130,
            y + 22,
            if (!badgeInput || badgeTexture == null) 92 else 83,
            0xFF3F3F3F.toInt(),
            false
        )

        if (badgeTexture != null) {
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                badgeTexture,
                badgeX,
                y + 22,
                0.0f,
                0.0f,
                7,
                7,
                7,
                7
            )
        }

        if (filePath != null && container != null) {
            if (isImporting()) {
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    ADD,
                    x + 145, y + 201, 0.0f, 0.0f,
                    25, 13, 25, 13,
                    if (mouseX in x + 145..x + 145 + 25 && mouseY in y + 201..y + 201 + 13) 0xFFCCCCCD.toInt() else 0xFFFFFFFF.toInt()
                )
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    UPDATE,
                    x + 145 + 27, y + 201, 0.0f, 0.0f,
                    43, 13, 43, 13,
                    if (mouseX in x + 145 + 27..x + 145 + 27 + 43 && mouseY in y + 201..y + 201 + 13) 0xFFCCCCCD.toInt() else 0xFFFFFFFF.toInt()
                )
            } else {
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    EXPORT,
                    x + 173, y + 201, 0.0f, 0.0f,
                    42, 13, 42, 13,
                    if (mouseX in x + 173..x + 145 + 42 && mouseY in y + 201..y + 201 + 13) 0xFFCCCCCD.toInt() else 0xFFFFFFFF.toInt()
                )
            }
        }

        renderFiles(guiGraphics, mouseX, mouseY, delta)
    }

    private fun renderFiles(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val left = isLeftBrowsing()
        val scroll = if (isImporting()) {
            if (left) fileScroll else contextScroll
        } else {
            if (left) contextScroll else fileScroll
        }

        if (scroll == null) return
        scroll.setPosition(x + 10, y + 53)
        scroll.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        scrollHeight = scroll.scrollAmount()

        searchBox?.setPosition(x + 11, y + 204)
        searchBox?.setSize(if (isImporting()) 128 else 156, 13)
        searchBox?.extractRenderState(guiGraphics, mouseX, mouseY, delta)
    }

    private fun renderBackground(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val texture = if (isImporting()) IMPORTING else EXPORTING

        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x,
            y,
            0.0f,
            0.0f,
            width,
            height,
            width,
            height
        )
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        val scroll = if (isImporting()) {
            if (isLeftBrowsing()) fileScroll else contextScroll
        } else {
            if (isLeftBrowsing()) contextScroll else fileScroll
        }
        if (scroll?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) == true) {
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val left = isLeftBrowsing()

        if (!left && event.x.toInt() in x + 9..x + 9 + 97 && event.y.toInt() in y + 19..y + 19 + 13) {
            HTSLScreen.browsingType = when (HTSLScreen.browsingType) {
                HTSLScreen.BrowsingType.IMPORT_RIGHT -> HTSLScreen.BrowsingType.IMPORT_LEFT
                HTSLScreen.BrowsingType.EXPORT_RIGHT -> HTSLScreen.BrowsingType.EXPORT_LEFT
                else -> return false
            }
            return true
        } else if (left && event.x.toInt() in x + 118..x + 118 + 97 && event.y.toInt() in y + 19..y + 19 + 13) {
            HTSLScreen.browsingType = when (HTSLScreen.browsingType) {
                HTSLScreen.BrowsingType.IMPORT_LEFT -> HTSLScreen.BrowsingType.IMPORT_RIGHT
                HTSLScreen.BrowsingType.EXPORT_LEFT -> HTSLScreen.BrowsingType.EXPORT_RIGHT
                else -> return false
            }
            return true
        }

        if (searchBox?.mouseClicked(event, doubled) == true) {
            searchBox?.isFocused = true
            return true
        }
        searchBox?.isFocused = false

        if (filePath != null && container != null) {
            val htslFile = filePath ?: return false
            if (isImporting()) {
                if (event.x.toInt() in x + 145..x + 145 + 25 && event.y.toInt() in y + 201..y + 201 + 13) {
                    try {
                        val ast = HtslAstBuilder.parseFile(htslFile)
                        Importer.process(ast, htslFile, container?.context, container?.target)
                        HTSLScreen.notBrowsing()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }
                    return true
                } else if (event.x.toInt() in x + 145 + 27..x + 145 + 27 + 43 && event.y.toInt() in y + 201..y + 201 + 13) {
                    try {
                        val ast = HtslAstBuilder.parseFile(htslFile)
                        Differ.process(ast, htslFile, container?.context, container?.target)
                        HTSLScreen.notBrowsing()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }
                    return true
                }
            } else {
                if (event.x.toInt() in x + 173..x + 145 + 42 && event.y.toInt() in y + 201..y + 201 + 13) {
                    try {
                        Exporter.process(container!!, htslFile)
                        HTSLScreen.notBrowsing()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }
                    return true

                }
            }
        }

        val names = getNames()
        for ((i, element) in names.withIndex()) {
            val elementX =
                x + 9 + MC.font.width(
                    names.subList(0, i).joinToString(" > ")
                ) + if (i > 0) MC.font.width(" > ") else 0
            val elementY = y + 41
            val elementWidth = MC.font.width(element)
            val elementHeight = MC.font.lineHeight

            if (event.x.toInt() in elementX..(elementX + elementWidth) && event.y.toInt() in elementY..(elementY + elementHeight)) {
                if (left && isImporting() || !left && !isImporting()) {
                    val newDir = baseDir.resolve(names.subList(1, i + 1).joinToString("/"))
                    FileHandler.currentDir = newDir
                    FileHandler.refreshFiles()
                    setWatchedDir(newDir)
                    fileScroll?.setScrollAmount(0.0)
                } else {
                    if (i == 0) {
                        openContext = null
                    } else if (i == 1) {
                        val context = ImportContext.entries.find { it.name == names[1].lowercase() }
                        if (context != null) {
                            openContext = context
                        }
                    }
                    contextScroll?.setScrollAmount(0.0)

                }
                return true
            }
        }

        val scroll = if (isImporting()) {
            if (left) fileScroll else contextScroll
        } else {
            if (left) contextScroll else fileScroll
        }
        return scroll?.mouseClicked(event, doubled) ?: false
    }

    override fun mouseDragged(event: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        if (searchBox?.mouseDragged(event, offsetX, offsetY) == true) {
            return true
        }
        val scroll = if (isImporting()) {
            if (isLeftBrowsing()) fileScroll else contextScroll
        } else {
            if (isLeftBrowsing()) contextScroll else fileScroll
        }
        if (scroll?.mouseDragged(event, offsetX, offsetY) == true) {
            return true
        }
        return super.mouseDragged(event, offsetX, offsetY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val scroll = if (isImporting()) {
            if (isLeftBrowsing()) fileScroll else contextScroll
        } else {
            if (isLeftBrowsing()) contextScroll else fileScroll
        }
        if (scroll?.mouseReleased(event) == true) {
            return true
        }
        return super.mouseReleased(event)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (searchBox?.isFocused == true) {
            if (searchBox?.keyPressed(keyEvent) == true) {
                return true
            }
            if (keyEvent.key() == 256) {
                searchBox?.isFocused = false
            }
            return true
        }
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (searchBox?.charTyped(characterEvent) == true) {
            return true
        }
        return super.charTyped(characterEvent)
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
    }
}
