# Testing Policy

Status: authoritative for all Thaumaturgical Legacy work. Establishes the
automated test layers, how to run them, and the rules for writing new tests
(TL-010). Referenced by the CI workflow and the release process
(`docs/VERSIONING.md` step 1: clean build and passing tests).

## Two layers

| Layer | Where | Runtime | Runs in |
| --- | --- | --- | --- |
| Unit tests | `src/test/java/` | Plain JVM, no game | `./gradlew test` (part of `./gradlew build`, CI) |
| Game tests | `src/main/java/.../gametest/` | Real NeoForge game test server | `./gradlew runGameTestServer` (CI step) |

Unit tests guard build wiring, configuration, and pure logic. The `test`
source set deliberately has **no Minecraft/NeoForge classpath** — if a test
needs game types, it is a game test, not a unit test.

Game tests verify in-world behavior on a real game test server. The mod's
entry point registers gametest classes through `RegisterGameTestsEvent`; the
listener is inert in normal play. The run configurations already set
`neoforge.enabledGameTestNamespaces` to the mod id, so only this mod's
namespaced tests run.

## Commands

```
./gradlew test                # unit tests only (fast)
./gradlew build               # compiles + unit tests (what CI runs first)
./gradlew runGameTestServer   # boots the game test server, runs gametests, exits
./gradlew runGameTestServer -Ptl.noCurios   # same, WITHOUT the optional Curios
                              # dependency on the runtime classpath — the
                              # in-game no-Curios startup check (TL-019 pair)
```

CI (`.github/workflows/build.yml`) runs `build` then `runGameTestServer`.
A red CI means do not release (see the release process in VERSIONING.md).

## Unit test rules

- Never hardcode versions, mod ids, or platform versions in tests. The `test`
  task injects them from `gradle.properties` as `tl.*` system properties
  (`tl.modId`, `tl.modVersion`, `tl.neoVersion`, `tl.minecraftVersion`,
  `tl.curiosVersion`, `tl.modLicense`, `tl.modName`, `tl.generatedModsToml`,
  `tl.modSourceFile`, `tl.mainClassesDir`).
  This keeps the single-source-of-truth rule of VERSIONING.md enforced
  mechanically — the existing guards live in `ModMetadataConsistencyTest`
  and `VersionPolicyTest`.
- Unit tests must not import `net.minecraft.*`, `net.neoforged.*`, or
  `com.mojang.*`. They will not compile (by design).

## Game test rules

- Place gametest classes in `com.jonnyutah12.thaumaturgicallegacy.gametest`
  next to the feature they verify, and register them in
  `ThaumaturgicalLegacy#registerGameTests`.
- Annotate the class with `@GameTestHolder(ThaumaturgicalLegacy.MODID)` —
  without it the test is namespaced to `minecraft` and silently filtered out
  by the enabled-namespace gate. **Exception:** classes that must only
  *exist* when an optional dependency is present (e.g. the Curios-path
  accessory test references Curios types) must NOT carry `@GameTestHolder`:
  annotated holders are auto-discovered regardless of the
  `RegisterGameTestsEvent` gating. Omit the annotation, set
  `templateNamespace = ThaumaturgicalLegacy.MODID` on each `@GameTest`,
  keep `@PrefixGameTestTemplate(false)`, and rely on the conditional
  event registration.
- Structure templates live in `src/main/resources/data/thaumaturgicallegacy/structure/*.nbt`
  and are registered in `docs/ASSET_PROVENANCE.md` like any other asset.
  `@PrefixGameTestTemplate(false)` keeps template names predictable.
- Keep each gametest focused; assert with `GameTestHelper.assertTrue/fail`
  and finish with `helper.succeed()`.

## Current coverage

- `ModMetadataConsistencyTest` — generated `neoforge.mods.toml` matches
  `gradle.properties`, the `MODID` constant matches `mod_id`, required
  platform dependencies are declared with derived ranges, and the optional
  Curios integration is declared `optional` with a range derived from
  `curios_version`.
- `VersionPolicyTest` — `mod_version` satisfies the SemVer + SNAPSHOT scheme
  from VERSIONING.md; `mod_id` satisfies the loader naming rules.
- `accessory/core/VisCostRulesTest` — the vis-discount rule: ceil rounding,
  floor of 1 vis, clamped percentages.
- `OptionalIntegrationIsolationTest` — bytecode guard: no compiled class
  outside the Curios bridge (and the Curios-path game test) references
  `top.theillusivec4.curios`, so no-Curios loading cannot regress silently.
- `ModSmokeGameTest` — the game boots, the mod loads, runtime metadata is
  readable. This is the harness's in-game proof of life; feature gametests
  land with their features.
- `AccessoryCuriosGameTest` / `AccessoryFallbackGameTest` — a
  provider-conditional pair (TL-019): the charm slot + amulet equip +
  discounted-cast loop runs identically under the Curios bridge
  (`runGameTestServer`) and the built-in fallback
  (`runGameTestServer -Ptl.noCurios`); each asserts the other provider is
  inert.

## Triage

- `runGameTestServer` exits 0 even when the server crashes during mod
  loading — no tests ever ran. Always look for
  `All N required tests passed :)` in the output (CI asserts this line from
  `run/logs/latest.log` as a separate step). A mod that passes
  `./gradlew build` can still fail to load in-game; this is what that layer
  catches.
- `No test functions were given!` from the game test server → the test was
  filtered out: missing `@GameTestHolder`, missing registration, or the
  namespace is not enabled.
- Missing-structure errors → template name/namespace mismatch between
  `@GameTest(template=...)` and the file under `data/<ns>/structure/`.
- Unit test fails on version/id values → fix `gradle.properties`, never the
  test.
