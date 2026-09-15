package llc.redstone.htslreborn.utils

//? if >=26.2 {
/*import llc.redstone.htslreborn.screen
*///?}

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.utils.ItemStackUtils.giveItem
import llc.redstone.htslreborn.utils.TextUtils.convertTextToString
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object InputUtils {
    var pendingInput: CompletableDeferred<Type>? = null
    var type: Type? = null

    suspend fun doInput(input: String): Boolean {
        pendingInput = CompletableDeferred()

        return try {
            if (type != null) {
                return handleInput(input, type!!)
            }
            withTimeout(5000.milliseconds) {
                val type = pendingInput?.await() ?: return@withTimeout false
                return@withTimeout handleInput(input, type)
            }
        } catch (e: Exception) {
            false
        } finally {
            pendingInput = null
        }
    }

    suspend fun handleInput(input: String, type: Type): Boolean {
        try {
            return when (type) {
                Type.CHAT -> {
                    ClientThread.send {
                        Minecraft.getInstance().connection
                            ?.sendChat(input) ?: error("Failed to send chat message")
                    }
                    this.type = null
                    true
                }

                Type.ANVIL -> {
                    val screen = MC.screen as? AnvilScreen ?: return false
                    delay(200.milliseconds)

                    if (ClientThread.run { screen.menu.setItemName(input) }) {
                        ClientThread.send { MC.connection?.send(ServerboundRenameItemPacket(input)) }
                    }
                    MenuUtils.interactionClick(2)
                    awaitScreen("Failed to close anvil screen") { it !is AnvilScreen }
                    this.type = null
                    true
                }
            }
        } finally {
        }
    }

    private suspend fun awaitScreen(failure: String, timeout: Duration = 5000.milliseconds, predicate: (Screen?) -> Boolean) {
        val deadline = System.currentTimeMillis() + timeout.inWholeMilliseconds
        while (!predicate(MC.screen)) {
            if (System.currentTimeMillis() > deadline) error(failure)
            delay(50.milliseconds)
        }
    }

    fun handleInputType(type: Type) {
        this.type = type
        pendingInput?.complete(type)
    }

    fun getClientPing(): Int {
        val playerInfo = MC.connection?.getPlayerInfo(MC.player?.uuid ?: return -1) ?: return -1
        return playerInfo.latency
    }

    class ReceivedItem(val stack: ItemStack, val inventorySlot: Int, val previous: ItemStack) {
        suspend fun restore() {
            previous.giveItem(ItemUtils.convertSlot(inventorySlot) ?: return)
        }
    }

    suspend fun getItemFromMenu(
        displayName: String?, compareStack: ItemStack?,
        click: suspend () -> Unit
    ): ReceivedItem {
        val before = ClientThread.run { snapshotInventory() }

        click()

        val deadline = System.currentTimeMillis() + 5000
        while (true) {
            val received = ClientThread.run { diffInventory(before) }
                .firstOrNull { matchesPendingItem(it.stack, displayName, compareStack) }
            if (received != null) return received
            if (System.currentTimeMillis() > deadline) {
                error("Timed out waiting to receive an item from the menu (is your inventory full?)")
            }
            delay(50.milliseconds)
        }
    }

    private fun snapshotInventory(): List<ItemStack> {
        val inventory = MC.player?.inventory ?: error("No player")
        return (0 until Inventory.INVENTORY_SIZE).map { inventory.getItem(it).copy() }
    }

    private fun diffInventory(before: List<ItemStack>): List<ReceivedItem> {
        val inventory = MC.player?.inventory ?: error("No player")
        return before.mapIndexedNotNull { slot, old ->
            val current = inventory.getItem(slot)
            val gained = when {
                current.isEmpty -> return@mapIndexedNotNull null
                old.isEmpty || !ItemStack.isSameItemSameComponents(old, current) -> current.copy()
                current.count > old.count -> current.copyWithCount(current.count - old.count)
                else -> return@mapIndexedNotNull null
            }
            ReceivedItem(gained, slot, old)
        }
    }

    private fun matchesPendingItem(stack: ItemStack, displayName: String?, compareStack: ItemStack?): Boolean {
        val customName = convertTextToString(stack.customName, false) ?: I18n.get(stack.item.descriptionId)
        if (customName.contains("Housing Menu")) return false // Housing menu item, not an actual item
        if (displayName != null && customName.split(" ").any { !displayName.contains(it) }) return false
        if (compareStack != null && stack.item != compareStack.item) return false
        return true
    }


    internal var pendingString: CompletableDeferred<String>? = null
    suspend fun getPreviousInput(click: suspend () -> Unit): String {
        val deferred = CompletableDeferred<String>()
        pendingString?.cancel()
        pendingString = deferred

        return try {
            click()
            withTimeout(5000.milliseconds) { deferred.await() }
        } finally {
            if (pendingString === deferred) pendingString = null
            CommandUtils.runCommand("chatinput cancel")
            if (MC.screen is AnvilScreen) {
                throw IllegalStateException("Received a potentially inaccurate value while exporting. To fix this, set your text input method to Chat in /settings and export again.")
            }
        }
    }
    internal fun receivePreviousInput(value: String) {
        pendingString?.let { current ->
            pendingString = null
            current.complete(value)
        }
    }

    enum class Type {
        CHAT,
        ANVIL
    }
}