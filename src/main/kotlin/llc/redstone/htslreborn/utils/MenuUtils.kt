package llc.redstone.htslreborn.utils

//? if >=26.2 {
/*import llc.redstone.htslreborn.screen
*///?}

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.utils.InputUtils.Type
import llc.redstone.htslreborn.utils.PredicateUtils.ItemMatch
import llc.redstone.htslreborn.utils.PredicateUtils.ItemMatch.ItemExact
import llc.redstone.htslreborn.utils.PredicateUtils.ItemSelector
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameWithin
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput
*///?} else {
import net.minecraft.world.inventory.ClickType
//?}
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.collections.forEach
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
        if (screen is AnvilScreen) {
            InputUtils.handleInputType(Type.ANVIL)
        }

        val pending = pendingScreen ?: return
        val nameMatch = pendingNameMatch

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

    fun currentMenu() = MC.screen as? ContainerScreen

    suspend fun findSlots(
        predicate: (ItemStack) -> Boolean,
        paginated: Boolean = false
    ): List<Slot> {
        fun currentSlots() = currentMenu()?.menu?.slots?.filter { predicate(it.item) } ?: emptyList()

        var slots = currentSlots()
        var turns = 0
        while (slots.isEmpty() && paginated) {
            val nextPageSlot = findSlots(GlobalMenuItems.NEXT_PAGE).firstOrNull() ?: return emptyList()
            packetClick(nextPageSlot.index)
            onOpen(null, checkIfOpened = false)
            turns++
            slots = currentSlots()
        }
        return slots
    }

    suspend fun findSlots(name: String, paginated: Boolean = false, partial: Boolean = false): List<Slot> {
        return findSlots({
            if (!partial) it.hoverName.string == name else it.hoverName.string.contains(name)
        }, paginated)
    }

    suspend fun findSlots(name: String, item: Item, paginated: Boolean = false): List<Slot> {
        return findSlots({
            it.hoverName.string == name &&
                    it.item == item
        }, paginated)
    }

    suspend fun findSlots(selector: ItemSelector, paginated: Boolean = false): List<Slot> {
        return findSlots(
            selector.toPredicate(),
            paginated
        )
    }

    // CLICKING ITEMS IN MENUS
    suspend fun clickItems(
        predicate: (ItemStack) -> Boolean,
        packet: Boolean = true,
        button: Int = 0,
        paginated: Boolean = false,
        cacheKey: String? = null
    ) {
        findSlots(predicate, paginated).forEach { slot ->
            when (packet) {
                true -> packetClick(slot.index, button)
                false -> interactionClick(slot.index, button)
            }
        }
    }

    suspend fun clickItems(name: String, packet: Boolean = false, button: Int = 0, paginated: Boolean = false) {
        clickItems(
            {
                it.hoverName.string == name
            },
            packet,
            button,
            paginated,
            cacheKey = name
        )
    }

    suspend fun clickItems(name: String, item: Item, packet: Boolean = true, button: Int = 0, paginated: Boolean = false) {
        clickItems(
            {
                it.hoverName.string == name &&
                        it.item == item
            },
            packet,
            button,
            paginated,
            cacheKey = name
        )
    }

    suspend fun clickItems(selector: ItemSelector, packet: Boolean = true, button: Int = 0, paginated: Boolean = false) {
        clickItems(
            selector.toPredicate(),
            packet,
            button,
            paginated,
            cacheKey = selector.cacheKey
        )
    }

    object GlobalMenuItems {
        val NEXT_PAGE = ItemSelector(
            name = NameWithin(listOf("Next Page", "Left-click for next page!")),
            item = ItemExact(Items.ARROW)
        )
        val PREVIOUS_PAGE = ItemSelector(
            name = NameWithin(listOf("Last Page", "Left-click for previous page!")),
            item = ItemExact(Items.ARROW)
        )
    }
}