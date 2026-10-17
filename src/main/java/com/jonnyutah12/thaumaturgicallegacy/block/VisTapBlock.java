package com.jonnyutah12.thaumaturgicallegacy.block;

import com.jonnyutah12.thaumaturgicallegacy.blockentity.VisTapBlockEntity;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Vis Tap: the first system's vis source. It gathers ambient vis slowly
 * into an internal buffer; players channel that buffer into a wand. See
 * docs/architecture/MAGIC_SYSTEM_V0_1.md for the design record.
 */
public class VisTapBlock extends BaseEntityBlock {

    public static final com.mojang.serialization.MapCodec<VisTapBlock> CODEC =
            simpleCodec(VisTapBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public VisTapBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VisTapBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null
                : createTickerHelper(blockEntityType, ModBlockEntities.VIS_TAP.get(), VisTapBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof VisTapBlockEntity tap) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        tap.statusMessage(), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
