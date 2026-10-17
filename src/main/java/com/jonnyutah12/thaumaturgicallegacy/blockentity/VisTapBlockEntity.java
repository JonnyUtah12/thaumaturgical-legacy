package com.jonnyutah12.thaumaturgicallegacy.blockentity;

import com.jonnyutah12.thaumaturgicallegacy.magic.VisStorage;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds the Vis Tap's ambient vis buffer. The buffer logic is the shared
 * {@link VisStorage} from the pure {@code magic} package; this class only adds
 * persistence and the slow environmental gather.
 */
public class VisTapBlockEntity extends BlockEntity {

    /** Buffer capacity in vis. */
    public static final int CAPACITY = 100;
    /** One vis is gathered every this many ticks. */
    public static final int TICKS_PER_VIS = 20;

    private final VisStorage storage = new VisStorage(CAPACITY);
    private int tickCounter = 0;

    public VisTapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VIS_TAP.get(), pos, state);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            VisTapBlockEntity be) {
        if (++be.tickCounter >= TICKS_PER_VIS) {
            be.tickCounter = 0;
            be.storage.deposit(1);
        }
    }

    /**
     * Moves up to {@code max} vis out of the buffer.
     *
     * @return the amount actually removed
     */
    public int draw(int max) {
        int amount = Math.min(Math.max(max, 0), storage.amount());
        storage.withdraw(amount);
        return amount;
    }

    public int bufferAmount() {
        return storage.amount();
    }

    public Component statusMessage() {
        return Component.translatable("message.thaumaturgicallegacy.tap_status", storage.amount(), CAPACITY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("vis_buffer", storage.amount());
        tag.putInt("vis_tick", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storage.setAmount(tag.getInt("vis_buffer"));
        tickCounter = tag.getInt("vis_tick");
    }
}
