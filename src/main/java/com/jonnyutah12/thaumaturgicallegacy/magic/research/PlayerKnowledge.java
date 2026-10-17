package com.jonnyutah12.thaumaturgicallegacy.magic.research;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * One player's completed research. Pure logic; persisted through a NeoForge
 * player data attachment (registration in {@code ModAttachments}), which
 * handles save/load, death copy and client sync. The model follows
 * Thaumcraft 4's per-player knowledge set (CORE_MECHANICS.md §7.1, verified).
 */
public final class PlayerKnowledge {

    private final EnumSet<ResearchId> completed = EnumSet.noneOf(ResearchId.class);

    public boolean isKnown(ResearchId id) {
        return completed.contains(id);
    }

    /** Read-only view of the completed set. */
    public Set<ResearchId> known() {
        return Collections.unmodifiableSet(completed);
    }

    /** Completed ids in enum-declaration order (stable for serialization). */
    public List<ResearchId> knownIds() {
        List<ResearchId> ids = new ArrayList<>();
        for (ResearchId id : ResearchId.values()) {
            if (completed.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** True if the entry is unknown and every prerequisite is completed. */
    public boolean canComplete(ResearchDefinition definition) {
        return !completed.contains(definition.id()) && completed.containsAll(definition.prerequisites());
    }

    /**
     * Marks the entry completed if its prerequisites are met.
     *
     * @return true if this call newly completed the entry, false if it was
     *         already known or a prerequisite is missing
     */
    public boolean complete(ResearchDefinition definition) {
        if (!canComplete(definition)) {
            return false;
        }
        completed.add(definition.id());
        return true;
    }

    /**
     * Restore path for the persistence codec: marks an entry known without
     * prerequisite re-validation (the save was already validated when written).
     */
    public void completeRaw(ResearchId id) {
        completed.add(id);
    }
}
