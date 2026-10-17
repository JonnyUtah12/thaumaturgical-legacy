# ATM10 Compatibility — TL-021

Date: 2026-10-16 (EDT). All version facts below were verified against the pack's
own published artifacts (downloaded and parsed in this session), not from memory.

## Selected release

**All the Mods 10 (ATM10) version 8.3** — the latest release as of 2026-10-16.

| Fact | Value | Source |
| --- | --- | --- |
| Pack | All the Mods 10 - ATM10, by ATMTeam | CurseForge project 925200 (`https://www.curseforge.com/minecraft/modpacks/all-the-mods-10`) |
| Pack version | **8.3** (release type "R") | `manifest.json` inside `All the Mods 10-8.3.zip` (`"name": "All the Mods 10", "version": "8.3"`); CurseForge file ID 9190157, uploaded 2026-10-15 |
| Release cadence | 8.0 (2026-08-14) → 8.1 (08-29) → 8.2 (09-22) → 8.3 (10-15); 146 released files total | CurseForge files listing |
| Config repo | `github.com/AllTheMods/ATM-10` (default branch `main`, last push 2026-10-15) | GitHub API |

## Platform versions (verified)

| Component | ATM10 8.3 | Our project | Match |
| --- | --- | --- | --- |
| Minecraft | **1.21.1** | 1.21.1 (`gradle.properties`) | exact |
| Mod loader | **NeoForge 21.1.259** (`manifest.json` → `modLoaders: [{id: "neoforge-21.1.259", primary: true}]`; 8.3 release notes state "Neoforge Version is 21.1.259") | 21.1.259 | exact |
| Java | **Java 21** — the pack's own `startserver.sh` aborts unless `java -fullversion` major ≥ 21 ("Minecraft 1.21 requires Java 21") | JDK 21 (Temurin, `~/opt/jdk21`) | exact |

Strongest corroboration: the ServerFiles archive bundles
`neoforge-21.1.259-installer.jar` whose SHA-256
(`bb971287ab44fc43f27da77ac34e263a3885d73fb842ceeb20ee5c517ff4378c`) is
**byte-identical to the installer we audited in TL-001**
(`/home/aaron/minecraft mods/neoforge-21.1.259-installer.jar`). A mod built
against our exact toolchain loads in ATM10 8.3 with zero loader friction.

## Pack contents (evidence)

- `All the Mods 10-8.3.zip` manifest: **499 mod file entries** (projectID/fileID).
- `ServerFiles-8.3.zip` (CurseForge file 9190182): **465 jars** in `mods/`
  (client-only UI mods such as FancyMenu/PackMenu/SimpleMenu have configs in
  `overrides/` but are excluded from server files). Full jar list captured at
  `~/.hermes/cache/scratch/atm10_jars.txt`.

### Accessory / magic-system mods present

**Accessory framework: Curios API `9.5.1+1.21.1` — the only one in the pack.**

- `curios-neoforge-9.5.1+1.21.1.jar`, `overrides/config/curios-client.toml`.
- Curios consumers in-pack: `baubley-heart-canisters-1.21.1-1.4.3`,
  `refinedstorage-curios-integration-1.0.0`, `allthearcanistgear-1.21.1-21.3.2`
  (gear explicitly built on Curios), one KubeJS script (`Adorn/Tags.js`).
- **No Baubles** (1.7.10-only, as expected) and **no Accessories mod jar** —
  an `overrides/config/accessories.json5` exists but no corresponding jar ships
  in the runtime set; treat it as a leftover config, not a second framework.
- Adjacent but separate: `ToolBelt-1.21.1-2.2.10` (own belt slots),
  `cosmeticarmorreworked-1.21.1-v1` (cosmetic armor slots).

**Magic mods in 8.3** (jar versions from ServerFiles):

| Mod | Version | Notes |
| --- | --- | --- |
| Ars Nouveau (+9 addons: additions, controle, creo, elemancy, elemental, occultas, polymorphia, technica, unification) | 5.13.3 | dominant magic system: wands, spells, runic crafting, summoning; Patchouli lexicon |
| Astral Sorcery | 2.0.2.39 | **added in 8.3**; star-channeling, attunement, rituals |
| Forbidden Arcanus | 2.6.1 | runic magic, altars, enderpower — closest thematic neighbor to Thaumcraft |
| Apotheosis + Apothic Attributes | 8.9.0 / 2.11.0 | enchanting, runewords, item affixes |
| Aether (+ Giselle addon) | 1.5.10 | dimension |
| Twilight Forest | 4.8.3345 | dimension/dungeons |
| Ice and Fire | 2.0-beta.17 | dragons/creatures |
| Reliquary | 2.0.80.1570 | wands/items (classic TC-adjacent flavor) |
| Mahoutsukai | 1.36.28 | alchemy/mirror magic |
| All The Arcanist Gear / All the Wizard Gear | 21.3.2 / 1.2.0 | Curios-based magic gear |
| Cataclysm, Tombstone, Yungs Better Witch Huts | 26.x / 8.8.x / 4.1.1 | dungeon/curio/structure flavor |

**Documentation & recipe UI**: Patchouli `1.21.1-93`, JEI
`19.64.0.454`, Jade `15.10.6`, EnchDesc `21.1.11`, FTB Quests `2101.1.36`,
FTB JEI Extras. No EMI/Sable.

**Shared libraries**: GeckoLib `4.9.3`, Architectury `13.0.11`, Cloth Config
`15.0.140`, Balm `21.0.66`, Bookshelf `21.1.81`, Fusion `1.3.16`, Kotlin for
Forge `5.12.0`, AttributeFix `21.1.3`, ImmediatelyFast `1.6.15`, FTB suite,
SuperMartijn core/config libs. ATM in-house: Allthemodium `3.2.1`, All The
Tweaks `2.12.1`, **ATM: Compat `1.0.0` (new in 8.3)**.

## Dependency overlap vs our planned dependencies

| Planned dependency | In ATM10 8.3? | Action for us |
| --- | --- | --- |
| NeoForge 21.1.259 / MC 1.21.1 / Java 21 | yes, exact | none — our `gradle.properties` already matches the pack |
| **Curios API** (Baubles replacement for accessory slots) | **yes, 9.5.1** | develop against Curios 9.5.x API as `compileOnly`/`localRuntime` (do not bundle); register our ring/amulet/belt equivalents as Curios slot types so pack gear (Arcanist Gear, heart canisters) and our slots coexist |
| Patchouli (research book, optional) | yes, 93 | if used, target Patchouli 93 for 1.21.1; multiple books coexist by design |
| JEI (recipe display, optional) | yes, 19.64.0.454 | plugin API stable; register crucible/infusion recipe categories against JEI 19.x |
| GeckoLib (animated entities, optional) | yes, 4.9.3 | safe to use; pin ≤4.9.3 |
| Baubles | no (1.7.10 only) | expected — clean-room design replaces it with Curios (see `reverse-engineering/BAUBLES_INVENTORY.md`) |
| Mod id collision (`thaumaturgicallegacy`) | none; no `thaum*` jar in pack | clear |

## Potential conflicts

1. **Functional overlap with existing magic mods.** Ars Nouveau (wands/spells/
   runic table), Forbidden Arcanus (runes/altars), Astral Sorcery
   (attunement/ritual progression), Apotheosis (affixes/runewords) already
   occupy much of the magic design space. Our differentiators are the
   Thaumcraft-derived core: vis/vis nodes, wand with aspect foci, crucible
   aspect crafting, and the research-gated knowledge loop — none of these
   exist in-pack. Keep UI patterns distinct (own lexicon style if not
   Patchouli; avoid "wand right-click spell" collision with Ars).
2. **Accessory slot crowding.** Curios is shared; register dedicated slot
   types with clear names rather than reusing generic `ring`/`belt` slots
   already used by Arcanist Gear and heart canisters.
3. **Energy/mana interop.** No vis-like world energy exists in-pack; Ars mana
   is internal, FE is the tech-currency (Mekanism/IE/etc.). If we later
   expose vis as a capability, keep it separate from FE by default.
4. **Renderer interactions.** 8.3 has a known issue where Draconic Evolution
   items render invisible with ImmediatelyFast `hud_batching` enabled — if our
   items render invisible in ATM10, check that config first. GeckoLib-based
   renders are fine (4.9.3 ships in-pack).
5. **Pack churn.** ATM10 ships a new minor every ~2 weeks (8.0→8.3 in 9
   weeks). Pin development to **8.3**; re-run the compatibility check on each
   pack minor (loader version can move within 21.1.x).
6. **Pack-level known issues (not ours):** Ars Lectern interaction crash
   (break/re-place workaround); Draconic invisible items (see 4).

## Verification record

- Downloaded and parsed `All the Mods 10-8.3.zip` (200,037,954 B) from
  `edge.forgecdn.net/files/9190/157/` — `manifest.json` quoted above.
- Downloaded `ServerFiles-8.3.zip` (1,239,095,516 B) from
  `edge.forgecdn.net/files/9190/182/` — jar list extracted to
  `atm10_jars.txt` (465 entries); bundled installer SHA-256 compared against
  the TL-001 artifact (identical).
- 8.3 release notes (CurseForge file page) confirm NeoForge 21.1.259 and the
  8.3 mod additions (Astral Sorcery, ATM: Compat, Ad Astra).
- `startserver.sh` inside ServerFiles enforces Java ≥ 21.
- GitHub `AllTheMods/ATM-10` inspected via API (config/kubejs repo; changelog
  headers only — release notes on CurseForge are the substantive changelog).
