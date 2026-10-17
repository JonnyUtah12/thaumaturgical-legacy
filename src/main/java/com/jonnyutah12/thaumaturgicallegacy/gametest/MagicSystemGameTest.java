package com.jonnyutah12.thaumaturgicallegacy.gametest;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.blockentity.VisTapBlockEntity;
import com.jonnyutah12.thaumaturgicallegacy.items.ThaumicWandItem;
import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;
import com.jonnyutah12.thaumaturgicallegacy.magic.SpellCatalog;
import com.jonnyutah12.thaumaturgicallegacy.magic.SpellDefinition;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModBlocks;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModDataComponents;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-game coverage for the TL-009 vis loop (t_c5894126): place a Vis Tap,
 * let its ticker accumulate ambient vis, charge a Thaumic Wand by using the
 * wand on the tap, then cast the active spell and assert the vis component
 * decrements by exactly the spell's cost.
 *
 * <p>The numeric rules themselves are unit-tested in
 * {@code src/test/java/.../magic/}; this gametest verifies the in-game
 * wiring: the block-entity ticker actually runs, {@code useOn} transfers
 * tap buffer into the wand's data component, and {@code Item.use} withdraws
 * vis and applies the cooldown path. Pure-logic edge cases (clamping,
 * affordability) are deliberately not re-verified here.</p>
 *
 * <p>Coordinates: every position is converted to absolute once via
 * {@link GameTestHelper#absolutePos} and then used with direct level access.
 * The {@code GameTestHelper} setBlock/placeAt/get overloads treat BlockPos
 * arguments as structure-relative (they call {@code absolutePos} internally),
 * so passing an already-absolute position double-offsets it silently — and
 * {@code placeAt} clicks {@code pos.relative(dir)}, not {@code pos}.</p>
 *
 * <p>Runs on the game test server only ({@code ./gradlew runGameTestServer});
 * see docs/TESTING.md for conventions.</p>
 */
@GameTestHolder(ThaumaturgicalLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class MagicSystemGameTest {

    /** Ticks to wait for the tap ticker: +1 vis per TICKS_PER_VIS, want >= 2. */
    private static final int CHARGE_WAIT_TICKS = 45;

    private MagicSystemGameTest() {
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void tapAccumulatesChargesWandAndCastDecrementsVis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos tapPos = helper.absolutePos(new BlockPos(1, 1, 1));

        level.setBlock(tapPos, ModBlocks.VIS_TAP.get().defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(tapPos).is(ModBlocks.VIS_TAP.get()),
                "vis tap was not placed at " + tapPos);

        BlockEntity blockEntity = level.getBlockEntity(tapPos);
        if (!(blockEntity instanceof VisTapBlockEntity tap)) {
            helper.fail("no VisTapBlockEntity at " + tapPos + " (got " + blockEntity + ")");
            return;
        }
        helper.assertTrue(tap.bufferAmount() == 0, "a freshly placed tap must start with an empty buffer");

        // Let the server ticker run: the tap gathers ambient vis over time.
        helper.runAfterDelay(CHARGE_WAIT_TICKS, () -> {
            int gathered = tap.bufferAmount();
            helper.assertTrue(gathered >= 2,
                    "tap did not accumulate vis after " + CHARGE_WAIT_TICKS + " ticks (buffer=" + gathered + ")");

            // Charge: using the wand on the tap moves buffer vis into the wand component.
            ItemStack wand = new ItemStack(ModItems.THAUMIC_WAND.get());
            int visBefore = wand.getOrDefault(ModDataComponents.VIS_STORED.get(), 0);
            helper.assertTrue(visBefore > 0, "a crafted wand must ship with its default vis");
            helper.assertTrue(visBefore < ThaumicWandItem.CAPACITY,
                    "test assumes the wand has room to receive vis");

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            // useOn() mutates context.getItemInHand() — the player must hold the wand.
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            BlockHitResult tapTop = new BlockHitResult(Vec3.atCenterOf(tapPos), Direction.UP, tapPos, false);
            wand.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, tapTop));

            int visCharged = wand.getOrDefault(ModDataComponents.VIS_STORED.get(), 0);
            helper.assertTrue(visCharged > visBefore,
                    "charging at the tap did not increase the wand's vis (" + visBefore + " -> " + visCharged + ")");
            helper.assertTrue(visCharged <= ThaumicWandItem.CAPACITY,
                    "wand vis exceeded its capacity: " + visCharged);
            helper.assertTrue(tap.bufferAmount() == 0,
                    "the tap buffer should be drained after charging (" + tap.bufferAmount() + " left)");

            // Cast: right-click use of the wand withdraws exactly the active spell's cost.
            Spell spell = ThaumicWandItem.activeSpell(wand);
            SpellDefinition definition = SpellCatalog.definition(spell);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            wand.getItem().use(level, player, InteractionHand.MAIN_HAND);

            int visAfterCast = wand.getOrDefault(ModDataComponents.VIS_STORED.get(), 0);
            helper.assertTrue(visAfterCast == visCharged - definition.visCost(),
                    "casting " + spell + " must decrement vis by " + definition.visCost()
                            + " (" + visCharged + " -> " + visAfterCast + ")");

            helper.succeed();
        });
    }
}
