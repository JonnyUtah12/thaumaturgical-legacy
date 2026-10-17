package com.jonnyutah12.thaumaturgicallegacy.magic;

/**
 * Gameplay numbers for one spell. All values are original to this project
 * (docs/REVERSE_ENGINEERING_POLICY.md: numbers may be informed by documented
 * original behavior but are ours). No Minecraft types in this record.
 *
 * @param visCost       vis withdrawn from the wand per cast
 * @param range         raycast range in blocks
 * @param cooldownTicks item-cooldown length after a cast
 * @param damage        magic damage dealt to a living target
 * @param fireSeconds   seconds the target burns (0 = no ignition)
 * @param slowTicks     slowness effect duration in ticks (0 = none)
 * @param slowAmplifier slowness amplifier (0 = Slowness I)
 */
public record SpellDefinition(
        Spell spell,
        int visCost,
        double range,
        int cooldownTicks,
        float damage,
        int fireSeconds,
        int slowTicks,
        int slowAmplifier) {

    public SpellDefinition {
        if (visCost < 0 || range <= 0 || cooldownTicks < 0 || damage < 0) {
            throw new IllegalArgumentException("invalid spell definition for " + spell);
        }
    }
}
