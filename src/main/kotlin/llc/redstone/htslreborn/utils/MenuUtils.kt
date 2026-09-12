package llc.redstone.htslreborn.utils

//? if >=26.2 {
/*import llc.redstone.htslreborn.screen
*///?}

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput
*///?} else {
import net.minecraft.world.inventory.ClickType
//?}
import kotlin.time.Duration.Companion.milliseconds

object MenuUtils {
    var pendingScreen: CompletableDeferred<Screen?>? = null
    var pendingNameMatch: NameMatch? = null
    suspend fun onOpen(nameMatch: NameMatch?, checkIfOpened: Boolean = true): Screen? {
        val deferred = CompletableDeferred<Screen?>()
        pendingScreen?.cancel()
        pendingScreen = deferred
        pendingNameMatch = nameMatch

        val alreadyOpen = if (checkIfOpened) {
            MC.screen?.takeIf { screen ->
                pendingNameMatch?.matches(screen.title.string) != false
            }
        } else null

        if (alreadyOpen != null) {
            pendingScreen = null
            pendingNameMatch = null
            return alreadyOpen
        } else {
            try {
                return withTimeout(5000.milliseconds) {
                     deferred.await()
                }
            } catch (e: Exception) {
                pendingScreen = null
                pendingNameMatch = null
                return null
            }
        }
    }

    fun onScreenOpen(screen: Screen) {
        val pending = pendingScreen ?: return
        val nameMatch = pendingNameMatch

        println("Screen opened: ${screen.title.string}, pending name match: $nameMatch")
        if (nameMatch == null || nameMatch.matches(screen.title.string)) {
            pendingScreen = null
            pendingNameMatch = null
            pending.complete(screen)
        }
    }

    fun packetClick(slot: Int, button: Int = 0) {
        val gui = MC.screen as? AbstractContainerScreen<*> ?: return

        val pkt = ServerboundContainerClickPacket(
            gui.menu.containerId,
            gui.menu.stateId,
            slot.toShort(),
            button.toByte(),
            //? if >=26.1 {
            /*ContainerInput.PICKUP,
            *///?} else {
            ClickType.PICKUP,
            //?}
            Int2ObjectOpenHashMap(),
            HashedStack.EMPTY
        )

        MC.connection?.send(pkt) ?: error("Failed to send click packet")
    }

    fun interactionClick(slot: Int, button: Int = 0) {
        val gui = MC.screen as? AbstractContainerScreen<*> ?: return

        val player = MC.player ?: return
        //? if >=26.1 {
        /*MC.gameMode?.handleContainerInput(
            gui.menu.containerId,
            slot,
            button,
            ContainerInput.PICKUP,
            player
        )
        *///?} else {
        MC.gameMode?.handleInventoryMouseClick(
            gui.menu.containerId,
            slot,
            button,
            ClickType.PICKUP,
            player
        )
        //?}
    }
}