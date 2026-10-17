package com.jonnyutah12.thaumaturgicallegacy.registration;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
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

    /** See {@link ModBlocks#register(IEventBus)}: must run before RegisterEvents fire. */
    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }

    private ModAttachments() {
    }
}
