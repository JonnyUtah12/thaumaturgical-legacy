package com.jonnyutah12.thaumaturgicallegacy.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for the spell catalog (t_c5894126, follow-up to TL-009):
 * every spell must have a definition, costs must be positive, and the
 * definition record must reject invalid numbers. Adding a Spell constant
 * without registering it here (or with a zero/negative cost) goes red.
 */
class SpellCatalogTest {

    @Test
    void everySpellHasADefinitionKeyedToItself() {
        for (Spell spell : Spell.values()) {
            SpellDefinition definition = SpellCatalog.definition(spell);
            assertNotNull(definition, "spell " + spell + " has no catalog definition");
            assertSame(spell, definition.spell(),
                    "the definition returned for " + spell + " must be keyed to that spell");
        }
        assertEquals(Spell.values().length, SpellCatalog.all().size(),
                "the catalog must define exactly one entry per spell constant");
    }

    @Test
    void everySpellCostsPositiveVisAndHasSaneNumbers() {
        for (Spell spell : Spell.values()) {
            SpellDefinition d = SpellCatalog.definition(spell);
            assertTrue(d.visCost() > 0, "vis cost must be positive for " + spell);
            assertTrue(d.range() > 0, "range must be positive for " + spell);
            assertTrue(d.cooldownTicks() > 0, "cooldown must be positive for " + spell);
            assertTrue(d.damage() >= 0, "damage must not be negative for " + spell);
            assertTrue(d.fireSeconds() >= 0, "fire seconds must not be negative for " + spell);
            assertTrue(d.slowTicks() >= 0, "slow ticks must not be negative for " + spell);
            assertTrue(d.slowAmplifier() >= 0, "slow amplifier must not be negative for " + spell);
            assertTrue(d.damage() > 0 || d.fireSeconds() > 0 || d.slowTicks() > 0,
                    "spell " + spell + " must have at least one effect");
        }
    }

    @Test
    void catalogViewIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> SpellCatalog.all().put(Spell.FIREBOLT, SpellCatalog.definition(Spell.FIREBOLT)),
                "the catalog must not be mutable from outside");
    }

    @Test
    void spellDefinitionRecordRejectsInvalidNumbers() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpellDefinition(Spell.FIREBOLT, -1, 10.0, 5, 1.0F, 0, 0, 0),
                "negative vis cost must be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new SpellDefinition(Spell.FIREBOLT, 1, 0.0, 5, 1.0F, 0, 0, 0),
                "zero range must be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new SpellDefinition(Spell.FIREBOLT, 1, -2.0, 5, 1.0F, 0, 0, 0),
                "negative range must be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new SpellDefinition(Spell.FIREBOLT, 1, 10.0, -1, 1.0F, 0, 0, 0),
                "negative cooldown must be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new SpellDefinition(Spell.FIREBOLT, 1, 10.0, 5, -1.0F, 0, 0, 0),
                "negative damage must be rejected");

        // The boundary the record deliberately allows: zero cost/cooldown/effects.
        SpellDefinition minimal = new SpellDefinition(Spell.FIREBOLT, 0, 0.1, 0, 0.0F, 0, 0, 0);
        assertEquals(0, minimal.visCost());
    }
}
