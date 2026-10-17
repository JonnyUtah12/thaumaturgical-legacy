package com.jonnyutah12.thaumaturgicallegacy.accessory.curios;

import com.jonnyutah12.thaumaturgicallegacy.accessory.AccessoryProvider;
import com.jonnyutah12.thaumaturgicallegacy.accessory.ThaumicAccessory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

/**
 * Worn-accessory queries backed by Curios' own inventory capability —
 * persistence, per-slot sync, and death handling are Curios-native here
 * (design record §Lifecycle contracts); we only read.
 */
final class CuriosAccessoryProvider implements AccessoryProvider {

    @Override
    public String id() {
        return CuriosBridge.ID;
    }

    @Override
    @SuppressWarnings("removal") // findCurios(LivingEntity, Predicate) is deprecated-for-removal
    // in 9.5.1 but remains the live query path there; the version floor pins 9.5.1.
    public List<ItemStack> wornStacks(Player player) {
        return CuriosApi.getCuriosHelper()
                .findCurios(player, stack -> stack.getItem() instanceof ThaumicAccessory)
                .stream()
                .map(SlotResult::stack)
                .toList();
    }
}
