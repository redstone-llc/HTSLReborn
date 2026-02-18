package llc.redstone.htslreborn.utils

import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.systemsapi.util.CommandUtils
import llc.redstone.systemsapi.util.ItemStackUtils.giveItem
import llc.redstone.systemsapi.util.ItemUtils
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringNbtReader
import net.minecraft.world.GameMode
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrNull

object ItemUtils {

    fun ClientPlayerEntity.giveItem(path: Path): ItemStack {
        if (this.gameMode != GameMode.CREATIVE) CommandUtils.runCommand("gmc")
        val item = FileHandler.getItemForFile(path) ?: throw IllegalStateException("Could not find item at $path.")
        val slot = convertSlot(this.inventory.emptySlot) ?: throw IllegalStateException("No empty inventory slot!")
        item.giveItem(slot)
        return item
    }

    fun ClientPlayerEntity.saveItem(path: Path): ItemStack {
        val item = this.inventory.selectedStack ?: throw IllegalStateException("Could not find held item.")
        itemStackToFile(item, path.toFile())
        return item
    }

    private fun convertSlot(slot: Int): Int? {
//        if (MC.currentScreen !is GenericContainerScreen) return slot
        return when (slot) {
            in 0..8 -> slot + 36
            in 9..35 -> slot
            else -> null
        }
    }

    fun fileToNbtCompound(path: Path): NbtCompound {
        val name = path.name
        if (name.endsWith(".nbt")) {
            val dataInputStream = DataInputStream(path.inputStream())
            return NbtIo.readCompound(dataInputStream).also {
                dataInputStream.close()
            }
        }
        error("Unsupported file extension for NBT conversion: $name")
    }

    fun itemStackToFile(itemStack: ItemStack, file: File) {
        val nbtCompound = NbtHelper.serializeItemStack(itemStack).getOrNull()
        val dataOut = DataOutputStream(FileOutputStream(file))
        NbtIo.write(nbtCompound, dataOut)
        dataOut.close()
    }
    fun stringToNbtCompound(nbtString: String): NbtCompound {
        return StringNbtReader.readCompound(nbtString)
    }

    fun fileToItemStack(path: Path) =
        NbtHelper.deserializeItemStack(fileToNbtCompound(path)).getOrNull()

}