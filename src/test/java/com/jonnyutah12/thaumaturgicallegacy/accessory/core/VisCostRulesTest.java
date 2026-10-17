package com.jonnyutah12.thaumaturgicallegacy.accessory.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure vis-cost discount rules (TL-019). The in-game wiring around these
 * numbers is covered by the accessory game tests; this layer pins the rule
 * itself: discount rounds up, effective cost never drops below 1,
 * percentages clamp.
 */
class VisCostRulesTest {

    @Test
    void noDiscountReturnsTheBaseCost() {
        assertEquals(2, VisCostRules.discountedCost(2, 0));
        assertEquals(50, VisCostRules.discountedCost(50, 0));
    }

    @Test
    void discountRoundsUpSoSmallCostsStillBenefit() {
        // 25% of 2 is 0.5 -> discount 1 -> cost 1 (the amulet must matter at v0.1 costs)
        assertEquals(1, VisCostRules.discountedCost(2, 25));
        assertEquals(2, VisCostRules.discountedCost(3, 25));
        assertEquals(3, VisCostRules.discountedCost(4, 25));
        // 25% of 10 is 2.5 -> discount 3 -> cost 7
        assertEquals(7, VisCostRules.discountedCost(10, 25));
    }

    @Test
    void effectiveCostNeverDropsBelowOne() {
        assertEquals(1, VisCostRules.discountedCost(1, 25));
        assertEquals(1, VisCostRules.discountedCost(2, 100));
        assertEquals(1, VisCostRules.discountedCost(9, 100));
    }

    @Test
    void percentagesAreClampedToTheValidRange() {
        assertEquals(VisCostRules.discountedCost(8, 100), VisCostRules.discountedCost(8, 150));
        assertEquals(8, VisCostRules.discountedCost(8, -10));
    }

    @Test
    void nonPositiveBaseCostsStayZero() {
        assertEquals(0, VisCostRules.discountedCost(0, 25));
        assertEquals(0, VisCostRules.discountedCost(-3, 25));
    }
}
