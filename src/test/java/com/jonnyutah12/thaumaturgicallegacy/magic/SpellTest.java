package com.jonnyutah12.thaumaturgicallegacy.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for the {@link Spell} enum's data-driven helpers
 * (t_c5894126, follow-up to TL-009): ordinal round-trip with fallback for
 * corrupt saved data, and the wand's spell-cycling contract.
 */
class SpellTest {

    @Test
    void byOrdinalRoundTripsEveryValidOrdinal() {
        for (Spell spell : Spell.values()) {
            assertSame(spell, Spell.byOrdinal(spell.ordinal()),
                    "byOrdinal must return the spell with that ordinal");
        }
    }

    @Test
    void byOrdinalFallsBackToFirstSpellForCorruptData() {
        Spell first = Spell.values()[0];
        int count = Spell.values().length;

        assertSame(first, Spell.byOrdinal(-1));
        assertSame(first, Spell.byOrdinal(-1000));
        assertSame(first, Spell.byOrdinal(Integer.MIN_VALUE));
        assertSame(first, Spell.byOrdinal(count), "one past the last ordinal is out of range");
        assertSame(first, Spell.byOrdinal(count + 7));
        assertSame(first, Spell.byOrdinal(Integer.MAX_VALUE));
    }

    @Test
    void nextIsAlwaysTheFollowingOrdinalAndWrapsAround() {
        int count = Spell.values().length;
        for (Spell spell : Spell.values()) {
            Spell next = spell.next();
            assertEquals((spell.ordinal() + 1) % count, next.ordinal(),
                    "next() must advance by one ordinal and wrap at the end");
        }
    }

    @Test
    void cyclingThroughEverySpellReturnsToTheStart() {
        Spell spell = Spell.values()[0];
        for (int i = 0; i < Spell.values().length; i++) {
            spell = spell.next();
        }
        assertSame(Spell.values()[0], spell, "a full cycle must return to the first spell");
    }

    @Test
    void everySpellHasAUsableDescriptionId() {
        for (Spell spell : Spell.values()) {
            String id = spell.descriptionId();
            assertNotNull(id, "spell " + spell + " must have a description id");
            assertTrue(id.startsWith("spell."),
                    "translation keys follow the '<type>.<ns>.<name>' convention: " + id);
            assertTrue(id.chars().filter(c -> c == '.').count() >= 2,
                    "translation key must be namespaced: " + id);
        }
    }
}
