package com.jonnyutah12.thaumaturgicallegacy.registration;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.items.ThaumicWandItem;
import com.jonnyutah12.thaumaturgicallegacy.items.VisAmuletItem;
import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumaturgicalLegacy.MODID);

    /**
     * The basic Thaumic Wand. Ships pre-loaded with a small amount of vis so
     * the first cast happens before the player builds a Vis Tap.
     *
     * <p>The properties are built inside the factory, not at class-init: the
     * default components reference data-component {@code DeferredHolder}s,
     * which are unbound until the DATA_COMPONENTS RegisterEvent fires. The
     * factory runs at ITEMS RegisterEvent time — after components, because the
     * mod constructor registers the holders in that order.</p>
     */
    public static final DeferredItem<ThaumicWandItem> THAUMIC_WAND =
            ITEMS.registerItem("thaumic_wand", props -> new ThaumicWandItem(props
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .setNoRepair()
                    .component(ModDataComponents.VIS_STORED.get(), 25)
                    .component(ModDataComponents.ACTIVE_SPELL.get(), Spell.FIREBOLT.ordinal())));

    public static final DeferredItem<BlockItem> VIS_TAP_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.VIS_TAP);

    /**
     * The Vis Amulet (TL-019): a {@code ThaumicAccessory} worn in the
     * dedicated charm slot. The item class is deliberately free of any
     * accessory-framework type — the Curios bridge attaches its behavior
     * via {@code CuriosApi.registerCurio}, and the fallback provider
     * equips it through the shared {@code ThaumicAccessory} marker.
     */
    public static final DeferredItem<VisAmuletItem> VIS_AMULET =
            ITEMS.registerItem("vis_amulet", props -> new VisAmuletItem(props
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)));

    /** See {@link ModBlocks#register(IEventBus)}: must run before RegisterEvents fire. */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {
    }
}
