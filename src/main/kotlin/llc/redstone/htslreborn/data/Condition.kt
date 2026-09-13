@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package llc.redstone.htslreborn.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import llc.redstone.htslreborn.data.DamageCause
import llc.redstone.htslreborn.data.FishingEnvironment
import llc.redstone.htslreborn.data.PortalType
import llc.redstone.htslreborn.data.enums.Permission
import llc.redstone.htslreborn.data.enums.PotionEffect
import llc.redstone.htslreborn.data.DamageCause as Damage
import llc.redstone.htslreborn.data.FishingEnvironment as FishingEnv
import llc.redstone.htslreborn.data.PortalType as Portal


@Serializable
sealed class Condition(
    @Transient private val conditionName: String = ""
) : PropertyHolder {

    var inverted = false

    @DisplayName("Required Group")
    data class RequiredGroup(
        @property:Pagination val group: String? = null,
        val includeHigherGroups: Boolean = false,
    ) : Condition("IN_GROUP")

    @DisplayName("Variable Requirement")
    sealed class VariableRequirement(
        val holder: VariableHolder
    ) : Condition("VARIABLE_REQUIREMENT")

    @DisplayName("Variable Requirement")
    data class PlayerVariableRequirement(
        val variable: String = "Kills",
        val op: Comparator = Comparator.EQUALS,
        val value: String? = null,
        val fallbackValue: String? = null,
    ) : VariableRequirement(VariableHolder.Player)

    @DisplayName("Variable Requirement")
    data class TeamVariableRequirement(
        @property:Pagination val team: String?,
        val variable: String = "Kills",
        val op: Comparator = Comparator.EQUALS,
        val value: String? = null,
        val fallbackValue: String? = null
    ) : VariableRequirement(VariableHolder.Team)

    @DisplayName("Variable Requirement")
    data class GlobalVariableRequirement(
        val variable: String = "Kills",
        val op: Comparator = Comparator.EQUALS,
        val value: String? = null,
        val fallbackValue: String? = null
    ) : VariableRequirement(VariableHolder.Global)

    @DisplayName("Required Permission")
    data class HasPermission(
        val permission: Permission? = null,
    ) : Condition("HAS_PERMISSION")

    @DisplayName("Within Region")
    data class InRegion(
        @property:Pagination val region: String? = null,
    ) : Condition("IN_REGION")

    @DisplayName("Has Item")
    data class HasItem(
        val item: ItemStack? = null,
        val whatToCheck: ItemCheck = ItemCheck.Metadata,
        val whereToCheck: InventoryLocation = InventoryLocation.Anywhere,
        val amount: ItemAmount = ItemAmount.Any,
    ) : Condition("HAS_ITEM")

    @DisplayName("Doing Parkour")
    class InParkour : Condition("IN_PARKOUR")

    @DisplayName("Has Potion Effect")
    data class RequiredEffect(
        val effect: PotionEffect? = null,
    ) : Condition("POTION_EFFECT")

    @DisplayName("Player Sneaking")
    class PlayerSneaking : Condition("SNEAKING")

    @DisplayName("Player Flying")
    class PlayerFlying : Condition("FLYING")

    @DisplayName("Player Health")
    data class RequiredHealth(
        val mode: Comparator = Comparator.EQUALS,
        val amount: Double? = null,
    ) : Condition("HEALTH")

    @DisplayName("Max Player Health")
    data class RequiredMaxHealth(
        val mode: Comparator = Comparator.EQUALS,
        val amount: Double? = null,
    ) : Condition("MAX_HEALTH")

    @DisplayName("Player Hunger")
    data class RequiredHungerLevel(
        val mode: Comparator = Comparator.EQUALS,
        val amount: Double? = null,
    ) : Condition("HUNGER_LEVEL")

    @DisplayName("Required Gamemode")
    data class RequiredGameMode(
        val gameMode: GameMode? = null
    ) : Condition("GAMEMODE")

    @DisplayName("Placeholder Number Requirement")
    data class RequiredPlaceholderNumber(
        val placeholder: String? = null,
        val mode: Comparator = Comparator.EQUALS,
        val amount: String? = null,
    ) : Condition("PLACEHOLDER_NUMBER")

    @DisplayName("Required Team")
    data class RequiredTeam(
        @property:Pagination val team: String? = null,
    ) : Condition("IN_TEAM")

    @DisplayName("Pvp Enabled")
    class PvpEnabled : Condition("PVP_ENABLED")

    @DisplayName("Fishing Environment")
    data class FishingEnvironment(
        val environment: FishingEnv = FishingEnv.Water
    ) : Condition("FISHING_ENVIRONMENT")

    @DisplayName("Portal Type")
    data class PortalType(
        val type: Portal = Portal.NetherPortal
    ) : Condition("PORTAL_TYPE")

    @DisplayName("Damage Cause")
    data class DamageCause(
        val cause: Damage? = null
    ) : Condition("DAMAGE_CAUSE")

    @DisplayName("Damage Amount")
    data class RequiredDamageAmount(
        val mode: Comparator = Comparator.EQUALS,
        val amount: String? = null,
    ) : Condition("DAMAGE_AMOUNT")

    @DisplayName("Block Type")
    data class BlockType(
        val item: ItemStack? = null,
        val matchTypeOnly: Boolean = false,
    ) : Condition("BLOCK_TYPE")

    @DisplayName("Is Item")
    data class IsItem(
        val item: ItemStack? = null,
        val whatToCheck: ItemCheck = ItemCheck.Metadata,
        val whereToCheck: InventoryLocation = InventoryLocation.Anywhere,
        val amount: ItemAmount = ItemAmount.Any,
    ) : Condition("IS_ITEM")
}

enum class Comparator(vararg val symbol: String, override val key: String): Keyed {
    EQUALS("==", "=", "equals", key = "Equals"),
    NOT_EQUALS("!=", "notEquals", key = "THIS DOESNT EXIST :)"),
    GREATER_THAN(">", "greaterThan", key = "Greater Than"),
    LESS_THAN("<", "lessThan", key = "Less Than"),
    GREATER_THAN_OR_EQUAL(">=", "greaterThanOrEqual", key = "Greater Than or Equal"),
    LESS_THAN_OR_EQUAL("<=", "lessThanOrEqual", key = "Less Than or Equal");

    companion object {
        fun fromSymbol(symbol: String): Comparator? {
            return entries.find { it.symbol.any { s -> s.equals(symbol, ignoreCase = true) } }
        }
    }
}

enum class ItemCheck(override val key: String) : KeyedCycle {
    ItemType("Item Type"),
    Metadata("Metadata");

    override fun getOrdinal(): Int {
        return ordinal
    }

    companion object {
        fun fromKey(key: String): ItemCheck? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class ItemAmount(override val key: String) : KeyedCycle {
    Any("Any Amount"),
    Ge("Equal or Greater Amount");

    override fun getOrdinal(): Int {
        return ordinal
    }

    companion object {
        fun fromKey(key: String): ItemAmount? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class InventoryLocation(override val key: String) : Keyed {
    Hand("Hand"),
    Armor("Armor"),
    HotBar("Hotbar"),
    Inventory("Inventory"),
    Anywhere("Anywhere");

    companion object {
        fun fromKey(key: String): InventoryLocation? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class FishingEnvironment(override val key: String) : Keyed {
    Water("Water"),
    Lava("Lava");

    companion object {
        fun fromKey(key: String): FishingEnv? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class PortalType(override val key: String) : Keyed {
    EndPortal("End Portal"),
    NetherPortal("Nether Portal");

    companion object {
        fun fromKey(key: String): Portal? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class DamageCause(override val key: String) : Keyed {
    EntityAttack("Entity Attack"),
    Projectile("Projectile"),
    Suffocation("Suffocation"),
    Fall("Fall"),
    Lava("Lava"),
    Fire("Fire"),
    FireTick("Fire Tick"),
    Drowning("Drowning"),
    Starvation("Starvation"),
    Poison("Poison"),
    Thorns("Thorns");

    companion object {
        fun fromKey(key: String): Damage? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}