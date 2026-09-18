package llc.redstone.htslreborn.utils

import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor


object TextUtils {
    fun convertTextToString(text: Component?, colors: Boolean = true): String? {
        if (text == null) return null
        val parts = if (text.siblings.isEmpty()) {
            mutableListOf(text)
        } else {
            mutableListOf(*text.siblings.toTypedArray())
        }
        return parts.joinToString("") { it ->
            var part = it.string.replace("§", "&")
            val style = it.style
            if (style.color != null && colors) {
                val color: TextColor = style.color!!
                for (format in ChatFormatting.values()) {
                    //? if >=26.2 {
                    /*if (color.value == TextColor.fromLegacyFormat(format)?.value) {
                    *///?} else {
                    if (color.value == format.color) {
                        //?}
                        part = (format.toString() + part).replace("§", "&")
                    }
                }
            }
            if (!colors) {
                part.replace(Regex("(?i)&[0-9A-FK-OR]"), "")
            } else {
                part
            }
        }
    }

    fun ellipsize(font: Font, text: Component, maxWidth: Int): Component {
        if (maxWidth <= 0) return Component.empty()
        if (font.width(text) <= maxWidth) return text

        val ellipsis = "..."
        val ellipsisWidth = font.width(ellipsis)
        if (ellipsisWidth >= maxWidth) return Component.literal(ellipsis)

        var remaining = maxWidth - ellipsisWidth
        val result = Component.empty()
        var lastStyle = net.minecraft.network.chat.Style.EMPTY
        text.visit({ style, str ->
            if (remaining <= 0) return@visit java.util.Optional.of(Unit)

            var i = 0
            while (i < str.length) {
                val next = i + Character.charCount(str.codePointAt(i))
                val charWidth = font.width(str.substring(i, next))
                if (charWidth > remaining) {
                    if (i > 0) {
                        result.append(Component.literal(str.substring(0, i)).withStyle(style))
                    }
                    remaining = 0
                    return@visit java.util.Optional.of(Unit)
                }
                remaining -= charWidth
                i = next
            }
            lastStyle = style
            result.append(Component.literal(str).withStyle(style))
            java.util.Optional.empty()
        }, net.minecraft.network.chat.Style.EMPTY)

        return result.append(Component.literal(ellipsis).withStyle(lastStyle))
    }

    fun GuiGraphics.drawEllipsis(
        font: Font,
        text: Component,
        x: Int,
        y: Int,
        maxWidth: Int,
        color: Int,
        dropShadow: Boolean = false,
    ) {
        drawString(font, ellipsize(font, text, maxWidth), x, y, color, dropShadow)
    }

    fun GuiGraphics.drawEllipsis(
        font: Font,
        text: String,
        x: Int,
        y: Int,
        maxWidth: Int,
        color: Int,
        dropShadow: Boolean = false,
    ) = drawEllipsis(font, Component.literal(text), x, y, maxWidth, color, dropShadow)
}
