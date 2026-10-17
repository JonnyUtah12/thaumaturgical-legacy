package com.jonnyutah12.thaumaturgicallegacy;

import com.jonnyutah12.thaumaturgicallegacy.gametest.ModSmokeGameTest;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModBlockEntities;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModCreativeTabs;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModDataComponents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Mod entry point. Common/server-side code only: client-only classes must live
 * under {@code client} and load through the dist-separated entry point.
 *
 * <p>Registration infrastructure is established here; actual content (aspects,
 * research, crafting, items) is added by the feature milestones and registered
 * through the deferred registers below.</p>
 */
@Mod(ThaumaturgicalLegacy.MODID)
public class ThaumaturgicalLegacy {
    public static final String MODID = "thaumaturgicallegacy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public ThaumaturgicalLegacy(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(this::registerGameTests);

        LOGGER.info("Thaumaturgical Legacy {} loading (NeoForge, MC 1.21.1)", version(modContainer));
    }

    /**
     * Game tests only exist while the gametest system is active (see
     * docs/TESTING.md); this listener is inert in normal play.
     */
    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(ModSmokeGameTest.class);
    }

    private static String version(ModContainer modContainer) {
        return modContainer.getModInfo().getVersion().toString();
    }
}
