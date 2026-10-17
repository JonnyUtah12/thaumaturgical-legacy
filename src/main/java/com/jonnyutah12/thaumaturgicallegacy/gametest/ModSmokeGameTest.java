package com.jonnyutah12.thaumaturgicallegacy.gametest;

import com.jonnyutah12.thaumaturgicallegacy.ThaumaturgicalLegacy;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/**
 * In-game smoke test (TL-010): proves the game boots, the mod loads, and its
 * metadata is readable at runtime. Registered through
 * {@code RegisterGameTestsEvent} only when the gametest system is active, so
 * it never runs in production. Run with {@code ./gradlew runGameTestServer}.
 *
 * <p>{@code @GameTestHolder} namespaces the test to this mod (required for the
 * enabled-namespace filter); the template is the bundled zero-size
 * {@code empty.nbt} structure, unprefixed.</p>
 *
 * <p>Future gameplay behavior (aspects, research, crafting, accessories) gets
 * its gametests next to the feature they verify — see docs/TESTING.md.</p>
 */
@GameTestHolder(ThaumaturgicalLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ModSmokeGameTest {

    private ModSmokeGameTest() {
    }

    @GameTest(template = "empty")
    public static void modLoadsInGame(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(ThaumaturgicalLegacy.MODID),
                "mod " + ThaumaturgicalLegacy.MODID + " is not loaded in the game test server");

        Optional<? extends ModContainer> container = ModList.get().getModContainerById(ThaumaturgicalLegacy.MODID);
        helper.assertTrue(container.isPresent(), "no mod container for " + ThaumaturgicalLegacy.MODID);
        helper.assertTrue(!container.get().getModInfo().getVersion().toString().isEmpty(),
                "mod version metadata is empty at runtime");

        helper.succeed();
    }
}
