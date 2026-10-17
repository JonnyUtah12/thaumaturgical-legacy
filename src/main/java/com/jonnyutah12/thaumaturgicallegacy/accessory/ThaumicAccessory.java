package com.jonnyutah12.thaumaturgicallegacy.accessory;

/**
 * Marker for items that can be worn as accessories (TL-019,
 * docs/architecture/ACCESSORY_SYSTEM.md). Gameplay effects are queried
 * through {@link WornAccessories} and never reference a specific accessory
 * framework, so they behave identically under the Curios bridge and the
 * built-in fallback provider.
 */
public interface ThaumicAccessory {

    /** Identifier of the slot kind this accessory occupies (see {@link AccessorySlots}). */
    String slotKind();

    /**
     * Percentage reduction to spell vis costs while this accessory is worn
     * (0..100). Applied through {@code accessory.core.VisCostRules}; the
     * strongest worn discount wins — discounts do not stack.
     */
    int visCostDiscountPercent();
}
