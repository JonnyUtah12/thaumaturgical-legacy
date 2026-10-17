# Core Mechanics — Behavioral Spec (from Thaumcraft 4.2.3.5) — TL-006

Purpose: a deep behavioral specification of the core magic loop — aspects,
vis/nodes, wands, crucible, infusion, research, warp — distilled for our
independent (NeoForge 21.x) implementation. Companion to
`MECHANICS_CATALOG.md` (breadth-first inventory of every subsystem); this
document states the *state machines, formulas, and constants* for the loop
that TL-009 implements and extends, and marks per mechanic what to preserve
and what to modernize.

Evidence: Vineflower-decompiled sources of `Thaumcraft-1.7.10-4.2.3.5.jar`
(SHA-256 3dba9786…), read-only at
`~/.hermes/kanban/boards/thaumaturgical-legacy/workspaces/t_da28536e/decompiled/vineflower/`
(never committed; a formatting-equivalent mirror sits at
`~/.hermes/cache/scratch/decompiled/thaumcraft/`). Citations are
`file:line` relative to the `thaumcraft/` package root and refer to the
vineflower tree. Labels: **[verified]** = read directly from decompiled
code; **[inferred]** = deduced from code reading without execution;
**[uncertain]** = needs a runtime check. Nothing here was executed.

Scope: the player-facing magic loop. Golems, runic armor, thaumic mirrors,
eldritch dimension content, and the golemancy/eldritch research branches are
inventoried in `MECHANICS_CATALOG.md` and not deepened here.

---

## 1. Aspect model

### 1.1 The aspect graph is code, not data
48 aspects ship as `public static final Aspect` constants: 6 primal
(`aer`, `terra`, `ignis`, `aqua`, `ordo`, `perditio`, `api/aspects/Aspect.java:17–22`)
and 42 compounds, each defined by exactly its two component aspects
(`Aspect.java:23+`, e.g. `MAGIC = new Aspect("praecantatio", …, {VOID, ENERGY})`).
`isPrimal()` is literally "components == null or length != 2". Aspects
self-register into a static `LinkedHashMap` keyed by tag; icon lookup is by
convention `textures/aspects/<tag>.png`. **[verified]**

→ Modern: keep the graph as data (a generated table or datapack), not static
fields; preserve the exact 6+42 composition pairs — they are load-bearing
for every recipe cost below and for the research-table adjacency rule (§7.3).

### 1.2 AspectList is a counted multiset
`api/aspects/AspectList` maps aspect→amount; `size()` counts *distinct*
aspects, `getAmount()` the count of one; `add`/`reduce` clamp at 0 and drop
zero entries. Every container interface (`IAspectContainer`, `IAspectSource`,
`IEssentiaTransport`, `IEssentiaContainerItem`) speaks this type. **[verified]**

### 1.3 Item/entity essentia registration
`ThaumcraftApi.objectTags` (filled by `registerObjectTag` /
`registerComplexObjectTag`, OreDictionary-aware) is the item→aspects map;
`scanEntities` + `registerEntityTag` (with optional `EntityTagsNBT`
predicates) is the entity→aspects map. Items with no registered tag get
aspects *derived recursively* from their crafting recipe (`generateTags` in
`common/lib/crafting/ThaumcraftCraftingManager`, recipe-type-specific
helpers with a recursion guard). `getBonusTags` adds dynamic aspects from
`IEssentiaContainerItem` (e.g. filled jars). **[verified]**

→ Modern: registration table + recipe-derived fallback is worth preserving;
it is what makes scanning *any* modded item meaningful.

---

## 2. Vis and world nodes

### 2.1 Node data model
`common/tiles/TileNode` holds two AspectLists: `aspectsBase` (capacity
baseline) and `aspects` (current pool). Type and modifier are stored as
ordinal bytes under NBT keys `type`/`modifier`; `NodeType` = NORMAL,
UNSTABLE, DARK, TAINTED, HUNGRY, PURE; `NodeModifier` = BRIGHT, PALE,
FADING (`api/nodes/NodeType.java`, `NodeModifier.java`). Nodes are invisible
without an `IRevealer` (Thaumcraft Goggles). **[verified]**

### 2.2 Worldgen
Per chunk, one node attempt with probability 1/`nodeRarity` (default 36,
`common/config/Config.java:123`; master switch `genAura`, `Config.java:105`).
With probability 1/`specialNodeRarity` (18, `Config.java:124`) the node is
special: DARK 30% / UNSTABLE 30% / PURE 30% / HUNGRY 10%; each modifier
with probability 1/27 (`common/lib/world/ThaumcraftWorldGenerator.java:220–403`). **[verified]**

Total vis = biome aura ±50% (`value = rand(baura/2) + baura/2`); biome aura
comes from a BiomeDictionary-type table, default 100
(`common/lib/world/biomes/BiomeHandler.java:19–60`). In a Taint biome the
total is ×1.5 and the node becomes TAINTED with 50% chance. Aspect makeup:
biome tag +2, or a random compound +1 plus a primal +1; then three coin
flips each add +1 primal (1/18 chance a compound instead). The total is
split across the aspects with random weights (25–74, or 50–74 for aspects
listed twice), normalized. **[verified]**

### 2.3 Recharge, degradation, discharge (the node lifecycle)
All in `TileNode` update; intervals are tick counts. **[verified]**

- **Recharge** (`TileNode.java:579–660`): every `regeneration` ticks —
  600 normal, 400 BRIGHT, 900 PALE, none FADING — add +1 to a random
  aspect below its base. A node stabilizer below the node doubles
  (`lock=1`) or ×20 (`lock=2`) the interval; the stabilizer is a
  stone-device block, meta 9 → lock 1, meta 10 → lock 2
  (`TileNode.checkLock`, `TileNode.java:946–964`). Offline time is
  caught up: `elapsed_ms / (regeneration × 75)` units.
- **Degradation** (`TileNode.java:660–720`): every 1200 ticks, for each
  aspect sitting at 0, base −1; 1/20 chance to delete the aspect entirely;
  1/5 chance to degrade the modifier one step (BRIGHT→none→PALE→FADING).
  When no aspects remain the node block is removed.
- **Discharge / node-to-node transfer** (`TileNode.java:720–779`): every
  2 ticks (BRIGHT: 1, PALE: 3; PALE also skips 50% of the time), pick a
  random offset ±4; if a neighbor node's average fill is lower, transfer
  1 vis. If the donor's aspect is already at base, with probability
  `1/(1+base)` the *neighbor's* base grows (node growth), and with
  probability 1/3 the donor's base shrinks by 1.

### 2.4 Node type behaviors (`TileNode.java:779–946`) **[verified]**
- UNSTABLE: every 100 ticks, 50% chance to spit a random primal as an
  `EntityAspectOrb`; a stabilizer gives a small chance to become NORMAL.
- TAINTED: every 50 ticks converts a random biome within ±8 to Taint;
  with `hardNode` also spreads taint fibres. Untainted nodes inside a
  Taint biome flip to TAINTED with probability 1/500 per 100 ticks.
- PURE: converts biome to Magical Forest (±8, not in nether/end).
- DARK: converts biome to Eerie (±12); with `hardNode` spawns Brainy
  Zombies (max 3 within 10×6×10).
- HUNGRY: every 50 ticks eats blocks (hardness <5) along rays within ±16.
- FADING + lock: small chance to recover to PALE.

### 2.5 Tapping vis from nodes (the player's vis source)
Right-click-hold on a node binds the wand to it; every 5 ticks
`TileNode.onUsingWandTick` (`TileNode.java:374–455`) transfers up to
`tap` vis: `tap = 1 + (NODETAPPER1 ? 1 : 0) + (NODETAPPER2 ? 1 : 0)`, max 3.
With the NODEPRESERVE research (and a non-wood rod,
non-iron cap, not sneaking) the node keeps its last 1 vis. The aspect
drawn is a random one the wand has room for. **[verified]**

### 2.6 Aspect orbs
Killed mobs drop primal orbs: for each primal in the entity's generated
aspects, 50% chance of an orb worth `1 + rand(amount)`
(`common/lib/events/EventHandlerEntity.java:581–590`). Orbs live 150
ticks, drift, and are collected by walking over them with a wand in the
hotbar — the vis goes into the wand (`common/entities/EntityAspectOrb.java:213–224`).
Unstable nodes spit the same entity type. **[verified]**

→ Modern: preserve the three-timescale node model (fast discharge, slow
recharge, very slow degradation) — it is what makes vis a *managed*
resource. The orb drop is a nice-to-have; the tap loop is the MVP.

---

## 3. The vis network (machine vis)

### 3.1 Natural nodes are NOT network sources
`VisNetHandler` (`api/visnet/VisNetHandler.java`) keeps a per-dimension
registry of `TileVisNode` sources (`addSource`, line 54) and a
`nearbyNodes` cache (line 141); `drainVis(world,x,y,z,aspect,amount)`
(line 20) walks reachable sources calling `consumeVis` until the cost is
met. The only registered sources are **energized nodes**
(`TileNodeEnergized`, `isSource=true`, range 8) and **vis relays**
(`TileVisRelay`, range 8, `common/tiles/TileNodeEnergized.java:156`,
`TileVisRelay.java:50–57`). `TileNode` (natural nodes) does not extend
`TileVisNode` and never registers — natural vis is wand-only. **[verified]**

### 3.2 Connection rules
A source/relay links to the nearest other node within its range if
(a) attunements match or either is −1 (relays use their dye color as
attunement, `TileVisRelay.java:50–52`), and (b) a ray trace ignoring
source blocks is clear (`VisNetHandler.java:96–98`, `canNodeBeSeen`
line 202). Consumption delegates up the tree to the root source. **[verified]**

### 3.3 Energized nodes and the node converter
Energized nodes hold a fixed pool (`vis`/`visBase`) and **do not
regenerate** (`TileNodeEnergized.java:160–200`). The node converter
(`common/tiles/TileNodeConverter.java`, placed above a natural node)
drains the natural node at 1 vis/tick for 1000 ticks, then converts the
node block into an energized node whose pool is the natural node's
`aspectsBase`; reverse mode restores the natural node. **[verified]**
(This closes the `[inferred — tick logic not fully traced]` gap in
`MECHANICS_CATALOG.md` §2.)

### 3.4 Machines
Devices pay via `VisNetHandler.drainVis` and must sit within a source's
range (8): arcane furnace (Fire), alchemy furnace (Fire/Entropy/Water),
arcane bore (Entropy), essentia crystalizer, mirror (Air), flux scrubber
(Air), focal manipulator. Call sites verified in `MECHANICS_CATALOG.md` §2.
The magic-workbench charger refills the wand in the workbench below it
from the network, 5 vis per aspect per tick
(`common/tiles/TileMagicWorkbenchCharger.java:32–41`). The wand pedestal
refills its wand from the node beneath it, 1 vis/tick, excluding
iron-cap/wood-rod wands (`common/tiles/TileWandPedestal.java:193–231`). **[verified]**

→ Modern: the two-source split (wand-from-nodes vs machines-from-
energized-nodes) is the load-bearing vis-economy design; preserve it.
Attunement colors are optional depth.

---

## 4. Wands and casting costs

### 4.1 Storage units: vis is stored ×100
Wand vis lives in NBT per primal tag in **hundredths** of a vis unit:
`addVis(aspect, amount)` stores `amount*100` and returns leftover/100;
`addRealVis` works in raw units (`common/items/wands/ItemWandCasting.java:338–380`).
`getMaxVis = rod.capacity × 100` (×150 when the rod is a staff rod —
a "sceptre") (`ItemWandCasting.java:82`). **[verified]**
→ Modern: pick ONE unit (whole vis) and say so; this ×100 is a legacy
of the workbench cost path (§4.4).

### 4.2 Tier tables (`common/config/ConfigItems.java:287–335`) **[verified]**
Caps (cost multiplier / crafting cost): copper 1.1 (1.0 for ordo+perditio),
iron 1.1, silver 1.0 (0.95 for the four elemental primals), gold 1.0,
thaumium 0.9, void 0.8.
Rods (capacity, whole vis): wood 25, greatwood 50, obsidian/blaze/ice/
quartz/bone/reed 75, silverwood 100. Staff rods: greatwood 125,
primal 175, silverwood/primal 250.

### 4.3 Cost model
`getConsumptionModifier` = cap modifier − equipment vis discount
(`WandManager.getTotalVisDiscount`: sum of `IVisDiscountGear` integer
percent-points / 100) − focus frugal/10 − 0.10 sceptre bonus, floored at
0.1. The "vis exhaustion" potion effect *subtracts* (level+1)×10 from the
discount total, i.e. +10% cost per level (`common/items/wands/WandManager.java:75–110`). **[verified]**
`consumeVis` multiplies the requested amount by the modifier and debits
stored vis; `consumeVisFromInventory` pays from a vis-amulet first, then
any wand. **[verified]**

### 4.4 Arcane crafting pays ×100
Arcane-workbench recipes charge the wand in slot 10 via
`consumeAllVisCrafting`, which costs `amount × 100` raw units — i.e. the
recipe AspectList amounts are whole-vis and the ×100 is the unit
conversion (`client/gui/slot/SlotCraftingArcaneWorkbench.java:103–105`). **[verified]**

### 4.5 Passive rod regeneration (`WandRodPrimalOnUpdate.java`) **[verified]**
Single-primal rods regenerate 1 vis of their primal every 200 ticks up to
10% of capacity; primal staff rods regenerate 1 random primal every 50
ticks up to 10% of capacity.

### 4.6 Focus costs (base; beam/ball variants differ) **[verified]**
`items/wands/foci/`: Fire F10 (beam F10+O3; ball F66+Ent33,
`ItemFocusFire.java:23–25`); Frost A5 E5 Ent2; Shock A25 W10 E25;
Excavation E15 O2; Hellbat F200 Ent100 A100 (bomb F100 Ent200 A100;
devil adds E100, `ItemFocusHellbat.java:29–31`); Pech W10; PortableHole
A10; Trade O5; Warding W10; Primal: six primals at `50 + 5·rand(5)`
each (`ItemFocusPrimal.java:78–83`).

→ Modern: preserve the *shape* — cap multiplier × rod capacity ×
per-spell aspect costs, with discount stacking from equipment. The
specific numbers are tuning; keep them as a starting table.

---

## 5. The crucible (alchemical state machine)

All in `common/tiles/TileCrucible.java`; the crucible is a metal-device
block and item entry is triggered by entity collision
(`common/blocks/BlockMetalDevice.java:215` → `attemptSmelt`). Thrown
items carry the thrower's name in entity NBT (`EventHandlerEntity.java:114`)
— that name gates research (§5.4). **[verified]**

### 5.1 Heat
`heat` 0–200. With a heat source below (fire, lava, or a burning airy
block): `heat += 1 + 2×bellows` per tick (bellows = adjacent bellows
blocks); otherwise `heat −1` per tick. "Active" means `heat > 150`; block
updates fire at the 151/149 crossings (`TileCrucible.java:70–94`). **[verified]**

### 5.2 Water and aspect pool
Tank capacity 1000 mB. The aspect pool holds at most `maxTags = 100`
total vis (`TileCrucible.java:37`). Above 100: every 5 ticks spill one
random unit and create flux gas/slime above the crucible (meta grows to
7; 25% chance to spill to an adjacent block) (`TileCrucible.java:101–103, 196`). **[verified]**

### 5.3 Decomposition (the idle decay)
A `counter` starts at −100 and increments every tick; throwing/crafting
resets it (§5.4). When `counter > 100` and `heat > 150` — i.e. ~100
ticks after the last activity — every such tick: pick a random aspect
(reroll once if primal, biasing compounds), drain 2 mB water, remove 1 of
it; if it was a compound, add +1 of one of its two components (coin
flip); if primal, spill instead (`TileCrucible.java:106–131`). **[verified]**
Net effect: an idle hot crucible *breaks compounds down into primals*
and slowly loses them — the pool is perishable, so players throw fresh
materials rather than stockpiling.

### 5.4 Throwing items: dissolve vs craft
`attemptSmelt(EntityItem)` (`TileCrucible.java:266–312`), per item in the
stack:
1. Try `findMatchingCrucibleRecipe(thrower, pool, item)`
   (`common/lib/crafting/ThaumcraftCraftingManager.java:107`). A recipe
   matches if the pool is a **superset** of the recipe's aspect costs
   (per-aspect `getAmount ≥ required`), the thrown item matches the
   recipe's catalyst (item or OreDict list), and the thrower has the
   recipe's research complete. Among matches, the one with the **most
   distinct aspects** wins (`api/crafting/CrucibleRecipe.java`
   `matches`/`removeMatching`). **[verified]**
2. On match (water required): output ejected, matching aspects removed,
   50 mB water drained, `counter = −250`, player-crafting event fired. **[verified]**
3. Else: add the item's object tags + bonus tags to the pool,
   `counter = −150`. An item with no tags bounces (pop sound, vy=0.35). **[verified]**

### 5.5 Cost scale (samples, `common/config/ConfigRecipes.java:287–350`) **[verified]**
Alumentum: catalyst coal, Entropy 3 + Fire 3 + Energy 3. Nitor:
glowstone dust, E3 F3 Light3. Thaumium: iron ingot, Magic 4. Void metal:
void seed, Metal 8. Tallow: rotten flesh, Magic 2. Gunpowder: F4 Ent4.

→ Modern: preserve the pool-perishability (§5.3) and the
superset-match-with-catalyst rule; they are the actual alchemy gameplay.
The 150/200 heat scale and mB numbers are arbitrary units — normalize.

---

## 6. The infusion matrix

All in `common/tiles/TileInfusionMatrix.java`. **[verified]**

### 6.1 Structure and activation
Valid matrix: center block; pedestal 2 below (y−2); four pillars at
(x±1, y−2, z±1). Input item sits on the pedestal; component items on the
other pedestals found by `getSurroundings` (scan x/z ±12, y −5..+10).
Activated by right-clicking with a wand; `craftingStart` matches an
`InfusionRecipe` (research-gated via the player,
`ThaumcraftCraftingManager.java:198`) and sets
`instability = symmetry + recipeInstability` (lines 291, 317).

### 6.2 Symmetry math (`getSurroundings`, lines 700–790)
Despite the name, `symmetry` is an **asymmetry penalty**. For each
pedestal: +2 (+1 if it holds an item); if its mirror across the matrix
center also has a pedestal: −2 (−1 if both hold items) — so a mirrored
pair nets 0 and an unpaired pedestal nets +2 (or +3 with items).
Stabilizers (gold blocks or `IInfusionStabiliser` tiles; the block is
`Blocks.field_150465_bP`, gold block per the 1.7.10 mapping
**[uncertain: SRG field identity not cross-checked]**): unpaired +0.1,
paired net −0.1. The sum is cast to int and added to instability.

### 6.3 The cycle
Every `countDelay` ticks (default 10; 20 while an item-consumption
animation runs; `TileInfusionMatrix.java:73, 438, 461`):
- Failure roll: `!valid || rand(500) ≤ instability` (line 346) → random
  event: eject the input, zap the nearest player, damage, or a warp event
  (lines 346–424, 608–666).
- Aspect cost: pay **1 unit per cycle** of each required aspect from
  essentia containers within range 12 (`EssentiaHandler.drainEssentia`,
  line 467). A failed drain raises instability with probability
  `1/(50 − 2·recipeInstability)`, capped at 25 (lines 448–452).
- Ingredients on pedestals are consumed one per cycle (5-tick animation;
  containers are returned empty). Enchantment recipes instead drain XP
  from nearby players (1 XP/cycle, plus random damage).
- When the aspect cost is fully paid and ingredients consumed →
  `craftingFinish` places the output on the pedestal.

### 6.4 Cost scale (sample, `ConfigRecipes.java:2813–2827`) **[verified]**
Focus of the Hellbat: instability 3; costs Fire 25 + Air 15 + Beast 15 +
Entropy 25; input cake; six component items.

→ Modern: preserve (a) per-cycle payment from local essentia, (b)
instability as a live risk stat fed by setup asymmetry, (c) the
500-denominator failure roll. The exact symmetry constants are tuning.

---

## 7. Research and the knowledge economy

### 7.1 Per-player knowledge model
`common/lib/research/PlayerKnowledge`: completed research keys;
`aspectsDiscovered` (an AspectList doubling as the **aspect pool** —
spendable knowledge points per aspect); scanned objects/entities/
phenomena; warp counters. The six primals are auto-seeded whenever the
discovered list is empty. `addAspectPool` clamps at 0
(`PlayerKnowledge.java:104–116`). **[verified]**

### 7.2 Earning knowledge (scanning)
The Thaumometer (25-tick use, 10-block pointed-entity range,
`common/items/relics/ItemThaumometer.java:56, 64`) scans items, entities,
nodes, and blocks. First discovery of an aspect yields `amount + 2` pool
points; above `Config.aspectTotalCap` (100, `Config.java:129`) gains
decay as `sqrt(remaining)`; above 1.25×cap they are flat 1
(`common/lib/research/ScanManager.java:474–505`). Mana beans add +1 to a
random aspect pool (`common/items/ItemManaBean.java`). Each scan also
runs `createClue`: any research whose item/entity/aspect triggers match
gets a research note (clue) generated for the player
(`common/lib/research/ResearchManager.java:175–204`). **[verified]**

### 7.3 Spending knowledge: the research table
`common/tiles/TileResearchTable.java`:
- Slot 0 scribe tools (one ink consumed per hex operation, line 127),
  slot 1 the note.
- Note geometry: hex grid radius `1 + min(3, complexity)`; the outer ring
  holds the research's tag aspects ("main" hexes).
- Placing an aspect hex costs 1 pool point; if the pool is empty it
  spends the table's `bonusAspects` instead (environmental accumulation
  from surrounding lava/air/altitude/etc., recalculated every 600 ticks,
  lines 98, 193–221). With RESEARCHER2, 10% of placements are free
  (line 137). Removing a placed hex refunds the pool point with
  probability 25% (RESEARCHER1) or 50% (RESEARCHER2) (lines 158–163). **[verified]**
- A note completes when every main hex is connected through adjacency
  where adjacent aspects share a parent/child relation (compound next to
  one of its components) and both are discovered. **[verified]**

### 7.4 Completing research
Direct completion from the Thaumonomicon (`PacketPlayerCompleteToServer`
type 0, `common/lib/network/playerdata/PacketPlayerCompleteToServer.java:47–85`):
requires all parent research, then **deducts the research's tag aspects
from the player's aspect pool** (clamped at 0 — no sufficiency check) and
completes; `siblings` auto-complete. Type 1 requests a note for that
research. `completeResearch` applies warp costs where the research
declares them: half permanent, half sticky
(`ResearchManager.java:593–615`). **[verified]**

### 7.5 Auto-unlock and crafting triggers
Research flagged auto-unlock completes on player join
(`common/lib/events/EventHandlerEntity.java:140–175`); crafting an item
that some hidden research triggers calls `findHiddenResearch`
(`EventHandlerEntity.java:121`, `ResearchManager.java:121`). **[verified]**

### 7.6 Gating
Crucible, infusion, and arcane recipes carry a research key checked
against the acting player (§5.4, §6.1); research visibility itself is
gated by parents and by the player's discovered aspects
(`ResearchItem.tags`). **[verified]**

→ Modern: the pool economy (scan→earn, spend-per-hex, pay-tags-to-
complete) is the distinctive part of TC4 progression; preserve the
currency model, simplify the hex puzzle if desired (it is presentation,
not mechanics).

---

## 8. Warp

Three counters per player — permanent, sticky, temporary — plus a
pending `warpCounter`. `addWarp(amount, …)` raises the chosen counters and
sets `warpCounter` to the new total (`common/Thaumcraft.java`,
`common/lib/WarpEvents.java`). While `warpCounter > 0`, each check fires
a warp event with probability `sqrt(counter)/100` and decays the counter;
events are drawn from a fixed effect list (vis drain, mob spawns,
sensory effects, damage). **[verified for the counter/probability model;
the full effect list is inventoried in MECHANICS_CATALOG.md §3]**

Sources: research with a warp cost (½ permanent + ½ sticky,
`ResearchManager.java:593–615`); crafting items registered with a warp
cost (sticky, `common/lib/events/EventHandlerWorld.java:118`); infusion
failure events; eldritch interactions; eating zombie brains. **[verified call sites]**

→ Modern: keep the three-counter split (permanent = story cost, sticky =
decaying debt, temporary = session cost) — it is what makes warp
legible. Event pool is content.

---

## 9. Auxiliary loops (brief)

- **Thaumatorium** (`common/tiles/TileThaumatorium.java`): automated
  crucible. A recipe is set on it; it pulls the recipe's essentia from
  its own container (fed by essentia tubes, lines 70, 316), needs heat
  like the crucible (line 162), and crafts automatically when the pool
  is sufficient (line 167). **[verified]**
- **Essentia tubes** (`common/tiles/TileTube.java:133–163`): the tube
  network propagates "suction" from consumers (`getSuctionType` /
  `getRequiredEssentia`); essentia flows from containers toward suction;
  filtered tubes restrict the aspect. **[verified for the suction model;
  flow-rate details not traced]**
- **Taint spread** (`common/blocks/BlockTaintFibres.java:174, 198–202`):
  fibre blocks tick and spread; terrain conversion rate scales with
  `Config.taintSpreadRate` (default 200, `Config.java:87`). Biome
  conversion is driven by TAINTED nodes (§2.4), not fibres. **[verified]**
- **Magic vs arcane workbench**: the magic workbench is plain crafting
  plus a wand slot and charger; the arcane workbench charges vis via the
  wand in slot 10 (§4.4). **[verified]**

---

## 10. Constants appendix (defaults, for tuning parity)

| Constant | Value | Source |
|---|---|---|
| node worldgen chance | 1/36 per chunk | Config.java:123 |
| special node chance | 1/18 (30/30/30/10 split) | Config.java:124 |
| biome aura default | 100 | BiomeHandler.java:32 |
| node recharge interval | 600t (400 BRIGHT / 900 PALE / 0 FADING) | TileNode.java:579+ |
| node degradation interval | 1200t | TileNode.java:660+ |
| node discharge interval | 2t (1 BRIGHT / 3 PALE) | TileNode.java:720+ |
| wand tap rate | 1/tick, +1 per NODETAPPER1/2, every 5t | TileNode.java:374+ |
| aspect knowledge cap | 100 (sqrt decay above) | Config.java:129, ScanManager.java:474+ |
| wand capacity | rod ×100 raw units (staff rod ×150) | ItemWandCasting.java:82 |
| cap cost multipliers | 1.1/1.1/1.0/1.0/0.9/0.8 | ConfigItems.java:287–335 |
| rod capacities | 25/50/75/100 (staff 125/175/250) | ConfigItems.java:287–309 |
| crucible heat | 0–200, active >150, +1+2×bellows | TileCrucible.java:85–94 |
| crucible water | 1000 mB; craft −50 mB; decay −2 mB | TileCrucible.java:115, 284 |
| crucible aspect cap | 100 total vis | TileCrucible.java:37 |
| crucible decay onset | counter>100 && heat>150 | TileCrucible.java:106 |
| infusion cycle | 10t (20t during consume) | TileInfusionMatrix.java:73 |
| infusion failure roll | rand(500) ≤ instability | TileInfusionMatrix.java:346 |
| infusion instability cap | 25 | TileInfusionMatrix.java:451 |
| infusion essentia range | 12 | TileInfusionMatrix.java:467 |
| symmetry penalties | +2 unpaired pedestal; ±0.1 stabilizer | TileInfusionMatrix.java:700–790 |
| vis relay / energized node range | 8 | TileVisRelay.java:55, TileNodeEnergized.java:156 |
| workbench charger refill | 5/aspect/tick | TileMagicWorkbenchCharger.java:41 |
| taint spread rate | 200 | Config.java:87 |
| warp event probability | sqrt(counter)/100 per check | WarpEvents.java |

---

## 11. Corrections and completions to MECHANICS_CATALOG.md

1. **Natural nodes are not vis-network sources** — only energized nodes
   and relays register (`VisNetHandler.addSource` callers; `TileNode`
   does not extend `TileVisNode`). The catalog's §2 could be read as
   implying nodes feed the network. **[verified]**
2. **TileNodeConverter tick logic** (drain 1/t for 1000t, then convert;
   reverse mode) — closes the catalog's `[inferred]` marker. **[verified]**
3. **Wand vis is stored in hundredths** (§4.1) — critical for any
   reimplementation that mixes wand storage with recipe costs. **[verified]**
4. **Crucible idle decomposition** (§5.3) — the pool is perishable and
   compounds decay to primals; not stated in the catalog. **[verified]**
5. **`taintSpreadRate` default is 200** (`Config.java:87`). **[verified]**

## 12. What the first playable loop needs (TL-009 mapping)

MVP per this spec: §1 (aspect graph + item tags), §2.1/2.2/2.5 (nodes,
worldgen, tapping), §4.1–4.3 (wand storage + cost model), §7.2/7.6
(scanning → knowledge → gating). §5–§6 (crucible, infusion) are the next
tier; §3 (vis network) follows with machines; §8 (warp) can land with
research costs. This matches the TL-009 "Vis Loop" scope (wand + vis
component + Vis Tap) and extends it.
