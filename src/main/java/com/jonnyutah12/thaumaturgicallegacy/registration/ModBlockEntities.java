package com.jonnyutah12.thaumaturgicallegacy.registration;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.block.VisTapBlock;
import com.jonnyutah12.thaumaturgicallegacy.blockentity.VisTapBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumaturgicalLegacy.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VisTapBlockEntity>> VIS_TAP =
            BLOCK_ENTITY_TYPES.register("vis_tap",
                    () -> BlockEntityType.Builder.of(VisTapBlockEntity::new, ModBlocks.VIS_TAP.get()).build(null));

    /**
     * See {@link ModBlocks#register(IEventBus)}: must run before RegisterEvents
     * fire. The supplier below also forces {@code ModBlocks} class-init, so
     * {@code ModBlocks.register} has to have run first.
     */
    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }

    private ModBlockEntities() {
    }
}
