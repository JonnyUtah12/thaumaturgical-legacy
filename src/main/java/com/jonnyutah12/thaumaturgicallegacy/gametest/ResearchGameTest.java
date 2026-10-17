package com.jonnyutah12.thaumaturgicallegacy.gametest;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.event.ResearchEvents;
import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;
import com.jonnyutah12.thaumaturgicallegacy.magic.research.PlayerKnowledge;
import com.jonnyutah12.thaumaturgicallegacy.magic.research.ResearchId;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModAttachments;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TL-017: the research progression loop end to end, against the real
 * attachment-backed PlayerKnowledge. The pure rules are unit-tested in
 * {@code magic/research}; this covers the shell: attachment access, the
 * trigger entry points, the note reward, and the spell gate.
 *
 * <p>Driven through the same {@link ResearchEvents} entry points the event
 * listeners call, so trigger wiring and rules are exercised together without
 * needing a logged-in session.</p>
 */
@GameTestHolder(ThaumaturgicalLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ResearchGameTest {

    private ResearchGameTest() {
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void researchChainGatesSpellsAndGrantsNote(final GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerKnowledge knowledge = ResearchEvents.knowledge(player);

        // Fresh player: no research, Frost Lance locked, Firebolt always on.
        helper.assertTrue(!knowledge.isKnown(ResearchId.WANDCRAFT),
                "fresh mock player must know no research");
        helper.assertTrue(!ResearchEvents.isSpellKnown(player, Spell.FROST_LANCE),
                "Frost Lance must be locked before research");
        helper.assertTrue(ResearchEvents.isSpellKnown(player, Spell.FIREBOLT),
                "Firebolt must always be channelable");

        // Prerequisites: the tree cannot be skipped ahead.
        helper.assertTrue(!ResearchEvents.completeResearch(player, ResearchId.VIS_TAPPING),
                "Tapping the Flow must not complete before Wandwright");

        // Join-trigger entry completes once, idempotently.
        helper.assertTrue(ResearchEvents.completeResearch(player, ResearchId.FIRST_SIGHT),
                "First Sight must complete on first call");
        helper.assertTrue(!ResearchEvents.completeResearch(player, ResearchId.FIRST_SIGHT),
                "completion must be idempotent");

        // Craft trigger: the wand reveals Wandwright and pays the note reward.
        ResearchEvents.onCrafted(player, new ItemStack(ModItems.THAUMIC_WAND.get()));
        helper.assertTrue(knowledge.isKnown(ResearchId.WANDCRAFT),
                "crafting the wand must unlock Wandwright");
        helper.assertTrue(player.getInventory().countItem(ModItems.RESEARCH_NOTE.get()) == 1,
                "Wandwright must grant exactly one research note");

        // Place trigger: the vis tap reveals Tapping the Flow and Frost Lance.
        ResearchEvents.onVisTapPlaced(player);
        helper.assertTrue(knowledge.isKnown(ResearchId.VIS_TAPPING),
                "placing a vis tap must unlock Tapping the Flow");
        helper.assertTrue(ResearchEvents.isSpellKnown(player, Spell.FROST_LANCE),
                "Tapping the Flow must unlock Frost Lance");

        // Persistence (TL-020 requirement): the attachment's save path is
        // exactly RESEARCH_CODEC, so an NBT round-trip through it is the
        // mechanical proof that progression survives a world restart.
        // Unknown ids must be dropped, not fatal (forward tolerance).
        var codec = ModAttachments.RESEARCH_CODEC;
        var encoded = codec.encode(knowledge, net.minecraft.nbt.NbtOps.INSTANCE,
                net.minecraft.nbt.NbtOps.INSTANCE.empty()).getOrThrow();
        var restored = codec.decode(net.minecraft.nbt.NbtOps.INSTANCE, encoded)
                .getOrThrow().getFirst();
        helper.assertTrue(restored.knownIds().equals(knowledge.knownIds()),
                "the save codec must round-trip completed research exactly");
        var futureSave = new net.minecraft.nbt.ListTag();
        futureSave.add(net.minecraft.nbt.StringTag.valueOf("FIRST_SIGHT"));
        futureSave.add(net.minecraft.nbt.StringTag.valueOf("AN_ENTRY_FROM_A_NEWER_VERSION"));
        var tolerant = codec.decode(net.minecraft.nbt.NbtOps.INSTANCE, futureSave)
                .result().orElse(null);
        helper.assertTrue(tolerant != null
                        && tolerant.getFirst().knownIds().equals(java.util.List.of(ResearchId.FIRST_SIGHT)),
                "unknown entry ids from newer versions must be dropped, not crash the load");

        helper.succeed();
    }
}
