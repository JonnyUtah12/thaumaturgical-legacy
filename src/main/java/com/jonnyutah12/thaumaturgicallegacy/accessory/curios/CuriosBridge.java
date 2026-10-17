package com.jonnyutah12.thaumaturgicallegacy.accessory.curios;

import com.jonnyutah12.thaumaturgicallegacy.accessory.AccessoryProvider;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Optional Curios integration (TL-019). This class — and everything in its
 * package — is loaded only when Curios is present (design record: "the
 * bridge is a separate class set loaded only when
 * ModList.get().isLoaded(\"curios\")"). Item classes stay Curios-free:
 * behavior is attached via {@code CuriosApi.registerCurio}, and item→slot
 * routing happens through the {@code curios:thaumaturgicallegacy.charm}
 * item tag, not through code.
 *
 * <p>The charm slot type itself is declared as data, not code:
 * {@code data/thaumaturgicallegacy/curios/slots/thaumaturgicallegacy.charm.json}
 * (size 1, dedicated id — not a preset slot; extra slots become research
 * rewards later via {@code CuriosApi.addSlotModifier}). In Curios 9.5.1
 * slot types are loaded from the datapack by its slot manager; the
 * programmatic {@code ISlotHelper.addSlotType} is deprecated-for-removal
 * and the slot helper does not even exist until
 * {@code ServerAboutToStartEvent}, so common-setup registration would
 * race it (verified against the ATM10-pinned 9.5.1 jar).</p>
 */
public final class CuriosBridge {

    public static final String ID = "curios";

    private CuriosBridge() {
    }

    /** Called once from the mod constructor when Curios is present. */
    public static AccessoryProvider activate() {
        return new CuriosAccessoryProvider();
    }

    /** Hooked onto the mod event bus only when Curios is present. */
    public static void registerBusHooks(IEventBus modEventBus) {
        modEventBus.addListener(CuriosBridge::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Item behavior without VisAmuletItem implementing any Curios type.
            CuriosApi.registerCurio(ModItems.VIS_AMULET.get(), new VisAmuletCurio());
        });
    }
}
