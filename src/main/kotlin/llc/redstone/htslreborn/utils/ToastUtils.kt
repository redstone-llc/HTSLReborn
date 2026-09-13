package llc.redstone.htslreborn.utils

import llc.redstone.htslreborn.HTSLReborn.MC
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component

object ToastUtils {
    fun send(title: String, description: String) {
        MC.toastManager.addToast(
            SystemToast.multiline(
                MC,
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal(title),
                Component.literal(description),
            )
        );
    }
}