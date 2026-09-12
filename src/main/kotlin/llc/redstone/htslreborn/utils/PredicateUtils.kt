package llc.redstone.htslreborn.utils

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.intellij.lang.annotations.RegExp

object PredicateUtils {
    data class ItemSelector(
        val name: NameMatch? = null,
        val item: ItemMatch? = null,
        val extra: ((ItemStack) -> Boolean)? = null,
    ) {
        constructor(name: String, item: Item) : this(
            name = NameMatch.NameExact(name),
            item = ItemMatch.ItemExact(item)
        )

        fun toPredicate(): (ItemStack) -> Boolean = { stack ->
            val nameOk = name?.matches(stack.hoverName.string) ?: true
            val itemOk = item?.matches(stack.item) ?: true
            val extraOk = extra?.invoke(stack) ?: true

            nameOk && itemOk && extraOk
        }

        /** Stable identifier for this selector, used to learn how many page turns it sits behind. */
        val cacheKey: String? get() = name?.cacheKey
    }

    sealed interface NameMatch {
        data class NameExact(val value: String) : NameMatch
        data class NameWithin(val values: List<String>) : NameMatch
        data class NameContains(val value: String) : NameMatch
        data class NameRegex(@param:RegExp val value: String) : NameMatch {
            val regex: Regex = Regex(value)
        }

        fun matches(actual: String): Boolean = when (this) {
            is NameExact -> actual == this.value
            is NameWithin -> this.values.contains(actual)
            is NameContains -> actual.contains(this.value)
            is NameRegex -> this.regex.matches(actual)
        }

        /** Stable identifier for this matcher, used to learn how many page turns it sits behind. */
        val cacheKey: String
            get() = when (this) {
                is NameExact -> this.value
                is NameWithin -> this.values.joinToString("/")
                is NameContains -> this.value
                is NameRegex -> this.value
            }
    }

    sealed interface ItemMatch {
        data class ItemExact(val item: Item) : ItemMatch
        data class ItemWithin(val items: List<Item>) : ItemMatch

        fun matches(actual: Item): Boolean = when (this) {
            is ItemExact -> this.item == actual
            is ItemWithin -> this.items.contains(actual)
        }
    }
}