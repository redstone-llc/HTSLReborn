package llc.redstone.htslreborn.utils

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.data.ImportContext
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component

object ToastUtils {
    fun send(title: Component, description: Component) {
        MC.execute {
            //? if >=26.2 {
            //SystemToast.add(
            //   MC.gui.toastManager(),
            //   SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
            //   title,
            //   description,
            //)
            //? } else {
            MC.toastManager.addToast(
                SystemToast.multiline(
                    MC,
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
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
            ).withStyle(ChatFormatting.GRAY)
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
}