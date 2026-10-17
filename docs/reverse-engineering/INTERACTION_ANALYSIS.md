# Thaumcraft 4.2.3.5 × Baubles 1.0.1.10 — Interaction Analysis (TL-009)

Evidence base: Vineflower-decompiled Thaumcraft sources at
`/home/aaron/.hermes/cache/scratch/decompiled/thaumcraft/` and Baubles
sources at `/home/aaron/.hermes/cache/scratch/decompiled/baubles/`
(both read-only, never committed). All `file:line` citations are relative
to those roots. Companion docs: `DEPENDENCY_GRAPH.md` (TL-007),
`BAUBLES_API_ANALYSIS.md` + `ACCESSORY_MECHANICS.md` (TL-008).

Confidence labels — **[verified]**: direct reading of decompiled source.
**[inferred]**: deduced from code but not executed; each is marked.

Naming note: identifiers are 1.7.10 SRG/obfuscated names as they appear
in the decompiled output; well-known MCP names are annotated *(MCP)*.

---

## 0. Correcting the "17 classes import baubles.*" count

A case-insensitive grep for `baubles` matches 17 Thaumcraft files, but
they split into three distinct groups [verified]:

| Group | Files | Nature |
|---|---|---|
| Import the external `baubles.api` package | 12 | The real TC→Baubles coupling (sections 2–4) |
| Import TC's own `thaumcraft.common.items.baubles` package | 4 | `ConfigItems`, `Config`, `TileWandPedestal`, `BlockStoneDevice` — internal references to TC's bauble *items*, no Baubles-API dependency (section 5) |
| String reference only | 1 | `codechicken/core/launch/DepLoader.java:300` — dependency auto-install (section 1) |

The 12 files importing `baubles.api`: `WandManager`, `EventHandlerRunic`,
`WarpEvents`, `Hover`, `REHWandHandler` (consumers via `BaublesApi`), and
the 7 bauble items (implementors of `IBauble`/`BaubleType`, section 3).

---

## 1. How the dependency is declared and enforced [verified]

- **Hard mod dependency.** `thaumcraft/common/Thaumcraft.java:66-72`:
  `@Mod(... dependencies = "required-after:Forge@[10.13.2,);required-after:Baubles@[1.0.1.10,)")`.
  FML load-order and version gate; TC never runs without Baubles.
- **End-user auto-install.** The bundled CodeChicken DepLoader coremod
  (`DepLoader.java:298-302`) checks each declared dependency and, if the
  `baubles` dependency is missing, downloads it at launch
  (`dep.file.name.equalsIgnoreCase("baubles") → download(dep)`). The
  concrete version list is fetched remotely from
  `https://dl.dropboxusercontent.com/u/47135879/dependancies.info`
  (`DepLoader.java:313-323`), not hardcoded — so the shipped pin is
  whatever that JSON said at runtime **[verified mechanism; the
  1.0.1.10 value itself is only pinned in the `@Mod` string]**.
- **No soft/optional path.** There is no `isModLoaded("Baubles")` guard
  anywhere in the 12 consumer files; every `BaublesApi.getBaubles(...)`
  call site dereferences the returned `IInventory` without a null check
  (e.g. `WandManager.java:57`, `EventHandlerRunic.java:68`). Safe only
  because the dependency is hard **[verified]**.

---

## 2. TC classes that call `BaublesApi` (the read side)

`BaublesApi.getBaubles(player)` returns the player's 4-slot bauble
`IInventory` (reflection bridge into `PlayerHandler.getPlayerBaubles`;
see `BAUBLES_API_ANALYSIS.md` §BaublesApi). TC calls it at 11 sites in
6 classes [verified]. Every consumer iterates `for (a = 0; a < 4; a++)`
— the 4-slot count is hardcoded in TC, mirroring Baubles' fixed layout
(0=AMULET, 1–2=RING, 3=BELT).

### 2.1 `WandManager` (5 call sites) — casting economy

- `getTotalVisDiscount` (`WandManager.java:51-79`, call at :55): sums
  `IVisDiscountGear.getVisDiscount` over all 4 bauble slots **and** the
  4 armor slots; result is divided by 100 and applied to wand costs.
  Baubles are first-class citizens in the discount pool alongside armor.
- `consumeVisFromInventory` (:88-105, call at :89): vis payment order is
  **amulet first** — any `ItemAmuletVis` in any bauble slot gets
  `consumeAllVis` tried before any wand in the 36-slot main inventory.
- `changeFocus` (:554-600, call at :560): builds the wand's focus index
  from `ItemFocusPouch` items found in bauble slots as well as the main
  inventory. Bauble pouches are encoded with negative indices
  (`pouches.put(pouchcount, a - 4)`), main-inventory pouches with
  positive ones — a slot-domain encoding trick unique to TC.
- `fetchFocusFromPouch` (:661-679, call at :673) and `addFocusToPouch`
  (:682-700, call at :683): **write back** into the bauble inventory
  (`baubles.func_70299_a(pouchslot + 4, pouch)` + `func_70296_d()`),
  i.e. TC mutates Baubles-owned state programmatically, bypassing any
  GUI-level `canUnequip` gate.

### 2.2 `EventHandlerRunic` (1 call site) — runic armor aggregation

`EventHandlerRunic.java:66-86`: on player tick (every 40 ticks or when
`isDirty`), sums `IRunicArmor.getRunicCharge` over armor slots **and**
bauble slots, and special-cases variants by metadata: `ItemRingRunic`
meta 2 → charged, meta 3 → healing; `ItemAmuletRunic` meta 1 →
emergency; `ItemGirdleRunic` meta 1 → kinetic. The aggregate is pushed
to the client via `PacketRunicCharge`. This is the same aggregation the
armor-only path does, extended across the accessory inventory.

### 2.3 `WarpEvents` (1 call site) — warp from gear

`WarpEvents.java:353-365` (`getWarpFromGear`, call at :360): permanent
warp is the sum of `EventHandlerRunic.getFinalWarp` over cursed armor
slots **and** all 4 bauble slots. Any mod's bauble carrying warp NBT is
counted, not just TC's.

### 2.4 `Hover` (3 call sites) — hover harness synergy

`Hover.java:101-104`: while hover is active, if **slot 3** (belt) holds
`ItemGirdleHover`, the horizontal-damping factor gets `+0.21` (capped
at 1.0). `Hover.java:154`: same check reduces hover charge expenditure
to `×0.8`. Note the hardcoded slot index 3 — TC assumes the belt slot
position, not `getBaubleType` routing.

### 2.5 `REHWandHandler` (client, 1 call site) — radial focus HUD

`client/lib/REHWandHandler.java:81-98`: when the radial-menu HUD opens,
the client scans `BaublesApi.getBaubles(mc.player)` for
`ItemFocusPouch` and mirrors their contents into the HUD's focus list —
the client-side counterpart of `WandManager.changeFocus`. This read
happens on the logical client, so it depends on the client's bauble
mirror being populated (singleplayer: same-JVM `PlayerHandler`;
dedicated server: `PacketSyncBauble` — see `ACCESSORY_MECHANICS.md`
defect list). **[verified call site; the dedicated-server HUD behavior
is inferred from the sync path, not executed]**

---

## 3. TC items implementing `IBauble` (the write side)

7 classes [verified]. All return `true` from `canUnequip`; only one
gates `canEquip`.

| Item | Type | `getBaubleType` | Lifecycle behavior beyond no-ops | Extra TC interfaces |
|---|---|---|---|---|
| `ItemAmuletVis` (`ItemAmuletVis.java:28`) | AMULET (:65-67) | fixed | `onWornTick` (:69-108): **server-guarded** (`!world.isRemote`, every 5 ticks) — transfers up to 5 vis/tick per primal aspect from amulet into held wand, and drains nearby `TileVisRelay` (range 26) into the amulet. `canEquip` (:224-226): greater variant (meta 1) requires research `VISAMULET` | `IRunicArmor` (charge 0, :233) |
| `ItemAmuletRunic` (`ItemAmuletRunic.java:19`) | AMULET (:52-54) | fixed | `onEquipped`/`onUnequipped` (:68-74) set `Thaumcraft.instance.runicEventHandler.isDirty = true` — cache invalidation for §2.2 | `IRunicArmor` (charge 8/7 by meta, :61) |
| `ItemRingRunic` (`ItemRingRunic.java:18`) | RING (:59-61) | fixed | same `isDirty` invalidation (:66-72) | (charge 1/5/4 by meta, :75) |
| `ItemGirdleRunic` (`ItemGirdleRunic.java:18`) | BELT (:60-62) | fixed | same `isDirty` invalidation (:67-73) | (charge 10/9 by meta, :56) |
| `ItemGirdleHover` (`ItemGirdleHover.java:18`) | BELT (:48-50) | fixed | `onWornTick` (:52-56): reduces `fallDistance` by 0.33/tick — **no side guard**, runs on both logical sides (relies on Baubles' side-agnostic tick driver) | `IRunicArmor` (charge 0, :73) |
| `ItemBaubleBlanks` (`ItemBaubleBlanks.java:22`) | **meta-driven** (:57-72) | meta 0→AMULET, 1/3–8→RING, 2→BELT | none (crafting material) | `IVisDiscountGear`: metas 3–8 give 1% discount for one primal aspect (:84-86); `IRunicArmor` (0, :119) |
| `ItemFocusPouchBauble` (`ItemFocusPouchBauble.java:8`) | BELT (:9-11) | fixed | all callbacks no-op — pure slot-portability marker | extends `ItemFocusPouch` |

Registration [verified]: all seven are registered unconditionally in
`ConfigItems.java` — `itemFocusPouch = new ItemFocusPouchBauble()` at
:333 (the shipped FocusPouch **is** the bauble variant), and the six
`common/items/baubles` items at :453-464.

### Lifecycle usage summary

Of the six `IBauble` callbacks, TC actually *behaves* in only three:

- `onWornTick` — `ItemAmuletVis` (vis logistics), `ItemGirdleHover`
  (fall damage). Driver: Baubles' `EventHandlerEntity.playerTick`
  (`baubles/common/event/EventHandlerEntity.java:16-29`) fires it from
  `LivingUpdateEvent` on **both sides** with no guard.
- `onEquipped`/`onUnequipped` — the three runic items flip the
  `isDirty` flag so the runic-charge cache recomputes immediately
  instead of after ≤40 ticks. Driver: `InventoryBaubles`
  (`baubles/common/container/InventoryBaubles.java:71,84,105,110`).
- `canEquip` — `ItemAmuletVis` research gate; plus Baubles' own routing
  (`InventoryBaubles.java:142-149`, `SlotBauble.java:23-24`,
  `ContainerPlayerExpanded.java:124-151`) consults `getBaubleType` +
  `canEquip` for slot legality.
- `canUnequip` — always `true` in TC; TC never uses the lock-in
  mechanic. `onInventoryChanged` — not part of the 1.0.1.10 interface.

---

## 4. The aggregation pattern TC relies on [verified]

TC never asks Baubles "does this player wear a vis amulet". The
contract TC uses is:

1. `BaublesApi.getBaubles(player)` → raw `IInventory`, iterate slots
   0..3.
2. `instanceof` against **TC's own API interfaces** — `IVisDiscountGear`
   (`thaumcraft/api/IVisDiscountGear.java`), `IRunicArmor`
   (`thaumcraft/api/IRunicArmor.java`) — or concrete TC classes
   (`ItemAmuletVis`, `ItemFocusPouch`, `ItemGirdleHover`).
3. Aggregate the results into TC systems (costs, protection, warp,
   focus cycling).

So Baubles supplies *inventory + slot routing + lifecycle callbacks*;
TC's cross-slot features are its own capability-interface scan over the
bauble inventory. Any third-party mod's bauble that implements
`IVisDiscountGear`/`IRunicArmor` is picked up automatically — the
capability interfaces live in the Thaumcraft API, not the Baubles API.

---

## 5. Coupling that only looks like Baubles usage [verified]

- `TileWandPedestal` (`:254-280, :345-356, :374-375`) accepts
  `ItemAmuletVis` in its single slot and drains vis from nearby nodes
  into it — same item class, but this is block↔item interaction, no
  Baubles API involved.
- `BlockStoneDevice.java:377-388`: right-clicking a wand pedestal
  places the held wand **or vis amulet** onto it.
- `Config.initLoot` (`Config.java:517-524`): loot bags can contain a
  pre-charged lesser `ItemAmuletVis` (random 0–4 vis per primal
  aspect); `Config.java:538-544, 592-605`: `ItemBaubleBlanks` in all
  metas appear in loot-bag tables.

---

## 6. Config gating — there is none [verified by absence]

No option in `Config.java` / `ConfigItems.java` enables, disables, or
tunes the Baubles integration; grep for `bauble` in the config classes
returns only item registration and loot tables. The gating that exists
is:

1. **Load-time**: hard `required-after:Baubles@[1.0.1.10,)` + DepLoader
   auto-install (section 1).
2. **Progression**: `ItemAmuletVis` meta 1 gated by research
   `VISAMULET` through `canEquip` — progression gating is expressed
   *through the Baubles API*, not around it.
3. **Acquisition**: loot tables and crafting only.

**[inferred]** This is a deliberate design: because the dependency is
hard, the authors put no defensive branches anywhere; the only runtime
switch is the research gate.

---

## 7. Side-awareness and sync notes

- `ItemAmuletVis.onWornTick` guards `!world.isRemote` and rate-limits
  to every 5 ticks [verified].
- `ItemGirdleHover.onWornTick` has **no** side guard [verified]; it
  mutates `fallDistance` on both sides — harmless in singleplayer,
  and on a dedicated server the client-side call is what actually
  softens the client's own fall prediction **[inferred]**.
- `REHWandHandler` reads the bauble inventory on the client (§2.5);
  correctness depends on Baubles' client sync, which TL-008 catalogued
  as dedicated-server-only with a singleplayer-mirror defect.
- TC's programmatic writes (`WandManager.fetchFocusFromPouch`,
  `addFocusToPouch`) mutate the server-side bauble inventory directly;
  no TC code re-syncs them beyond Baubles' own `PacketSyncBauble`
  **[verified call sites; sync behavior inferred from TL-008]**.
