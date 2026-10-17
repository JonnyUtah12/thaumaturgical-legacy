package com.jonnyutah12.thaumaturgicallegacy.magic.research;

import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;

import java.util.Set;

/**
 * One research entry: its prerequisites and the spells its completion makes
 * channelable. Pure logic (no Minecraft types). Item rewards are wired in the
 * shell ({@code event.ResearchEvents}) because they reference registered
 * items; the catalog stays game-free and unit-testable.
 *
 * @param id             the entry this definition describes
 * @param prerequisites  entries that must all be completed first
 * @param unlockedSpells spells the player may channel once this entry is done
 */
public record ResearchDefinition(
        ResearchId id,
        Set<ResearchId> prerequisites,
        Set<Spell> unlockedSpells) {

    public ResearchDefinition {
        prerequisites = Set.copyOf(prerequisites);
        unlockedSpells = Set.copyOf(unlockedSpells);
        if (prerequisites.contains(id)) {
            throw new IllegalArgumentException("research entry cannot require itself: " + id);
        }
    }
}
