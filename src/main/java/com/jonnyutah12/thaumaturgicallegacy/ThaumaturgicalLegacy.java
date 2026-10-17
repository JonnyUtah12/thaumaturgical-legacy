package com.jonnyutah12.thaumaturgicallegacy;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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

        LOGGER.info("Thaumaturgical Legacy {} loading (NeoForge, MC 1.21.1)", version(modContainer));
    }

    private static String version(ModContainer modContainer) {
        return modContainer.getModInfo().getVersion().toString();
    }
}
