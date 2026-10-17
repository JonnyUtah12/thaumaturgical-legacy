package com.jonnyutah12.thaumaturgicallegacy.registration;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.block.VisTapBlock;
import com.jonnyutah12.thaumaturgicallegacy.blockentity.VisTapBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumaturgicalLegacy.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VisTapBlockEntity>> VIS_TAP =
            BLOCK_ENTITY_TYPES.register("vis_tap",
                    () -> BlockEntityType.Builder.of(VisTapBlockEntity::new, ModBlocks.VIS_TAP.get()).build(null));

    private ModBlockEntities() {
    }
}
