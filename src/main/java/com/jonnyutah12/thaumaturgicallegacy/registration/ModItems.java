package com.jonnyutah12.thaumaturgicallegacy.registration;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.items.ThaumicWandItem;
import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = ThaumaturgicalLegacy.ITEMS;

    /**
     * The basic Thaumic Wand. Ships pre-loaded with a small amount of vis so
     * the first cast happens before the player builds a Vis Tap.
     */
    public static final DeferredItem<ThaumicWandItem> THAUMIC_WAND =
            ITEMS.registerItem("thaumic_wand", ThaumicWandItem::new, new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .setNoRepair()
                    .component(ModDataComponents.VIS_STORED.get(), 25)
                    .component(ModDataComponents.ACTIVE_SPELL.get(), Spell.FIREBOLT.ordinal()));

    public static final DeferredItem<BlockItem> VIS_TAP_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.VIS_TAP);

    private ModItems() {
    }
}
