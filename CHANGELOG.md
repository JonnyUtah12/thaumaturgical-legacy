# Changelog

All notable changes to Thaumaturgical Legacy are documented here.
The format follows Keep a Changelog; this project adheres to Semantic Versioning.
Versions below refer to the **project** version (Minecraft 1.21.1 / NeoForge 21.1.259 are separate).
Versioning rules, the SNAPSHOT/tagging convention, and the release process are
specified in [`docs/VERSIONING.md`](docs/VERSIONING.md).

## [Unreleased]

### Added
- Automated testing: JUnit 5 unit harness wired into `./gradlew build` and CI,
  NeoForge gametest smoke test via `runGameTestServer`, testing policy and CI
  integration (`docs/TESTING.md`, TL-010).
- First playable magic system (TL-009): Thaumic Wand with a vis energy
  component and two castable spells (Firebolt, Frost Lance), and the Vis Tap
  recharge block; design record in `docs/architecture/MAGIC_SYSTEM_V0_1.md`.
- ATM10 compatibility study: pack 8.3 pinned (MC 1.21.1 / NeoForge 21.1.259 /
  Java 21, exact match to our toolchain), Curios 9.5.1 confirmed as the pack's
  accessory framework, overlap and conflict analysis (`docs/ATM10_COMPATIBILITY.md`, TL-021).
- Versioning and changelog management: version scheme, SNAPSHOT/tagging
  convention, changelog rules, release process (`docs/VERSIONING.md`, TL-011).
- Project infrastructure: git repository, MIT license, changelog, README (TL-001..TL-006).
- Environment audit with verified artifact/toolchain facts (`docs/ENVIRONMENT_AUDIT.md`, TL-001).
- REA + decompilation workflow with capability matrix (`docs/reverse-engineering/REA_WORKFLOW.md`, TL-002/TL-005).
- Thaumcraft 4.2.3.5 artifact inventory: API surface, content catalog, subsystem matrix (TL-003).
- Baubles 1.0.1.10 artifact inventory: API contract, 4-slot model, persistence/sync (TL-004).
- Reverse-engineering policy and asset provenance register; license findings (TL-006).

### Changed
- Nothing yet.

### Fixed
- In-game mod load crash: deferred holder classes (`ModBlocks`, `ModItems`)
  were never initialized before RegisterEvents fired, so their entries were
  added too late — the block-entity supplier forced `ModBlocks` class-init
  during the `BLOCK_ENTITY_TYPES` event, after the `BLOCKS` event had already
  fired (`IllegalStateException: Cannot register new entries to
  DeferredRegister after RegisterEvent has been fired`). Registration is now
  explicit and dependency-ordered from the mod constructor; the shared
  block/item DeferredRegisters moved into their holder classes, and the
  wand's default components are now built inside the item factory because
  data-component holders are unbound until their RegisterEvent fires
  (TL-009 follow-up, found by the TL-010 gametest harness).
