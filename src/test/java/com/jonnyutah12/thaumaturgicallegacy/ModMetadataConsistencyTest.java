package com.jonnyutah12.thaumaturgicallegacy;

import org.junit.jupiter.api.Test;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the build/metadata wiring that FML depends on at load time (TL-010).
 *
 * <p>Everything asserted here is derived from gradle.properties via system
 * properties injected by the {@code test} task — nothing is hardcoded, so the
 * single-source-of-truth rule in docs/VERSIONING.md holds and these tests keep
 * guarding after version bumps.</p>
 */
class ModMetadataConsistencyTest {

    private static String property(String name) {
        String value = System.getProperty(name);
        assertNotNull(value,
                "system property '" + name + "' is not set — run tests through ./gradlew test, not a bare runner");
        return value;
    }

    private static TomlParseResult generatedMetadata() throws IOException {
        Path file = Path.of(property("tl.generatedModsToml"));
        assertTrue(Files.isRegularFile(file),
                "generated mods.toml not found at " + file + " — generateModMetadata must run before tests");
        TomlParseResult result = Toml.parse(file);
        assertFalse(result.hasErrors(),
                "generated mods.toml is not valid TOML: " + result.errors());
        return result;
    }

    @Test
    void generatedModsTomlDeclaresTheModWithBuildWiredValues() throws IOException {
        TomlParseResult toml = generatedMetadata();

        assertEquals("javafml", toml.getString("modLoader"), "modLoader must be javafml for @Mod mods");

        TomlArray mods = toml.getArray("mods");
        assertNotNull(mods, "mods.toml must contain a [[mods]] entry");
        assertEquals(1, mods.size(), "exactly one [[mods]] entry is expected for this single-mod project");
        TomlTable mod = mods.getTable(0);

        String modId = property("tl.modId");
        assertEquals(modId, mod.getString("modId"), "modId must match gradle.properties mod_id");
        assertEquals(property("tl.modVersion"), mod.getString("version"),
                "version must come from gradle.properties mod_version (docs/VERSIONING.md)");
        assertEquals(property("tl.modName"), mod.getString("displayName"),
                "displayName must match gradle.properties mod_name");
        // license is top-level mandatory metadata, not part of [[mods]]
        assertEquals(property("tl.modLicense"), toml.getString("license"),
                "license must match gradle.properties mod_license");
    }

    @Test
    void modIdConstantMatchesTheDeclaredModId() throws IOException {
        String source = Files.readString(Path.of(property("tl.modSourceFile")));
        Matcher matcher = Pattern.compile("MODID\\s*=\\s*\"([^\"]+)\"").matcher(source);
        assertTrue(matcher.find(),
                "no String MODID constant found in the mod entry point — @Mod requires it");
        assertEquals(property("tl.modId"), matcher.group(1),
                "the MODID constant must equal gradle.properties mod_id or FML will not bind the mod");
    }

    @Test
    void requiredPlatformDependenciesAreDeclared() throws IOException {
        TomlParseResult toml = generatedMetadata();
        String modId = property("tl.modId");

        TomlArray dependencies = toml.getArray("dependencies." + modId);
        assertNotNull(dependencies,
                "dependencies." + modId + " section is missing — the mod must declare its platform deps");

        TomlTable neoforge = findDependency(dependencies, "neoforge");
        assertEquals("required", neoforge.getString("type"), "neoforge must be a required dependency");
        assertEquals("[" + property("tl.neoVersion") + ",)", neoforge.getString("versionRange"),
                "neoforge range must be derived from gradle.properties neo_version");

        TomlTable minecraft = findDependency(dependencies, "minecraft");
        assertEquals("required", minecraft.getString("type"), "minecraft must be a required dependency");
        assertTrue(minecraft.getString("versionRange").contains(property("tl.minecraftVersion")),
                "minecraft range must cover the pinned minecraft_version");
    }

    private static TomlTable findDependency(TomlArray dependencies, String dependencyModId) {
        for (int i = 0; i < dependencies.size(); i++) {
            TomlTable dependency = dependencies.getTable(i);
            if (dependencyModId.equals(dependency.getString("modId"))) {
                return dependency;
            }
        }
        fail("no dependency entry declared for " + dependencyModId);
        return null; // unreachable
    }
}
