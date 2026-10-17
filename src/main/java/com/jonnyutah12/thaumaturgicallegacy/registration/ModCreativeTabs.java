package com.jonnyutah12.thaumaturgicallegacy.registration;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A small dedicated creative tab so the first system's items are easy to find. */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumaturgicalLegacy.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THAUMATURGY =
            CREATIVE_TABS.register("thaumaturgy", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thaumaturgicallegacy.thaumaturgy"))
                    .icon(() -> new ItemStack(ModItems.THAUMIC_WAND.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.THAUMIC_WAND.get());
                        output.accept(ModItems.VIS_TAP_ITEM.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
