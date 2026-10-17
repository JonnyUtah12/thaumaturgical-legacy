# Thaumcraft 4.2.3.5 Artifact Inventory — TL-003

Source: `/home/aaron/minecraft mods/Thaumcraft-1.7.10-4.2.3.5.jar` (never executed).
Evidence: `unzip -l` listings, embedded metadata, REA `open_binary`/`inspect_artifact`
identity data, and Vineflower-decompiled sources (859 `.java`, kept outside this
repository). Confidence tags: **[verified]** = direct artifact evidence;
**[inferred]** = reading of decompiled code; **[uncertain]** = needs runtime check.

## Identity [verified]

- SHA-256: `3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be` (REA open_binary)
- 2136 archive entries; 942 `.class` files; 12,565,807 bytes.
- `mcmod.info`: modid `Thaumcraft`, version `1.7.10-4.2.3.5`, mcversion `1.7.10`,
  author **Azanor**, `dependencies: ["Baubles"]`.
- `META-INF/MANIFEST.MF`: `FMLCorePlugin: thaumcraft.codechicken.core.launch.DepLoader`,
  `FMLCorePluginContainsFMLMod: true` — ships CodeChicken **DepLoader** as a coremod;
  `dependancies.info` declares the Baubles 1.0.1.10 download it auto-installs.
- API package declares `@API(owner="Thaumcraft", apiVersion="4.2.2.0", provides="Thaumcraft|API")`.
- Bundled `changelog.txt` documents 4.2.3.x behavior fixes (e.g. runic/vis mechanics,
  golems, vis relay network chunk fix) — useful behavioral hints.

## Package map [verified]

| Package | Classes | Role |
| --- | --- | --- |
| `thaumcraft.common.items` | 84 | items incl. `wands/`, `baubles/`, `armor/`, `equipment/`, `relics/` |
| `thaumcraft.common.lib` | 137 | research, events, networking, utils |
| `thaumcraft.common.entities` | 136 | golems, monsters, projectiles, ai |
| `thaumcraft.common.tiles` | 80 | tile devices |
| `thaumcraft.common.blocks` | 71 | blocks |
| `thaumcraft.client.*` | ~250 | renderers, gui, fx (client-only) |
| `thaumcraft.api.*` | ~90 | public API (see below) |
| `thaumcraft.codechicken.*` | 73 | bundled CodeChicken core/lib |
| `truetyper` | 5 | bundled font lib |

## Public API surface [verified from decompiled sources]

- **Aspects** (`api/aspects`): `Aspect` (tag, component aspects, color), `AspectList`,
  `IAspectContainer`, `IAspectSource`, `IEssentiaTransport`, `IEssentiaContainerItem`,
  `AspectSourceHelper`. Aspects are compositional (higher aspects from components).
- **Research** (`api/research`): `ResearchItem`, `ResearchPage`, `ResearchCategories`,
  `ScanResult`, `IScanEventHandler`. Persistence/progression in
  `common/lib/research/ResearchManager` + `PlayerKnowledge`. **[verified]** research
  definitions are code-registered (ResearchManager), not data files in the JAR.
- **Crafting** (`api/crafting`): `ShapedArcaneRecipe`, `ShapelessArcaneRecipe`,
  `CrucibleRecipe` (crucible/alchemy), `InfusionRecipe`, `InfusionEnchantmentRecipe`,
  `IArcaneRecipe`, `IInfusionStabiliser`.
- **Wands** (`api/wands`): `WandCap`, `WandRod`, `IWandFocus`, `ItemFocusBasic`,
  `WandTriggerRegistry`, `IWandable`, focus/rod callback interfaces.
- **Vis & nodes** (`api/nodes`, `api/visnet`): `INode`, `NodeType`, `NodeModifier`,
  `IRevealer`, `TileVisNode`, `VisNetHandler` — vis is transmitted via node networks.
- Misc API: `ThaumcraftApi` (essentia/research/warp registration entry points),
  `ItemApi`, `IGoggles`, `IRepairable`, `IRunicArmor`, `IVisDiscountGear`,
  `IWarpingGear`, `IArchitect`, potions, damagesource, entities.

## Content catalog (representative, from decompiled class names) [verified presence]

- **Baubles items** (`common/items/baubles`): `ItemAmuletRunic`, `ItemRingRunic`,
  `ItemGirdleRunic` (runic armor set), `ItemAmuletVis` (vis discount),
  `ItemGirdleHover` (hover/step assist), `ItemBaubleBlanks` (crafting blanks).
  Also `ItemFocusPouchBauble` (focus pouch as bauble).
- **Wands/equipment**: wand caps/rods/sets, foci (`items/wands`, `api/wands`),
  staffs, `ItemCrystalEssence` (wand progression), `IScribeTools` (research unlock).
- **Devices** (`common/tiles`): `TileArcaneWorkbench`, `TileCrucible`,
  `TileInfusionMatrix`+`TileInfusionPillar`, `TileResearchTable`, `TileAlembic`,
  `TileThaumatorium`, `TileEssentiaReservoir`, `TileEssentiaCrystalizer`,
  `TileMirror`/`TileMirrorEssentia`, `TileTube*` (essentia logistics),
  `TileVisRelay`, `TileNodeConverter`, `TileNodeStabilizer`, `TileArcaneFurnace`,
  `TileArcaneLamp*`, `TileDeconstructionTable`, `TileWandPedestal`, `TileArcaneBore`.
- **Blocks**: arcane furnaces/doors, alchemy furnace, jars (incl. `ItemJarNode`),
  magical logs/leaves/plants (`BlockCustomPlant`, `BlockMagicalLog`), taint
  (`BlockTaint`, `BlockTaintFibres`, `EntityFallingTaint`), flux gases/goo,
  crystal/pure/death fluids, eldritch portals, mirrors, chests (hungry), tables.
- **World gen / aura**: `TileNode`, `WorldCoordinates`, node types/modifiers;
  `ComponentBankerHome`/`ComponentWizardTower` structures; `BlockCustomOre` (cinnabar).
- **Entities**: golems (`entities/golems`), `EntityAspectOrb`, pech
  (`ContainerPech`), wisp/fairy types (`entities/monster`), taint mobs.
- **Progression state**: `ResearchManager`/`PlayerKnowledge` (research), warp
  (`WarpEvents`, `PacketSyncWarp`), vis config (`Config*` classes).

## Resources [verified counts]

291 item textures, 262 block textures, 216 model textures, 50 aspect icons,
21 foci icons, 111 sounds + `sounds.json`, 24 lang files, GUI textures,
7+4 Minecraft shader programs (vision effects), `TCLogo.png`.

## Subsystem presence vs. the investigation checklist

Confirmed present: initialization (SidedProxy pre/init/post), items/blocks/registration
(GameRegistry + Config* id registries), aspects, vis/nodes/aura, research/progression,
wands, arcane crafting, infusion, alchemy (crucible/alembic), essentia (tubes,
reservoir, mirrors), devices, constructs (golems), magical armor/tools, magical
plants, taint/environmental effects, networking (PacketHandler + packet classes),
rendering (client packages), persistence (PlayerKnowledge NBT), configuration
(Config/ConfigAspects/…), optional integrations (DepLoader, IMC), Baubles
integration (17 classes import `baubles.*` — see THAUMCRAFT_BAUBLES_INTEGRATION.md).
Essentia "essentia mining" via `ScanManager` [verified class]; exact runtime
behavior of every device is **[inferred]** from code, not executed.
