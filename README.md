# Thaumaturgical Legacy

An original magic mod for Minecraft **1.21.1** / **NeoForge 21.1.259**,
independently implemented from a documented reverse-engineering study of
Thaumcraft 4.2.3.5 and its Baubles 1.7.10 dependency.

- Mod id: `thaumaturgicallegacy` · Package: `com.jonnyutah12.thaumaturgicallegacy`
- Current version: `0.1.0-SNAPSHOT` (see `CHANGELOG.md`)
- Target modpack compatibility: All the Mods 10 (see `docs/ATM10_COMPATIBILITY.md`)

Thaumaturgical Legacy is **not** a port and contains **no** code or assets from
Thaumcraft or Baubles. It is a clean-room implementation based on documented
gameplay behavior — see `docs/REVERSE_ENGINEERING_POLICY.md`.

## Status

Foundation phase: environment audit, artifact analysis, and licensing research
are complete (`docs/`). Implementation of the first playable magic loop is in
progress. Task state lives on the Hermes Kanban board and in `project/KANBAN.md`.

## Documentation map

| Document | Contents |
| --- | --- |
| `docs/ENVIRONMENT_AUDIT.md` | Verified toolchain and artifact facts |
| `docs/reverse-engineering/` | REA workflow, artifact inventories, mechanics analysis |
| `docs/REVERSE_ENGINEERING_POLICY.md` | License findings and clean-room policy |
| `docs/VERSIONING.md` | Version scheme, changelog rules, release process |
| `docs/ASSET_PROVENANCE.md` | Asset origin register |
| `docs/architecture/` | Modern architecture decisions |

## Building

Requires JDK 21. `./gradlew build` produces the mod JAR in `build/libs/`.
Neither original JAR is required to build or run this mod.
