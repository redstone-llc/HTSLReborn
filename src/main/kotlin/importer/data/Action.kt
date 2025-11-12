@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package dev.wekend.housingtoolbox.feature.importer.data

import dev.wekend.housingtoolbox.feature.importer.lexar.Operators
import dev.wekend.housingtoolbox.feature.importer.lexar.Tokens
import guru.zoroark.tegral.niwen.lexer.Token
import guru.zoroark.tegral.niwen.parser.ParserNodeDeclaration
import guru.zoroark.tegral.niwen.parser.reflective
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.StringifiedNbt
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/*
Borrowed from https://github.com/sndyx/hsl, licensed under the MIT License
 */

@Serializable
sealed class Action(
    @Transient val actionName: String = ""
) {
    companion object {

        fun createAction(keyword: String, iterator: Iterator<Token>): Action? {
            val clazz = Action::class.sealedSubclasses.find {
                it.annotations
                    .filterIsInstance<Keyword>()
                    .any { it.keyword == keyword }
            } ?: return null

            return clazz.primaryConstructor?.let { constructor ->
                val args = constructor.parameters.associateWith args@{ param ->
                    val prop = clazz.memberProperties.find { it.name == param.name }!!
                    return@args when (prop.returnType.classifier) {
                        String::class -> iterator.next().string
                        Int::class -> iterator.next().string.toInt()
                        Long::class -> iterator.next().string.removeSuffix("L").toLong()
                        Double::class -> iterator.next().string.toDouble()
                        Boolean::class -> iterator.next().string.toBoolean()
                        StatValue::class -> {
                            val token = iterator.next()
                            when (token.tokenType) {
                                Tokens.STRING -> StatValue.Str(token.string)
                                Tokens.INT -> StatValue.I64(token.string.toLong())
                                Tokens.LONG -> StatValue.I64(token.string.removeSuffix("L").toLong())
                                Tokens.DOUBLE -> StatValue.Dbl(token.string.toDouble())
                                else -> error("Unknown StatValue token: ${token.string}")
                            }
                        }

                        StatOp::class -> {
                            val token = iterator.next()
                            when(token.tokenType) {
                                Operators.SET -> StatOp.Set
                                Operators.INCREMENT -> StatOp.Inc
                                Operators.DECREMENT -> StatOp.Dec
                                Operators.MULTIPLY -> StatOp.Mul
                                Operators.DIVIDE -> StatOp.Div
                                else -> error("Unknown StatOp: ${token.string}")
                            }
                        }

                        PotionEffect::class -> {
                            val token = iterator.next()
                            PotionEffect.entries.find { it.key == token.string }
                                ?: error("Unknown PotionEffect: ${token.string}")
                        }

                        Enchantment::class -> {
                            val token = iterator.next()
                            Enchantment.entries.find { it.key == token.string }
                                ?: error("Unknown Enchantment: ${token.string}")
                        }

                        GameMode::class -> {
                            val token = iterator.next()
                            GameMode.entries.find { it.key == token.string }
                                ?: error("Unknown GameMode: ${token.string}")
                        }

                        Sound::class -> {
                            val token = iterator.next()
                            Sound.entries.find { it.key == token.string }
                                ?: error("Unknown Sound: ${token.string}")
                        }

                        else -> error("Unsupported parameter type: ${prop.returnType}")
                    }
                }

                return constructor.callBy(args)
            }
        }

    }


    @Serializable
    @SerialName("APPLY_LAYOUT")
    @Keyword("applyLayout")
    data class ApplyInventoryLayout(val layout: String) : Action("APPLY_LAYOUT") 

    @Serializable
    @SerialName("POTION_EFFECT")
    @Keyword("applyPotion")
    data class ApplyPotionEffect(
        val effect: PotionEffect,
        val duration: Int,
        val level: Int,
        @SerialName("override_existing_effects")
        val override: Boolean,
        @SerialName("show_potion_icon")
        val showIcon: Boolean,
    ) : Action("POTION_EFFECT") 

    @Serializable
    @SerialName("BALANCE_PLAYER_TEAM")
    @Keyword("balanceTeam")
    class BalancePlayerTeam : Action("BALANCE_PLAYER_TEAM") 

    @Serializable
    @SerialName("CANCEL_EVENT")
    @Keyword("cancelEvent")
    class CancelEvent : Action("CANCEL_EVENT") 

    @Serializable
    @SerialName("CHANGE_HEALTH")
    @Keyword("changeHealth")
    data class ChangeHealth(
        @SerialName("mode") val op: StatOp,
        val amount: StatValue,
    ) : Action("CHANGE_HEALTH") 

    @Serializable
    @SerialName("CHANGE_HUNGER")
    @Keyword("changeHunger")
    data class ChangeHunger(
        @SerialName("mode") val op: StatOp,
        val amount: StatValue,
    ) : Action("CHANGE_HUNGER") 

    @Keyword("var")
    @Keyword("stat")
    data class PlayerVariable(
        val variable: String,
        val op: StatOp,
        val amount: StatValue,
        val unset: Boolean = false
    ) : Action("CHANGE_VARIABLE") {
        val holder = VariableHolder.Player
    }

    @Keyword("teamvar")
    @Keyword("teamstat")
    data class TeamVariable(
        val teamName: String,
        val variable: String,
        val op: StatOp,
        val amount: StatValue,
        val unset: Boolean = false
    ) : Action("CHANGE_VARIABLE") {
        val holder = VariableHolder.Team
    }

    @Keyword("globalvar")
    @Keyword("globalstat")
    data class GlobalVariable(
        val variable: String,
        val op: StatOp,
        val amount: StatValue,
        val unset: Boolean = false
    ) : Action("CHANGE_VARIABLE") {
        val holder = VariableHolder.Global
    }

    @Serializable
    @SerialName("CLEAR_EFFECTS")
    @Keyword("clearEffects")
    class ClearAllPotionEffects : Action("CLEAR_EFFECTS") 

    @Serializable
    @SerialName("CLOSE_MENU")
    @Keyword("closeMenu")
    class CloseMenu : Action("CLOSE_MENU") 

    @Serializable
    @SerialName("CONDITIONAL")
    @Keyword("if") //TODO
    data class Conditional(
        val conditions: List<Condition>,
        @SerialName("match_any_condition") val matchAnyCondition: Boolean,
        @SerialName("if_actions") val ifActions: List<Action>,
        @SerialName("else_actions") val elseActions: List<Action>,
    ) : Action("CONDITIONAL") 

    @Serializable
    @SerialName("ACTION_BAR")
    @Keyword("actionBar")
    data class DisplayActionBar(val message: String) : Action("ACTION_BAR") 

    @Serializable
    @SerialName("DISPLAY_MENU")
    @Keyword("displayMenu")
    data class DisplayMenu(val menu: String) : Action("DISPLAY_MENU") 

    @Serializable
    @SerialName("TITLE")
    @Keyword("title")
    data class DisplayTitle(
        val title: String,
        val subtitle: String,
        @SerialName("fade_in") val fadeIn: Int,
        val stay: Int,
        @SerialName("fade_out") val fadeOut: Int,
    ) : Action("TITLE") 

    @Serializable
    @SerialName("ENCHANT_HELD_ITEM")
    @Keyword("enchant")
    data class EnchantHeldItem(
        val enchantment: Enchantment,
        val level: Int,
    ) : Action("ENCHANT_HELD_ITEM") 

    @Serializable
    @SerialName("EXIT")
    @Keyword("exit")
    class Exit : Action("EXIT") 

    @Serializable
    @SerialName("FAIL_PARKOUR")
    @Keyword("failParkour")
    data class FailParkour(val reason: String) : Action("FAIL_PARKOUR") 

    @Serializable
    @SerialName("FULL_HEAL")
    @Keyword("fullHeal")
    class FullHeal : Action("FULL_HEAL") 

    @Serializable
    @SerialName("GIVE_EXP_LEVELS")
    @Keyword("xpLevel")
    data class GiveExperienceLevels(val levels: Int) : Action("GIVE_EXP_LEVELS") 

    @Serializable
    @SerialName("GIVE_ITEM")
    @Keyword("giveItem")
    data class GiveItem(
        val item: ItemStack,
        @SerialName("allow_multiple") val allowMultiple: Boolean,
        @SerialName("inventory_slot") val inventorySlot: StatValue,
        @SerialName("replace_existing_item") val replaceExistingItem: Boolean,
    ) : Action("GIVE_ITEM") 

    @Serializable
    @SerialName("KILL")
    @Keyword("kill")
    class KillPlayer : Action("KILL") 

    @Serializable
    @SerialName("PARKOUR_CHECKPOINT")
    @Keyword("parkCheck")
    class ParkourCheckpoint : Action("PARKOUR_CHECKPOINT") 

    @Serializable
    @SerialName("PAUSE")
    @Keyword("pause")
    data class PauseExecution(@SerialName("ticks_to_wait") val ticks: Int) : Action("PAUSE") 

    @Serializable
    @SerialName("PLAY_SOUND")
    @Keyword("sound")
    data class PlaySound(
        val sound: Sound,
        val volume: Double,
        val pitch: Double,
        val location: Location,
    ) : Action("PLAY_SOUND") 

    @Serializable
    @SerialName("RANDOM_ACTION")
    data class RandomAction(
        val actions: List<Action>,
    ) : Action("RANDOM_ACTION") 

    @Serializable
    @SerialName("SEND_MESSAGE")
    @Keyword("chat")
    data class SendMessage(val message: String) : Action("SEND_MESSAGE") 

    @Serializable
    @SerialName("TRIGGER_FUNCTION")
    @Keyword("function")
    data class ExecuteFunction(val name: String, val global: Boolean) : Action("TRIGGER_FUNCTION") 

    @Serializable
    @SerialName("RESET_INVENTORY")
    @Keyword("resetInventory")
    class ResetInventory : Action("RESET_INVENTORY") 

    @Serializable
    @SerialName("REMOVE_ITEM")
    @Keyword("removeItem")
    data class RemoveItem(val item: ItemStack) : Action("REMOVE_ITEM") 

    @Serializable
    @SerialName("SET_PLAYER_TEAM")
    @Keyword("setTeam")
    data class SetPlayerTeam(val team: String) : Action("SET_PLAYER_TEAM") 

    @Serializable
    @SerialName("USE_HELD_ITEM")
    @Keyword("consumeItem")
    class UseHeldItem : Action("USE_HELD_ITEM") 

    @Serializable
    @SerialName("SET_GAMEMODE")
    @Keyword("gamemode")
    data class SetGameMode(val gamemode: GameMode) : Action("SET_GAMEMODE") 

    @Serializable
    @SerialName("SET_COMPASS_TARGET")
    @Keyword("compassTarget")
    data class SetCompassTarget(val location: Location) : Action("SET_COMPASS_TARGET") 

    @Serializable
    @SerialName("TELEPORT_PLAYER")
    @Keyword("tp")
    data class TeleportPlayer(
        val location: Location,
        @SerialName("prevent_teleport_inside_blocks") val preventInsideBlocks: Boolean,
    ) : Action("TELEPORT_PLAYER") 

    @Serializable
    @SerialName("SEND_TO_LOBBY")
    @Keyword("lobby")
    data class SendToLobby(val location: Lobby) : Action("SEND_TO_LOBBY") 

    @Serializable
    @SerialName("DROP_ITEM")
    @Keyword("dropItem")
    data class DropItem(
        val item: ItemStack,
        val location: Location,
        @SerialName("drop_naturally") val dropNaturally: Boolean,
        @SerialName("prevent_item_merging") val disableMerging: Boolean,
        @SerialName("prioritize_player") val prioritizePlayer: Boolean,
        @SerialName("fallback_to_inventory") val inventoryFallback: Boolean,
        @SerialName("despawn_duraction_ticks") val despawnDurationTicks: Int,
        @SerialName("pickup_delay_ticks") val pickupDelayTicks: Int,
    ) : Action("DROP_ITEM") 

    @Serializable
    @SerialName("CHANGE_VELOCITY")
    @Keyword("changeVelocity")
    data class ChangeVelocity(
        val x: StatValue,
        val y: StatValue,
        val z: StatValue,
    ) : Action("CHANGE_VELOCITY") 

    @Serializable
    @SerialName("LAUNCH_TO_TARGET")
    @Keyword("launchTarget")
    data class LaunchToTarget(
        val location: Location,
        val strength: StatValue
    ) : Action("LAUNCH_TO_TARGET") 

    @Serializable
    @SerialName("SET_PLAYER_WEATHER")
    @Keyword("playerWeather")
    data class SetPlayerWeather(val weather: Weather) : Action("SET_PLAYER_WEATHER") 

    @Serializable
    @SerialName("SET_PLAYER_TIME")
    @Keyword("playerTime")
    data class SetPlayerTime(val time: Time) : Action("SET_PLAYER_TIME") 

    @Serializable
    @SerialName("TOGGLE_NAMETAG_DISPLAY")
    @Keyword("displayNametag")
    data class ToggleNametagDisplay(@SerialName("display_nametag") val displayNametag: Boolean) :
        Action("TOGGLE_NAMETAG_DISPLAY") 

}

interface Keyed {
    val key: String
}

interface KeyedLabeled : Keyed {
    val label: String
}

object KeyedSerializer : KSerializer<Keyed> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Keyed", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Keyed {
        error("not implemented!")
    }

    override fun serialize(encoder: Encoder, value: Keyed) {
        encoder.encodeString(value.key)
    }
}

@Serializable(with = ItemStackSerializer::class)
data class ItemStack(
    val nbt: NbtCompound,
    var name: String? = null,
)

@Serializable
sealed class Location() {

    @Serializable
    @SerialName("custom_coordinates")
    class Custom(
        val relX: Boolean,
        val relY: Boolean,
        val relZ: Boolean,
        val x: Double?,
        val y: Double?,
        val z: Double?,
        val pitch: Float?,
        val yaw: Float?,
    ) : Location()

    @Serializable
    @SerialName("house_spawn")
    object HouseSpawn : Location()

    @Serializable
    @SerialName("current_location")
    object CurrentLocation : Location()

    @Serializable
    @SerialName("invokers_location")
    object InvokersLocation : Location()

}

enum class PotionEffect(override val key: String) : Keyed {
    Speed("Speed"),
    Slowness("Slowness"),
    Haste("Haste"),
    MiningFatigue("Mining Fatigue"),
    Strength("Strength"),
    InstantHealth("Instant Health"),
    InstantDamage("Instant Damage"),
    JumpBoost("Jump Boost"),
    Nausea("Nausea"),
    Regeneration("Regeneration"),
    Resistance("Resistance"),
    FireResistance("Fire Resistance"),
    WaterBreathing("Water Breathing"),
    Invisibility("Invisibility"),
    Blindness("Blindness"),
    NightVision("Night Vision"),
    Hunger("Hunger"),
    Weakness("Weakness"),
    Poison("Poison"),
    Wither("Wither"),
    HealthBoost("Health Boost"),
    Absorption("Absorption");
}

enum class Enchantment(override val key: String) : Keyed {
    Protection("Protection"),
    FireProtection("Fire Protection"),
    FeatherFalling("Feather Falling"),
    BlastProtection("Blast Protection"),
    ProjectileProtection("Projectile Protection"),
    Respiration("Respiration"),
    AquaAffinity("Aqua Affinity"),
    Thorns("Thorns"),
    DepthStrider("Depth Strider"),
    Sharpness("Sharpness"),
    Smite("Smite"),
    BaneOfArthropods("Bane Of Arthropods"),
    Knockback("Knockback"),
    FireAspect("Fire Aspect"),
    Looting("Looting"),
    Efficiency("Efficiency"),
    SilkTouch("Silk Touch"),
    Unbreaking("Unbreaking"),
    Fortune("Fortune"),
    Power("Power"),
    Punch("Punch"),
    Flame("Flame"),
    Infinity("Infinity");
}

enum class GameMode(override val key: String) : Keyed {
    Adventure("Adventure"),
    Survival("Survival"),
    Creative("Creative");
}

@Serializable(with = KeyedSerializer::class)
enum class Lobby(override val key: String) : Keyed {
    MainLobby("Main Lobby"),
    TournamentHall("Tournament Hall"),
    BlitzSG("Blitz SG"),
    TNTGames("The TNT Games"),
    MegaWalls("Mega Walls"),
    ArcadeGames("Arcade Games"),
    CopsAndCrims("Cops and Crims"),
    UHCChampions("UHC Champions"),
    Warlords("Warlords"),
    SmashHeroes("Smash Heroes"),
    Housing("Housing"),
    SkyWars("SkyWars"),
    SpeedUHC("Speed UHC"),
    ClassicGames("Classic Games"),
    Prototype("Prototype"),
    BedWars("Bed Wars"),
    MurderMystery("Murder Mystery"),
    BuildBattle("Build Battle"),
    Duels("Duels"),
    WoolWars("Wool Wars");
}

enum class StatOp {
    @SerialName("SET")
    Set,

    @SerialName("INCREMENT")
    Inc,

    @SerialName("DECREMENT")
    Dec,

    @SerialName("MULTIPLY")
    Mul,

    @SerialName("DIVIDE")
    Div,
}

@Serializable(with = StatValueBaseSerializer::class)
sealed class StatValue {
    @Serializable(with = StatI64Serializer::class)
    data class I64(val value: Long) : StatValue()


    data class Dbl(val value: Double) : StatValue()

    @Serializable(with = StatStrSerializer::class)
    data class Str(val value: String) : StatValue()
}

object ItemStackSerializer : KSerializer<ItemStack> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ItemStack", PrimitiveKind.STRING)

    private val snbt = StringifiedNbt { }
    override fun deserialize(decoder: Decoder): ItemStack {
        error("not implemented!")
    }

    override fun serialize(encoder: Encoder, value: ItemStack) {
        encoder.encodeString(snbt.encodeToString(value.nbt))
    }
}

// For the love of god, Kotlin will not choose a fucking polymorphic serializer
// for my sealed class (or tell me fucking why)!!!!! We have to do this garbage.
object StatValueBaseSerializer : KSerializer<StatValue> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("StatValueHellfireDespairPitsSerializer")

    override fun deserialize(decoder: Decoder): StatValue {
        error("not implemented!")
    }

    override fun serialize(encoder: Encoder, value: StatValue) {
        if (value is StatValue.I64) {
            StatI64Serializer.serialize(encoder, value)
        } else if (value is StatValue.Str) {
            StatStrSerializer.serialize(encoder, value)
        } else {
            StatDblSerializer.serialize(encoder, value as StatValue.Dbl)
        }
    }

}

object StatDblSerializer : KSerializer<StatValue.Dbl> {
    override val descriptor: SerialDescriptor = Double.serializer().descriptor
    override fun serialize(encoder: Encoder, value: StatValue.Dbl) {
        encoder.encodeDouble(value.value)
    }

    override fun deserialize(decoder: Decoder): StatValue.Dbl {
        error("not implemented!")
    }
}

object StatI64Serializer : KSerializer<StatValue.I64> {
    override val descriptor: SerialDescriptor = Int.serializer().descriptor
    override fun serialize(encoder: Encoder, value: StatValue.I64) {
        encoder.encodeLong(value.value)
    }

    override fun deserialize(decoder: Decoder): StatValue.I64 {
        error("not implemented!")
    }
}

object StatStrSerializer : KSerializer<StatValue.Str> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: StatValue.Str) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): StatValue.Str {
        error("not implemented!")
    }
}

@Serializable(with = KeyedSerializer::class)
enum class Sound(override val label: String, override val key: String) : KeyedLabeled {
    AmbienceCave("Ambience Cave", "ambient.cave.cave"),
    AmbienceRain("Ambience Rain", "ambient.weather.rain"),
    AmbienceThunder("Ambience Thunder", "ambient.weather.thunder"),
    AnvilBreak("Anvil Break", "random.anvil_break"),
    AnvilLand("Anvil Land", "random.anvil_land"),
    AnvilUse("Anvil Use", "random.anvil_use"),
    ArrowHit("Arrow Hit", "random.bowhit"),
    Burp("Burp", "random.burp"),
    ChestClose("Chest Close", "random.chestclosed"),
    ChestOpen("Chest Open", "random.chestopen"),
    Click("Click", "random.click"),
    DoorClose("Door Close", "random.door_close"),
    DoorOpen("Door Open", "random.door_open"),
    Drink("Drink", "random.drink"),
    Eat("Eat", "random.eat"),
    Explode("Explode", "random.explode"),
    FallBig("Fall Big", "game.player.hurt.fall.big"),
    FallSmall("Fall Small", "game.player.hurt.fall.small"),
    Fizz("Fizz", "random.fizz"),
    Fuse("Fuse", "game.tnt.primed"),
    Glass("Glass", "dig.glass"),
    HurtFlesh("Hurt Flesh", "game.player.hurt"),
    ItemBreak("Item Break", "random.break"),
    ItemPickup("Item Pickup", "random.pop"),
    LavaPop("Lava Pop", "liquid.lavapop"),
    LevelUp("Level Up", "random.levelup"),
    NoteBass("Note Bass", "note.bass"),
    NotePiano("Note Piano", "note.harp"),
    NoteBassDrum("Note Bass Drum", "note.bd"),
    NoteSticks("Note Sticks", "note.hat"),
    NoteBassGuitar("Note Bass Guitar", "note.bassattack"),
    NoteSnareDrum("Note Snare Drum", "note.snare"),
    NotePling("Note Pling", "note.pling"),
    OrbPickup("Orb Pickup", "random.orb"),
    ShootArrow("Shoot Arrow", "random.bow"),
    Splash("Splash", "game.player.swim.splash"),
    Swim("Swim", "game.player.swim"),
    WoodClick("Wood Click", "random.wood_click"),
    BatDeath("Bat Death", "mob.bat.death"),
    BatHurt("Bat Hurt", "mob.bat.hurt"),
    BatIdle("Bat Idle", "mob.bat.idle"),
    BatLoop("Bat Loop", "mob.bat.loop"),
    BatTakeoff("Bat Takeoff", "mob.bat.takeoff"),
    BlazeBreath("Blaze Breath", "mob.blaze.breathe"),
    BlazeDeath("Blaze Death", "mob.blaze.death"),
    BlazeHit("Blaze Hit", "mob.blaze.hit"),
    CatHiss("Cat Hiss", "mob.cat.hiss"),
    CatHit("Cat Hit", "mob.cat.hitt"),
    CatMeow("Cat Meow", "mob.cat.meow"),
    CatPurr("Cat Purr", "mob.cat.purr"),
    CatPurreow("Cat Purreow", "mob.cat.purreow"),
    ChickenIdle("Chicken Idle", "mob.chicken.say"),
    ChickenHurt("Chicken Hurt", "mob.chicken.hurt"),
    ChickenEggPop("Chicken Egg Pop", "mob.chicken.plop"),
    ChickenWalk("Chicken Walk", "mob.chicken.step"),
    CowIdle("Cow Idle", "mob.cow.say"),
    CowHurt("Cow Hurt", "mob.cow.hurt"),
    CowWalk("Cow Walk", "mob.cow.step"),
    CreeperHiss("Creeper Hiss", "mob.creeper.say"),
    CreeperDeath("Creeper Death", "mob.creeper.death"),
    EnderdragonDeath("Enderdragon Death", "mob.enderdragon.end"),
    EnderdragonGrowl("Enderdragon Growl", "mob.enderdragon.growl"),
    EnderdragonHit("Enderdragon Hit", "mob.enderdragon.hit"),
    EnderdragonWings("Enderdragon Wings", "mob.enderdragon.wings"),
    EndermanDeath("Enderman Death", "mob.endermen.death"),
    EndermanHit("Enderman Hit", "mob.endermen.hit"),
    EndermanIdle("Enderman Idle", "mob.endermen.idle"),
    EndermanTeleport("Enderman Teleport", "mob.endermen.portal"),
    EndermanScream("Enderman Scream", "mob.endermen.scream"),
    EndermanStare("Enderman Stare", "mob.endermen.stare"),
    GhastScream("Ghast Scream", "mob.ghast.scream"),
    GhastScream2("Ghast Scream2", "mob.ghast.affectionate_scream"),
    GhastCharge("Ghast Charge", "mob.ghast.charge"),
    GhastDeath("Ghast Death", "mob.ghast.death"),
    GhastFireball("Ghast Fireball", "mob.ghast.fireball"),
    GhastMoan("Ghast Moan", "mob.ghast.moan"),
    GuardianHit("Guardian Hit", "mob.guardian.hit"),
    GuardianIdle("Guardian Idle", "mob.guardian.idle"),
    GuardianDeath("Guardian Death", "mob.guardian.death"),
    GuardianElderHit("Guardian Elder Hit", "mob.guardian.elder.hit"),
    GuardianElderIdle("Guardian Elder Idle", "mob.guardian.elder.idle"),
    GuardianElderDeath("Guardian Elder Death", "mob.guardian.elder.death"),
    GuardianLandHit("Guardian Land Hit", "mob.guardian.land.hit"),
    GuardianLandIdle("Guardian Land Idle", "mob.guardian.land.idle"),
    GuardianLandDeath("Guardian Land Death", "mob.guardian.land.death"),
    GuardianCurse("Guardian Curse", "mob.guardian.curse"),
    GuardianAttack("Guardian Attack", "mob.guardian.attack"),
    GuardianFlop("Guardian Flop", "mob.guardian.flop"),
    IrongolemDeath("Irongolem Death", "mob.irongolem.death"),
    IrongolemHit("Irongolem Hit", "mob.irongolem.hit"),
    IrongolemThrow("Irongolem Throw", "mob.irongolem.throw"),
    IrongolemWalk("Irongolem Walk", "mob.irongolem.walk"),
    MagmacubeWalk("Magmacube Walk", "mob.magmacube.small"),
    MagmacubeWalk2("Magmacube Walk2", "mob.magmacube.big"),
    MagmacubeJump("Magmacube Jump", "mob.magmacube.jump"),
    PigIdle("Pig Idle", "mob.pig.say"),
    PigDeath("Pig Death", "mob.pig.death"),
    PigWalk("Pig Walk", "mob.pig.step"),
    RabbitAmbient("Rabbit Ambient", "mob.rabbit.idle"),
    RabbitDeath("Rabbit Death", "mob.rabbit.death"),
    RabbitHurt("Rabbit Hurt", "mob.rabbit.hurt"),
    RabbitJump("Rabbit Jump", "mob.rabbit.hop"),
    SheepIdle("Sheep Idle", "mob.sheep.say"),
    SheepShear("Sheep Shear", "mob.sheep.shear"),
    SheepWalk("Sheep Walk", "mob.sheep.step"),
    SilverfishHit("Silverfish Hit", "mob.silverfish.hit"),
    SilverfishKill("Silverfish Kill", "mob.silverfish.kill"),
    SilverfishIdle("Silverfish Idle", "mob.silverfish.say"),
    SilverfishWalk("Silverfish Walk", "mob.silverfish.step"),
    SkeletonIdle("Skeleton Idle", "mob.skeleton.say"),
    SkeletonDeath("Skeleton Death", "mob.skeleton.death"),
    SkeletonHurt("Skeleton Hurt", "mob.skeleton.hurt"),
    SkeletonWalk("Skeleton Walk", "mob.skeleton.step"),
    SlimeAttack("Slime Attack", "mob.slime.attack"),
    SlimeWalk("Slime Walk", "mob.slime.small"),
    SlimeWalk2("Slime Walk2", "mob.slime.big"),
    SpiderIdle("Spider Idle", "mob.spider.say"),
    SpiderDeath("Spider Death", "mob.spider.death"),
    SpiderWalk("Spider Walk", "mob.spider.step"),
    WitherDeath("Wither Death", "mob.wither.death"),
    WitherHurt("Wither Hurt", "mob.wither.hurt"),
    WitherIdle("Wither Idle", "mob.wither.idle"),
    WitherShoot("Wither Shoot", "mob.wither.shoot"),
    WitherSpawn("Wither Spawn", "mob.wither.spawn"),
    WolfBark("Wolf Bark", "mob.wolf.bark"),
    WolfDeath("Wolf Death", "mob.wolf.death"),
    WolfGrowl("Wolf Growl", "mob.wolf.growl"),
    WolfHowl("Wolf Howl", "mob.wolf.howl"),
    WolfHurt("Wolf Hurt", "mob.wolf.hurt"),
    WolfPant("Wolf Pant", "mob.wolf.panting"),
    WolfShake("Wolf Shake", "mob.wolf.shake"),
    WolfWalk("Wolf Walk", "mob.wolf.step"),
    WolfWhine("Wolf Whine", "mob.wolf.whine"),
    ZombieMetal("Zombie Metal", "mob.zombie.metal"),
    ZombieWood("Zombie Wood", "mob.zombie.wood"),
    ZombieWoodbreak("Zombie Woodbreak", "mob.zombie.woodbreak"),
    ZombieIdle("Zombie Idle", "mob.zombie.say"),
    ZombieDeath("Zombie Death", "mob.zombie.death"),
    ZombieHurt("Zombie Hurt", "mob.zombie.hurt"),
    ZombieInfect("Zombie Infect", "mob.zombie.infect"),
    ZombieUnfect("Zombie Unfect", "mob.zombie.unfect"),
    ZombieRemedy("Zombie Remedy", "mob.zombie.remedy"),
    ZombieWalk("Zombie Walk", "mob.zombie.step"),
    ZombiePigIdle("Zombie Pig Idle", "mob.zombiepig.zpig"),
    ZombiePigAngry("Zombie Pig Angry", "mob.zombiepig.zpigangry"),
    ZombiePigDeath("Zombie Pig Death", "mob.zombiepig.zpigdeath"),
    ZombiePigHurt("Zombie Pig Hurt", "mob.zombiepig.zpighurt"),
    FireworkBlast("Firework Blast", "fireworks.blast"),
    FireworkBlast2("Firework Blast2", "fireworks.blast_far"),
    FireworkLargeBlast("Firework Large Blast", "fireworks.largeBlast"),
    FireworkLargeBlast2("Firework Large Blast2", "fireworks.largeBlast_far"),
    FireworkTwinkle("Firework Twinkle", "fireworks.twinkle"),
    FireworkTwinkle2("Firework Twinkle2", "fireworks.twinkle_far"),
    FireworkLaunch("Firework Launch", "fireworks.launch"),
    FireworksBlast("Fireworks Blast", "fireworks.blast"),
    FireworksBlast2("Fireworks Blast2", "fireworks.blast_far"),
    FireworksLargeBlast("Fireworks Large Blast", "fireworks.largeBlast"),
    FireworksLargeBlast2("Fireworks Large Blast2", "fireworks.largeBlast_far"),
    FireworksTwinkle("Fireworks Twinkle", "fireworks.twinkle"),
    FireworksTwinkle2("Fireworks Twinkle2", "fireworks.twinkle_far"),
    FireworksLaunch("Fireworks Launch", "fireworks.launch"),
    SuccessfulHit("Successful Hit", "random.successful_hit"),
    HorseAngry("Horse Angry", "mob.horse.angry"),
    HorseArmor("Horse Armor", "mob.horse.armor"),
    HorseBreathe("Horse Breathe", "mob.horse.breathe"),
    HorseDeath("Horse Death", "mob.horse.death"),
    HorseGallop("Horse Gallop", "mob.horse.gallop"),
    HorseHit("Horse Hit", "mob.horse.hit"),
    HorseIdle("Horse Idle", "mob.horse.idle"),
    HorseJump("Horse Jump", "mob.horse.jump"),
    HorseLand("Horse Land", "mob.horse.land"),
    HorseSaddle("Horse Saddle", "mob.horse.leather"),
    HorseSoft("Horse Soft", "mob.horse.soft"),
    HorseWood("Horse Wood", "mob.horse.wood"),
    DonkeyAngry("Donkey Angry", "mob.horse.donkey.angry"),
    DonkeyDeath("Donkey Death", "mob.horse.donkey.death"),
    DonkeyHit("Donkey Hit", "mob.horse.donkey.hit"),
    DonkeyIdle("Donkey Idle", "mob.horse.donkey.idle"),
    HorseSkeletonDeath("Horse Skeleton Death", "mob.horse.skeleton.death"),
    HorseSkeletonHit("Horse Skeleton Hit", "mob.horse.skeleton.hit"),
    HorseSkeletonIdle("Horse Skeleton Idle", "mob.horse.skeleton.idle"),
    HorseZombieDeath("Horse Zombie Death", "mob.horse.zombie.death"),
    HorseZombieHit("Horse Zombie Hit", "mob.horse.zombie.hit"),
    HorseZombieIdle("Horse Zombie Idle", "mob.horse.zombie.idle"),
    VillagerDeath("Villager Death", "mob.villager.death"),
    VillagerHaggle("Villager Haggle", "mob.villager.haggle"),
    VillagerHit("Villager Hit", "mob.villager.hit"),
    VillagerIdle("Villager Idle", "mob.villager.idle"),
    VillagerNo("Villager No", "mob.villager.no"),
    VillagerYes("Villager Yes", "mob.villager.yes");
}

enum class Weather(override val key: String) : Keyed {
    SUNNY("Sunny"),
    RAINY("Rainy"),
}

@Serializable
sealed class Time() {

    @Serializable
    @SerialName("custom_time")
    class Custom(
        val time: Long
    ) : Time()

    @Serializable
    @SerialName("reset_to_world_time")
    object ResetToWorldTime : Time()

    @Serializable
    @SerialName("sunrise")
    object Sunrise : Time()

    @Serializable
    @SerialName("noon")
    object Noon : Time()

    @Serializable
    @SerialName("sunset")
    object Sunset : Time()

    @Serializable
    @SerialName("midnight")
    object Midnight : Time()
}

enum class VariableHolder(override val key: String) : Keyed {
    Player("Player"),
    Global("Global"),
    Team("Team"),
}