# Versioning and Changelog Policy

Status: authoritative for all Thaumaturgical Legacy work. Establishes how the
mod is versioned, how `CHANGELOG.md` is maintained, and how a release is cut.
Referenced by `CHANGELOG.md` and followed by the release task (TL-015).

## Version scheme

The mod version follows [Semantic Versioning 2.0.0](https://semver.org/)
(`<MAJOR>.<MINOR>.<PATCH>`).

- **Pre-1.0 (current):** `0.MINOR.PATCH`. Breaking changes to world-visible
  behavior, save data, or the mod's API may occur in MINOR bumps; no stability
  is implied. The first public release is **0.1.0**.
- **1.0 and later:** standard SemVer.
  - MAJOR — breaking change to save data, world-visible behavior, or any
    public API other mods may depend on.
  - MINOR — backward-compatible functionality.
  - PATCH — backward-compatible fixes only.

### Platform versions are separate

Minecraft and NeoForge versions are **not** part of the mod version. They are
pinned in `gradle.properties` (`minecraft_version`, `neo_version`) and change
independently.

- A platform bump (e.g. new Minecraft minor version) with no new features is
  a MINOR bump of the mod version, recorded under **Changed** in the changelog.
- Any compatibility claim (README, modpack listings, release notes) must name
  the exact Minecraft + NeoForge versions it was verified against — never a
  range that was not tested.

## Single source of truth

`gradle.properties` → `mod_version` is the **only** place a version number is
written. Everything else derives from it:

- `build.gradle` sets `version = mod_version` (already wired).
- `src/main/templates/META-INF/neoforge.mods.toml` uses `version="${mod_version}"`
  token replacement (already wired).

Never hardcode a version in source, metadata, docs, or CI. If a file needs the
version, it must read it from `mod_version`.

### SNAPSHOT convention

- Between releases, `mod_version` is the **next planned release** with a
  `-SNAPSHOT` suffix (e.g. `0.1.0-SNAPSHOT`). SNAPSHOT builds are never
  published or announced.
- A release commit removes `-SNAPSHOT`; the commit immediately after the tag
  bumps to the next SNAPSHOT (e.g. `0.2.0-SNAPSHOT`).

## Git tags

- Releases are tagged with **annotated tags** named `v<version>` (e.g.
  `v0.1.0`) pointing at the release commit on `main`.
- One tag per release. Published tags are never moved or deleted; a bad release
  gets a new version, not a re-tag.
- Tags are the anchor for changelog compare links (see below).

## Changelog maintenance

`CHANGELOG.md` at the repo root, English, [Keep a Changelog 1.1.0](https://keepachangelog.com/)
format. Rules:

- Every user-visible or release-relevant change gets an entry in the
  `[Unreleased]` section **in the same commit** that introduces it. Sections:
  Added / Changed / Fixed / Removed / Migration Notes (omit empty ones).
- Entry style: one imperative line ("Add aspect visor bauble slot"), with the
  originating task id (TL-xxx) where one exists.
- Internal-only changes (pure refactors, tooling, CI) need no entry unless
  they change what contributors or pack developers must do.
- Save-data or world-visible behavior changes must state the player-visible
  impact, and any required migration under **Migration Notes**.

## Release process

Followed by the release task (TL-015); each step is verified before the next:

1. Confirm a clean build and passing tests (`./gradlew build`).
2. In `CHANGELOG.md`: rename `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD`, add a
   fresh empty `[Unreleased]` above it, and add a link definition
   `[X.Y.Z]: https://github.com/JonnyUtah12/thaumaturgical-legacy/compare/v<prev>...vX.Y.Z`
   (first release compares from the initial commit).
3. Set `mod_version` to `X.Y.Z` (drop `-SNAPSHOT`).
4. Commit as `chore(release): vX.Y.Z`; create annotated tag `vX.Y.Z`.
5. Build the release JAR, record its SHA-256 checksum.
6. Push `main` and the tag to GitHub; publish a GitHub Release whose notes are
   the changelog section for that version, with the JAR attached and its
   checksum stated.
7. Bump `mod_version` to the next `-SNAPSHOT` and commit; push.

## Scope boundaries

- This policy governs the **mod** version. The documentation set (`docs/`)
  carries no version of its own — it always describes the current `main`.
- ATM10/modpack compatibility is tracked in `docs/ATM10_COMPATIBILITY.md` and
  never implied by the version number alone.
