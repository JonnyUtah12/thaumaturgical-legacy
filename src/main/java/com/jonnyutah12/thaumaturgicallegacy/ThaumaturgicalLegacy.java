package com.jonnyutah12.thaumaturgicallegacy;

import com.jonnyutah12.thaumaturgicallegacy.gametest.ModSmokeGameTest;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModBlockEntities;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModBlocks;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModCreativeTabs;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModDataComponents;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

/**
 * Mod entry point. Common/server-side code only: client-only classes must live
 * under {@code client} and load through the dist-separated entry point.
 *
 * <p>Registration infrastructure is established here; actual content (aspects,
 * research, crafting, items) is added by the feature milestones through the
 * {@code registration} holder classes, each of which must be initialized from
 * this constructor before any RegisterEvent fires.</p>
 */
@Mod(ThaumaturgicalLegacy.MODID)
public class ThaumaturgicalLegacy {
    public static final String MODID = "thaumaturgicallegacy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ThaumaturgicalLegacy(IEventBus modEventBus, ModContainer modContainer) {
        // Every deferred holder must be initialized — its class-init run and
        // its entries created — before any RegisterEvent fires. Registering
        // only the shared DeferredRegisters is not enough: a holder referenced
        // from another holder's supplier (ModBlockEntities -> ModBlocks) would
        // otherwise class-init inside a RegisterEvent dispatch, too late for
        // its own registry ("Cannot register new entries to DeferredRegister
        // after RegisterEvent has been fired"). Order follows dependencies:
        // components, blocks, items, block entities, tabs.
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
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
