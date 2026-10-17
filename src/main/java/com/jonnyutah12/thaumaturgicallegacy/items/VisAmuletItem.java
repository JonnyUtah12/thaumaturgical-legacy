package com.jonnyutah12.thaumaturgicallegacy.items;

import com.jonnyutah12.thaumaturgicallegacy.accessory.AccessoryProviders;
import com.jonnyutah12.thaumaturgicallegacy.accessory.AccessorySlots;
import com.jonnyutah12.thaumaturgicallegacy.accessory.ThaumicAccessory;
import com.jonnyutah12.thaumaturgicallegacy.accessory.fallback.FallbackAccessoryProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Vis Amulet — our vis-amulet analogue (TL-019): while worn it reduces
 * the vis cost of wand spells. The behavioral model (TC pays vis from its
 * amulet first, docs/reverse-engineering/CORE_MECHANICS.md) is adapted,
 * not copied: the discount rule and its numbers are ours, applied through
 * the provider-independent {@code WornAccessories.visCostFor} cost path.
 *
 * <p>Equipping is provider-driven: with Curios present this {@code use}
 * returns PASS and Curios handles the GUI plus right-click equip
 * (canEquipFromUse in the bridge); without Curios the fallback provider
 * toggles equip/unequip on right-click, as the design record's minimal
 * shim specifies.</p>
 */
public class VisAmuletItem extends Item implements ThaumicAccessory {

    /** Percentage off spell vis costs while worn. Our number (clean-room). */
    public static final int VIS_DISCOUNT_PERCENT = 25;

    public VisAmuletItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public String slotKind() {
        return AccessorySlots.CHARM;
    }

    @Override
    public int visCostDiscountPercent() {
        return VIS_DISCOUNT_PERCENT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide
                || !(AccessoryProviders.active() instanceof FallbackAccessoryProvider fallback)) {
            return InteractionResultHolder.pass(stack);
        }
        if (fallback.isWorn(player)) {
            fallback.unequip(player);
            player.displayClientMessage(
                    Component.translatable("message.thaumaturgicallegacy.amulet_unequipped"), true);
        } else if (fallback.equip(player, stack, usedHand)) {
            player.displayClientMessage(
                    Component.translatable("message.thaumaturgicallegacy.amulet_equipped"), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component
                .translatable("tooltip.thaumaturgicallegacy.amulet_effect", VIS_DISCOUNT_PERCENT)
                .withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component
                .translatable("tooltip.thaumaturgicallegacy.amulet_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
