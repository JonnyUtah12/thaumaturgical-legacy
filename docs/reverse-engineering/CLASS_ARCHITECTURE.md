# Thaumcraft 4.2.3.5 Class Architecture — TL-007

Static analysis of the Vineflower-decompiled sources (read-only at
`~/.hermes/cache/scratch/decompiled/thaumcraft`, never committed). Confidence:
**[verified]** = read from decompiled code; **[inferred]** = deduced, not
executed. Note: decompiled bodies contain MCP/SRG names (`func_70005_c_`,
`field_73011_w`, …) — these are obfuscation mappings of Minecraft methods,
not Thaumcraft's real names.

## Package layout [verified]

| Package | Contents |
| --- | --- |
| `thaumcraft.common` | `Thaumcraft` (@Mod entry), `CommonProxy` |
| `thaumcraft.api` | public API: `ThaumcraftApi`, `ThaumcraftApiHelper`, `ItemApi`, `WorldCoordinates`, marker interfaces (`IGoggles`, `IScribeTools`, `IRunicArmor`, `IVisDiscountGear`, `IWarpingGear`, `IArchitect`, `IRepairable*`) |
| `thaumcraft.api.aspects` | `Aspect`, `AspectList`, `IAspectContainer`, `IAspectSource`, `IEssentiaTransport`, `IEssentiaContainerItem` |
| `thaumcraft.api.nodes` / `api.visnet` | `INode`, `NodeType`, `NodeModifier`, `IRevealer`; `TileVisNode`, `VisNetHandler` |
| `thaumcraft.api.research` | `ResearchItem`, `ResearchPage`, `ResearchCategories`, `IScanEventHandler`, `ScanResult` |
| `thaumcraft.api.crafting` | `ShapedArcaneRecipe`, `ShapelessArcaneRecipe`, `CrucibleRecipe`, `InfusionRecipe`, `InfusionEnchantmentRecipe`, `IArcaneRecipe`, `IInfusionStabiliser` |
| `thaumcraft.api.wands` | `WandCap`, `WandRod`, `StaffRod`, `ItemFocusBasic`, `IWandFocus`, `IWandable`, `WandTriggerRegistry`, `FocusUpgradeType` |
| `thaumcraft.api.internal` | `IInternalMethodHandler` — API→impl indirection |
| `thaumcraft.common.config` | `Config` + `ConfigBlocks/Items/Entities/Recipes/Research/Aspects` (id registries + content registration), `MaterialTaint/MaterialAiry` |
| `thaumcraft.common.blocks` / `items` / `tiles` | content: blocks, items (incl. `wands/`, `baubles/`, `armor/`, `equipment/`, `relics/`), ~80 tile devices |
| `thaumcraft.common.container` | server-side containers + custom slots (`SlotCraftingArcaneWorkbench`, `SlotLimitedByWand`, ghost slots) |
| `thaumcraft.common.entities` | golems, monsters, projectiles, AI tasks |
| `thaumcraft.common.lib` | `events/` (global handlers), `network/` (packets), `research/` (`ResearchManager`, `PlayerKnowledge`, `ScanManager`), `crafting/` (`ThaumcraftCraftingManager`), `world/` (worldgen, biomes, `dim/` outer realm), `utils/`, `potions/`, `enchantment/` |
| `thaumcraft.client` | `ClientProxy`, `gui/` (Thaumonomicon browser, device GUIs, `ThaumcraftGuiConfig`), `renderers/`, `fx/`, `lib/RenderEventHandler` |
| `thaumcraft.codechicken` | relocated CodeChicken core/lib (DepLoader + math/vec/raytracer/colour/render helpers) |
| `truetyper` | bundled font library (5 classes) |

The API/impl split is the addon contract: `ThaumcraftApi.internalMethods`
(defaults to `DummyInternalMethodHandler`, replaced in preInit by
`common/lib/InternalMethodHandler`) routes API calls like
`consumeVisFromWand`/`addWarpToPlayer` into the implementation. **[verified]**

## Mod entry and init flow [verified]

`@Mod(modid="Thaumcraft", version="4.2.3.5", guiFactory="thaumcraft.client.ThaumcraftGuiFactory",
dependencies="required-after:Forge@[10.13.2,);required-after:Baubles@[1.0.1.10,)")`
with `@SidedProxy(clientSide="thaumcraft.client.ClientProxy", serverSide="thaumcraft.common.CommonProxy")`.
The jar manifest additionally declares `FMLCorePlugin: thaumcraft.codechicken.core.launch.DepLoader`
— a coremod that auto-downloads a missing Baubles (see DEPENDENCY_GRAPH.md).

`preInit` (order as written in `Thaumcraft.preInit`):
1. `Config.initialize(suggestedConfigurationFile)` + `Config.save()`.
2. `ThaumcraftApi.internalMethods = new InternalMethodHandler()`.
3. Construct `PlayerKnowledge` + `ResearchManager` (held by the proxy),
   `EventHandlerWorld`, `ServerTickEventsFML`, `EventHandlerEntity`,
   `EventHandlerRunic`, `RenderEventHandler`, `EventHandlerNetwork`.
4. `PacketHandler.init()` (39 packet registrations on channel `thaumcraft`).
5. Register handlers on `MinecraftForge.EVENT_BUS`, `TERRAIN_GEN_BUS`,
   `FMLCommonHandler.instance().bus()`; `GameRegistry.registerFuelHandler(worldEventHandler)`.
6. `GameRegistry.registerWorldGenerator(new ThaumcraftWorldGenerator(), 0)`;
   `ThaumcraftApi.registerScanEventhandler(new ScanManager())`.
7. `ConfigBlocks.init()` / `ConfigItems.init()` — block/item registration
   through numeric-id config classes (1.7.10 pattern).
8. `MapGenStructureIO` registration of `TCVillageTower`/`TCVillageBanker`.
9. `proxy.registerHandlers()`, `worldGen.initialize()`, `Config.registerBiomes()`.

`init`: `ConfigEntities.init()` (entity ids), dispenser behavior
(`BehaviorDispenseAlumetum`), `NetworkRegistry.registerGuiHandler(instance, proxy)`,
villager ids + trade/house handlers (wizard, banker), `proxy.registerKeyBindings()`,
`DimensionManager.registerProviderType/registerDimension(Config.dimensionOuterId, WorldProviderOuter)`.

`postInit`: `Config.initPotions`, `ConfigEntities.initEntitySpawns`,
`Config.initModCompatibility`, `ConfigItems.postInit`, `ConfigRecipes.init`,
`ConfigAspects.init`, `ConfigResearch.init` (all research + gated recipes),
`Config.initLoot`, `Config.initMisc`, then an IMC sweep
(`harvestStandardCrop/ClickableCrop/StackedCrop`, `nativeCluster`,
`lampBlacklist`, `dimensionBlacklist`, `biomeBlacklist`, `championWhiteList`).

`FMLServerStartingEvent`: registers `/thaumcraft` (`CommandThaumcraft`).
`OnConfigChangedEvent`: `Config.syncConfigurable()` + save. **[verified]**

## Proxy split [verified]

`CommonProxy implements IGuiHandler` owns the server-side singletons
(`playerKnowledge`, `researchManager`, `wandManager`), a numeric GUI-id
switch (`getServerGuiElement` cases 0–20: golem, pech, trunk, thaumatorium,
focus pouch, deconstruction, alchemy furnace, research table, arcane
workbench, bore, hand mirror, hover harness, magic box, spa, focal
manipulator), and no-op FX stubs (`sparkle`, `wispFX`, `crucibleBoil`, …)
that `ClientProxy` overrides with real particle/render calls. Client-only
classes (`RenderEventHandler`, `KeyHandler`, all of `client/gui`) are only
touched through the proxy. **[verified]**

## Networking [verified]

`common/lib/network/PacketHandler`: one `SimpleNetworkWrapper` channel
`"thaumcraft"`, 39 `registerMessage` entries in three families:

- **playerdata/** (16 classes): login/full sync down
  (`PacketSyncResearch`, `PacketSyncAspects`, `PacketSyncScannedItems/
  Entities/Phenomena`, `PacketSyncWarp`, `PacketSyncWipe`,
  `PacketAspectPool`, `PacketAspectDiscovery`, `PacketResearchComplete`,
  `PacketRunicCharge`, `PacketWarpMessage`); player actions up
  (`PacketScannedToServer`, `PacketAspectCombinationToServer`,
  `PacketAspectPlaceToServer`, `PacketPlayerCompleteToServer`).
- **fx/** (14 classes): one-shot effect broadcasts to clients
  (`PacketFXZap`, `PacketFXBlockArc`, `PacketFXVisDrain`,
  `PacketFXEssentiaSource`, `PacketFXBeamPulse`, …).
- **misc/** (8 classes): config sync (`PacketConfig`), biome change, bore
  dig, fly/focus/item-key to server, `PacketNote`, `PacketMiscEvent`.

`EventHandlerNetwork` handles login/quit lifecycle. The pattern is
server-authoritative state + full-sync-on-login + fire-and-forget FX —
a NeoForge reimplementation would replace this with capability/data-attachment
sync (see `docs/REVERSE_ENGINEERING_POLICY.md`). **[verified registrations;
login-sync behavior inferred from handler names + packet contents]**

## Configuration system [verified]

`common/config/Config` wraps a Forge `Configuration` with categories
`Enchantments`, `Entities`, `Biomes`, `Research`, `World_Generation`,
`World_Regeneration`, `Monster_Spawning`, `Runic_Shielding` plus gameplay
toggles read across the codebase (`wuss` disables warp, `hardNode`,
`taintSpreadRate`, `taintFromFlux`, `genAura`/`nodeRarity`/`nodeRefresh`,
`shieldRecharge/shieldWait/shieldCost`, `dimensionOuterId`, potion ids,
`colorBlind`, `shaders`). `ConfigBlocks/ConfigItems/ConfigEntities` are
numeric-id registries (the 1.7.10 "config-driven IDs" idiom);
`ConfigRecipes/ConfigResearch/ConfigAspects` register content at postInit.
In-game config GUI via `guiFactory` → `client/gui/ThaumcraftGuiConfig`.
**[verified]**

## Persistence architecture [verified]

- **Player progression**: in-memory `PlayerKnowledge` (username-keyed maps)
  ↔ `ResearchManager.save/loadPlayerData` compressed NBT sidecar + backup
  file (hooked into `PlayerEvent.SaveToFile/LoadFromFile` in
  `EventHandlerEntity`) ↔ mirror in player entity NBT
  (`saveResearchNBT/saveAspectNBT/saveScannedNBT`, `Thaumcraft.eldritch*`,
  `Thaumcraft.shielding`).
- **World state**: tile NBT (`TileNode` type/modifier/aspects,
  `TileInfusionMatrix` crafting state, crucible contents, tube/reservoir
  contents); item NBT (wand charges, jarred nodes, research note puzzles).
- **Static content**: code registries only — aspects, research, recipes,
  wand caps/rods/foci are built at class-init/postInit, never data files.

## Event-handler map [verified]

| Handler | Bus | Responsibility |
| --- | --- | --- |
| `EventHandlerWorld` | EVENT_BUS + TERRAIN_GEN_BUS + FML bus + fuel handler | world/chunk save-load hooks, `ItemCraftedEvent` → sticky warp for warp-registered items, `HarvestDropsEvent` crop handling, fuel values |
| `EventHandlerEntity` | EVENT_BUS | player load/save file IO, `playerTick` (warp rolls every 2000 ticks, death gaze), mob death aspect drops, wand-repair-on-anvil (`IRepairableExtended`) |
| `EventHandlerRunic` | EVENT_BUS | runic shield charge/absorb, charge tooltips |
| `ServerTickEventsFML` | FML bus | `WorldTickEvent` chunk-regeneration bookkeeping, warded-block restore |
| `EventHandlerNetwork` | FML bus | login/quit packet lifecycle |
| `ScanManager` | via `ThaumcraftApi.registerScanEventhandler` | thaumometer scan results |
| `RenderEventHandler` | client bus | client render hooks |
| `KeyHandler` | client | keybinds (focus switch etc.) |

## Decompilation caveats [verified]

- SRG/MCP names (`func_*`, `field_*`) survive in bodies; real names are
  recoverable from MCP mappings for 1.7.10.
- Some decompiler artifacts remain (e.g. `new HashMap();` discarded result in
  `VisNetHandler.addNode`, empty `if` bodies) — do not treat as intended
  behavior. **[verified as artifacts]**
- `scheduleSave(EntityPlayer)` is an empty stub — saves happen through the
  player-file events, not this method. **[verified]**
