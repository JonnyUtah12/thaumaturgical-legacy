# ATM10 Integration Proposal — TL-027

Date: 2026-10-17. Audience: ATM10 maintainers (ATMTeam).

> **Status: this is a proposal only. Official inclusion of Thaumaturgical
> Legacy in All the Mods 10 is NOT confirmed.** Nothing in this document
> should be read as a commitment from either side. Every compatibility claim
> below is our own test result against ATM10 **8.3**, not an official ATM10
> statement.

## 1. What the mod is

**Thaumaturgical Legacy** (mod id `thaumaturgicallegacy`, author
JonnyUtah12, source at
<https://github.com/JonnyUtah12/thaumaturgical-legacy>) is an original
magic mod for Minecraft **1.21.1** / **NeoForge 21.1.259**. It is a
clean-room implementation informed by a documented reverse-engineering
study of Thaumcraft 4.2.3.5 and its Baubles 1.7.10 dependency — it is
**not a port** and contains **no code or assets** from Thaumcraft or
Baubles (`docs/REVERSE_ENGINEERING_POLICY.md`).

Current implemented scope (0.2.x): a first playable magic loop —
Thaumic Wand with a vis energy component and two castable spells, a Vis
Tap device that passively gathers ambient vis and recharges the wand,
per-player research progression gated by knowledge-as-ingredient crafting,
and a Vis Amulet accessory occupying a dedicated Curios slot
(`thaumaturgicallegacy.charm`). Loop description and verification:
`docs/PLAYABLE_LOOP.md`.

Why it does not duplicate what ATM10 8.3 already ships: the pack's magic
mods (Ars Nouveau, Forbidden Arcanus, Astral Sorcery, Apotheosis) cover
mana/wand-spell and rune/altar design space, but none offers the
Thaumcraft-derived core — ambient vis as a world resource, a research-
gated knowledge loop, or a knowledge-as-recipe gate. Full overlap and
conflict analysis: `docs/ATM10_COMPATIBILITY.md`.

## 2. Verified compatibility against ATM10 8.3

We ran a startup/gameplay/integration/performance/world-safety battery
(spec §18 categories, `docs/ATM10_RELEASE_CHECKLIST.md`) against a
**freshly installed, isolated ATM10 8.3 instance** (client: 496 pack jars
+ ours; server: 470 + ours). Full evidence, including log citations and
artifact SHA-256s: `docs/ATM10_TESTING.md`. Summary:

| Area | Result |
| --- | --- |
| Dedicated server startup | PASS — 5 lifecycles + re-test; cold `Done (17.0s)`, warm reloads 2.6–3.6s; clean SIGTERM saves every time |
| Client startup | PASS — headless client reached the title screen; our mod's client setup completed |
| Errors attributable to our mod | **Zero** in every run (136–177 server ERROR lines and 47–79 client ERROR lines per run, all pack-side and itemized in the testing doc) |
| Optional Curios integration | PASS — `Accessory provider active: curios` on both sides; the optional path, not the fallback, is what ATM10 exercises |
| Recipes / tags / registries | PASS — 0 recipe errors, 0 duplicate-recipe warnings, 0 tag or registry errors from our mod |
| Performance | PASS — no visible startup or tick cost; no log spam; gameplay listeners are event-driven only (join/craft/place), no per-tick work |
| World safety | PASS — existing worlds reload cleanly; no duplication across restarts; no worldgen of ours to migrate |
| Defects found | One cosmetic defect (missing `vis_tap` item model), **fixed and verified gone** by re-running both sides against the same instance |

Not verified on our test host: a live multiplayer join (the host's 14 GiB
RAM cannot hold the ATM10 client and server simultaneously; the sync
logic is covered by dev game tests on the same artifact instead). Stated
honestly in `docs/ATM10_TESTING.md` §C.

The tested artifact is `thaumaturgicallegacy-0.2.0-SNAPSHOT.jar`
(SHA-256 `406e4887e2667511fe2fcfaa55659f1bac8fa1b03177ab3baf792da377aef157`,
built from `main` at commit `1954d4e`). Per our versioning rules only
tagged releases are published; a tagged `v0.2.0` with identical content
would be cut before any integration (`docs/VERSIONING.md`,
`docs/RELEASE_CHECKLIST.md`).

## 3. Dependency profile

| Kind | Dependency | Notes |
| --- | --- | --- |
| Required | Minecraft 1.21.1, NeoForge ≥ 21.1.259, Java 21 | **exact match** to ATM10 8.3 (`manifest.json` verified in `docs/ATM10_COMPATIBILITY.md`) |
| Optional | Curios API ≥ 9.5.1 | **already in ATM10 8.3** (`curios-neoforge-9.5.1+1.21.1.jar`). Declared `optional` + `ordering=AFTER` in `neoforge.mods.toml`; `compileOnly` in the build — **never bundled**. The core mod loads and runs with Curios absent (verified via `runGameTestServer -Ptl.noCurios`); with Curios present the bridge activates automatically. |
| Bundled libraries | none | the JAR contains only this project's code and assets |

No mod-id collision (no `thaum*` jar in the pack). We register a
dedicated Curios slot type rather than reusing generic ring/belt slots,
to avoid accessory-slot crowding with Arcanist Gear and heart canisters.

## 4. License and asset provenance

- **License: MIT** (`mod_license=MIT`, expanded into the built JAR's
  `META-INF/neoforge.mods.toml`).
- **All assets are original** (`docs/ASSET_PROVENANCE.md`): textures are
  16×16 pixel art generated by project tooling and licensed CC0-1.0;
  lang strings, models, recipes, and research data are authored for this
  project; code is 100% independent implementation. The only third-party
  byte in the JAR is a zero-size vanilla structure NBT copied from the
  Mojang client jar (Minecraft EULA game data). The original
  Thaumcraft/Baubles jars are never bundled or redistributed.
- Honest note for maintainers: current textures are **placeholder-grade**
  (tool-generated), functional but not commissioned art.

## 5. Install instructions

1. Take the tagged release JAR `thaumaturgicallegacy-<version>.jar` from
   GitHub Releases (the only published channel today —
   `docs/DISTRIBUTION.md`; no CurseForge/Modrinth listing is claimed).
2. Place the same JAR in `mods/` on **both** the client and the server.
   The mod is required on both sides; there is no server-only mode.
3. Nothing else: no config files, no data packs, no bundled libraries,
   no load-order constraints beyond the optional `AFTER` curios ordering
   the manifest declares itself.

## 6. Known limitations (honest scope)

- **Early content stage.** One playable loop is implemented (wand, vis
  tap, research notes, amulet). Aspects, crucible/infusion crafting,
  vis nodes, and a research GUI are designed but not yet built;
  progression announcements are chat messages today.
- **Research Note is a consumable:** losing it loses the Vis Tap recipe
  gate (documented with a planned fix in
  `docs/architecture/RESEARCH_SYSTEM_V0_2.md`).
- **Live multiplayer sync** is verified only by dev game tests, not by an
  in-pack join (test-host memory ceiling, §2).
- **Uninstall behavior:** removing the mod silently drops our
  player-attachment data (research knowledge, fallback accessory state).
  There is no world data of ours to migrate (no worldgen).
- **Pack churn:** ATM10 ships a minor roughly every two weeks; our
  testing is pinned to **8.3** and we would re-run the battery on each
  pack minor we claim support for. Compatibility claims always name exact
  tested versions.
- If our items ever render invisible in-pack, the first thing to check is
  the pack's known ImmediatelyFast `hud_batching` issue (already affects
  Draconic Evolution items in 8.3) — not a defect of this mod.

## 7. What is explicitly not claimed

- Official ATM10 inclusion — **not confirmed; this document is only a
  proposal.**
- An official ATM10 compatibility statement — our results are our own,
  from our own isolated instance.
- CurseForge or Modrinth presence — nothing is claimed there
  (`docs/DISTRIBUTION.md`).
- Support for any pack version other than 8.3.

## 8. Suggested next steps (if maintainers are interested)

1. Cut and publish tagged `v0.2.0` (same content as the tested build) so
   the pack can pin a release artifact with a recorded SHA-256.
2. Re-run the §18 battery against the next ATM10 minor before pinning.
3. Complete the live multiplayer join on a larger test host to close the
   one unverified row in §2.
