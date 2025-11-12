@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package dev.wekend.housingtoolbox.feature.importer.data

import dev.wekend.housingtoolbox.feature.importer.lexar.Comparators
import dev.wekend.housingtoolbox.feature.importer.lexar.Operators
import dev.wekend.housingtoolbox.feature.importer.lexar.Tokens
import guru.zoroark.tegral.niwen.lexer.Token
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/*
Borrowed from https://github.com/sndyx/hsl, licensed under the MIT License
 */

@Serializable
sealed class Condition(
    @Transient val conditionName: String = ""
) {
    
    companion object {
        fun createCondition(keyword: String, iterator: Iterator<Token>, inverted: Boolean = false): Condition? {
            val clazz = Condition::class.sealedSubclasses.find {
                it.annotations
                    .filterIsInstance<Keyword>()
                    .any { it.keyword == keyword }
            } ?: return null

            return clazz.primaryConstructor?.let { constructor ->
                val args = constructor.parameters.associateWith args@{ param ->
                    val prop = clazz.memberProperties.find { it.name == param.name }!!
                    val re = when (prop.returnType.jvmErasure.simpleName) {
                        "String" -> iterator.next().string
                        "Int" -> iterator.next().string.toInt()
                        "Long" -> iterator.next().string.removeSuffix("L").toLong()
                        "Double" -> iterator.next().string.toDouble()
                        "Boolean" -> iterator.next().string.toBoolean()
                        "StatValue" -> {
                            val token = iterator.next()
                            when (token.tokenType) {
                                Tokens.STRING -> StatValue.Str(token.string)
                                Tokens.INT -> StatValue.I64(token.string.toLong())
                                Tokens.LONG -> StatValue.I64(token.string.removeSuffix("L").toLong())
                                Tokens.DOUBLE -> StatValue.Dbl(token.string.toDouble())
                                else -> StatValue.Str(token.string)
                            }
                        }

                        "Comparison" -> {
                            val token = iterator.next()
                            Comparison.entries.find {
                                when (token.tokenType) {
                                    Comparators.EQUALS -> it == Comparison.Eq
                                    Comparators.GREATER_THAN -> it == Comparison.Gt
                                    Comparators.GREATER_THAN_OR_EQUAL -> it == Comparison.Ge
                                    Comparators.LESS_THAN -> it == Comparison.Lt
                                    Comparators.LESS_THAN_OR_EQUAL -> it == Comparison.Le
                                    else -> false
                                }
                            } ?: error("Unknown Comparison: ${token.string}")
                        }

                        "PotionEffect" -> {
                            val token = iterator.next()
                            PotionEffect.entries.find { it.key == token.string }
                                ?: error("Unknown PotionEffect: ${token.string}")
                        }

                        "GameMode" -> {
                            val token = iterator.next()
                            GameMode.entries.find { it.key == token.string }
                                ?: error("Unknown GameMode: ${token.string}")
                        }

                        "Permission" -> {
                            val token = iterator.next()
                            Permission.entries.find { it.key == token.string }
                                ?: error("Unknown Permission: ${token.string}")
                        }

                        "ItemCheck" -> {
                            val token = iterator.next()
                            ItemCheck.entries.find { it.key == token.string }
                                ?: error("Unknown ItemCheck: ${token.string}")
                        }

                        "ItemAmount" -> {
                            val token = iterator.next()
                            ItemAmount.entries.find { it.key == token.string }
                                ?: error("Unknown ItemAmount: ${token.string}")
                        }

                        "InventoryLocation" -> {
                            val token = iterator.next()
                            InventoryLocation.entries.find { it.key == token.string }
                                ?: error("Unknown InventoryLocation: ${token.string}")
                        }

                        "FishingEnvironment" -> {
                            val token = iterator.next()
                            dev.wekend.housingtoolbox.feature.importer.data.FishingEnvironment.entries.find { it.key == token.string }
                                ?: error("Unknown FishingEnvironment: ${token.string}")
                        }

                        "PortalType" -> {
                            val token = iterator.next()
                            dev.wekend.housingtoolbox.feature.importer.data.PortalType.entries.find { it.key == token.string }
                                ?: error("Unknown PortalType: ${token.string}")
                        }

                        "DamageCause" -> {
                            val token = iterator.next()
                            dev.wekend.housingtoolbox.feature.importer.data.DamageCause.entries.find { it.key == token.string }
                                ?: error("Unknown DamageCause: ${token.string}")
                        }

                        else -> error("Unsupported parameter type: ${prop.returnType.jvmErasure.simpleName} in Condition ${clazz.simpleName}")
                    }

                    return@args re
                }

                val con = constructor.callBy(args)
                con.inverted = inverted
                return@let con
            }
        }
    }

    var inverted = false

    @Serializable
    @SerialName("IN_GROUP")
    @Keyword("hasGroup")
    data class RequiredGroup(
        @SerialName("required_group")
        val group: String,
        @SerialName("include_higher_groups")
        val includeHigherGroups: Boolean,
    ) : Condition("IN_GROUP")

    @Keyword("var")
    @Keyword("stat")
    data class PlayerVariableRequirement(
        val variable: String,
        val op: Comparison,
        val value: StatValue,
    ) : Condition("VARIABLE_REQUIREMENT") {
        val holder = VariableHolder.Player
    }

    @Keyword("teamvar")
    @Keyword("teamstat")
    data class TeamVariableRequirement(
        val team: String?,
        val variable: String,
        val op: Comparison,
        val value: StatValue,
    ) : Condition("VARIABLE_REQUIREMENT") {
        val holder = VariableHolder.Team
    }

    @Keyword("globalvar")
    @Keyword("globalstat")
    data class GlobalVariableRequirement(
        val variable: String,
        val op: Comparison,
        val value: StatValue,
    ) : Condition("VARIABLE_REQUIREMENT") {
        val holder = VariableHolder.Global
    }

    @Serializable
    @SerialName("HAS_PERMISSION")
    @Keyword("hasPermission")
    data class HasPermission(
        @SerialName("required_permission")
        val permission: Permission,
    ) : Condition("HAS_PERMISSION")
    @Serializable
    @SerialName("IN_REGION")
    @Keyword("inRegion")
    data class InRegion(
        val region: String,
    ) : Condition("IN_REGION")
    @Serializable
    @SerialName("HAS_ITEM")
    @Keyword("hasItem")
    data class HasItem(
        val item: ItemStack,
        @SerialName("what_to_check") val whatToCheck: ItemCheck,
        @SerialName("where_to_check") val whereToCheck: InventoryLocation,
        @SerialName("required_amount") val amount: ItemAmount,
    ) : Condition("HAS_ITEM")
    @Serializable
    @SerialName("IN_PARKOUR")
    @Keyword("doingParkour")
    data object InParkour : Condition("IN_PARKOUR")
    @Serializable
    @SerialName("POTION_EFFECT")
    @Keyword("hasPotion")
    data class RequiredEffect(
        val effect: PotionEffect,
    ) : Condition("POTION_EFFECT")
    @Serializable
    @SerialName("SNEAKING")
    @Keyword("isSneaking")
    data object PlayerSneaking : Condition("SNEAKING")
    @Serializable
    @SerialName("FLYING")
    @Keyword("isFlying")
    data object PlayerFlying : Condition("FLYING")
    @Serializable
    @SerialName("HEALTH")
    @Keyword("health")
    data class RequiredHealth(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("HEALTH")
    @Serializable
    @SerialName("MAX_HEALTH")
    @Keyword("maxHealth")
    data class RequiredMaxHealth(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("MAX_HEALTH")
    @Serializable
    @SerialName("HUNGER_LEVEL")
    @Keyword("hunger")
    data class RequiredHungerLevel(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("HUNGER_LEVEL")
    @Serializable
    @SerialName("GAMEMODE")
    @Keyword("gamemode")
    data class RequiredGameMode(
        @SerialName("required_gamemode")
        val gameMode: GameMode
    ) : Condition("GAMEMODE")
    @Serializable
    @SerialName("PLACEHOLDER_NUMBER")
    @Keyword("placeholder")
    data class RequiredPlaceholderNumber(
        val placeholder: String,
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("PLACEHOLDER_NUMBER")
    @Serializable
    @SerialName("IN_TEAM")
    @Keyword("inTeam")
    data class RequiredTeam(
        @SerialName("required_team")
        val team: String,
    ) : Condition("IN_TEAM")
    @Serializable
    @SerialName("PVP_ENABLED")
    @Keyword("canPvp")
    data object PvpEnabled : Condition("PVP_ENABLED")
    @Serializable
    @SerialName("FISHING_ENVIRONMENT")
    @Keyword("fishingEnv")
    data class FishingEnvironment(
        val environment: dev.wekend.housingtoolbox.feature.importer.data.FishingEnvironment
    ) : Condition("FISHING_ENVIRONMENT")
    @Serializable
    @SerialName("PORTAL_TYPE")
    @Keyword("portal")
    data class PortalType(
        @SerialName("portal_type")
        val type: dev.wekend.housingtoolbox.feature.importer.data.PortalType
    ) : Condition("PORTAL_TYPE")
    @Serializable
    @SerialName("DAMAGE_CAUSE")
    @Keyword("damageCause")
    data class DamageCause(
        val cause: dev.wekend.housingtoolbox.feature.importer.data.DamageCause
    ) : Condition("DAMAGE_CAUSE")
    @Serializable
    @SerialName("DAMAGE_AMOUNT")
    @Keyword("damageAmount")
    data class RequiredDamageAmount(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("DAMAGE_AMOUNT")
    @Serializable
    @SerialName("BLOCK_TYPE")
    @Keyword("blockType")
    data class BlockType(
        val item: ItemStack,
        @SerialName("match_type_only")
        val matchTypeOnly: Boolean,
    ) : Condition("BLOCK_TYPE")
    @Serializable
    @SerialName("IS_ITEM")
    @Keyword("isItem")
    data class IsItem(
        val item: ItemStack,
        @SerialName("what_to_check") val whatToCheck: ItemCheck,
        @SerialName("where_to_check") val whereToCheck: InventoryLocation,
        @SerialName("required_amount") val amount: ItemAmount,
    ) : Condition("IS_ITEM")
}

enum class Comparison {
    @SerialName("EQUAL") Eq,
    @SerialName("GREATER_THAN") Gt,
    @SerialName("GREATER_THAN_OR_EQUAL") Ge,
    @SerialName("LESS_THAN") Lt,
    @SerialName("LESS_THAN_OR_EQUAL") Le;
}

@Serializable(with = KeyedSerializer::class)
enum class Permission(override val key: String) : Keyed {
    Fly("Fly"),
    WoodDoor("Wood Door"),
    IronDoor("Iron Door"),
    WoodTrapDoor("Wood Trap Door"),
    IronTrapDoor("Iron Trap Door"),
    FenceGate("Fence Gate"),
    Button("Button"),
    Lever("Lever"),
    UseLaunchPads("Use Launch Pads"),
    Tp("/tp"),
    TpOtherPlayers("/tp Other Players"),
    Jukebox("Jukebox"),
    Kick("Kick"),
    Ban("Ban"),
    Mute("Mute"),
    PetSpawning("Pet Spawning"),
    Build("Build"),
    OfflineBuild("Offline Build"),
    Fluid("Fluid"),
    ProTools("Pro Tools"),
    UseChests("Use Chests"),
    UseEnderChests("Use Ender Chests"),
    ItemEditor("Item Editor"),
    SwitchGameMode("Switch Game Mode"),
    EditStats("Edit Stats"),
    ChangePlayerGroup("Change Player Group"),
    ChangeGameRules("Change Gamerules"),
    HousingMenu("Housing Menu"),
    TeamChatSpy("Team Chat Spy"),
    EditActions("Edit Actions"),
    EditRegions("Edit Regions"),
    EditScoreboard("Edit Scoreboard"),
    EditEventActions("Edit Event Actions"),
    EditCommands("Edit Commands"),
    EditFunctions("Edit Functions"),
    EditInventoryLayouts("Edit Inventory Layouts"),
    EditTeams("Edit Teams"),
    EditCustomMenus("Edit Custom Menus"),
    ItemMailbox("Item: Mailbox"),
    ItemEggHunt("Item: Egg Hunt"),
    ItemTeleportPad("Item: Teleport Pad"),
    ItemLaunchPad("Item: Launch Pad"),
    ItemActionPad("Item: Action Pad"),
    ItemHologram("Item: Hologram"),
    ItemNPC("Item: NPCs"),
    ItemActionButton("Item: Action Button"),
    ItemLeaderboard("Item: Leaderboard"),
    ItemTrashCan("Item: Trash Can"),
    ItemBiomeStick("Item: Biome Stick");
}

@Serializable(with = KeyedSerializer::class)
enum class ItemCheck(override val key: String) : Keyed {
    ItemType("Item Type"),
    Metadata("Metadata");
}

@Serializable(with = KeyedSerializer::class)
enum class ItemAmount(override val key: String) : Keyed {
    Any("Any Amount"),
    Ge("Equal or Greater Amount");
}

@Serializable(with = KeyedSerializer::class)
enum class InventoryLocation(override val key: String) : Keyed {
    Hand("Hand"),
    Armor("Armor"),
    HotBar("Hotbar"),
    Inventory("Inventory"),
    Anywhere("Anywhere");
}

@Serializable(with = KeyedSerializer::class)
enum class FishingEnvironment(override val key: String) : Keyed {
    Water("Water"),
    Lava("Lava");
}

@Serializable(with = KeyedSerializer::class)
enum class PortalType(override val key: String) : Keyed {
    EndPortal("End Portal"),
    NetherPortal("Nether Portal")
}

@Serializable(with = KeyedSerializer::class)
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
}