package com.jonnyutah12.thaumaturgicallegacy;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the versioning policy from docs/VERSIONING.md mechanically (TL-010):
 * the version in gradle.properties must be valid SemVer with at most the
 * {@code -SNAPSHOT} pre-release suffix, and the mod id must satisfy FML's
 * naming rules. Values are injected from gradle.properties by the test task,
 * never hardcoded here.
 */
class VersionPolicyTest {

    /** SemVer 2.0.0 core, restricted to the pre-1.0 0.MINOR.PATCH scheme plus the exact -SNAPSHOT suffix. */
    private static final Pattern ALLOWED_VERSION =
            Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(-SNAPSHOT)?$");

    /** Mod id contract from gradle.properties / FML: [a-z][a-z0-9_]{1,63}. */
    private static final Pattern ALLOWED_MOD_ID = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");

    private static String property(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, "system property '" + name + "' is not set — run tests through ./gradlew test");
        return value;
    }

    @Test
    void modVersionFollowsTheProjectVersionScheme() {
        String version = property("tl.modVersion");
        assertTrue(ALLOWED_VERSION.matcher(version).matches(),
                "mod_version '" + version + "' violates docs/VERSIONING.md — expected <MAJOR>.<MINOR>.<PATCH>"
                        + " (pre-1.0: 0.MINOR.PATCH) with an optional -SNAPSHOT suffix");
    }

    @Test
    void modIdSatisfiesLoaderNamingRules() {
        String modId = property("tl.modId");
        assertTrue(ALLOWED_MOD_ID.matcher(modId).matches(),
                "mod_id '" + modId + "' violates the FML mod id pattern [a-z][a-z0-9_]{1,63}");
    }
}
