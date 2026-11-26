package llc.redstone.htslreborn.utils

import llc.redstone.htslreborn.HTSLReborn.MC
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.world.item.ItemStack
import java.util.*

object NbtHelper {
    fun serializeItemStack(stack: ItemStack): Optional<CompoundTag> {
        val access = getRegistryAccess() ?: return Optional.empty()
        val ops = access.createSerializationContext(NbtOps.INSTANCE)
        return ItemStack.CODEC.encodeStart(ops, stack).map { it as CompoundTag }.resultOrPartial()
    }

    fun deserializeItemStack(tag: CompoundTag): Optional<ItemStack> {
        val access = getRegistryAccess() ?: return Optional.empty()
        val ops = access.createSerializationContext(NbtOps.INSTANCE)
        return ItemStack.CODEC.parse(ops, tag).resultOrPartial()
    }

    private fun getRegistryAccess(): RegistryAccess? {
        return MC.level?.registryAccess()
    }
}
