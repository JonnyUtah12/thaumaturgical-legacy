package com.jonnyutah12.thaumaturgicallegacy.registration;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import com.jonnyutah12.thaumaturgicallegacy.magic.research.PlayerKnowledge;
import com.jonnyutah12.thaumaturgicallegacy.magic.research.ResearchId;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Player attachments backing the no-Curios fallback charm slot (TL-019).
 * Persistence is the vanilla player save (design record: "fallback uses
 * vanilla player-save attachments"). Death handling is explicit in
 * {@code FallbackAccessoryEvents} so it can honor {@code keepInventory} —
 * the attachment deliberately does NOT use {@code copyOnDeath()}.
 */
public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ThaumaturgicalLegacy.MODID);

    /** The single charm slot worn via the fallback provider (EMPTY when unworn). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> THAUMIC_CHARM =
            ATTACHMENT_TYPES.register("thaumic_charm",
                    () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                            .serialize(ItemStack.OPTIONAL_CODEC)
                            .build());

    /**
     * Codec over the completed-id list. Decodes leniently (unknown ids from
     * newer versions are dropped rather than failing the whole attachment);
     * encodes in stable enum order. Public so the gametest can assert the
     * exact save/load path the loader uses.
     */
    public static final Codec<PlayerKnowledge> RESEARCH_CODEC =
            Codec.STRING.listOf().xmap(names -> {
                PlayerKnowledge knowledge = new PlayerKnowledge();
                for (String name : names) {
                    try {
                        knowledge.completeRaw(ResearchId.valueOf(name));
                    } catch (IllegalArgumentException ignored) {
                        // unknown entry id (e.g. from a newer version) — drop it
                    }
                }
                return knowledge;
            }, knowledge -> knowledge.knownIds().stream().map(Enum::name).toList());

    /**
     * Per-player research knowledge (TL-017). Serialized with the lenient
     * enum-name list codec above; synced to clients so the wand's spell-cycle
     * gate reads the same state locally; copied on death because knowledge,
     * unlike equipment, survives death (TC model: PlayerKnowledge persists
     * across respawn, CORE_MECHANICS.md §7.1).
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerKnowledge>> RESEARCH =
            ATTACHMENT_TYPES.register("research",
                    () -> AttachmentType.builder(PlayerKnowledge::new)
                            .serialize(RESEARCH_CODEC)
                            .sync(ByteBufCodecs.fromCodec(RESEARCH_CODEC))
                            .copyOnDeath()
                            .build());

    /** See {@link ModBlocks#register(IEventBus)}: must run before RegisterEvents fire. */
    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }

    private ModAttachments() {
    }
}
