package llc.redstone.htslreborn.data.enums

import llc.redstone.htslreborn.data.Keyed

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
    Infinity("Infinity"),
    LuckOfTheSea("Luck Of The Sea"),
    Lure("Lure");

    companion object {
        fun fromKey(key: String): Enchantment? = entries.find { it.key.equals(key, true) }
    }
}