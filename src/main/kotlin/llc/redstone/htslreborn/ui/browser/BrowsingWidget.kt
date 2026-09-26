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
import llc.redstone.htslreborn.utils.CursorManager
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
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

class BrowsingWidget(x: Int, y: Int) :
    AbstractWidget(x, y, 223, 222, Component.translatable("htslreborn.browser.title")) {
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

        var fileText = ""
        var contextText = ""
        private var searchSideIsFiles: Boolean? = null

        val cachedContexts = mutableMapOf<ImportContext, List<ScriptContainer>>()
        var openContext: ImportContext? = null

        var fileScroll: HTSLScrollWidget? = null
        var contextScroll: HTSLScrollWidget? = null

        var searchBox: EditBox? = null

        fun browsingFiles(): Boolean {
            return when (HTSLScreen.browsingType) {
                HTSLScreen.BrowsingType.IMPORT_LEFT, HTSLScreen.BrowsingType.EXPORT_RIGHT -> true
                else -> false
            }
        }

        fun fileLabel(): Component =
            if (fileText.isBlank()) Component.translatable("htslreborn.browser.not_set")
            else Component.literal(fileText)

        fun contextLabel(): Component = when {
            contextText.isNotBlank() -> Component.literal(contextText)
            MenuUtils.isActionContainerOpen() -> Component.translatable("htslreborn.browser.current_container")
            else -> Component.translatable("htslreborn.browser.not_set")
        }

        fun fillSearch(text: String) {
            searchBox?.value = text
        }

        fun clearSelection() {
            fileText = ""
            contextText = ""
            searchSideIsFiles = null
            val box = searchBox
            if (box != null && box.value.isNotEmpty()) {
                box.value = ""
            } else {
                FileHandler.search = ""
                FileHandler.refreshFiles()
            }
        }

        fun resolvedFile(): Path? {
            val query = fileText.trim()
            if (query.isEmpty() || query.contains('/') || query.contains('\\')) return null
            val existing = FileHandler.files.firstOrNull { path ->
                !path.isDirectory() && path.name.endsWith(".htsl", ignoreCase = true) &&
                    (path.name.equals(query, ignoreCase = true) ||
                        path.nameWithoutExtension.equals(query, ignoreCase = true))
            }
            if (existing != null) return existing
            val importing = when (HTSLScreen.browsingType) {
                HTSLScreen.BrowsingType.IMPORT_LEFT, HTSLScreen.BrowsingType.IMPORT_RIGHT -> true
                else -> false
            }
            if (importing) return null
            val fileName = if (query.endsWith(".htsl", ignoreCase = true)) query else "$query.htsl"
            return FileHandler.currentDir.resolve(fileName)
        }

        fun resolvedContainer(): ScriptContainer? {
            val query = contextText.trim()
            if (query.isEmpty()) {
                return if (MenuUtils.isActionContainerOpen()) ScriptContainer(ImportContext.DEFAULT) else null
            }
            val currentContainer = TextUtils.translate("htslreborn.browser.current_container")
            val defaultName = TextUtils.translate("htslreborn.browser.default")
            if (query.equals(currentContainer, ignoreCase = true) || query.equals(defaultName, ignoreCase = true)) {
                return ScriptContainer(ImportContext.DEFAULT)
            }
            fun matches(container: ScriptContainer): Boolean {
                val name = container.target.name ?: return false
                return name.equals(query, ignoreCase = true) ||
                    TextUtils.titleCase(name).equals(query, ignoreCase = true)
            }
            openContext?.let { context ->
                cachedContexts[context]?.firstOrNull(::matches)?.let { return it }
            }
            for (containers in cachedContexts.values) {
                containers.firstOrNull(::matches)?.let { return it }
            }
            return null
        }
    }

    init {
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
        val root = TextUtils.translate("htslreborn.browser.contexts")
        return if (context != null) {
            listOf(root, TextUtils.translate(context.translationKey()))
        } else {
            listOf(root)
        }
    }

    init {
        fileScroll = HTSLScrollWidget(0, 0, 204, 145, FileLayout(0, 0, 204, 145), scrollHeight)
        contextScroll = HTSLScrollWidget(0, 0, 204, 145, ContextLayout(0, 0, 204, 145), scrollHeight)

        searchBox = EditBox(MC.font, 0, 0, 135, 13, Component.translatable("htslreborn.browser.search")).apply {
            setHint(Component.translatable("htslreborn.browser.search").withColor(0x3F3F3F).withoutShadow())
            isBordered = false
            setTextShadow(false)
            setTextColor(0xFF3F3F3F.toInt())
            setMaxLength(64)
            value = FileHandler.search
            setResponder { query ->
                if (browsingFiles()) fileText = query else contextText = query
                if (FileHandler.search == query) return@setResponder
                FileHandler.search = query
                FileHandler.refreshFiles()
                fileScroll?.setScrollAmount(0.0)
                contextScroll?.setScrollAmount(0.0)
            }
        }
    }

    private data class Breadcrumb(val index: Int, val text: String, val x: Int, val width: Int)

    private fun layoutBreadcrumbs(names: List<String>): List<Breadcrumb> {
        if (names.isEmpty()) return emptyList()

        val maxWidth = width - 18
        val separator = TextUtils.translate("htslreborn.browser.separator")
        var count = 1
        while (count < names.size) {
            val start = names.size - (count + 1)
            val parts = ArrayList<String>(count + 2)
            if (start > 0) parts.add("...")
            parts.addAll(names.subList(start, names.size))
            if (MC.font.width(parts.joinToString(separator)) > maxWidth) break
            count++
        }

        val start = names.size - count
        val crumbs = ArrayList<Breadcrumb>(count + 1)
        if (start > 0) crumbs.add(Breadcrumb(-1, "...", 0, 0))
        for (i in start until names.size) {
            crumbs.add(Breadcrumb(i, names[i], 0, 0))
        }

        val fullWidth = MC.font.width(crumbs.joinToString(separator) { it.text })
        if (fullWidth > maxWidth) {
            val last = crumbs.last()
            val prefixWidth = if (crumbs.size == 1) {
                0
            } else {
                MC.font.width(crumbs.dropLast(1).joinToString(separator) { it.text } + separator)
            }
            val fitted = TextUtils.ellipsize(
                MC.font,
                Component.literal(last.text),
                (maxWidth - prefixWidth).coerceAtLeast(0)
            ).string
            crumbs[crumbs.lastIndex] = Breadcrumb(last.index, fitted, 0, 0)
        }

        var cursor = x + 9
        val separatorWidth = MC.font.width(separator)
        return crumbs.map { crumb ->
            val crumbWidth = MC.font.width(crumb.text)
            val laidOut = Breadcrumb(crumb.index, crumb.text, cursor, crumbWidth)
            cursor += crumbWidth + separatorWidth
            laidOut
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

    private fun syncSearchSide() {
        val files = browsingFiles()
        if (searchSideIsFiles == files) return
        searchSideIsFiles = files
        val text = if (files) fileText else contextText
        val box = searchBox ?: return
        if (box.value != text) {
            box.value = text
        } else if (FileHandler.search != text) {
            FileHandler.search = text
            FileHandler.refreshFiles()
        }
    }

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        syncSearchSide()
        this.renderBackground(guiGraphics, mouseX, mouseY, delta)

        val left = isLeftBrowsing()

        guiGraphics.text(
            MC.font,
            Component.translatable(if (isImporting()) "htslreborn.browser.importing" else "htslreborn.browser.exporting"),
            x + 8,
            y + 8,
            0xFF3F3F3F.toInt(),
            false
        )


        val crumbs = layoutBreadcrumbs(getNames())
        val nameY = y + 41
        for ((i, crumb) in crumbs.withIndex()) {
            val hovered = crumb.index >= 0 &&
                mouseX in crumb.x..(crumb.x + crumb.width) &&
                mouseY in nameY..(nameY + MC.font.lineHeight)
            guiGraphics.text(
                MC.font,
                Component.literal(crumb.text),
                crumb.x,
                nameY,
                if (hovered) 0xFF595959.toInt() else 0xFF3F3F3F.toInt(),
                false
            )
            if (hovered) {
                CursorManager.setHandCursor()
            }
            if (i < crumbs.lastIndex) {
                guiGraphics.text(
                    MC.font,
                    Component.literal(TextUtils.translate("htslreborn.browser.separator")),
                    crumb.x + crumb.width,
                    nameY,
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
        if (!left && mouseX in x + 8..x + 8 + 97 && mouseY in y + 19..y + 19 + 13) {
            CursorManager.setHandCursor()
        }
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
        if (left && mouseX in x + 118..x + 118 + 97 && mouseY in y + 19..y + 19 + 13) {
            CursorManager.setHandCursor()
        }


        val input = if (isImporting()) fileLabel() else contextLabel()
        val output = if (isImporting()) contextLabel() else fileLabel()
        val selectedContainer = resolvedContainer()

        val badgeTexture = when (selectedContainer?.context) {
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
            input,
            if (badgeInput || badgeTexture == null) x + 11 else x + 20,
            y + 22,
            if (badgeInput || badgeTexture == null) 92 else 83,
            0xFF3F3F3F.toInt(),
            false
        )

        guiGraphics.drawEllipsis(
            MC.font,
            output,
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

        if (resolvedFile() != null && selectedContainer != null) {
            if (isImporting()) {
                if (mouseX in x + 145..x + 145 + 25 && mouseY in y + 201..y + 201 + 13) {
                    CursorManager.setHandCursor()
                }
                if (mouseX in x + 145 + 27..x + 145 + 27 + 43 && mouseY in y + 201..y + 201 + 13) {
                    CursorManager.setHandCursor()
                }
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
                if (mouseX in x + 173..x + 145 + 42 && mouseY in y + 201..y + 201 + 13) {
                    CursorManager.setHandCursor()
                }
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

        val box = searchBox
        if (box != null) {
            box.setPosition(x + 11, y + 204)
            box.setSize(if (isImporting()) 128 else 156, 13)
            box.extractRenderState(guiGraphics, mouseX, mouseY, delta)
            if (box.isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
                CursorManager.setIBeamCursor()
            }
        }
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
            fileScroll?.setScrollAmount(0.0)
            contextScroll?.setScrollAmount(0.0)
            return true
        } else if (left && event.x.toInt() in x + 118..x + 118 + 97 && event.y.toInt() in y + 19..y + 19 + 13) {
            HTSLScreen.browsingType = when (HTSLScreen.browsingType) {
                HTSLScreen.BrowsingType.IMPORT_LEFT -> HTSLScreen.BrowsingType.IMPORT_RIGHT
                HTSLScreen.BrowsingType.EXPORT_LEFT -> HTSLScreen.BrowsingType.EXPORT_RIGHT
                else -> return false
            }
            fileScroll?.setScrollAmount(0.0)
            contextScroll?.setScrollAmount(0.0)
            return true
        }

        if (searchBox?.mouseClicked(event, doubled) == true) {
            searchBox?.isFocused = true
            return true
        }
        searchBox?.isFocused = false

        val htslFile = resolvedFile()
        val selectedContainer = resolvedContainer()
        if (htslFile != null && selectedContainer != null) {
            if (isImporting()) {
                if (event.x.toInt() in x + 145..x + 145 + 25 && event.y.toInt() in y + 201..y + 201 + 13) {
                    try {
                        val ast = HtslAstBuilder.parseFile(htslFile)
                        Importer.process(ast, htslFile, selectedContainer.context, selectedContainer.target)
                        HTSLScreen.notBrowsing()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }
                    return true
                } else if (event.x.toInt() in x + 145 + 27..x + 145 + 27 + 43 && event.y.toInt() in y + 201..y + 201 + 13) {
                    try {
                        val ast = HtslAstBuilder.parseFile(htslFile)
                        Differ.process(ast, htslFile, selectedContainer.context, selectedContainer.target)
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
                        Exporter.process(selectedContainer, htslFile)
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
        val elementY = y + 41
        val elementHeight = MC.font.lineHeight
        for (crumb in layoutBreadcrumbs(names)) {
            if (crumb.index < 0) continue
            if (event.x.toInt() in crumb.x..(crumb.x + crumb.width) && event.y.toInt() in elementY..(elementY + elementHeight)) {
                val i = crumb.index
                if (left && isImporting() || !left && !isImporting()) {
                    val newDir = baseDir.resolve(names.subList(1, i + 1).joinToString("/"))
                    FileHandler.currentDir = newDir
                    FileHandler.refreshFiles()
                    setWatchedDir(newDir)
                    fileScroll?.setScrollAmount(0.0)
                } else {
                    if (i == 0) {
                        openContext = null
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
