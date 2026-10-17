package com.jonnyutah12.thaumaturgicallegacy.accessory.fallback;

import com.jonnyutah12.thaumaturgicallegacy.accessory.AccessoryProvider;
import com.jonnyutah12.thaumaturgicallegacy.accessory.ThaumicAccessory;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModAttachments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The built-in fallback charm slot (design record: "one hidden charm slot,
 * right-click equip/unequip, no GUI ... a compatibility shim, not a second
 * framework"). Active only when Curios is absent.
 *
 * <p>Server-authoritative state lives in a player attachment (vanilla
 * player save). {@link FallbackAccessoryEvents} covers join sync and
 * death/respawn; every change pushes one {@link CharmSyncPayload} so the
 * display-only client mirror stays current.</p>
 */
public final class FallbackAccessoryProvider implements AccessoryProvider {

    public static final String ID = "fallback";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<ItemStack> wornStacks(Player player) {
        ItemStack charm = player.getData(ModAttachments.THAUMIC_CHARM.get());
        return charm.isEmpty() ? List.of() : List.of(charm);
    }

    public boolean isWorn(Player player) {
        return !player.getData(ModAttachments.THAUMIC_CHARM.get()).isEmpty();
    }

    /**
     * Moves one item from the given hand into the charm slot. Server-side;
     * validation mirrors the Curios bridge's {@code canEquip} contract:
     * only {@link ThaumicAccessory} items, only into an empty slot.
     *
     * @return false when the stack is not an accessory or the slot is occupied
     */
    public boolean equip(Player player, ItemStack stack, InteractionHand hand) {
        if (!(stack.getItem() instanceof ThaumicAccessory) || isWorn(player)) {
            return false;
        }
        player.setData(ModAttachments.THAUMIC_CHARM.get(), stack.copyWithCount(1));
        stack.shrink(1);
        FallbackAccessoryEvents.syncToClient(player);
        return true;
    }

    /**
     * Removes the worn accessory into the player's inventory, dropping it at
     * the player when the inventory is full. Server-side.
     */
    public boolean unequip(Player player) {
        ItemStack charm = player.getData(ModAttachments.THAUMIC_CHARM.get());
        if (charm.isEmpty()) {
            return false;
        }
        player.setData(ModAttachments.THAUMIC_CHARM.get(), ItemStack.EMPTY);
        if (!player.getInventory().add(charm)) {
            player.drop(charm, false);
        }
        FallbackAccessoryEvents.syncToClient(player);
        return true;
    }
}
