package llc.redstone.htslreborn.htslio

import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsapi.config.SystemsAPIConfig

object SystemsApiImportTuning {
    private const val DEFAULT_BASE_CLICK_DELAY_MS = 50L
    private const val IMPORT_BASE_CLICK_DELAY_MS = 25L

    fun apply(): RestoreToken {
        val config = getSystemsApiConfig() ?: return RestoreToken(null, null)
        val originalDelay = config.baseClickDelay

        if (originalDelay != DEFAULT_BASE_CLICK_DELAY_MS) {
            return RestoreToken(null, null)
        }

        config.baseClickDelay = IMPORT_BASE_CLICK_DELAY_MS
        return RestoreToken(config, originalDelay)
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
        private val originalDelay: Long?
    ) {
        fun restore() {
            if (config == null || originalDelay == null) return
            if (config.baseClickDelay == IMPORT_BASE_CLICK_DELAY_MS) {
                config.baseClickDelay = originalDelay
            }
        }
    }
}
