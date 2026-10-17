# Release Checklist — TL-026

Operational checklist for cutting a release. It is the executable form of
`docs/VERSIONING.md` (release process) and matches the actual tooling: the
gradle tasks in `build.gradle`, the CI steps in `.github/workflows/build.yml`,
and the channel rules in `docs/DISTRIBUTION.md`. Run the steps in order; each
must pass before the next.

For an ATM10-relevant release, also run `docs/ATM10_RELEASE_CHECKLIST.md`
after step 9.

## Prerequisites

- [ ] `main` is checked out, working tree clean (`git status --porcelain` empty).
- [ ] JDK 21 active (`java -version` → 21.x; same toolchain CI uses).
- [ ] CI green on the current `main` head: the `Build` workflow's three steps —
      `./gradlew build`, `./gradlew runGameTestServer`, and the
      `grep -q "required tests passed" run/logs/latest.log` assertion — all
      passed. A red CI means do not release (TESTING.md).
- [ ] `CHANGELOG.md` `[Unreleased]` contains every user-visible change since
      the last release (VERSIONING.md: entries land with their commits).

## 1. Version bump

- [ ] In `gradle.properties`, set `mod_version=X.Y.Z` (drop `-SNAPSHOT`).
      This is the single source of truth — do not touch any other file for
      the version (VERSIONING.md).

## 2. Changelog finalize

- [ ] Rename `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD`.
- [ ] Add a fresh empty `[Unreleased]` above it.
- [ ] Add the link definition
      `[X.Y.Z]: https://github.com/JonnyUtah12/thaumaturgical-legacy/compare/v<prev>...vX.Y.Z`
      (first release compares from the initial commit).

## 3. Tests (local, same commands as CI)

- [ ] `./gradlew test` — unit layer green.
- [ ] `./gradlew runGameTestServer` — then verify the summary line yourself,
      exactly as CI does:
      `grep -q "required tests passed" run/logs/latest.log && echo OK`.
      Exit code 0 alone is not proof (TESTING.md triage section).

## 4. Clean build

- [ ] `./gradlew clean build` — compiles, runs unit tests, produces
      `build/libs/thaumaturgicallegacy-X.Y.Z.jar`.

## 5. JAR inspection

- [ ] `unzip -l build/libs/thaumaturgicallegacy-X.Y.Z.jar` — expected:
      mod classes, `META-INF/neoforge.mods.toml`, project assets/data. No
      `.bbmodel` files (excluded by `build.gradle`), no Thaumcraft/Baubles
      content (ASSET_PROVENANCE.md exclusions).
- [ ] `unzip -p …/thaumaturgicallegacy-X.Y.Z.jar META-INF/neoforge.mods.toml`
      — every `${...}` token expanded: `version="X.Y.Z"`, `mod_id`,
      `license="MIT"`, loader ranges present. A leftover `${` means
      `generateModMetadata` did not run — stop and rebuild.
- [ ] Record the checksum: `sha256sum build/libs/thaumaturgicallegacy-X.Y.Z.jar`
      (VERSIONING.md step 5; it goes into the release notes).

## 6. License review

- [ ] `LICENSE` (MIT) present at repo root; `mod_license=MIT` in
      `gradle.properties` matches it.
- [ ] `docs/ASSET_PROVENANCE.md` register covers every asset shipped in the
      JAR (new textures/models/structures added since the last release are
      registered).
- [ ] No original Thaumcraft/Baubles jar or asset anywhere in the artifact.

## 7. Commit and tag

- [ ] Commit the version + changelog changes as `chore(release): vX.Y.Z`.
- [ ] Create an annotated tag: `git tag -a vX.Y.Z -m "vX.Y.Z"`.
      Published tags are never moved or deleted (VERSIONING.md).

## 8. Push and CI on the tag

- [ ] `git push origin main && git push origin vX.Y.Z`.
- [ ] Wait for the `Build` workflow green **on the tag commit** before
      publishing anything.

## 9. Publish the GitHub Release

- [ ] `gh release create vX.Y.Z build/libs/thaumaturgicallegacy-X.Y.Z.jar \
      --title "Thaumaturgical Legacy vX.Y.Z" --notes-file <notes>`
- [ ] Notes = the `CHANGELOG.md` section for `X.Y.Z`, plus the exact verified
      MC/NeoForge/Java versions and the JAR's SHA-256 (DISTRIBUTION.md rules).
- [ ] Verify the asset is attached: `gh release view vX.Y.Z` lists the jar.

## 10. Post-release SNAPSHOT bump

- [ ] In `gradle.properties`, set `mod_version` to the next planned release
      with `-SNAPSHOT` (e.g. `0.2.0-SNAPSHOT` after `0.1.0`).
- [ ] Commit (`chore: bump version to X.Y.Z-SNAPSHOT`) and push.

## Rollback rule

A bad release gets a new version, never a re-tag or an edited published
release (VERSIONING.md). If the mistake is caught before step 9, delete the
unpushed tag and redo from the failing step.
