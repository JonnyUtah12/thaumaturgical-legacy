package com.jonnyutah12.thaumaturgicallegacy.magic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The spells implemented by the first playable magic system. New spells are
 * added here (definition + effect mapping in the wand item); the catalog is the
 * single source of truth for costs and cooldowns.
 */
public final class SpellCatalog {

    private static final Map<Spell, SpellDefinition> DEFINITIONS = new EnumMap<>(Spell.class);

    static {
        register(new SpellDefinition(Spell.FIREBOLT,
                /* visCost */ 2, /* range */ 20.0, /* cooldownTicks */ 12,
                /* damage */ 4.0F, /* fireSeconds */ 4, /* slowTicks */ 0, /* slowAmplifier */ 0));
        register(new SpellDefinition(Spell.FROST_LANCE,
                /* visCost */ 3, /* range */ 24.0, /* cooldownTicks */ 16,
                /* damage */ 2.0F, /* fireSeconds */ 0, /* slowTicks */ 100, /* slowAmplifier */ 1));
    }

    private SpellCatalog() {
    }

    private static void register(SpellDefinition definition) {
        DEFINITIONS.put(definition.spell(), definition);
    }

    public static SpellDefinition definition(Spell spell) {
        SpellDefinition definition = DEFINITIONS.get(spell);
        if (definition == null) {
            throw new IllegalStateException("No definition registered for spell " + spell);
        }
        return definition;
    }

    public static Map<Spell, SpellDefinition> all() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }
}
