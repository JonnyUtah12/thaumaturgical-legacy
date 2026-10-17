# Thaumcraft × Baubles Integration Contract — What We Replicate vs Modernize (TL-009)

Companion to `INTERACTION_ANALYSIS.md` (mechanics + citations live
there) and `BAUBLES_API_ANALYSIS.md` / `ACCESSORY_MECHANICS.md`
(TL-008, the Baubles-side contract). Our target platform is
MC 1.21.1 / NeoForge (`gradle.properties: minecraft_version=1.21.1`),
so "modernize" below means 1.21 idioms, not 1.7.10 ports.

Confidence labels — **[verified]**: grounded in the cited decompiled
source. **[inferred]**: design judgment or behavior implied but not
executed.

---

## 1. The contract TC actually consumes from Baubles

Stated as requirements an accessory system must satisfy for TC-style
features to work [verified against `INTERACTION_ANALYSIS.md` §1–4]:

1. **A per-player accessory inventory reachable from any code path**
   via a static accessor (`BaublesApi.getBaubles`), returning a plain
   `IInventory` — mutable, not just readable.
2. **A fixed 4-slot layout** with type routing: 0=AMULET, 1–2=RING,
   3=BELT; `getBaubleType` decides legality, `canEquip` refines it.
   TC's consumers hardcode the 4-slot iteration; TC's `Hover` even
   hardcodes belt = index 3.
3. **Lifecycle callbacks** driven by the accessory mod: `onWornTick`
   from a living-update event on both sides; `onEquipped`/
   `onUnequipped` from inventory mutations; `canUnequip` only from
   GUI take-out (TL-008 defect: bypassed by death, programmatic
   writes, loads).
4. **Persistence and death handling owned by the accessory mod** —
   TC never saves or drops baubles itself; it relies on Baubles'
   `<username>.baub` files and `PlayerDropsEvent` handling.
5. **Client visibility** of the accessory inventory for client-side
   features (TC's radial focus HUD reads it on the client).

TC's own extension of the contract: cross-slot feature aggregation is
done by `instanceof` scans against **TC API interfaces**
(`IVisDiscountGear`, `IRunicArmor`) — the accessory system is just the
inventory; the semantics are the consumer's.

---

## 2. What our mod must replicate (behavioral parity)

These are the mechanics users would notice; MVP/LATER markers follow
`MECHANICS_CATALOG.md`.

| # | TC behavior (source) | Replicate as | Priority |
|---|---|---|---|
| R1 | Accessory slots exist and are reachable from casting code (`WandManager.java:55,89`) | Our vis/cast pipeline must consult an accessory inventory with the same "amulet pays first" order: `consumeVisFromInventory` tries every `ItemAmuletVis` before any wand | MVP (wand casting exists per TL-016) |
| R2 | Vis amulet (`ItemAmuletVis.java:69-108`) | Worn-tick transfer: ≤5 vis/tick per primal aspect amulet→held wand, server-side only, 5-tick cadence; capacity 2500/25000 (25/250 units) | LATER (needs vis storage items) |
| R3 | Vis discount aggregation (`WandManager.java:51-79`) | Sum per-aspect discount percent over accessories **and** armor; blanks give 1% per primal aspect (`ItemBaubleBlanks.java:84-86`) | MVP-adjacent (our wand cost system already has a discount hook) |
| R4 | Runic charge aggregation (`EventHandlerRunic.java:66-86`) | Aggregate protection charge across armor + accessories; equip/unequip invalidates the cache immediately (`isDirty`), else 40-tick refresh | LATER (runic armor subsystem) |
| R5 | Warp from gear (`WarpEvents.java:353-365`) | Permanent warp sums cursed modifiers over armor + accessory slots | LATER (warp subsystem) |
| R6 | Focus pouch as belt accessory (`ItemFocusPouchBauble.java`, `WandManager.java:554-700`) | Focus selection scans accessory slots for pouches; pouch contents are fetchable/storable programmatically from the wand UI | LATER (focus system) |
| R7 | Research-gated equipping (`ItemAmuletVis.java:224-226`) | `canEquip`-style gate: greater vis amulet requires the corresponding research entry — progression enforced *through* the accessory API | MVP pattern (we have research-gated casting per TL-016) |
| R8 | Pedestal accepts the vis amulet (`TileWandPedestal.java:254-280,345-356`) | Block-level item acceptance keyed on the item class, independent of worn state | LATER |
| R9 | Loot injection (`Config.java:517-524,538-544`) | Pre-charged amulet + blank accessories in loot tables | LATER |
| R10 | Hard dependency posture (`Thaumcraft.java:66-72`) | If we depend on an accessory mod, declare it hard in mod metadata and skip defensive null branches — TC's zero-guard style is only safe with a hard dep | MVP (metadata decision) |

## 3. What we must modernize (do not port as-is)

| # | TC/Baubles 1.7.10 mechanism | Why it must change on 1.21 | Replacement |
|---|---|---|---|
| M1 | `BaublesApi.getBaubles` reflection bridge (TL-008) | Reflection indirection + null-when-absent contract; NeoForge 1.21 has first-class data attachments | Direct accessor on the accessory mod's API (modern Baubles / Curios-style), or our own attachment if we own the slots |
| M2 | Fixed 4 slots, hardcoded `a < 4` loops, belt = index 3 (`Hover.java:101`) | Modern accessory mods use registry-defined slot types with variable counts; index assumptions break | Query by **slot type** (AMULET/RING/BELT equivalents), never by index |
| M3 | `instanceof` scans over an `IInventory` of `ItemStack`s | 1.21 `ItemStack`s are component-based; class-based capability checks still work but the interfaces should be NeoForge `AttachmentType`/item-interface lookups | Keep the TC pattern (consumer-owned capability interfaces) but express it as item interfaces or data components |
| M4 | NBT aspect keys (`ItemAmuletVis.storeVis`, raw `NBTTagInt` per aspect tag) | 1.21 forbids raw NBT on items; DataComponents are the sanctioned store | A `visStorage` DataComponent map (aspect→amount) |
| M5 | `onWornTick` fired on both sides with no guard (`ItemGirdleHover.java:52-56`) | TL-008 defect list; double-side mutation is a source of desync | Side-guard every worn-tick effect; server authoritative, client cosmetic only |
| M6 | `canUnequip` enforced only on GUI take-out (TL-008) | Death/load/programmatic writes bypass it; TC itself bypasses it via `fetchFocusFromPouch` | Enforce at every mutation point in our own code; if we depend on a third-party accessory mod, treat `canUnequip` as advisory |
| M7 | Client mirror defect (singleplayer never populates the client mirror; dedicated-only `PacketSyncBauble`, TL-008) | Our radial/focus UIs need reliable client data | Sync accessory contents via our own payload on open + on change, or render from server-sent state |
| M8 | Negative-index encoding of bauble slots (`pouchslot + 4` / `a - 4`, `WandManager.java:566,673`) | Fragile coupling to the 4-slot layout | Explicit `(source, slotId)` pair in any focus/pouch indexing |
| M9 | SRG names, `func_70301_a`-style calls | 1.21 mappings differ | Parchment/Mojmap names throughout |
| M10 | DepLoader launch-time auto-download from a dead Dropbox URL (`DepLoader.java:313-323`) | Modern modpacks manage deps via mod metadata + launcher | Declare dependency in `neoforge.mods` metadata only; no runtime downloader |

## 4. Decision for our architecture [inferred — recommendation]

TC's design splits cleanly into (a) an accessory inventory with typed
slots and lifecycle hooks, and (b) consumer-side aggregation scans.
For the legacy port we should:

1. **Depend on a modern accessory provider** (Baubles-for-1.21 lineage
   or Curios — whichever the ATM10 target ships; TL-021 identified the
   pack, its mod list check is a follow-up) rather than re-implementing
   accessory slots ourselves. The contract in §1 is provider-agnostic.
2. **Own the semantics**: define our own marker interfaces/components
   (vis-discount gear, runic charge, warp, vis storage) exactly as TC
   owned `IVisDiscountGear`/`IRunicArmor` — never put our semantics in
   the provider's API.
3. **Aggregate by slot type, pay vis amulet-first, gate equipping by
   research** — the three behaviors users actually feel (R1, R3, R7).
4. Apply M1–M10 as the port's standing rules; the do-not-replicate
   list in `ACCESSORY_MECHANICS.md` (TL-008) covers the provider side,
   this table covers the consumer side.

## 5. Open questions for the implementation card

- Which accessory provider does the ATM10 8.3 target actually ship?
  (Blocks R1/R7 slot-type names; verify against the pack manifest.)
- Do we need belt-slot fall-damage softening (R-hover girdle) in the
  MVP? TC's version is a 3-line `onWornTick`; cheap to include if a
  belt slot exists.
- Runic/warp aggregation (R4/R5) should not be built before their
  subsystems exist; only the aggregation *hook* over accessory slots
  should be reserved in their designs.
