package com.jonnyutah12.thaumaturgicallegacy.accessory.fallback;

import com.jonnyutah12.thaumaturgicallegacy.registration.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * Fallback lifecycle contracts (design record §Fallback contracts): join
 * sync, change sync, and death copy/drop honoring {@code keepInventory} —
 * the same death semantics Curios applies to its slots (Baubles spec §E,
 * docs/reverse-engineering/ACCESSORY_MECHANICS.md). Registered on the game
 * event bus only when the fallback provider is active.
 */
public final class FallbackAccessoryEvents {

    private FallbackAccessoryEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FallbackAccessoryEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(FallbackAccessoryEvents::onPlayerClone);
    }

    public static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack charm = serverPlayer.getData(ModAttachments.THAUMIC_CHARM.get());
            PacketDistributor.sendToPlayer(serverPlayer, new CharmSyncPayload(
                    charm.isEmpty() ? Optional.empty() : Optional.of(charm.copy())));
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncToClient(event.getEntity());
    }

    /**
     * Death/respawn: with {@code keepInventory} the charm survives the
     * clone; without it the charm drops at the death location. Non-death
     * clones (End exit) always copy, matching vanilla inventory behavior.
     */
    private static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        ItemStack charm = original.getData(ModAttachments.THAUMIC_CHARM.get());
        if (charm.isEmpty()) {
            return;
        }
        boolean keep = !event.isWasDeath()
                || original.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        if (keep) {
            event.getEntity().setData(ModAttachments.THAUMIC_CHARM.get(), charm.copy());
            syncToClient(event.getEntity());
        } else {
            original.setData(ModAttachments.THAUMIC_CHARM.get(), ItemStack.EMPTY);
            if (!original.level().isClientSide) {
                ServerLevel level = (ServerLevel) original.level();
                level.addFreshEntity(new ItemEntity(level,
                        original.getX(), original.getY(), original.getZ(), charm.copy()));
            }
        }
    }
}
