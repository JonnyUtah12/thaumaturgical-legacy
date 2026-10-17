package com.jonnyutah12.thaumaturgicallegacy.registration;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom item data components (1.20.5+ component model). The wand's vis and
 * active-spell state live on the stack itself, so they persist, sync, and
 * travel through inventories without any capability plumbing.
 */
public final class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ThaumaturgicalLegacy.MODID);

    /** Vis currently stored in a wand, 0..capacity. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VIS_STORED =
            DATA_COMPONENTS.registerComponentType("vis_stored",
                    (DataComponentType.Builder<Integer> builder) -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT));

    /** Ordinal of the wand's active {@code Spell}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ACTIVE_SPELL =
            DATA_COMPONENTS.registerComponentType("active_spell",
                    (DataComponentType.Builder<Integer> builder) -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT));

    /** See {@link ModBlocks#register(IEventBus)}: must run before RegisterEvents fire. */
    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }

    private ModDataComponents() {
    }
}
