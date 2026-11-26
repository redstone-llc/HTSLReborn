package llc.redstone.htslreborn.utils

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.data.ImportContext
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component

object ToastUtils {
    private val ERROR_NOTIFICATION = SystemToast.SystemToastId(10000L)
    private val INFO_NOTIFICATION = SystemToast.SystemToastId(5000L)
    fun send(title: Component, description: Component, error: Boolean = false) {
        MC.execute {
            //? if >=26.2 {
            //SystemToast.add(
            //   MC.gui.toastManager(),
            //   if (error) ERROR_NOTIFICATION else INFO_NOTIFICATION,
            //   title,
            //   description,
            //)
            //? } else {
            MC.toastManager.addToast(
                SystemToast.multiline(
                    MC,
                    if (error) ERROR_NOTIFICATION else INFO_NOTIFICATION,
                    title,
                    description,
                )
            );
            //? }
        }
    }

    fun resumableSession() {
        send(
            Component.translatable("htslreborn.toast.resumable").withStyle(ChatFormatting.GREEN),
            Component.translatable(
                "htslreborn.toast.resumable.description",
                Component.translatable("htslreborn.toast.command.resume").withStyle(ChatFormatting.YELLOW)
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    fun paused(reason: String) {
        send(
            Component.translatable("htslreborn.toast.paused").withStyle(ChatFormatting.RED),
            Component.translatable(
                "htslreborn.toast.paused.description",
                Component.literal(reason),
                Component.translatable("htslreborn.toast.command.resume").withStyle(ChatFormatting.YELLOW)
            ).withStyle(ChatFormatting.GRAY),
            true
        )
    }

    fun skippingClosedContainer(context: ImportContext) {
        send(
            Component.translatable(
                "htslreborn.toast.skipping",
                Component.translatable(context.translationKey())
            ).withStyle(ChatFormatting.RED),
            Component.translatable("htslreborn.toast.no_container").withStyle(ChatFormatting.GRAY)
        )
    }

    fun currentlyPaused() {
        send(
            Component.translatable("htslreborn.toast.currently_paused").withStyle(ChatFormatting.RED),
            Component.translatable(
                "htslreborn.toast.currently_paused.description",
                Component.translatable("htslreborn.toast.command.resume").withStyle(ChatFormatting.YELLOW),
                Component.translatable("htslreborn.toast.command.cancel").withStyle(ChatFormatting.YELLOW)
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    fun failedToCompile(reason: String) {
        send(
            Component.translatable("htslreborn.toast.failed_to_compile").withStyle(ChatFormatting.RED),
            Component.translatable(
                "htslreborn.toast.failed_to_compile.description",
                Component.literal(reason)
            ).withStyle(ChatFormatting.GRAY),
            true
        )
    }
}