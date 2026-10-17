package com.jonnyutah12.thaumaturgicallegacy.registration;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.block.VisTapBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = ThaumaturgicalLegacy.BLOCKS;

    /**
     * Vis Tap: passively gathers ambient vis; right-click with a wand to
     * channel it into the wand, right-click empty-handed to read the buffer.
     */
    public static final DeferredBlock<VisTapBlock> VIS_TAP =
            BLOCKS.registerBlock("vis_tap", VisTapBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 4));

    private ModBlocks() {
    }
}
