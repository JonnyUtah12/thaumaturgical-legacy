# ATM10 Release Checklist — TL-026

Additive checklist for releases intended to load in **All the Mods 10 (ATM10)**.
Run `docs/RELEASE_CHECKLIST.md` first; everything here happens *after* the
candidate JAR exists (step 5) and gates step 9 (publish) for pack-relevant
releases.

Check categories follow the project spec §18 (instance startup, gameplay,
integration, performance, world safety; task numbering per `project/KANBAN.md`).
There is **no automated ATM10 harness in this repository** — these are manual
instance checks against the pinned pack. Facts about the pack come from
`docs/ATM10_COMPATIBILITY.md` (TL-021): ATM10 **8.3**, MC 1.21.1, NeoForge
21.1.259, Java 21 — an exact match to our toolchain.

## Pinning rule

- [ ] Test against ATM10 **8.3** (the pinned pack version). ATM10 ships a new
      minor roughly every two weeks; if the pack has moved since the last
      check, re-run the TL-021 compatibility verification (loader version can
      move within 21.1.x) **before** this checklist, and update
      `docs/ATM10_COMPATIBILITY.md`.
- [ ] Use the exact release JAR from the release checklist (same SHA-256),
      not a SNAPSHOT build.

## 1. Instance startup

- [ ] Create a clean ATM10 8.3 instance (CurseForge launcher, or the pack zip
      + `ServerFiles-8.3.zip` for a server test) on Java 21.
- [ ] Copy `thaumaturgicallegacy-X.Y.Z.jar` into the instance's `mods/`
      (465 jars ship in-pack; ours is the only addition).
- [ ] Client launches to the main menu with no mod-loading crash; server
      starts to `Done`.
- [ ] `logs/latest.log`: no errors/warnings mentioning
      `thaumaturgicallegacy`; the mod appears in the loaded-mod list with the
      correct version.

## 2. Gameplay

- [ ] New creative world: our items are obtainable from creative tabs.
- [ ] Core loop works in-pack: craft the Thaumic Wand, cast each registered
      spell, confirm vis costs and feedback match standalone behavior.
- [ ] Vis Tap (or current recharge block) recharges the wand in the ATM10
      environment.
- [ ] JEI (19.64.x, in-pack) shows our items and any registered recipe
      categories.

## 3. Integration

- [ ] No id collisions: no `thaum*` mod in-pack (verified TL-021); confirm
      the loader reports no duplicate registrations at startup.
- [ ] Accessory framework coexistence: once accessories land, our Curios
      (9.5.1, in-pack) slot types register alongside Arcanist Gear and
      heart canisters without displacing them (ATM10_COMPATIBILITY.md
      conflict 2).
- [ ] No recipe conflicts: our crafting recipes don't shadow or get shadowed
      by in-pack recipes (check with JEI recipe view).
- [ ] Magic-system overlap is behavioral, not technical: Ars Nouveau /
      Forbidden Arcanus coexist; our wand does not trigger their item-use
      handlers (and vice versa).

## 4. Performance

- [ ] Baseline first: record client FPS and server tick time on the unmodified
      8.3 instance, then with our JAR added, in the same world/position.
      No meaningful regression (our mod is idle-cost only until used).
- [ ] Server: no tick-time warnings attributable to our mod id in the log.
- [ ] Known pack issue, not ours: with ImmediatelyFast `hud_batching`
      enabled, some items render invisible (seen with Draconic Evolution in
      8.3). If **our** items render invisible, check that config first
      (ATM10_COMPATIBILITY.md conflict 4) before filing it as our bug.

## 5. World safety

- [ ] Save, quit, reload the test world: our blocks/items and their data
      components (wand vis storage) survive the round-trip intact.
- [ ] Dedicated server: stop and restart it; world loads with our mod, no
      chunk-load errors mentioning our mod id.
- [ ] Uninstall safety: removing our JAR from a world that used our content
      must not corrupt the save (unknown block/item handling only).
- [ ] Worldgen: v0.1 ships none; when vis nodes land (0.2 scope), add a
      chunk-generation check here and re-verify node placement against
      ATM10's worldgen (sodium/rubidium-class mods present).

## Recording the result

- [ ] Record the run in the release notes for pack-relevant releases:
      "verified in ATM10 8.3 (MC 1.21.1 / NeoForge 21.1.259 / Java 21)" —
      exact versions only (VERSIONING.md claim rule).
- [ ] Any conflict found goes into `docs/ATM10_COMPATIBILITY.md`
      (Potential conflicts section) with evidence, before the next release.
