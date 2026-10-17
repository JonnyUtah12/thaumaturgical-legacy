package com.jonnyutah12.thaumaturgicallegacy.magic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for the pure vis-transfer rules shared by the wand's data
 * component and the Vis Tap buffer (t_c5894126, follow-up to TL-009). The
 * magic package has no Minecraft types, so this runs on the plain-JVM unit
 * layer (docs/TESTING.md).
 */
class VisStorageTest {

    @Test
    void constructorRejectsNegativeCapacityAndStartsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new VisStorage(-1),
                "negative capacity must be rejected");

        VisStorage storage = new VisStorage(10);
        assertEquals(10, storage.capacity());
        assertEquals(0, storage.amount(), "a fresh storage starts empty");
        assertTrue(storage.isEmpty());
        assertFalse(storage.isFull());
        assertEquals(10, storage.remaining());
        assertFalse(storage.canAfford(1));
        assertTrue(storage.canAfford(0));
    }

    @Test
    void setAmountClampsToCapacityBounds() {
        VisStorage storage = new VisStorage(50);

        storage.setAmount(25);
        assertEquals(25, storage.amount());

        storage.setAmount(-7);
        assertEquals(0, storage.amount(), "negative amounts clamp to 0");

        storage.setAmount(Integer.MAX_VALUE);
        assertEquals(50, storage.amount(), "oversized amounts clamp to capacity");
        assertTrue(storage.isFull());
        assertEquals(0, storage.remaining());

        storage.setAmount(Integer.MIN_VALUE);
        assertEquals(0, storage.amount());
        assertTrue(storage.isEmpty());
    }

    @Test
    void withdrawOnlySucceedsWhenAffordable() {
        VisStorage storage = new VisStorage(50);
        storage.setAmount(5);

        assertTrue(storage.canAfford(5));
        assertFalse(storage.canAfford(6));

        assertTrue(storage.withdraw(2));
        assertEquals(3, storage.amount());

        assertFalse(storage.withdraw(4), "withdrawal beyond the balance must fail");
        assertEquals(3, storage.amount(), "a failed withdrawal must not change the balance");

        assertTrue(storage.withdraw(3), "exact-afford withdrawal must succeed");
        assertEquals(0, storage.amount());
        assertTrue(storage.isEmpty());
        assertFalse(storage.withdraw(1), "cannot withdraw from an empty storage");
    }

    @Test
    void withdrawRejectsNegativeCostAndZeroIsNoOp() {
        VisStorage storage = new VisStorage(10);
        storage.setAmount(4);

        assertFalse(storage.withdraw(-1), "negative costs are invalid, not free casts");
        assertEquals(4, storage.amount());

        assertTrue(storage.withdraw(0));
        assertEquals(4, storage.amount(), "a zero-cost withdrawal changes nothing");
    }

    @Test
    void depositAcceptsOnlyWhatFits() {
        VisStorage storage = new VisStorage(10);

        assertEquals(4, storage.deposit(4));
        assertEquals(4, storage.amount());

        assertEquals(6, storage.deposit(100), "deposit clamps to the remaining space");
        assertEquals(10, storage.amount());
        assertTrue(storage.isFull());

        assertEquals(0, storage.deposit(5), "a full storage accepts nothing");
        assertEquals(0, storage.deposit(-3), "negative deposits are accepted as zero");
        assertEquals(10, storage.amount());
    }

    @Test
    void zeroCapacityStorageHoldsNothing() {
        VisStorage storage = new VisStorage(0);
        assertTrue(storage.isEmpty());
        assertTrue(storage.isFull());
        assertEquals(0, storage.deposit(5));
        assertFalse(storage.withdraw(1));
        assertTrue(storage.withdraw(0));
    }

    @Test
    void capacityInvariantsHoldUnderMixedTransfers() {
        VisStorage storage = new VisStorage(7);
        Random random = new Random(0x5EED_20261017L);

        for (int i = 0; i < 2000; i++) {
            if (random.nextBoolean()) {
                int cost = random.nextInt(15) - 2; // includes invalid negative costs
                boolean shouldSucceed = cost >= 0 && storage.amount() >= cost;
                assertEquals(shouldSucceed, storage.withdraw(cost));
            } else {
                int request = random.nextInt(15) - 2;
                int accepted = storage.deposit(request);
                assertTrue(accepted >= 0, "deposit must never report a negative acceptance");
                assertTrue(accepted <= Math.max(request, 0), "deposit must never accept more than requested");
            }

            assertTrue(storage.amount() >= 0 && storage.amount() <= storage.capacity(),
                    "amount must stay within [0, capacity]");
            assertEquals(storage.capacity() - storage.amount(), storage.remaining());
            assertEquals(storage.amount() <= 0, storage.isEmpty());
            assertEquals(storage.amount() >= storage.capacity(), storage.isFull());
            assertEquals(storage.amount() >= 3, storage.canAfford(3));
        }
    }
}
