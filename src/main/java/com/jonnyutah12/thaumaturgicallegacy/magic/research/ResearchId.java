package com.jonnyutah12.thaumaturgicallegacy.magic.research;

/**
 * Identifiers of the research entries implemented by the progression system.
 * Deliberately free of any Minecraft types (same purity rule as the parent
 * {@code magic} package — see docs/architecture/MAGIC_SYSTEM_V0_1.md); the
 * entry graph lives in {@link ResearchCatalog} and per-player state in
 * {@link PlayerKnowledge}.
 *
 * <p>Modeled on Thaumcraft 4's per-player knowledge model
 * ({@code PlayerKnowledge}, verified in docs/reverse-engineering/
 * CORE_MECHANICS.md §7): knowledge is per-player, entries have prerequisites,
 * entries auto-unlock on join or fire on world triggers (crafting, placing),
 * and completion gates gameplay (§7.6). The aspect-pool currency is NOT
 * replicated here — that is a later milestone.</p>
 */
public enum ResearchId {
    /** Auto-completed on player join; the entry point of the tree. */
    FIRST_SIGHT("research.thaumaturgicallegacy.first_sight"),
    /** Unlocked by crafting a Thaumic Wand; grants the research note. */
    WANDCRAFT("research.thaumaturgicallegacy.wandcraft"),
    /** Unlocked by placing a Vis Tap; unlocks the Frost Lance spell. */
    VIS_TAPPING("research.thaumaturgicallegacy.vis_tapping");

    private final String descriptionId;

    ResearchId(String descriptionId) {
        this.descriptionId = descriptionId;
    }

    /** Translation key for the entry's display name. */
    public String descriptionId() {
        return descriptionId;
    }
}
