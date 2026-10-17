package com.jonnyutah12.thaumaturgicallegacy.accessory;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.accessory.curios.CuriosBridge;
import com.jonnyutah12.thaumaturgicallegacy.accessory.fallback.FallbackAccessoryEvents;
import com.jonnyutah12.thaumaturgicallegacy.accessory.fallback.FallbackAccessoryProvider;
import net.neoforged.fml.ModList;

/**
 * Startup provider selection (design record: "provider selection at
 * startup, exactly one active"). Called once from the mod constructor.
 *
 * <p>Load safety: {@code CuriosBridge} is referenced only inside the
 * Curios-present branch, so its class (and every Curios type it touches)
 * is never loaded when Curios is absent — no reflection, no mixin tricks.
 * The unit test {@code OptionalIntegrationIsolationTest} guards that no
 * other core class references Curios types.</p>
 */
public final class AccessoryProviders {

    public static final String CURIOS_MOD_ID = "curios";

    private static AccessoryProvider active;

    private AccessoryProviders() {
    }

    public static boolean curiosLoaded() {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }

    public static void init() {
        if (curiosLoaded()) {
            active = CuriosBridge.activate();
        } else {
            active = new FallbackAccessoryProvider();
            FallbackAccessoryEvents.register();
        }
        ThaumaturgicalLegacy.LOGGER.info("Accessory provider active: {}", active.id());
    }

    public static AccessoryProvider active() {
        AccessoryProvider provider = active;
        if (provider == null) {
            throw new IllegalStateException("accessory providers were never initialized");
        }
        return provider;
    }
}
