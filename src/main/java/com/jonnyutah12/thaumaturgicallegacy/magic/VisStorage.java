package com.jonnyutah12.thaumaturgicallegacy.magic;

/**
 * A bounded amount of vis. Pure logic (no Minecraft types) shared by the wand's
 * data component and the Vis Tap block entity, so the transfer/withdrawal
 * rules have exactly one implementation and can be unit-tested directly.
 */
public final class VisStorage {

    private final int capacity;
    private int amount;

    public VisStorage(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0");
        }
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public int amount() {
        return amount;
    }

    public boolean isEmpty() {
        return amount <= 0;
    }

    public boolean isFull() {
        return amount >= capacity;
    }

    public int remaining() {
        return capacity - amount;
    }

    public boolean canAfford(int cost) {
        return amount >= cost;
    }

    /** Sets the amount, clamped to [0, capacity]. */
    public void setAmount(int amount) {
        this.amount = Math.clamp(amount, 0, capacity);
    }

    /**
     * Withdraws {@code cost} vis if affordable.
     *
     * @return true if the withdrawal happened, false if there was not enough
     */
    public boolean withdraw(int cost) {
        if (cost < 0 || !canAfford(cost)) {
            return false;
        }
        amount -= cost;
        return true;
    }

    /**
     * Deposits as much vis as fits.
     *
     * @return the amount actually accepted (may be less than requested)
     */
    public int deposit(int amount) {
        int accepted = Math.min(Math.max(amount, 0), remaining());
        this.amount += accepted;
        return accepted;
    }
}
