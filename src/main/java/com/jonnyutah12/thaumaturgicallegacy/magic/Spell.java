package com.jonnyutah12.thaumaturgicallegacy.magic;

/**
 * The set of spells a wand can currently channel. Deliberately free of any
 * Minecraft types: this package is pure logic so it can be unit-tested without
 * a game bootstrap (see docs/architecture/MAGIC_SYSTEM_V0_1.md).
 */
public enum Spell {
    FIREBOLT("spell.thaumaturgicallegacy.firebolt"),
    FROST_LANCE("spell.thaumaturgicallegacy.frost_lance");

    private final String descriptionId;

    Spell(String descriptionId) {
        this.descriptionId = descriptionId;
    }

    /** Translation key for the spell's display name. */
    public String descriptionId() {
        return descriptionId;
    }

    /** Next spell in the wand's cycle. */
    public Spell next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Safe lookup by ordinal; falls back to the first spell for bad data. */
    public static Spell byOrdinal(int ordinal) {
        Spell[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : all[0];
    }
}
