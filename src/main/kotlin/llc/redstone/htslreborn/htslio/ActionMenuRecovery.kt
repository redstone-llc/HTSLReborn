package llc.redstone.htslreborn.htslio

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsapi.importer.ActionContainer
import llc.redstone.systemsapi.util.MenuUtils
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen

internal object ActionMenuRecovery {
    private fun Throwable.isMenuTimeout(name: String): Boolean {
        val currentMessage = message
        if (this is IllegalStateException && currentMessage != null) {
            val expectedTitle = "Timed out waiting for menu: NameContains(value=$name)"
            if (currentMessage.contains(expectedTitle)) {
                return true
            }
        }
        return cause?.isMenuTimeout(name) == true
    }

    fun Throwable.isTransientMenuClose(): Boolean {
        if (this is ClassCastException && message?.contains("Expected GenericContainerScreen but found null") == true) {
            return true
        }
        return cause?.isTransientMenuClose() == true
    }

    fun Throwable.isActionMenuTimeout(title: String): Boolean {
        return isMenuTimeout(title) || isMenuTimeout(" $title")
    }

    fun Throwable.isActionSettingsTimeout(): Boolean {
        return isMenuTimeout("Action Settings")
    }

    fun Throwable.isSettingsTimeout(): Boolean {
        return isMenuTimeout("Settings")
    }

    fun Throwable.isRecoverableActionMenuFailure(title: String): Boolean {
        return isTransientMenuClose() || isActionMenuTimeout(title)
    }

    suspend fun recover(title: String): Boolean {
        repeat(8) {
            val currentTitle = waitForContainerMenu()?.title?.string

            if (currentTitle != null && currentTitle.contains(title)) {
                rewindToFirstActionPage(title)
                return true
            }

            if (currentTitle != null && shouldClickBack(currentTitle, title)) {
                runCatching { MenuUtils.clickItems(ActionContainer.MenuItems.BACK) }
                SystemsAPI.scaledDelay(2.0)
            }

            val returnedTitle = waitForContainerMenu()?.title?.string
            if (returnedTitle != null && returnedTitle.contains(title)) {
                rewindToFirstActionPage(title)
                return true
            }

            SystemsAPI.scaledDelay(2.0)
        }

        return (MC.currentScreen as? GenericContainerScreen)
            ?.title
            ?.string
            ?.contains(title) == true
    }

    private fun shouldClickBack(currentTitle: String, title: String): Boolean {
        if (currentTitle.contains(title)) return false

        return currentTitle == "Action Settings" ||
                currentTitle == "Settings" ||
                currentTitle.contains("Edit Actions") ||
                currentTitle.contains("Edit Conditions") ||
                currentTitle == "Add Action" ||
                currentTitle.contains("Select Option") ||
                currentTitle.contains("Select Inventory Slot") ||
                currentTitle.contains("Select an Item")
    }

    private suspend fun waitForContainerMenu(): GenericContainerScreen? {
        repeat(20) {
            (MC.currentScreen as? GenericContainerScreen)?.let { return it }
            SystemsAPI.scaledDelay()
        }
        return MC.currentScreen as? GenericContainerScreen
    }

    private suspend fun rewindToFirstActionPage(title: String) {
        repeat(10) {
            val currentTitle = (MC.currentScreen as? GenericContainerScreen)?.title?.string ?: return
            if (!currentTitle.contains(title)) return

            val previousPageSlot = runCatching {
                MenuUtils.findSlots(MenuUtils.GlobalMenuItems.PREVIOUS_PAGE).firstOrNull()
            }.getOrNull() ?: return

            MenuUtils.packetClick(previousPageSlot.id, button = 1)
            SystemsAPI.scaledDelay(2.0)
        }
    }
}
