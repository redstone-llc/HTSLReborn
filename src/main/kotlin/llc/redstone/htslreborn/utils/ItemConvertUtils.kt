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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.name

object ItemConvertUtils {
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
        val nbtCompound = ItemUtils.toNBT(itemStack)
        val dataOut = DataOutputStream(FileOutputStream(file))
        NbtIo.write(nbtCompound, dataOut)
        dataOut.close()
    }

    fun stringToNbtCompound(nbtString: String): NbtCompound {
        return StringNbtReader.readCompound(nbtString)
    }

    fun fileToItemStack(path: Path) =
        ItemUtils.createFromNBT(fileToNbtCompound(path))
}