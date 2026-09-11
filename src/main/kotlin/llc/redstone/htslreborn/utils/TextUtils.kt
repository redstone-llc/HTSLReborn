package llc.redstone.htslreborn.utils

import net.minecraft.ChatFormatting
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

}
