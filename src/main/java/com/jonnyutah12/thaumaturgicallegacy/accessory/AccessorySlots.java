package com.jonnyutah12.thaumaturgicallegacy.accessory;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import net.minecraft.resources.ResourceLocation;

/**
 * Slot identifiers shared by the Curios bridge and the fallback provider.
 *
 * <p>The charm slot is deliberately dedicated (design record: NOT a preset
 * ring/necklace/belt): it avoids crowding slots shared with in-pack Curios
 * gear (docs/ATM10_COMPATIBILITY.md conflict note 2) and lets research
 * later grant "+1 charm slot" via {@code CuriosApi.addSlotModifier}.</p>
 */
public final class AccessorySlots {

    /**
     * Our dedicated accessory slot id. Curios routes items into it through
     * the item tag {@code curios:thaumaturgicallegacy.charm} (the dotted
     * id is a single legal tag path segment — the tag file is
     * {@code data/curios/tags/item/thaumaturgicallegacy.charm.json}).
     */
    public static final String CHARM = "thaumaturgicallegacy.charm";

    /** GUI icon for the charm slot (textures/slot/charm.png). */
    public static final ResourceLocation CHARM_ICON =
            ResourceLocation.fromNamespaceAndPath(ThaumaturgicalLegacy.MODID, "slot/charm");

    private AccessorySlots() {
    }
}
