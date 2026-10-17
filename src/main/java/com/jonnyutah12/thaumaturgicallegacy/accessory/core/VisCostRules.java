package com.jonnyutah12.thaumaturgicallegacy.accessory.core;

/**
 * Pure vis-cost modifier rules (TL-019). No Minecraft or Curios types: the
 * cost math has exactly one implementation and is unit-tested directly
 * (docs/TESTING.md unit layer).
 *
 * <p>Rule: a worn accessory's percentage discount is applied to a spell's
 * base cost; the discount amount rounds up so small costs still benefit,
 * and the effective cost never drops below 1 vis. The strongest single
 * discount applies — discounts do not stack (aggregation happens in
 * {@code WornAccessories}).</p>
 */
public final class VisCostRules {

    private VisCostRules() {
    }

    /**
     * @param baseCost       the spell's catalog cost
     * @param discountPercent 0..100 (values outside the range are clamped)
     * @return the effective vis cost
     */
    public static int discountedCost(int baseCost, int discountPercent) {
        if (baseCost <= 0) {
            return 0;
        }
        int clamped = Math.clamp(discountPercent, 0, 100);
        int discount = (baseCost * clamped + 99) / 100; // ceil(base * pct / 100)
        return Math.max(1, baseCost - discount);
    }
}
