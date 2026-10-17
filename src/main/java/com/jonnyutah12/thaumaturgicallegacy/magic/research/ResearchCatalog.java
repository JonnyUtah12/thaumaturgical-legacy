package com.jonnyutah12.thaumaturgicallegacy.magic.research;

import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The research tree: the single source of truth for entry prerequisites and
 * spell unlocks (same catalog pattern as {@code SpellCatalog}).
 *
 * <p>The tree mirrors the progression shape documented for Thaumcraft 4 in
 * docs/reverse-engineering/CORE_MECHANICS.md §7.6 — knowledge gates recipes
 * and spells — simplified to direct unlock triggers instead of the hex-note
 * puzzle (the RE docs mark the puzzle as presentation, not mechanics).</p>
 */
public final class ResearchCatalog {

    private static final Map<ResearchId, ResearchDefinition> DEFINITIONS = new EnumMap<>(ResearchId.class);

    static {
        register(new ResearchDefinition(ResearchId.FIRST_SIGHT, Set.of(), Set.of()));
        register(new ResearchDefinition(ResearchId.WANDCRAFT,
                Set.of(ResearchId.FIRST_SIGHT), Set.of()));
        register(new ResearchDefinition(ResearchId.VIS_TAPPING,
                Set.of(ResearchId.WANDCRAFT), Set.of(Spell.FROST_LANCE)));
    }

    private ResearchCatalog() {
    }

    private static void register(ResearchDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    public static ResearchDefinition definition(ResearchId id) {
        ResearchDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalStateException("No definition registered for research " + id);
        }
        return definition;
    }

    public static Map<ResearchId, ResearchDefinition> all() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    /**
     * Every spell unlocked by the given completed entries. Firebolt is not
     * listed here: it is always channelable (the gate helper in the shell
     * treats it as the default).
     */
    public static Set<Spell> spellsUnlockedBy(Set<ResearchId> completed) {
        Set<Spell> spells = new HashSet<>();
        for (ResearchId id : completed) {
            ResearchDefinition definition = DEFINITIONS.get(id);
            if (definition != null) {
                spells.addAll(definition.unlockedSpells());
            }
        }
        return spells;
    }
}
