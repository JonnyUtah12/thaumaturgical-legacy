# Baubles 1.7.10-1.0.1.10 Artifact Inventory — TL-004

Source: `/home/aaron/minecraft mods/Baubles-1.7.10-1.0.1.10.jar` (never executed).
Evidence: `unzip -l` (complete 69-entry listing), embedded metadata, REA
`open_binary` + `inspect_artifact` (full artifact graph retained as evidence
`ev_904776d627e5d6dfd5f263db87441263c6cde8a6b74c7b99ea5cf1d2b20c4ae0`,
inspection `ai_04380c4e892a…`), and Vineflower decompilation (26 `.java` files,
kept outside this repository). Confidence: **[verified]** = direct artifact
evidence; **[inferred]** = reading of decompiled code.

## Identity [verified]

- SHA-256: `05ae8f11b8f9012ebebbf179227b010829739c6284e30da5dd3db220ee682f8a`
- 69 entries; 26 classes; 95,415 bytes. `mcmod.info`: modid `Baubles`,
  version `1.7.10-1.0.1.10`, author Azanor, no dependencies.
- **Version-critical fact [verified]:** this artifact implements **4 accessory
  slots** (`InventoryBaubles`: `new ItemStack[4]`) with slot types
  `0=AMULET, 1=RING, 2=RING, 3=BELT` (`ContainerPlayerExpanded` lines 55–58).
  The "7 slots (amulet, belt, head, body, charm, 2 rings)" description on
  CurseForge describes the later 1.1.x line — **not** this artifact.

## Complete member map [verified]

- `baubles/api/`: `BaublesApi`, `BaubleType`, `IBauble`, `package-info` — the public API.
- `baubles/common/`: `Baubles` (mod entry), `CommonProxy`, `Config`.
- `baubles/common/container/`: `ContainerPlayerExpanded` (replaces player
  inventory GUI container), `InventoryBaubles` (IInventory impl), `SlotBauble`.
- `baubles/common/event/`: `EventHandlerEntity` (tick/death/load/save),
  `EventHandlerNetwork`, `KeyHandler` ('b' hotkey).
- `baubles/common/items/`: `ItemRing` (Miners Ring example item, creative-only).
- `baubles/common/lib/`: `PlayerHandler` (per-player inventory cache + file persistence), `CreativeTabBaubles`.
- `baubles/common/network/`: `PacketHandler`, `PacketSyncBauble`,
  `PacketOpenBaublesInventory`, `PacketOpenNormalInventory`, `AbstractPacket`, `PacketPipeline`.
- `baubles/client/`: `ClientProxy`, `gui/GuiPlayerExpanded`, `GuiEvents`, `GuiBaublesButton`.
- Resources: `expanded_inventory.png` (GUI), `ring.png`, 17 lang files, `BLogo.png`, `changelog.txt`, `mcmod.info`, `pack.mcmeta`.

## API contract [verified from decompiled sources]

- `BaublesApi.getBaubles(EntityPlayer) -> IInventory` — resolved **by reflection**
  to `PlayerHandler.getPlayerBaubles` (so other mods compile against the API
  without the implementation).
- `enum BaubleType { RING, AMULET, BELT }` — exactly three types in this version.
- `interface IBauble`:
  - `getBaubleType(ItemStack)` — slot routing;
  - `onWornTick(ItemStack, EntityLivingBase)` — driven every tick from
    `EventHandlerEntity.playerTick` for all equipped baubles;
  - `onEquipped` / `onUnequipped` — fired by `InventoryBaubles.setInventorySlotContents`
    / stack removal (guarded by a `blockEvents` flag);
  - `canEquip` / `canUnequip` — item-side validation, consulted in
    `InventoryBaubles.isItemValidForSlot` alongside type matching.

## Mechanics [verified from decompiled sources]

- **Slot validation**: `isItemValidForSlot(i, stack)` requires `IBauble`,
  `canEquip`, and `getBaubleType(stack)` matching the slot's fixed type.
- **Persistence**: `PlayerHandler` keeps a `HashMap<username, InventoryBaubles>`;
  saved to per-player NBT files `baub` (+ `baubback` backup) via
  `PlayerEvent.SaveToFile/LoadFromFile`, with migration paths from UUID-named
  and pre-1.7.10 files. Inventory NBT key: `"Baubles.Inventory"` (slot-tagged list).
- **Death/respawn**: on `PlayerDropsEvent` with `keepInventory` off, equipped
  baubles are added to the drop list; otherwise they persist.
- **Networking**: server pushes `PacketSyncBauble` per changed slot
  (`syncSlotToClients`); 'b' hotkey swaps between normal and expanded
  containers via `PacketOpenBaublesInventory`/`PacketOpenNormalInventory`.
- **GUI**: `GuiPlayerExpanded` renders the vanilla player sheet + 4 bauble slots
  + 2×2 crafting; `GuiBaublesButton` adds the inventory-screen button.
- **Content**: only the Miners Ring example (`ItemRing`, creative-only, no recipe).

## Design notes for the modern implementation

- The reflection-based API, username-keyed sidecar files, and manual per-slot
  packet sync are 1.7.10 idioms; a NeoForge 1.21.1 design should use data
  attachments + existing sync instead (see `docs/architecture/ACCESSORY_SYSTEM.md`).
- The lifecycle model (type routing, equip/unequip/tick hooks, item-side
  validation, keepInventory-aware drops) is the durable contract worth preserving.
