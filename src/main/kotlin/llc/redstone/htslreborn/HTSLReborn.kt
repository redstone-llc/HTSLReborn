package llc.redstone.htslreborn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import llc.redstone.htslreborn.importer.Operation
import llc.redstone.htslreborn.importer.Operation.Chat
import llc.redstone.htslreborn.importer.Operation.OpenMenu
import llc.redstone.htslreborn.importer.Queue
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.*
import net.fabricmc.api.ClientModInitializer
//? if <26.1 {
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
//?} else {
/*import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
*///?}
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files

//? if >=26.2 {
/*val Minecraft.screen: net.minecraft.client.gui.screens.Screen?
    get() = this.gui.screen()
*///?}

object HTSLReborn : ClientModInitializer {
    const val MOD_ID = "htslreborn"
    val LOGGER: Logger = LoggerFactory.getLogger("HTSL Reborn")
    const val VERSION = /*$ mod_version*/ "0.2.3";
    const val MINECRAFT = /*$ minecraft*/ "1.21.11";

    val MC = Minecraft.getInstance();

    val SCOPE = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    fun Player.sendSystemMessage(comp: Component) {
        //? if <26.1 {
        this.displayClientMessage(comp, false)
        //?} else {
        /*this.sendSystemMessage(comp)
       *///?}
    }

    override fun onInitializeClient() {
        LOGGER.info("Loaded HTSL Reborn v$VERSION for Minecraft $MINECRAFT.")

        ClientTickEvents.END_CLIENT_TICK.register {
            SCOPE.launch {
                Queue.onTick()
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, context ->
            dispatcher.register(literal("htsl")
                .executes {
                    Queue.clear()
                    Queue.enqueue {
                        +Chat("function edit test", command=true)
                        +OpenMenu(NameContains("Actions"))
                        +OpenMenu(NameExact("Add Action"), slot = 50)
                        +OpenMenu(NameContains("Action Settings"), slot = 10)
                    }
                    1
                }
            )
        }

        runCatching { Files.createDirectories(MC.gameDirectory.toPath().resolve("htsl")) }
    }
}