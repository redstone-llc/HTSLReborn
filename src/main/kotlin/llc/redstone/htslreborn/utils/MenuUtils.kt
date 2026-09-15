package llc.redstone.htslreborn.utils

//? if >=26.2 {
/*import llc.redstone.htslreborn.screen
*///?}

//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput
*///?} else {
//?}
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.utils.InputUtils.Type
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
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.milliseconds

object MenuUtils {
    var pendingScreen: CompletableDeferred<Screen?>? = null
    var pendingNameMatch: NameMatch? = null

    private var screenGeneration = 0L
    private var consumedGeneration = 0L

    fun markScreenConsumed() {
        consumedGeneration = screenGeneration
    }

    suspend fun onOpen(nameMatch: NameMatch?, checkIfOpened: Boolean = false): Screen? {
        val deferred = CompletableDeferred<Screen?>()
        pendingScreen?.cancel()
        pendingScreen = deferred
        pendingNameMatch = nameMatch

        val alreadyOpen = MC.screen?.takeIf { screen ->
            nameMatch?.matches(screen.title.string) != false &&
                    (checkIfOpened || screenGeneration > consumedGeneration)
        }

        if (alreadyOpen != null) {
            pendingScreen = null
            pendingNameMatch = null
            markScreenConsumed()
            if (!checkIfOpened) awaitUntilMenuItemsLoaded()
            return alreadyOpen
        } else {
            return try {
                withTimeout(5000.milliseconds) {
                    deferred.await()
                }
            } catch (e: Exception) {
                null
            } finally {
                pendingScreen = null
                pendingNameMatch = null
                awaitUntilMenuItemsLoaded()
            }
        }
    }

    var isLoading = false
    var lastItemAddedTimestamp = 0L
    var itemsLoaded = mutableMapOf<String, ItemStack>()

    var pendingLoaded: CompletableDeferred<Screen>? = null
    private suspend fun awaitUntilMenuItemsLoaded(): Screen {
        val deferred = CompletableDeferred<Screen>()
        pendingLoaded?.cancel()
        pendingLoaded = deferred

        return try {
            isLoading = true
            itemsLoaded.clear()
            lastItemAddedTimestamp = System.currentTimeMillis()
            withTimeout(5000.milliseconds) {
                deferred.await()
            }
        } finally {
            if (pendingLoaded === deferred) pendingLoaded = null
            ClientThread.settleMenu()
        }
    }

    internal fun render() {
        if (!isLoading) return
        val screen = MC.screen as? AbstractContainerScreen<*> ?: return
        val delay = 0L // your gui delay
        if (System.currentTimeMillis() - lastItemAddedTimestamp < delay) return

        val slots = screen.menu.slots
        var startIndex = slots.size - 44
        if (startIndex < 0) {
            startIndex = 0
        }
        val hotbarSlots = slots.subList(startIndex, startIndex + 9)
        if (hotbarSlots.all { it.item.isEmpty }) return
        isLoading = false
        val pending = pendingLoaded ?: return
        pendingLoaded = null
        pending.complete(screen)
    }

    internal fun renderStack(stack: ItemStack) {
        if (!isLoading) return
        val displayName = stack.hoverName.string
        if (itemsLoaded.containsKey(displayName)) return
        lastItemAddedTimestamp = System.currentTimeMillis()
        itemsLoaded[displayName] = stack
    }

    fun onScreenOpen(screen: Screen) {
        screenGeneration++

        if (screen is AnvilScreen) {
            InputUtils.handleInputType(Type.ANVIL)
        }

        val pending = pendingScreen ?: return
        val nameMatch = pendingNameMatch

        if (nameMatch == null || nameMatch.matches(screen.title.string)) {
            pendingScreen = null
            pendingNameMatch = null
            markScreenConsumed()
            pending.complete(screen)
        }
    }

    suspend fun packetClick(slot: Int, button: Int = 0) = ClientThread.send {
        val gui = MC.screen as? AbstractContainerScreen<*> ?: return@send
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

    suspend fun interactionClick(slot: Int, button: Int = 0) = ClientThread.send {
        val gui = MC.screen as? AbstractContainerScreen<*> ?: return@send

        val player = MC.player ?: return@send
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

    suspend fun clickPlayerSlot(slot: Int, button: Int = 0) {
        val gui = currentMenu() ?: return
        val playerSlot = when (slot) {
            in 0..8 -> slot + gui.menu.slots.size - 9
            in 9..35 -> {
                slot + gui.menu.slots.size - 45
            }
            else -> throw IllegalArgumentException("Invalid player slot index: $slot")
        }
        packetClick(playerSlot, button)
    }

    fun currentMenu() = MC.screen as? ContainerScreen

    fun isActionContainerOpen() = currentMenu()?.title?.string?.contains("Actions", ignoreCase = true) == true

    suspend fun findSlots(
        predicate: (ItemStack) -> Boolean,
        paginated: Boolean = false
    ): List<Slot> {
        fun currentSlots() = currentMenu()?.menu?.slots?.filter { predicate(it.item) } ?: emptyList()

        var slots = currentSlots()
        var turns = 0
        while (slots.isEmpty() && paginated) {
            val nextPageSlot = findSlots(GlobalMenuItems.NEXT_PAGE).firstOrNull() ?: return emptyList()
            // Make sure onOpen waits for the *next* page rather than handing back the current one.
            markScreenConsumed()
            packetClick(nextPageSlot.index)
            onOpen(null, checkIfOpened = false)
            turns++
            slots = currentSlots()
        }
        return slots
    }

    // PAGINATION + ACTION COUNTING
    val ACTION_SLOTS = ((10..16) + (19..25) + (28..34)).toList()
    private val NO_ACTIONS = ItemSelector(
        name = NameMatch.NameExact("No Actions!"),
        item = ItemExact(Items.BEDROCK)
    ).toPredicate()

    suspend fun nextPage(): Boolean {
        val next = findSlots(GlobalMenuItems.NEXT_PAGE).firstOrNull() ?: return false
        markScreenConsumed()
        interactionClick(next.index)
        onOpen(null, checkIfOpened = false)
        return true
    }

    suspend fun goToFirstPage() {
        val prev = findSlots(GlobalMenuItems.PREVIOUS_PAGE).firstOrNull() ?: return
        markScreenConsumed()
        interactionClick(prev.index, button = 1)
        onOpen(null, checkIfOpened = false)
    }

    fun actionSlotsOnPage(): List<Slot> =
        currentMenu()?.menu?.slots?.filter {
            it.index in ACTION_SLOTS && !it.item.isEmpty && !NO_ACTIONS(it.item)
        } ?: emptyList()

    /** Walks every page; leaves the menu on the last page. */
    suspend fun countActions(): Int {
        goToFirstPage()
        var count = actionSlotsOnPage().size
        while (nextPage()) count += actionSlotsOnPage().size
        return count
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

    suspend fun clickItems(
        name: String,
        item: Item,
        packet: Boolean = true,
        button: Int = 0,
        paginated: Boolean = false
    ) {
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

    suspend fun clickItems(
        selector: ItemSelector,
        packet: Boolean = true,
        button: Int = 0,
        paginated: Boolean = false
    ) {
        clickItems(
            selector.toPredicate(),
            packet,
            button,
            paginated,
            cacheKey = selector.cacheKey
        )
    }

    fun getSlot(propertySlotIndex: Int): Slot {
        val gui = currentMenu() ?: error("No menu open")
        return gui.menu.slots.getOrNull(propertySlotIndex)
            ?: throw IllegalStateException("Property slot index $propertySlotIndex out of bounds for menu with ${gui.menu.slots.size} slots")
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