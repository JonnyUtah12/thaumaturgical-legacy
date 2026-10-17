package com.jonnyutah12.thaumaturgicallegacy.accessory.fallback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;

import java.util.Optional;

/**
 * The fallback provider's single S2C sync payload (design record: "one S2C
 * sync payload on join and on change; server-authoritative; client mirror
 * is display-only"). Never sent while the Curios provider is active —
 * Curios owns its own per-slot sync there.
 */
public record CharmSyncPayload(Optional<ItemStack> charm) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CharmSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(ThaumaturgicalLegacy.MODID, "charm_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharmSyncPayload> STREAM_CODEC =
            ByteBufCodecs.optional(ItemStack.OPTIONAL_STREAM_CODEC)
                    .map(CharmSyncPayload::new, CharmSyncPayload::charm);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
