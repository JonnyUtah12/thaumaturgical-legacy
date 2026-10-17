package com.jonnyutah12.thaumaturgicallegacy.event;

import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;
import com.jonnyutah12.thaumaturgicallegacy.magic.research.PlayerKnowledge;
import com.jonnyutah12.thaumaturgicallegacy.magic.research.ResearchCatalog;
import com.jonnyutah12.thaumaturgicallegacy.magic.research.ResearchDefinition;
import com.jonnyutah12.thaumaturgicallegacy.magic.research.ResearchId;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModAttachments;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModBlocks;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Research progression wiring (TL-017). The pure rules live in
 * {@code magic.research}; this class is the only place that touches the game:
 * it subscribes the unlock triggers, grants item rewards, announces
 * completions, and answers the spell-gate query.
 *
 * <p>Triggers mirror verified Thaumcraft 4 behavior (CORE_MECHANICS.md §7.5):
 * auto-unlock on join, hidden-research completion on crafting, and — for the
 * vis-tapping entry — completion on placing the device. TC's research-table
 * hex puzzle is deliberately not replicated; the RE docs mark it as
 * presentation, not mechanics.</p>
 *
 * <p>Every trigger method is split into an event listener plus a plain
 * {@code (Player, ...)} method so game tests can drive the exact same code
 * path without a real player session.</p>
 */
public final class ResearchEvents {

    private ResearchEvents() {
    }

    /** Subscribes on the game event bus. Called once from the mod constructor. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(ResearchEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(ResearchEvents::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(ResearchEvents::onBlockPlaced);
    }

    // --- queries ---------------------------------------------------------------

    public static PlayerKnowledge knowledge(Player player) {
        return player.getData(ModAttachments.RESEARCH.get());
    }

    /**
     * Whether the player may channel {@code spell}: Firebolt is always
     * channelable; every other spell requires the research entry that unlocks
     * it (gating model verified for TC recipes/spells in CORE_MECHANICS.md §7.6).
     */
    public static boolean isSpellKnown(Player player, Spell spell) {
        if (spell == Spell.FIREBOLT) {
            return true;
        }
        return ResearchCatalog.spellsUnlockedBy(knowledge(player).known()).contains(spell);
    }

    // --- completion ------------------------------------------------------------

    /**
     * Attempts to complete a research entry for the player. No-op (returns
     * false) when a prerequisite is missing or the entry is already known.
     * On success: persists + syncs the attachment, grants item rewards, and
     * announces the completion in chat.
     */
    public static boolean completeResearch(Player player, ResearchId id) {
        PlayerKnowledge knowledge = knowledge(player);
        ResearchDefinition definition = ResearchCatalog.definition(id);
        if (!knowledge.complete(definition)) {
            return false;
        }
        // setData with the (mutated) instance triggers save-marking and the
        // attachment's client sync — mutating getData() alone would not.
        player.setData(ModAttachments.RESEARCH.get(), knowledge);

        grantRewards(player, id);
        player.displayClientMessage(
                Component.translatable("message.thaumaturgicallegacy.research_unlocked",
                        Component.translatable(id.descriptionId())),
                false);
        if (id == ResearchId.FIRST_SIGHT) {
            player.displayClientMessage(
                    Component.translatable("message.thaumaturgicallegacy.research_first_sight_hint"),
                    false);
        }
        return true;
    }

    // --- trigger entry points (game-testable) -----------------------------------

    /** Crafting trigger: completing the wand reveals Wandwright (TC §7.5 analogue). */
    public static void onCrafted(Player player, ItemStack crafted) {
        if (crafted.is(ModItems.THAUMIC_WAND.get())) {
            completeResearch(player, ResearchId.WANDCRAFT);
        }
    }

    /** Placing a Vis Tap reveals Tapping the Flow (and with it, Frost Lance). */
    public static void onVisTapPlaced(Player player) {
        completeResearch(player, ResearchId.VIS_TAPPING);
    }

    // --- event listeners ---------------------------------------------------------

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        completeResearch(event.getEntity(), ResearchId.FIRST_SIGHT);
    }

    private static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        onCrafted(event.getEntity(), event.getCrafting());
    }

    private static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        // EntityPlaceEvent carries a nullable Entity; only player placements count.
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getPlacedBlock().is(ModBlocks.VIS_TAP.get())) {
            onVisTapPlaced(player);
        }
    }

    // --- rewards -----------------------------------------------------------------

    private static void grantRewards(Player player, ResearchId id) {
        if (id == ResearchId.WANDCRAFT) {
            giveItem(player, new ItemStack(ModItems.RESEARCH_NOTE.get()));
            player.displayClientMessage(
                    Component.translatable("message.thaumaturgicallegacy.research_note_granted"),
                    false);
        }
    }

    private static void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            if (!player.level().isClientSide) {
                player.drop(stack, false);
            }
        }
    }
}
