package llc.redstone.htslreborn.htslio

import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsapi.config.SystemsAPIConfig

object SystemsApiImportTuning {
    private const val DEFAULT_BASE_CLICK_DELAY_MS = 50L
    private const val DEFAULT_MENU_TIMEOUT_MS = 1000L
    private const val DEFAULT_MENU_ITEM_LOADED_TIMEOUT_MS = 1000L
    private const val IMPORT_BASE_CLICK_DELAY_MS = 25L
    private const val IMPORT_MENU_TIMEOUT_MS = 2500L
    private const val IMPORT_MENU_ITEM_LOADED_TIMEOUT_MS = 2000L

    fun apply(): RestoreToken {
        val config = getSystemsApiConfig() ?: return RestoreToken(null, null, null, null)
        val originalDelay = config.baseClickDelay
        val originalMenuTimeout = config.menuTimeout
        val originalMenuItemLoadedTimeout = config.menuItemLoadedTimeout

        val delayToRestore = if (originalDelay == DEFAULT_BASE_CLICK_DELAY_MS) {
            config.baseClickDelay = IMPORT_BASE_CLICK_DELAY_MS
            originalDelay
        } else {
            null
        }

        val menuTimeoutToRestore = if (originalMenuTimeout <= DEFAULT_MENU_TIMEOUT_MS) {
            config.menuTimeout = IMPORT_MENU_TIMEOUT_MS
            originalMenuTimeout
        } else {
            null
        }

        val menuItemLoadedTimeoutToRestore = if (originalMenuItemLoadedTimeout <= DEFAULT_MENU_ITEM_LOADED_TIMEOUT_MS) {
            config.menuItemLoadedTimeout = IMPORT_MENU_ITEM_LOADED_TIMEOUT_MS
            originalMenuItemLoadedTimeout
        } else {
            null
        }

        return RestoreToken(config, delayToRestore, menuTimeoutToRestore, menuItemLoadedTimeoutToRestore)
    }

    private fun getSystemsApiConfig(): SystemsAPIConfig? {
        return runCatching {
            SystemsAPI::class.java
                .getMethod("getCONFIG\$systemsapi")
                .invoke(SystemsAPI) as SystemsAPIConfig
        }.getOrNull()
    }

    class RestoreToken internal constructor(
        private val config: SystemsAPIConfig?,
        private val originalDelay: Long?,
        private val originalMenuTimeout: Long?,
        private val originalMenuItemLoadedTimeout: Long?
    ) {
        fun restore() {
            if (config == null) return
            if (originalDelay != null && config.baseClickDelay == IMPORT_BASE_CLICK_DELAY_MS) {
                config.baseClickDelay = originalDelay
            }
            if (originalMenuTimeout != null && config.menuTimeout == IMPORT_MENU_TIMEOUT_MS) {
                config.menuTimeout = originalMenuTimeout
            }
            if (
                originalMenuItemLoadedTimeout != null &&
                config.menuItemLoadedTimeout == IMPORT_MENU_ITEM_LOADED_TIMEOUT_MS
            ) {
                config.menuItemLoadedTimeout = originalMenuItemLoadedTimeout
            }
        }
    }
}
