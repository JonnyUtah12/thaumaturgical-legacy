# Thaumcraft 4.2.3.5 Mechanics Catalog — TL-007

Source: Vineflower-decompiled sources of `Thaumcraft-1.7.10-4.2.3.5.jar`
(859 `.java`, kept read-only at `~/.hermes/cache/scratch/decompiled/thaumcraft`,
never committed). Companion inventories: `THAUMCRAFT_INVENTORY.md` (TL-003),
`BAUBLES_INVENTORY.md` (TL-004).

Confidence tags (same as the inventories): **[verified]** = read directly from
decompiled code or artifact resources; **[inferred]** = behavior deduced from
code reading without execution; **[uncertain]** = needs a runtime check.
Nothing here was executed — all claims are static analysis.

Priority markers for the reimplementation: **[MVP]** = required for a first
playable loop (discover → scan → research → craft → cast); **[LATER]** =
content that can follow once the loop works.

---

## 1. Aspects and essentia [MVP]

**What it is.** The data model under everything else. An `Aspect` is a tagged
token with optional two-component composition; 48 aspects ship as static
constants in `thaumcraft.api.aspects.Aspect`: 6 primal (`aer`, `terra`,
`ignis`, `aqua`, `ordo`, `perditio`) and 42 compounds defined by their
component pair (e.g. `MAGIC = new Aspect("praecantatio", …, {VOID, ENERGY})`).
`isPrimal()` is literally "components == null or length != 2". Aspects
self-register into a static `LinkedHashMap aspects` keyed by tag; icons are
derived by convention from `textures/aspects/<tag>.png` (50 icons in the jar).

**Evidence.** `api/aspects/Aspect.java`, `AspectList` (counted multiset with
`add/reduce/visSize`), `IAspectContainer` (tiles holding aspects),
`IAspectSource`, `IEssentiaTransport`, `IEssentiaContainerItem`,
`AspectSourceHelper`. Item/entity essentia maps live in `ThaumcraftApi`:
`objectTags` (ConcurrentHashMap filled by `registerObjectTag` /
`registerComplexObjectTag`, oreDict-aware), `scanEntities`
(`registerEntityTag` with optional `EntityTagsNBT` predicates). For items
with no registered tag, `common/lib/crafting/ThaumcraftCraftingManager.generateTags`
derives aspects recursively from vanilla/crucible/arcane/infusion recipes
(`generateTagsFromCrucibleRecipes` etc., with a `history` list guarding
recursion). **[verified]**

**Player-progression role.** Players *discover* aspects (Thaumometer scans,
crucible drops, mob kills) and compound them by hand in the Thaumonomicon
(`ResearchManager.getCombinationResult`, `PacketAspectCombinationToServer`).
Discovered aspects gate research visibility (`ResearchItem.tags`) and crucible
recipe matching. The six primal aspects are auto-granted on first access:
`PlayerKnowledge.getAspectsDiscovered` seeds them whenever the list size ≤ 6.
**[verified]**

**Persistence.** Per-player `aspectsDiscovered` AspectList (including the
"aspect pool" knowledge counter used by the research table puzzle,
`addAspectPool`/`getAspectPoolFor`) — saved by `ResearchManager.saveAspectNBT`
into the player sidecar file and entity NBT. The aspect *registry* itself is
code, not data. **[verified]**

**Client/server.** Server authoritative; client mirror synced by
`PacketSyncAspects`, `PacketAspectDiscovery`, `PacketAspectPool`; player
combination attempts go up via `PacketAspectCombinationToServer`. **[verified]**

---

## 2. Vis, nodes, and the vis network [MVP]

**What it is.** Vis is ambient magical flux stored in world **nodes**.
`common/tiles/TileNode extends TileThaumcraft implements INode, IWandable`
holds two AspectLists: `aspectsBase` (the node's capacity) and `aspects`
(current pool, regenerated over time). Node kinds: `NodeType` = NORMAL,
UNSTABLE, DARK, TAINTED, HUNGRY, PURE; `NodeModifier` = BRIGHT, PALE, FADING
(saved as ordinal bytes under NBT keys `type`/`modifier`). Nodes are invisible
unless the player wears/holds an `IRevealer` — `ItemGoggles` implements it
(also `IGoggles`, `IVisDiscountGear`, `IRunicArmor`). **[verified]**

**Vis network.** `api/visnet/VisNetHandler` keeps a per-dimension registry of
`TileVisNode` sources and a `nearbyNodes` cache; `drainVis(world,x,y,z,aspect,amount)`
walks reachable nodes calling `TileVisNode.consumeVis` until the cost is met.
`TileVisRelay` extends a network; `TileNodeStabilizer` raises a node's
`lock` (slows fading/destabilizing); `TileNodeConverter` converts node types
**[inferred — class names + config, tick logic not fully traced]**;
`TileJarNode` + `ItemJarNode` capture a node (type, modifier, aspects) into
item NBT and redeploy it. **[verified for the drain/registry code]**

**Wand recharging.** Right-clicking a node binds the wand to it
(`ItemWandCasting.setObjectInUse`, NBT keys `IIUX/IIUY/IIUZ`); while held,
`onUsingTick` → `TileNode.onUsingWandTick` transfers vis into the wand every
5 ticks, tap rate +1 each for research keys `NODETAPPER1`/`NODETAPPER2`.
**[verified]**

**Devices that drain vis directly** via `VisNetHandler.drainVis`:
`TileArcaneFurnace` (Fire), `TileAlchemyFurnaceAdvanced` (Fire/Entropy/Water),
`TileArcaneBore` (Entropy), `TileEssentiaCrystalizer`, `TileMirror` (Air),
`TileFluxScrubber` (Air), `TileFocalManipulator`. **[verified call sites]**

**Persistence.** Node state is tile NBT (`writeToNBT`/`readFromNBT` in
`TileNode`); pools regenerate and modifiers degrade (PALE→FADING, FADING→PALE
recovery scaled by `lock`) in `TileNode` tick handlers (`handleNodeStability`
etc.). Worldgen knobs: `Config.genAura`, `nodeRarity`, `nodeRefresh`,
`hardNode`. **[verified]**

**Client/server.** Node simulation server-side; visibility/reveal and beam
effects client (`RenderEventHandler`, `PacketFXVisDrain`). **[verified split,
inferred effect wiring]**

---

## 3. Research, scanning, and warp [MVP]

**What it is.** Progression is a per-player unlock graph. `api/research/ResearchItem`
(key, category, aspect `tags`, grid col/row, complexity 1–3, `parents` /
`parentsHidden` / `siblings`, hidden/special flags) + `ResearchPage`
(text/crafting pages) registered into `ResearchCategories`. All content is
**code-registered at postInit**: `common/config/ConfigResearch.init()` calls
`initCategories/initBasicResearch/initThaumaturgyResearch/initAlchemyResearch/
initArtificeResearch/initGolemancyResearch/initEldritchResearch` — 6
categories (BASICS, THAUMATURGY, ALCHEMY, ARTIFICE, GOLEMANCY, ELDRITCH) and
201 `new ResearchItem(...)` constructions; there are no research data files in
the jar. **[verified]**

**Unlock flow.** `ItemThaumonomicon` opens the research browser
(`client/gui/GuiResearchBrowser`, `GuiResearchPopup`, `GuiResearchRecipe`;
`MappingThread` lays out the graph client-side). Unlocking a research note
happens at `TileResearchTable` (+ `ContainerResearchTable`,
`GuiResearchTable`): a blank note (`ItemResearchNotes`) plus ink
(`ItemInkwell`, `IScribeTools` penalty) is processed by
`ResearchManager.createNote`, which hex-encodes an aspect puzzle sized by the
research's aspect cost (`ResearchNoteData`, `HexEntry`); the player places
the matching aspects on the table and `checkResearchCompletion` validates it,
consuming ink (`consumeInkFromTable`). Hidden research auto-fires via
`findHiddenResearch` on scans/crafts; aspect combinations unlock research via
`findMatchingResearch(player, aspect)`. **[verified]**

**Scanning.** `ScanManager implements IScanEventHandler` (registered in
preInit via `ThaumcraftApi.registerScanEventhandler`) backs the
`ItemThaumometer`: `scanEntities` matching with NBT predicates
(`generateEntityHash`), plus `scanPhenomena`. Results recorded in
`PlayerKnowledge.objectsScanned/entitiesScanned/phenomenaScanned`;
`PacketScannedToServer` up, `PacketSyncScanned*` down. **[verified]**

**Warp.** A per-player debt with three buckets — `warp` (permanent),
`warpSticky` (from crafting registered warp items; `EventHandlerWorld.onCrafting`
calls `Thaumcraft.addStickyWarpToPlayer` when `ThaumcraftApi.getWarp(crafting) > 0`),
`warpTemp` — plus a `warpCount` decay counter. Registration entry points:
`ThaumcraftApi.addWarpToResearch/addWarpToItem`. Consequences:
`WarpEvents.checkWarpEvent` rolls every 2000 player ticks
(`EventHandlerEntity.playerTick`, `player.ticksExisted % 2000 == 0`, skipped
under `Config.wuss` or with the Warp Ward potion): probability scales with
warp, outcomes are potion effects (vis exhaust, thaumarhia, un-hunger, sun
scorned, blurred vision…), chat messages, and mist spawning;
`checkDeathGaze` also fires from the tick handler. Gear warp from
`IWarpingGear`/runic gear is added in `WarpEvents.getWarpFromGear`. **[verified]**

**Persistence.** `PlayerKnowledge` is an in-memory set of username-keyed maps;
`ResearchManager.savePlayerData/loadPlayerData` write a compressed NBT sidecar
file per player plus a backup (`EventHandlerEntity` hooks `PlayerEvent.SaveToFile/
LoadFromFile`, backup tag `thaumback`), and mirror the same state into the
player entity NBT (`saveResearchNBT/saveAspectNBT/saveScannedNBT`, keys
`Thaumcraft.shielding`, `Thaumcraft.eldritch[.temp/.sticky/.counter]`).
Username-keyed storage is a 1.7.10 idiom; the modern design should use
data attachments (see `docs/REVERSE_ENGINEERING_POLICY.md`). **[verified]**

**Client/server.** Server authoritative; full sync on login
(`PacketSyncResearch`, `PacketSyncAspects`, `PacketSyncScannedItems/Entities/Phenomena`,
`PacketSyncWarp`), completion via `PacketResearchComplete` /
`PacketPlayerCompleteToServer`. All research UI is client-only. **[verified]**

---

## 4. Wands, sceptres, staffs, foci [MVP]

**What it is.** `common/items/wands/ItemWandCasting` is one item class; the
look and stats come from three registries: `api/wands/WandCap.caps`,
`WandRod.rods` (and `StaffRod`), all static `LinkedHashMap`s populated in
`ConfigItems.init()` (caps: iron/copper/silver/gold/thaumium/void; rods:
wood/greatwood/obsidian/blaze/ice/quartz/bone/reed/silverwood + staff
variants). The wand ItemStack NBT stores the cap/rod keys and an embedded
`focus` stack (`setCap/setRod/setFocus`). **[verified]**

**Foci.** `api/wands/ItemFocusBasic` + `IWandFocus` define the callback
surface: `getVisCost` (AspectList), `isVisCostPerTick`,
`onFocusRightClick`, `onUsingFocusTick`, `onPlayerStoppedUsingFocus`,
`onFocusBlockStartBreak`, `getActivationCooldown`, plus `FocusUpgradeType`
upgrades gated by rod rank (potency/frugal/enlarge/extend/treasure, applied
at `TileFocalManipulator`). Ten shipped foci in `common/items/wands/foci`:
Fire, Frost, Shock, Hellbat, Excavation, PortableHole, Primal, Trade, Pech,
Warding. **[verified]**

**Cost model.** `ItemWandCasting.getConsumptionModifier` = cap
base/special modifier − `WandManager.getTotalVisDiscount` (sums
`IVisDiscountGear.getVisDiscount` over inventory **and** bauble slots) −
frugal upgrade/10 − 0.1 for sceptres, floored at 0.1. Charges are stored
per-aspect in wand NBT (`getVis/storeVis`). **[verified]**

**Interaction plumbing.** `WandManager implements IWandTriggerManager`
dispatches wand right-clicks through `WandTriggerRegistry.performTrigger`
(block/meta/modid-keyed trigger table, also how addons bind wand actions),
manages per-player activation cooldowns, and exposes
`consumeVisFromInventory` (shared by runic shielding, wand-repair via
`IRepairableExtended`, and arcane crafting). `IWandable` tiles (nodes, tubes,
reservoirs, bores, relays) implement `onWandRightClick`/`onUsingWandTick` for
wand-as-tool behavior. `WandManager.createCrucible/createInfusionAltar/
createThaumatorium/createArcaneFurnace/createNodeJar/createThaumonomicon`
assemble multi-block devices by right-clicking the item in-world. **[verified]**

**Persistence.** Wand vis charges, cap/rod keys, focus stack, and bound-node
coords all in item NBT. **[verified]**

**Client/server.** Activation logic server-side; focus switching via keybind
sends `PacketItemKeyToServer`/`PacketFocusChangeToServer`; beam/hit effects
via the `PacketFX*` family. **[verified]**

---

## 5. Arcane workbench, crucible, infusion [MVP for arcane+crucible; LATER for infusion]

**Arcane workbench [MVP].** `TileArcaneWorkbench`/`TileMagicWorkbench` +
`ContainerArcaneWorkbench`. Recipes are `api/crafting/ShapedArcaneRecipe` /
`ShapelessArcaneRecipe`: a vanilla shaped/shapeless grid **plus** an aspect
cost and a research key gate. On craft,
`SlotCraftingArcaneWorkbench.onCraft` asks
`ThaumcraftCraftingManager.findMatchingArcaneRecipeAspects` and consumes the
cost from the wand sitting in the extra matrix slot (slot 10,
`SlotLimitedByWand`) via `consumeVisFromWandCrafting`. **[verified call
sites; exact slot semantics inferred from the container code]**

**Crucible / alchemy [MVP].** `TileCrucible` is an `IAspectContainer` with a
Forge fluid tank (water). Thrown items dump their essentia into the crucible
(`getTags` from `ThaumcraftCraftingManager.getObjectTags`);
`ThaumcraftCraftingManager.findMatchingCrucibleRecipe(username, aspects, lastItem)`
returns the first `CrucibleRecipe` whose research is complete and whose
`matches(AspectList, catalyst)` holds (catalyst item/oredict + aspect
subset); success drains 50 mB water and `removeMatching` subtracts the
aspects. `TileThaumatorium` (+container/GUI) is the crystal-powered
no-water upgrade. **[verified]**

**Infusion matrix [LATER].** `TileInfusionMatrix` (1019 lines) with
`TileInfusionPillar` ×4, `TilePedestal` for components, optional
`TileInfusionAltar`. `api/crafting/InfusionRecipe` carries research key,
ingredient list, aspect cost, `instability`, XP; `InfusionEnchantmentRecipe`
enchant-at-the-matrix. Mechanics read from the tile: a **symmetry** score is
computed from pillars and `IInfusionStabiliser` blocks around the matrix
(`+2` per matching side); `instability = symmetry-deficit + recipe
instability`, capped at 25, and while > 0 a random failure roll
(`random.nextInt(500) <= instability`) aborts the craft. The recipe's
essentia cost is paid one unit at a time via
`EssentiaHandler.drainEssentia(this, aspect, UNKNOWN, 12)` — i.e. from
essentia networks within 12 blocks (reservoirs/tubes), **not** from raw vis;
failed draws raise instability. Matrix crafting state (recipeEssentia,
instability, ingredients) is in tile NBT. **[verified]**

**Registration.** All recipes are registered in code: `ConfigRecipes.init()`
and `ConfigResearch.init()` at postInit; addons use `ThaumcraftApi.addArcaneCraftingRecipe/
addCrucibleRecipe/addInfusionCraftingRecipe/addInfusionEnchantmentRecipe`.
`ThaumcraftApi.getCraftingRecipeKey` powers "what recipe made this" research
pages. **[verified]**

---

## 6. Essentia logistics and devices [LATER]

**What it is.** Essentia = aspects as a flowable fluid. The contract is
`api/aspects/IEssentiaTransport`: suction-based (`setSuction`,
`getSuctionType/Amount`, `getMinimumSuction`) push/pull between connectable
faces (`isConnectable`, `canInputFrom/canOutputTo`, `takeEssentia/addEssentia`).
`common/lib/events/EssentiaHandler.drainEssentia/findEssentia` searches
networks within a range and records `EssentiaSourceFX` for the client beam.

**Device set** (`common/tiles`, all present **[verified]**; individual tick
behavior **[inferred]** unless noted): `TileTube` + `TileTubeBuffer/Filter/
Oneway/Restrict/Valve` (logistics), `TileEssentiaReservoir` (bulk store,
wand-tappable via `onUsingWandTick`), `TileAlembic` (crucible essentia →
usable essentia), `TileEssentiaCrystalizer` (essentia → crystals, pays vis
via `VisNetHandler.drainVis`), `TileMirror`/`TileMirrorEssentia` (paired
long-range transport, vis-drained), `TileDeconstructionTable` (item →
essentia), `TileSensor` (essentia/vis detection), `TileCentrifuge`,
`TileSpa`, `TileWandPedestal`, `TileWardingStone*` (chunk warding, with
`ServerTickEventsFML.RestorableWardedBlock` bookkeeping).
`IEssentiaContainerItem` covers portable essentia items (jars, crystal
essence).

**Persistence.** Contents in tile NBT; mirrors are paired by coordinates in
NBT **[inferred]**.

**Client/server.** Suction/transfer logic server-side; beams/particles via
`PacketFXEssentiaSource`/`PacketFXSourceStream`-style FX packets and
`EssentiaHandler.sourceFX`. **[verified pattern]**

---

## 7. Baubles and equipment [LATER]

**What it is.** TC ships six baubles in `common/items/baubles`:
`ItemAmuletRunic`/`ItemRingRunic`/`ItemGirdleRunic` (the runic armor set,
`IRunicArmor`), `ItemAmuletVis` (`IVisDiscountGear`), `ItemGirdleHover`
(hover/step assist, backed by `common/items/armor/Hover` +
`ContainerHoverHarness`), `ItemBaubleBlanks` (crafting material), plus
`ItemFocusPouchBauble` (focus pouch worn as a bauble). They implement the
Baubles `IBauble` contract (see `BAUBLES_INVENTORY.md` for the 4-slot model
this version targets). **[verified]**

**Integration points (13 source files import `baubles.*`):**
`WandManager.getTotalVisDiscount` and `EventHandlerRunic` iterate
`BaublesApi.getBaubles(player)`; `WarpEvents.getWarpFromGear` counts
`IWarpingGear` in bauble slots. **[verified]**

**Runic shielding.** `EventHandlerRunic` keeps `runicCharge` (entityId →
charge). On `LivingUpdateEvent` it sums `IRunicArmor` pieces (armor **and**
baubles), recharges by paying `Config.shieldCost` of Air+Earth vis through
`WandManager.consumeVisFromInventory` on a `Config.shieldRecharge`/
`shieldWait` timer, and on `LivingHurtEvent` absorbs damage against charge
(`IRunicArmor.getRunicAbsorb` per piece). Charge persists to the player
sidecar (`Thaumcraft.shielding` NBT key). **[verified]**

**Persistence.** The bauble inventory itself belongs to the Baubles mod
(per-player `baub` files); TC only persists derived state (shield charge).
**[verified]**

**Client/server.** Equip/tick logic server-side; tooltip charge display is
client (`ItemTooltipEvent` handler in `EventHandlerRunic`). **[verified]**

---

## 8. Worldgen, biomes, taint, eldritch realm [LATER except nodes/ores for MVP]

**Worldgen.** `common/lib/world/ThaumcraftWorldGenerator implements
IWorldGenerator`, registered with priority 0 in preInit. Per chunk it can
generate: aura nodes (`createRandomNodeAt`, gated by `Config.genAura` and
`Config.nodeRarity`), ores via `generateOres` (cinnabar `BlockCustomOre`,
amber, infused stone — each behind `Config.genCinnibar/genAmber/
genInfusedStone`), greatwood/silverwood trees (`WorldGenGreatwoodTrees`,
`WorldGenSilverwoodTrees`), mounds, hilltop stones, mana pods, eldritch
rings, custom flowers, big magic trees. IMC `dimensionBlacklist`/
`biomeBlacklist` restrict TC content by dimension/biome/Y. **[verified]**

**Structures/villagers.** `ComponentWizardTower`/`ComponentBankerHome`
registered with `MapGenStructureIO` as `TCVillageTower`/`TCVillageBanker`;
`VillageWizardManager`/`VillageBankerManager` add wizard and banker
villagers (trades + house generation). **[verified]**

**Biomes.** `BiomeGenTaint`, `BiomeGenMagicalForest`, `BiomeGenEerie`,
`BiomeGenEldritch` + `BiomeHandler`, default IDs 192–195 (`Config`). **[verified]**

**Taint.** A spreading corruption: `BlockTaint` + `BlockTaintFibres`
(`spreadFibres`, `taintBiomeSpread` — converts the biome at a column via
`Utils.setBiomeAt`, rate knob `Config.taintSpreadRate`), `EntityFallingTaint`,
taint mobs, `ItemBottleTaint`. TAINTED nodes actively convert nearby biomes
every 50 ticks and spread fibres when `Config.hardNode`; ordinary nodes
standing in a taint biome become TAINTED (1/500 per 100 ticks) —
`TileNode.handleTaintNode`. Flux gas becomes taint under `Config.taintFromFlux`.
**[verified]**

**Eldritch outer realm.** `WorldProviderOuter` (default dim id −42,
registered in `init`), `ChunkProviderOuter`, and an async maze:
`MazeGenerator`/`MazeHandler`/`MazeThread` assemble rooms
(`Gen2x2`, `GenBossRoom`, `GenKeyRoom`, `GenLibraryRoom`, `GenNestRoom`,
`GenPassage`, `GenPortal`) with `MapBossData` tracking the boss; entry via
`TileEldritchPortal` + `TeleporterThaumcraft`; `WorldGenEldritchRing` places
gateways in the overworld. **[verified presence + registration; room
behavior inferred]**

**Persistence.** Standard chunk/tile NBT; biome overrides persist through
Forge's per-chunk biome storage **[uncertain — 1.7.10 biome-overwrite
mechanism needs a runtime check]**; maze layout regenerated deterministically
by `MazeHandler` **[inferred]**.

---

## 9. Entities [LATER]

Golems (`EntityGolemBase`, programming via `ItemGolemCore` + `ItemGolemBell`,
`EntityTravelingTrunk` pack animal, `ContainerGolem` GUI), `EntityPech`
(trade mob, `ContainerPech`), wisps/fairies (`common/entities/monster`),
`EntityAspectOrb` (thrown by UNSTABLE nodes), taint mobs, champion mob
modifiers (`ConfigEntities.championModWhitelist`, IMC `championWhiteList`).
Entity essentia comes from `ScanManager.generateEntityAspects` +
`ThaumcraftApi.registerEntityTag`. **[verified presence; behavior inferred]**

---

## First playable loop vs. later

The canonical TC loop is: **get the Thaumonomicon → craft Thaumometer +
Goggles + a wand → find and tap a vis node → scan the world → unlock research
at a research table → craft at the arcane workbench → alchemy in the crucible
→ thaumium gear → (then) infusion and everything else.**

Essential for a first playable loop **[MVP]**:
aspects/essentia model (§1), nodes + reveal + wand recharge + `drainVis`
(§2), research graph + notes/table + scanning + basic warp (§3), wands with
cap/rod/focus registries and the cost model (§4), arcane workbench and
crucible crafting (§5), worldgen of nodes + basic ores (§8), and the
Baubles slot contract only if runic gear ships in the first slice (§7).

Deliberately **[LATER]**: infusion matrix + pillars/stabilizers, essentia
logistics (tubes/reservoir/alembic/crystalizer/mirrors), thaumatorium,
deconstruction, golemancy, eldritch realm + maze, taint spread + biomes,
runic shielding + baubles set, node jars/stabilizers/converters/relays,
focal manipulator upgrades, villagers, champion mobs, arcane bore, spa,
magic box, and the IMC compatibility surface.
