package llc.redstone.htslreborn.integration

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import llc.redstone.htslreborn.config.createSettingsGui

object ModMenuIntegration: ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory { parent ->
        createSettingsGui(parent)
    }
}