# The First Playable Magic Loop (TL-020)

Status: implemented and verified on `main` (project 0.2.0-SNAPSHOT,
Minecraft 1.21.1 / NeoForge 21.1.259). This document states the loop a real
player performs, which code implements each step, and exactly how each step
is verified. Design rationale lives in
[`architecture/MAGIC_SYSTEM_V0_1.md`](architecture/MAGIC_SYSTEM_V0_1.md) and
[`architecture/RESEARCH_SYSTEM_V0_2.md`](architecture/RESEARCH_SYSTEM_V0_2.md).

## The loop

1. **Craft a magical resource.** Amethyst + wood + iron → **Thaumic Wand**
   (ships with a small vis charge). Vanilla shaped recipe
   (`data/thaumaturgicallegacy/recipe/thaumic_wand.json`).
2. **Discover an initial research entry.** On joining the world the player
   automatically learns **First Sight** and gets a hint pointing at the wand.
3. **Gather ingredients / use the tool.** Cast Firebolt (right-click) to
   confirm the wand works; crafting the wand itself reveals **Wandwright**
   and grants one **Research Note: Tapping the Flow**.
4. **Unlock a magical recipe.** The **Vis Tap** recipe requires the Research
   Note — knowledge is a physical ingredient, so the gate is enforced by
   vanilla crafting itself.
5. **Craft a functional device.** Place the Vis Tap; it passively gathers
   ambient vis; right-click the wand on the tap to transfer the buffer into
   the wand's vis component.
6. **Progress further.** Placing the tap reveals **Tapping the Flow**, which
   unlocks **Frost Lance** — the wand now cycles a second, stronger spell.
   Knowledge persists across death and world restarts; the loop can repeat
   per player on a shared server.

## Where each step lives

| Step | Implementation |
| --- | --- |
| Wand, vis component, casting, cooldowns | `items/ThaumicWandItem`, `magic/VisStorage`, `magic/SpellCatalog` |
| Research tree + per-player knowledge | `magic/research/*`, `event/ResearchEvents`, `registration/ModAttachments.RESEARCH` |
| Recipe gate (Research Note) | `registration/ModItems.RESEARCH_NOTE`, `data/thaumaturgicallegacy/recipe/vis_tap.json` |
| Vis Tap device | `block/VisTapBlock`, `blockentity/VisTapBlockEntity` |
| Spell unlock | `ResearchEvents.isSpellKnown` + cycle/cast gates in `ThaumicWandItem` |
| Localization / tooltips / creative tab | `assets/.../lang/en_us.json`, item `appendHoverText`, `registration/ModCreativeTabs` |

## How it is verified (all commands run 2026-10-17, all green)

| Requirement | Evidence |
| --- | --- |
| Steps 1–5 end-to-end in-game (tap accumulates, charging transfers, cast decrements exactly the cost) | `MagicSystemGameTest.tapAccumulatesChargesWandAndCastDecrementsVis` via `./gradlew runGameTestServer` — **passed** |
| Step 2/3/6: triggers, prerequisites, note reward, spell unlock, idempotence | `ResearchGameTest.researchChainGatesSpellsAndGrantsNote` — **passed** |
| Progression persists across world restart | NBT round-trip through the exact attachment save codec (`RESEARCH_CODEC`) asserted inside `ResearchGameTest`, including forward-tolerance for unknown ids — **passed** |
| Knowledge survives death | attachment `copyOnDeath()` (`ModAttachments.RESEARCH`) |
| Research rules (gating, acyclicity, stable save order) | `PlayerKnowledgeTest` (4), `ResearchCatalogTest` (3) — **passed** |
| Energy/spell arithmetic | `VisStorageTest` (7), `SpellTest` (5), `SpellCatalogTest` (4) — **passed** |
| Loop content registered + localized | `ModSmokeGameTest` (registry smoke), `ModMetadataConsistencyTest` — **passed** |
| Works with and without the optional accessory mod | `./gradlew runGameTestServer` and `./gradlew runGameTestServer -Ptl.noCurios` — **all 4 gametests pass in both configurations** |

Totals at this commit: **37 unit tests / 9 suites, 0 failures; 4 gametests,
0 failures, both accessory configurations.**

## Known limitations (honest scope)

- The Research Note is a consumable: losing it loses the Vis Tap recipe gate
  (Wandwright completes once and does not re-grant). Documented in
  `RESEARCH_SYSTEM_V0_2.md` with the planned fix.
- No research GUI; progression announcements are chat messages.
- The loop is single-player-shaped but per-player: on a server each player
  has their own knowledge (attachment-backed, synced).
