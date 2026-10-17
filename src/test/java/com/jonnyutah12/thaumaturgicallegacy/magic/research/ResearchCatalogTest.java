package com.jonnyutah12.thaumaturgicallegacy.magic.research;

import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Structural tests for the research tree (TL-017). */
class ResearchCatalogTest {

    @Test
    void everyEntryHasADefinitionAndResolvablePrerequisites() {
        for (ResearchId id : ResearchId.values()) {
            ResearchDefinition definition = ResearchCatalog.definition(id);
            assertEquals(id, definition.id());
            for (ResearchId prerequisite : definition.prerequisites()) {
                ResearchCatalog.definition(prerequisite); // must not throw
            }
        }
    }

    @Test
    void treeIsAcyclicAndEntryPointsHaveNoPrerequisites() {
        ResearchDefinition entry = ResearchCatalog.definition(ResearchId.FIRST_SIGHT);
        assertTrue(entry.prerequisites().isEmpty(), "First Sight is the tree entry point");
        // Walk from each entry; a cycle would revisit a node on the stack.
        for (ResearchId id : ResearchId.values()) {
            walk(id, EnumSet.noneOf(ResearchId.class));
        }
    }

    private static void walk(ResearchId id, EnumSet<ResearchId> stack) {
        assertTrue(stack.add(id), "research prerequisite cycle at " + id);
        for (ResearchId prerequisite : ResearchCatalog.definition(id).prerequisites()) {
            walk(prerequisite, stack);
        }
        stack.remove(id);
    }

    @Test
    void spellsUnlockOnlyThroughTheirEntry() {
        assertTrue(ResearchCatalog.spellsUnlockedBy(Set.of()).isEmpty());
        assertTrue(ResearchCatalog.spellsUnlockedBy(Set.of(ResearchId.FIRST_SIGHT)).isEmpty());
        assertTrue(ResearchCatalog.spellsUnlockedBy(Set.of(ResearchId.WANDCRAFT)).isEmpty());
        assertEquals(Set.of(Spell.FROST_LANCE),
                ResearchCatalog.spellsUnlockedBy(Set.of(ResearchId.VIS_TAPPING)));
        assertTrue(ResearchCatalog.spellsUnlockedBy(EnumSet.noneOf(ResearchId.class)).isEmpty());
    }
}
