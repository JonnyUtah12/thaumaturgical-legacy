package com.jonnyutah12.thaumaturgicallegacy.gametest;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.accessory.AccessoryProviders;
import com.jonnyutah12.thaumaturgicallegacy.accessory.WornAccessories;
import com.jonnyutah12.thaumaturgicallegacy.accessory.core.VisCostRules;
import com.jonnyutah12.thaumaturgicallegacy.accessory.fallback.FallbackAccessoryProvider;
import com.jonnyutah12.thaumaturgicallegacy.items.VisAmuletItem;
import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;
import com.jonnyutah12.thaumaturgicallegacy.magic.SpellCatalog;
import com.jonnyutah12.thaumaturgicallegacy.magic.SpellDefinition;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModDataComponents;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The in-game no-Curios startup check plus fallback contracts (TL-019).
 *
 * <p>Deliberately NOT annotated with {@code @GameTestHolder} (auto-discovery
 * would run it under Curios too): it is registered only when Curios is
 * absent, via {@code ThaumaturgicalLegacy#registerGameTests}, and the game
 * test server is booted without the optional dependency for it
 * ({@code ./gradlew runGameTestServer -Ptl.noCurios}). Reaching this test
 * at all proves the core mod loads and runs standalone; the assertions
 * prove the fallback provider took over and the equipped effect behaves
 * identically to the Curios path (design record: "effects ... behave
 * identically under either provider").</p>
 *
 * <p>Covers: right-click equip (hand consumed, validation), the discounted
 * cast, right-click unequip (worn stack returned), and the full-cost cast
 * afterwards. Death/respawn copy semantics are exercised by the clone
 * handler's keepInventory branch — deferred to manual verification in
 * TL-023's end-to-end pass (a scripted death needs a real player).</p>
 */
@PrefixGameTestTemplate(false)
public final class AccessoryFallbackGameTest {

    private AccessoryFallbackGameTest() {
    }

    @GameTest(template = "empty", templateNamespace = ThaumaturgicalLegacy.MODID, timeoutTicks = 240)
    public static void standaloneBootsFallbackEquipsAndDiscountsVis(GameTestHelper helper) {
        helper.assertTrue(!net.neoforged.fml.ModList.get().isLoaded(AccessoryProviders.CURIOS_MOD_ID),
                "this test must only run when Curios is absent");
        helper.assertTrue(FallbackAccessoryProvider.ID.equals(AccessoryProviders.active().id()),
                "the fallback provider must be active without Curios");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // Right-click equip: the held amulet moves into the charm slot.
        ItemStack amulet = new ItemStack(ModItems.VIS_AMULET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, amulet);
        amulet.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(WornAccessories.isWorn(player, ModItems.VIS_AMULET.get()),
                "right-click must equip the amulet via the fallback provider");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "equipping must consume the held stack");

        // Effect while equipped: the cast withdraws the discounted cost.
        SpellDefinition definition = SpellCatalog.definition(Spell.FIREBOLT);
        int discounted = castAndMeasureWithdrawal(helper, player);
        helper.assertTrue(discounted == VisCostRules.discountedCost(
                        definition.visCost(), VisAmuletItem.VIS_DISCOUNT_PERCENT),
                "the worn amulet must discount the cast cost identically to the Curios path (got "
                        + discounted + ", base " + definition.visCost() + ")");

        // Right-click while worn: the worn amulet is removed to the inventory.
        ItemStack second = new ItemStack(ModItems.VIS_AMULET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, second);
        second.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!WornAccessories.isWorn(player, ModItems.VIS_AMULET.get()),
                "right-click while worn must unequip via the fallback provider");

        int full = castAndMeasureWithdrawal(helper, player);
        helper.assertTrue(full == definition.visCost(),
                "an unequipped cast must pay the full catalog cost (got " + full + ")");

        helper.succeed();
    }

    /** Casts the default wand's active spell and returns the vis actually withdrawn. */
    private static int castAndMeasureWithdrawal(GameTestHelper helper, Player player) {
        ItemStack wand = new ItemStack(ModItems.THAUMIC_WAND.get());
        int before = wand.getOrDefault(ModDataComponents.VIS_STORED.get(), 0);
        player.getCooldowns().removeCooldown(wand.getItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        return before - wand.getOrDefault(ModDataComponents.VIS_STORED.get(), 0);
    }
}
