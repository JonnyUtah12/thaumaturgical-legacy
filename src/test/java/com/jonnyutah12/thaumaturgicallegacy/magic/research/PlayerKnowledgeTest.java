package com.jonnyutah12.thaumaturgicallegacy.magic.research;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-logic tests for the per-player research state (TL-017). */
class PlayerKnowledgeTest {

    @Test
    void freshPlayerKnowsNothing() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertFalse(knowledge.isKnown(ResearchId.FIRST_SIGHT));
        assertTrue(knowledge.known().isEmpty());
        assertTrue(knowledge.knownIds().isEmpty());
    }

    @Test
    void prerequisitesGateCompletion() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertFalse(knowledge.complete(ResearchCatalog.definition(ResearchId.WANDCRAFT)),
                "Wandwright requires First Sight");
        assertFalse(knowledge.complete(ResearchCatalog.definition(ResearchId.VIS_TAPPING)),
                "Tapping the Flow cannot be reached before its prerequisites");
        assertFalse(knowledge.isKnown(ResearchId.WANDCRAFT));

        // The chain completes only in order.
        assertTrue(knowledge.complete(ResearchCatalog.definition(ResearchId.FIRST_SIGHT)));
        assertTrue(knowledge.complete(ResearchCatalog.definition(ResearchId.WANDCRAFT)));
        assertTrue(knowledge.complete(ResearchCatalog.definition(ResearchId.VIS_TAPPING)));
    }

    @Test
    void completionIsIdempotent() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        ResearchDefinition first = ResearchCatalog.definition(ResearchId.FIRST_SIGHT);
        assertTrue(knowledge.complete(first));
        assertFalse(knowledge.complete(first), "re-completing a known entry must return false");
        assertTrue(knowledge.isKnown(ResearchId.FIRST_SIGHT));
    }

    @Test
    void knownIdsAreStableOrdered() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.completeRaw(ResearchId.VIS_TAPPING);
        knowledge.completeRaw(ResearchId.FIRST_SIGHT);
        // Enum-declaration order regardless of insertion order (stable saves).
        assertEquals(java.util.List.of(ResearchId.FIRST_SIGHT, ResearchId.VIS_TAPPING),
                knowledge.knownIds());
    }
}
