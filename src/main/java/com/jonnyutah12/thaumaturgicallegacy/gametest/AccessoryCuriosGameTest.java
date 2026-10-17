package com.jonnyutah12.thaumaturgicallegacy.gametest;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.accessory.AccessoryProviders;
import com.jonnyutah12.thaumaturgicallegacy.accessory.AccessorySlots;
import com.jonnyutah12.thaumaturgicallegacy.accessory.WornAccessories;
import com.jonnyutah12.thaumaturgicallegacy.accessory.core.VisCostRules;
import com.jonnyutah12.thaumaturgicallegacy.items.ThaumicWandItem;
import com.jonnyutah12.thaumaturgicallegacy.items.VisAmuletItem;
import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;
import com.jonnyutah12.thaumaturgicallegacy.magic.SpellCatalog;
import com.jonnyutah12.thaumaturgicallegacy.magic.SpellDefinition;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModAttachments;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModDataComponents;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

/**
 * In-game coverage for the Curios integration path of the accessory system
 * (TL-019): the dedicated charm slot is loaded from its data file, the
 * amulet routes into it via the {@code curios:thaumaturgicallegacy.charm}
 * item tag, insertion/removal drive the {@code WornAccessories} query, and
 * the equipped effect changes the wand's real vis withdrawal. It also
 * asserts the fallback shim stays inert under Curios (no double state).
 *
 * <p>Deliberately NOT annotated with {@code @GameTestHolder}: annotated
 * holders are auto-discovered regardless of mod presence, and this class
 * references Curios types and must never load without Curios. It is
 * registered only when Curios is present, via
 * {@code ThaumaturgicalLegacy#registerGameTests}. The no-Curios side of
 * the pair is {@link AccessoryFallbackGameTest}.</p>
 *
 * <p>Runs on the game test server with the optional dependency on the
 * runtime classpath ({@code ./gradlew runGameTestServer}; see
 * docs/TESTING.md). Pure discount math is unit-tested in
 * {@code accessory/core/VisCostRulesTest}; this test verifies the
 * in-game wiring around it.</p>
 */
@PrefixGameTestTemplate(false)
public final class AccessoryCuriosGameTest {

    private AccessoryCuriosGameTest() {
    }

    @GameTest(template = "empty", templateNamespace = ThaumaturgicalLegacy.MODID, timeoutTicks = 240)
    @SuppressWarnings("removal") // getSlotHelper is deprecated-for-removal in 9.5.1 but is
    // still the live registration/query path there; the version floor pins 9.5.1.
    public static void charmSlotEquipsAndDiscountsVisCost(GameTestHelper helper) {
        helper.assertTrue("curios".equals(AccessoryProviders.active().id()),
                "the Curios provider must be active when Curios is loaded");

        var slotType = CuriosApi.getSlotHelper().getSlotType(AccessorySlots.CHARM);
        helper.assertTrue(slotType.isPresent(),
                "the charm slot type must be loaded from its curios/slots data file");
        helper.assertTrue(slotType.get().getSize() == 1,
                "the charm slot must be size 1 (design record)");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack amulet = new ItemStack(ModItems.VIS_AMULET.get());

        // Equipment validation: the item tag routes the amulet into our slot.
        SlotContext context = new SlotContext(AccessorySlots.CHARM, player, 0, false, true);
        helper.assertTrue(CuriosApi.isStackValid(context, amulet),
                "the amulet must be valid for the charm slot via its item tag (tags=["
                        + amulet.getTags().map(t -> t.location().toString()).sorted()
                        .collect(java.util.stream.Collectors.joining(" "))
                        + "] curioRegistered=" + CuriosApi.getCurio(amulet).isPresent() + ")");

        // Insertion.
        ICuriosItemHandler curios = CuriosApi.getCuriosInventory(player).orElse(null);
        if (curios == null) {
            helper.fail("no Curios inventory on the mock player");
            return;
        }
        curios.setEquippedCurio(AccessorySlots.CHARM, 0, amulet);
        helper.assertTrue(WornAccessories.isWorn(player, ModItems.VIS_AMULET.get()),
                "the amulet must read as worn through WornAccessories after insertion");

        // Effect while equipped: the cast withdraws the discounted cost.
        SpellDefinition definition = SpellCatalog.definition(Spell.FIREBOLT);
        int discounted = castAndMeasureWithdrawal(helper, player);
        helper.assertTrue(discounted == VisCostRules.discountedCost(
                        definition.visCost(), VisAmuletItem.VIS_DISCOUNT_PERCENT),
                "the worn amulet must discount the cast cost to the VisCostRules value (got "
                        + discounted + ", base " + definition.visCost() + ")");
        helper.assertTrue(discounted < definition.visCost(),
                "the discounted cost must be below the base cost");

        // Removal.
        curios.setEquippedCurio(AccessorySlots.CHARM, 0, ItemStack.EMPTY);
        helper.assertTrue(!WornAccessories.isWorn(player, ModItems.VIS_AMULET.get()),
                "the amulet must be gone from WornAccessories after removal");

        int full = castAndMeasureWithdrawal(helper, player);
        helper.assertTrue(full == definition.visCost(),
                "an unequipped cast must pay the full catalog cost (got " + full + ")");

        // Provider exclusivity: the fallback attachment must stay empty.
        helper.assertTrue(player.getData(ModAttachments.THAUMIC_CHARM.get()).isEmpty(),
                "the fallback attachment must stay inert while Curios is active");

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
