package llc.redstone.htslreborn.utils

import llc.redstone.systemsapi.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.StringNbtReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ItemConvertUtils {
    fun fileToNbtCompound(file: File): NbtCompound {
        val name = file.name
        if (name.endsWith(".nbt")) {
            val dataInputStream = DataInputStream(FileInputStream(file))
            return NbtIo.readCompound(dataInputStream).also {
                dataInputStream.close()
            }
        }
        error("Unsupported file extension for NBT conversion: $name")
    }

    fun itemStackToFile(itemStack: ItemStack, file: File) {
        val nbtCompound = ItemUtils.toNBT(itemStack)
        val dataOut = DataOutputStream(FileOutputStream(file))
        NbtIo.write(nbtCompound, dataOut)
        dataOut.close()
    }

    fun stringToNbtCompound(nbtString: String): NbtCompound {
        return StringNbtReader.readCompound(nbtString)
    }

    fun fileToItemStack(file: File) =
        ItemUtils.createFromNBT(fileToNbtCompound(file))
}