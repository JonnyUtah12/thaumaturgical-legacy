# Distribution Channels — TL-026

Status: authoritative for where Thaumaturgical Legacy artifacts are published.
Companion to `docs/VERSIONING.md` (release process) and `docs/RELEASE_CHECKLIST.md`
(the operational steps).

## Channel status

| Channel | Status | Notes |
| --- | --- | --- |
| GitHub Releases | **active** — the only published channel | `github.com/JonnyUtah12/thaumaturgical-legacy`, releases created per VERSIONING.md step 6 |
| CurseForge | not used — nothing claimed | no project, no upload, no listing; see requirements below |
| Modrinth | not used — nothing claimed | no project, no upload, no listing; see requirements below |
| Maven repository | local only | `maven-publish` in `build.gradle` publishes to `file://$projectDir/repo` via `./gradlew publish`; a dev convenience, not a public channel |

No README, doc, or release note claims a CurseForge or Modrinth presence. Do
not add such a claim until a project actually exists there.

## GitHub Releases (active)

- Artifact: `build/libs/thaumaturgicallegacy-<version>.jar`, produced by
  `./gradlew build` (`base.archivesName = mod_id`, `version = mod_version`).
- A release exists only for a tagged, non-SNAPSHOT version (`vX.Y.Z`), and
  only after CI on the tag is green (see `docs/RELEASE_CHECKLIST.md`).
- Release notes are the `CHANGELOG.md` section for that version and must name
  the exact verified Minecraft + NeoForge versions (VERSIONING.md: never an
  untested range).
- The JAR is attached to the release with its SHA-256 checksum stated
  (`sha256sum build/libs/thaumaturgicallegacy-<version>.jar`).
- v0.1.0 (2026-10-17) is the reference example: tag `v0.1.0`, asset
  `thaumaturgicallegacy-0.1.0.jar`, notes pinned to MC 1.21.1 / NeoForge
  21.1.259 / Java 21.

## CurseForge (not claimed — what adding it would require)

Nothing in the repository blocks a future CurseForge listing; nothing supports
one today. To add it we would need, at minimum:

1. A CurseForge project under the author's account for mod id
   `thaumaturgicallegacy` (name, description, artwork — all original, per
   `docs/ASSET_PROVENANCE.md`).
2. Per-file upload metadata: game version `1.21.1`, mod loader `NeoForge`,
   the exact `21.1.259` range we test against, license shown as MIT
   (`mod_license` in `gradle.properties`).
3. A release-process addition: upload the same tagged JAR (same SHA-256) to
   CurseForge after the GitHub Release, and keep both listings in sync. This
   could be manual (`gh`-style step in the checklist) or automated with a
   CurseForge Gradle plugin — **no such plugin is in `build.gradle` today**;
   adding one is a deliberate build change, not part of this document.
4. A decision on where "latest release" is canonical; until then GitHub
   Releases stays canonical and other listings are mirrors.

## Modrinth (not claimed — what adding it would require)

Same posture as CurseForge:

1. A Modrinth project for `thaumaturgicallegacy` with original branding.
2. Upload metadata: game version `1.21.1`, loader `NeoForge`, license MIT.
3. The same tagged, checksummed JAR as GitHub Releases; a Modrinth deploy
   plugin is **not** configured in `build.gradle`.
4. Any dependency declarations (e.g. Curios as an optional dependency, once
   accessories land) must match what the JAR actually requires at runtime —
   the v0.1 core has no external mod dependencies.

## Rules that apply to every channel

- Only non-SNAPSHOT, tagged versions are published anywhere (VERSIONING.md).
- Compatibility claims name exact tested versions only.
- Original Thaumcraft/Baubles JARs and assets are never bundled or
  redistributed (ASSET_PROVENANCE.md); the published JAR contains only this
  project's code and registered assets.
- The published JAR must be byte-identical to the one built from the tag
  (same SHA-256 recorded in the release process).
