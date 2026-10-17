package com.jonnyutah12.thaumaturgicallegacy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Load-safety guard for the optional Curios integration (TL-019,
 * docs/architecture/ACCESSORY_SYSTEM.md): the core mod must load and run
 * without Curios, which holds exactly when no class outside the bridge is
 * ever loaded in a no-Curios runtime. This test enforces that mechanically
 * at the bytecode level: compiled main classes outside the bridge package
 * (and the Curios-only game test, which is registered only when Curios is
 * present) must not reference {@code top.theillusivec4.curios} at all.
 *
 * <p>The in-game half of the same guarantee — the server actually booting
 * without Curios — is the {@code -Ptl.noCurios} game-test run
 * (docs/TESTING.md). This unit layer catches regressions before a game
 * test ever boots: e.g. someone importing a Curios type into an item or
 * provider class.</p>
 */
class OptionalIntegrationIsolationTest {

    /** Internal-name fragment of every Curios class ("top/theillusivec4/curios/..."). */
    private static final String CURIOS_INTERNAL = "top/theillusivec4/curios";

    /**
     * The only classes allowed to reference Curios:
     * <ul>
     * <li>the bridge package — loaded only inside the
     * {@code ModList.isLoaded("curios")} branch;</li>
     * <li>the Curios-path game test — registered only when Curios is
     * present (ThaumaturgicalLegacy#registerGameTests).</li>
     * </ul>
     */
    private static final List<String> ALLOWED_PREFIXES = List.of(
            "com/jonnyutah12/thaumaturgicallegacy/accessory/curios/",
            "com/jonnyutah12/thaumaturgicallegacy/gametest/AccessoryCuriosGameTest");

    private static String property(String name) {
        String value = System.getProperty(name);
        assertNotNull(value,
                "system property '" + name + "' is not set — run tests through ./gradlew test, not a bare runner");
        return value;
    }

    @Test
    void noCoreClassReferencesCuriosOutsideTheBridge() throws IOException {
        Path root = Path.of(property("tl.mainClassesDir"));
        assertTrue(Files.isDirectory(root),
                "compiled main classes not found at " + root + " — ./gradlew test must run after compilation");

        // Non-vacuity: the bridge package must exist, or this guard proves nothing.
        Path bridgeDir = root.resolve("com/jonnyutah12/thaumaturgicallegacy/accessory/curios");
        try (Stream<Path> bridge = Files.list(bridgeDir)) {
            assertTrue(bridge.anyMatch(p -> p.toString().endsWith(".class")),
                    "the Curios bridge package is missing — the optional integration was removed or renamed");
        }

        try (Stream<Path> classes = Files.walk(root)) {
            List<Path> offenders = classes
                    .filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> !isAllowed(root.relativize(p).toString().replace('\\', '/')))
                    .filter(OptionalIntegrationIsolationTest::referencesCurios)
                    .toList();
            assertTrue(offenders.isEmpty(),
                    "classes outside the Curios bridge reference Curios types and would break no-Curios loading: "
                            + offenders);
        }
    }

    @Test
    void theBridgeItselfDoesReferenceCurios() throws IOException {
        // Guards the guard: if the bridge stopped using Curios at all, the
        // isolation test above would pass vacuously forever.
        Path bridge = Path.of(property("tl.mainClassesDir"))
                .resolve("com/jonnyutah12/thaumaturgicallegacy/accessory/curios/CuriosBridge.class");
        assertTrue(Files.isRegularFile(bridge), "CuriosBridge.class not found at " + bridge);
        assertTrue(referencesCurios(bridge), "CuriosBridge must be the class that touches Curios");
    }

    private static boolean isAllowed(String relativePath) {
        for (String prefix : ALLOWED_PREFIXES) {
            if (relativePath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean referencesCurios(Path classFile) {
        try {
            String content = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
            return content.contains(CURIOS_INTERNAL);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void theFallbackProviderIsNamedForTestPairing() throws IOException {
        // The two game tests pair on provider ids ("curios" / "fallback");
        // keep the fallback constant readable from the game test source.
        Path fallback = Path.of(property("tl.mainClassesDir"))
                .resolve("com/jonnyutah12/thaumaturgicallegacy/accessory/fallback/FallbackAccessoryProvider.class");
        assertTrue(Files.isRegularFile(fallback), "FallbackAccessoryProvider.class not found");
        String content = new String(Files.readAllBytes(fallback), StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("fallback"), "the fallback provider must carry its stable id");
        assertFalse(content.contains(CURIOS_INTERNAL), "the fallback must stay Curios-free");
    }
}
