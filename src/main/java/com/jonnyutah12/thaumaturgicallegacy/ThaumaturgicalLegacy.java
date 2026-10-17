package com.jonnyutah12.thaumaturgicallegacy;

import com.jonnyutah12.thaumaturgicallegacy.accessory.AccessoryProviders;
import com.jonnyutah12.thaumaturgicallegacy.accessory.curios.CuriosBridge;
import com.jonnyutah12.thaumaturgicallegacy.accessory.fallback.CharmSyncPayload;
import com.jonnyutah12.thaumaturgicallegacy.event.ResearchEvents;
import com.jonnyutah12.thaumaturgicallegacy.gametest.AccessoryCuriosGameTest;
import com.jonnyutah12.thaumaturgicallegacy.gametest.AccessoryFallbackGameTest;
import com.jonnyutah12.thaumaturgicallegacy.gametest.MagicSystemGameTest;
import com.jonnyutah12.thaumaturgicallegacy.gametest.ModSmokeGameTest;
import com.jonnyutah12.thaumaturgicallegacy.gametest.ResearchGameTest;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModAttachments;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModBlockEntities;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModBlocks;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModCreativeTabs;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModDataComponents;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
        // components, blocks, items, block entities, tabs, attachments.
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.register(modEventBus);

        // Accessory provider selection, exactly one active (TL-019 design
        // record). The Curios bridge's bus hooks — and every class it
        // loads — are attached only when Curios is present.
        AccessoryProviders.init();
        if (AccessoryProviders.curiosLoaded()) {
            CuriosBridge.registerBusHooks(modEventBus);
        }

        // Research progression (TL-017): game-bus triggers, always active.
        ResearchEvents.register();

        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerGameTests);

        LOGGER.info("Thaumaturgical Legacy {} loading (NeoForge, MC 1.21.1)", version(modContainer));
    }

    /**
     * The fallback provider's single S2C payload (design record). Registered
     * unconditionally — it is simply never sent while the Curios provider
     * is active — and its handler uses only {@code IPayloadContext}, so no
     * client-only class is referenced from common code.
     */
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(CharmSyncPayload.TYPE, CharmSyncPayload.STREAM_CODEC,
                (payload, context) -> context.player().setData(
                        ModAttachments.THAUMIC_CHARM.get(), payload.charm().orElse(ItemStack.EMPTY)));
    }

    /**
     * Game tests only exist while the gametest system is active (see
     * docs/TESTING.md); this listener is inert in normal play. The
     * accessory game tests are provider-specific: the Curios-path test
     * class references Curios types and must not load without Curios, and
     * the fallback test only has meaningful assertions when Curios is
     * absent (it is the in-game no-Curios startup check).
     */
    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(ModSmokeGameTest.class);
        event.register(MagicSystemGameTest.class);
        event.register(ResearchGameTest.class);
        if (AccessoryProviders.curiosLoaded()) {
            event.register(AccessoryCuriosGameTest.class);
        } else {
            event.register(AccessoryFallbackGameTest.class);
        }
    }

    private static String version(ModContainer modContainer) {
        return modContainer.getModInfo().getVersion().toString();
    }
}
