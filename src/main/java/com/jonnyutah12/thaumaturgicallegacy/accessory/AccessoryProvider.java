package com.jonnyutah12.thaumaturgicallegacy.accessory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Provider SPI from the accessory design record. Exactly one provider is
 * active at runtime: the Curios bridge when Curios is present, otherwise
 * the built-in fallback. Gameplay effects consume
 * {@link WornAccessories}, never a provider directly.
 */
public interface AccessoryProvider {

    /** Stable identifier for logs and game tests ("curios" or "fallback"). */
    String id();

    /**
     * Stacks currently worn by the player. Server-authoritative; the
     * fallback provider additionally keeps a display-only client mirror
     * (design record: one tiny S2C payload), Curios has its own per-slot
     * sync.
     */
    List<ItemStack> wornStacks(Player player);
}
