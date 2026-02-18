package llc.redstone.htslreborn.utils

import llc.redstone.htslreborn.HTSLReborn.MC
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.registry.RegistryWrapper
import java.util.Optional

object NbtHelper {
    fun serializeItemStack(stack: ItemStack): Optional<NbtCompound> {
        val access = getRegistryAccess() ?: return Optional.empty()
        val ops = access.getOps(NbtOps.INSTANCE)
        return ItemStack.CODEC.encodeStart(ops, stack).map { it as NbtCompound }.resultOrPartial()
    }

    fun deserializeItemStack(tag: NbtCompound): Optional<ItemStack> {
        val access = getRegistryAccess() ?: return Optional.empty()
        val ops = access.getOps(NbtOps.INSTANCE)
        return ItemStack.CODEC.parse(ops, tag).resultOrPartial()
    }

    private fun getRegistryAccess(): RegistryWrapper.WrapperLookup? {
        return MC.world?.registryManager
    }
}
