package com.jonnyutah12.thaumaturgicallegacy.accessory.curios;

import com.jonnyutah12.thaumaturgicallegacy.accessory.ThaumicAccessory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Curios-side behavior for our accessories, attached via
 * {@code CuriosApi.registerCurio} so item classes never reference Curios
 * (the load-safety mechanism from the design record).
 *
 * <p>The vis discount itself is provider-independent — it is queried
 * through {@code WornAccessories} at cast time — so these hooks cover the
 * lifecycle feedback the design record requires (exactly-once
 * equip/unequip, right-click equip parity with the fallback shim).</p>
 */
final class VisAmuletCurio implements ICurioItem {

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack prevStack) {
        return prevStack.getItem() instanceof ThaumicAccessory;
    }

    /** Right-click equips into our slot, mirroring the fallback's interaction. */
    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return stack.getItem() instanceof ThaumicAccessory;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack newStack) {
        announce(slotContext.entity(), "message.thaumaturgicallegacy.amulet_equipped");
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack prevStack, ItemStack newStack) {
        announce(slotContext.entity(), "message.thaumaturgicallegacy.amulet_unequipped");
    }

    private static void announce(LivingEntity entity, String messageKey) {
        if (entity instanceof Player player && !entity.level().isClientSide) {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }
}
