package com.jonnyutah12.thaumaturgicallegacy.client;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only entry point. Never loaded on a dedicated server; all client
 * rendering and GUI code for the mod belongs under this package.
 */
@EventBusSubscriber(modid = ThaumaturgicalLegacy.MODID, value = Dist.CLIENT)
public class ThaumaturgicalLegacyClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ThaumaturgicalLegacy.LOGGER.info("Thaumaturgical Legacy client setup complete");
    }
}
