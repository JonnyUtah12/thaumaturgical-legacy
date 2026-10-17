package com.jonnyutah12.thaumaturgicallegacy.accessory;

import com.jonnyutah12.thaumaturgicallegacy.accessory.core.VisCostRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The single worn-accessories query every gameplay effect consumes
 * (design record: "effects consume WornAccessories only"). Delegates to
 * the active provider, so effects are identical under Curios and the
 * fallback; this class never imports {@code top.theillusivec4.curios}.
 */
public final class WornAccessories {

    private WornAccessories() {
    }

    public static List<ItemStack> worn(Player player) {
        return AccessoryProviders.active().wornStacks(player);
    }

    public static boolean isWorn(Player player, Item item) {
        for (ItemStack stack : worn(player)) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Effective vis cost for {@code player} casting a spell of
     * {@code baseCost}: the strongest discount among worn accessories,
     * applied through the pure {@link VisCostRules}.
     */
    public static int visCostFor(Player player, int baseCost) {
        int discount = 0;
        for (ItemStack stack : worn(player)) {
            if (stack.getItem() instanceof ThaumicAccessory accessory) {
                discount = Math.max(discount, accessory.visCostDiscountPercent());
            }
        }
        return VisCostRules.discountedCost(baseCost, discount);
    }
}
