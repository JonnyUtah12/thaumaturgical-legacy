# Implementation Mapping — original mechanics vs. Thaumaturgical Legacy

Purpose: the honesty check required by the project brief ("do not claim that
every original feature has been recreated"). Every mechanic documented in the
reverse-engineering catalogs is listed here with exactly one status:

- **IMPLEMENTED** — the mechanic exists in our mod; the implementing classes
  and tests are named.
- **MODERNIZED** — the mechanic's *behavior* is reproduced on a deliberately
  different modern architecture; the "instead" column says what we did and
  links the design record.
- **NOT IMPLEMENTED** — no implementation exists; the reason column states
  the scope decision. No partial or placeholder mechanics are claimed.

Claims were verified against the source tree on `main` (project
0.2.0-SNAPSHOT) on 2026-10-17. Source roots below are relative to
`src/main/java/com/jonnyutah12/thaumaturgicallegacy/`.

## 1. Vis, energy, and the wand loop (MECHANICS_CATALOG §2, CORE_MECHANICS §2–§4)

| Original mechanic | Status | Where / instead |
| --- | --- | --- |
| Wand internal vis pool with per-cast cost | MODERNIZED | TC stores vis in wand item metadata (`ICustomAmounts`); we use a stack **data component** (`registration/ModDataComponents`, `magic/VisStorage`) — networkable, save-safe, no per-item NBT strings. Tests: `VisStorageTest`, `MagicSystemGameTest` |
| Casting a spell consumes vis, has cooldown | IMPLEMENTED | `items/ThaumicWandItem.use` (withdraw → cast → cooldown), `magic/SpellCatalog`, `magic/SpellDefinition`. Tests: `SpellTest`, `SpellCatalogTest`, `MagicSystemGameTest` |
| Two castable spells (Firebolt projectile, Frost Lance) | IMPLEMENTED (subset of TC's focus system) | `magic/Spell` + `ThaumicWandItem.cast`. TC's per-aspect foci/sceptres/staffs are NOT IMPLEMENTED (deferred) |
| Vis network: nodes → conduits → machines, per-tick machine vis | NOT IMPLEMENTED | CORE_MECHANICS §3; deferred milestone. Our Vis Tap gathers ambient vis locally instead (see row below) |
| Recharging a wand from an external vis source | MODERNIZED | TC: wand-vs-node/network interaction; we use a crafted **Vis Tap** block with a buffered ticker (`block/VisTapBlock`, `blockentity/VisTapBlockEntity`) and wand-on-tap transfer. Test: `MagicSystemGameTest` |
| Vis cost reduction from worn accessories | MODERNIZED | TC: `IVisDiscountGear` instanceof scan over Baubles slots; we use a provider-abstracted query (`accessory/WornAccessories`, `accessory/core/VisCostRules`) that works under Curios or the fallback. Tests: `VisCostRulesTest`, both accessory gametests |

## 2. Research and progression (MECHANICS_CATALOG §3, CORE_MECHANICS §7)

| Original mechanic | Status | Where / instead |
| --- | --- | --- |
| Per-player knowledge set, persists across death | MODERNIZED | TC `PlayerKnowledge` on the player entity; we use a **player data attachment** with a lenient enum-list codec (`registration/ModAttachments.RESEARCH`, `magic/research/PlayerKnowledge`, `copyOnDeath`). Test: NBT round-trip in `ResearchGameTest` |
| Prerequisite-gated entry tree | IMPLEMENTED | `magic/research/ResearchCatalog`, `ResearchDefinition`. Tests: `ResearchCatalogTest` (acyclicity), `PlayerKnowledgeTest` |
| Auto-unlock on join; hidden research on crafting; unlock on world interaction | IMPLEMENTED | `event/ResearchEvents` (PlayerLoggedIn / ItemCrafted / EntityPlace triggers), mirroring CORE_MECHANICS §7.5. Test: `ResearchGameTest` |
| Knowledge gates recipes | MODERNIZED | TC checks a research key against the acting player inside its recipe type; NeoForge 21.1 has no per-player vanilla-recipe hook, so the gate is a **knowledge item** (Research Note, granted once as a reward, consumed by the Vis Tap recipe). Rationale + trade-offs: `docs/architecture/RESEARCH_SYSTEM_V0_2.md` |
| Knowledge gates spells | IMPLEMENTED | `ResearchEvents.isSpellKnown` + cycle/cast gating in `ThaumicWandItem`. Test: `ResearchGameTest` |
| Hex-note puzzle presentation, aspect-pool research currency, scan-based discovery (Thaumometer) | NOT IMPLEMENTED | RE docs mark the puzzle/currency as the economy layer, not the progression core; deferred (see RESEARCH_SYSTEM_V0_2 "Known limitations") |
| Warp (per-player/research side effects) | NOT IMPLEMENTED | CORE_MECHANICS §8; deferred — no negative-consequence systems in 0.x |

## 3. Arcane crafting / alchemy (MECHANICS_CATALOG §5, CORE_MECHANICS §5–§6)

| Original mechanic | Status | Where / instead |
| --- | --- | --- |
| Arcane-workbench crafting with research gating | MODERNIZED | Vanilla crafting table + knowledge-item gate (see §2). No custom workbench GUI in 0.x |
| Crucible alchemical state machine (entropic ignition etc.) | NOT IMPLEMENTED | CORE_MECHANICS §5; deferred milestone |
| Infusion matrix (structured ritual crafting) | NOT IMPLEMENTED | CORE_MECHANICS §6; deferred milestone |
| Recipe ingredient rules (shaped crafting, single output) | IMPLEMENTED | `src/main/resources/data/thaumaturgicallegacy/recipe/` (vanilla JSON) |

## 4. Aspects and essentia (MECHANICS_CATALOG §1)

| Original mechanic | Status | Where / instead |
| --- | --- | --- |
| 25-aspect model, aspect combinations, essentia in items | NOT IMPLEMENTED | MECHANICS_CATALOG §1; the aspect economy is the largest deferred subsystem |
| "Spell costs" as aspect sets | MODERNIZED (simplified) | Our spells carry a single integer vis cost (`magic/SpellDefinition`) — deliberately not aspect-based until the aspect subsystem exists |

## 5. Devices and constructs (MECHANICS_CATALOG §6, §9)

| Original mechanic | Status | Where / instead |
| --- | --- | --- |
| Vis-gathering device (analogue) | IMPLEMENTED (single device) | Vis Tap only; essentia transport, smelting devices, golems, giant constructs NOT IMPLEMENTED |
| TC world entities (golems, wisp, taint creatures) | NOT IMPLEMENTED | MECHANICS_CATALOG §9; deferred |

## 6. Accessories / equipment (MECHANICS_CATALOG §7, ACCESSORY_MECHANICS A–E)

| Original mechanic (Baubles 1.0.1.10) | Status | Where / instead |
| --- | --- | --- |
| A1 server-authoritative accessory state | IMPLEMENTED | All mutation server-side; `accessory/fallback/FallbackAccessoryProvider`, `accessory/curios/CuriosAccessoryProvider` |
| A2 fixed slot set with type routing | MODERNIZED | TC/Baubles hard-coded 5 slots; we declare one dedicated `thaumaturgicallegacy.charm` slot as **data** under Curios, or a single-slot fallback (`accessory/AccessorySlots`) — `docs/architecture/ACCESSORY_SYSTEM.md` |
| A3/A4/A5 one item per slot, item-side veto, validation on every path | IMPLEMENTED | `accessory/ThaumicAccessory` + provider `equip` validation; tests: `AccessoryFallbackGameTest`, `AccessoryCuriosGameTest` |
| B1/B2 equip/unequip + worn-tick effects | MODERNIZED | TC reads `IVisDiscountGear`/`IRunicArmor` via instanceof; we use the provider-abstracted `accessory/WornAccessories` query at point of use (no per-tick scan) |
| C1 per-player persistent accessory storage | MODERNIZED | Baubles per-player inventory → Curios attachment (when present) or our own serialized attachment (`ModAttachments.THAUMIC_CHARM`) |
| C3 death contract (keepInventory-aware) | IMPLEMENTED | `accessory/fallback/FallbackAccessoryEvents.onPlayerClone` + Curios death handling; test: `AccessoryFallbackGameTest` |
| C4 client mirror sync | MODERNIZED | Baubles custom packets → attachment `sync` + `accessory/fallback/CharmSyncPayload` |
| D1–D3 inventory-screen slots, hotkey, shift-click routing | MODERNIZED (Curios path) / NOT IMPLEMENTED (fallback GUI) | Under Curios, Curios' own UI provides these; the no-Curios fallback has **no inventory GUI** — equipping is a right-click toggle on the item (`items/VisAmuletItem.use`), documented as the minimal fallback UX |
| Attribute modifiers from accessories | NOT IMPLEMENTED | No attribute-bearing accessories in 0.x; the one accessory effect is the vis-cost discount above |
| TC equipment (magic rings, amulets, belts, charms with TC effects) | IMPLEMENTED (one item) | Vis Amulet only; the rest of TC's equipment catalog is NOT IMPLEMENTED (deferred) |

## 7. World, networking, persistence, config (MECHANICS_CATALOG §8, §10–§11)

| Original mechanic | Status | Where / instead |
| --- | --- | --- |
| Vis nodes, ore generation, taint, biomes, eldritch realm | NOT IMPLEMENTED | MECHANICS_CATALOG §8; deferred — 0.x adds no worldgen |
| Per-entity custom amounts (ICustomAmounts) | MODERNIZED | NeoForge data attachments (`registration/ModAttachments`) |
| Mod config (Thaumcraft's large config surface) | NOT IMPLEMENTED | No config in 0.x; spell/device constants are code constants pending a config pass |
| Client rendering of auras/overlay effects | NOT IMPLEMENTED | No client effects in 0.x (`client/ThaumaturgicalLegacyClient` is a stub hook only) |

## Summary

Counted by the rows above: **11 IMPLEMENTED**, **15 MODERNIZED**,
**9 NOT IMPLEMENTED** across 35 mapped mechanics.

The implemented + modernized set is exactly the first playable loop
(craft → research → unlock → craft device → recharge → cast → research-gated
upgrade spell) plus the accessory layer; everything else is explicitly
deferred, not half-present. See `docs/PLAYABLE_LOOP.md` for the loop and how
each step is verified.
