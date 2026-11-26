package llc.redstone.htslreborn.data.enums

import llc.redstone.htslreborn.data.Keyed

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

    companion object {
        fun fromKey(key: String?): PotionEffect? = entries.find { it.key.equals(key, true) }
    }
}