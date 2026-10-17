# Baubles 1.7.10-1.0.1.10 — API Contract Analysis (TL-008)

Source artifact: `/home/aaron/minecraft mods/Baubles-1.7.10-1.0.1.10.jar`
(SHA-256 `05ae8f11b8f9012ebebbf179227b010829739c6284e30da5dd3db220ee682f8a`,
identity per TL-004 / `BAUBLES_INVENTORY.md`).

Evidence base: Vineflower-decompiled sources at
`/home/aaron/.hermes/cache/scratch/decompiled/baubles/` (read-only, 21 `.java`
files + resources; all file citations below are relative to that root),
cross-checked against raw bytecode via `javap -p` on the original JAR
(`javap` signatures match the decompiled signatures exactly, including the
2-argument `InventoryBaubles.func_70298_a(int,int)`). The embedded
`changelog.txt` is cited for design intent.

Confidence labels — **[verified]**: direct reading of decompiled source or
bytecode. **[inferred]**: reading/interpretation, obfuscated-name mapping, or
behavior implied by code but not directly stated.

Naming note: identifiers are the 1.7.10 SRG/obfuscated names as they appear in
the decompiled output. Where a well-known MCP name is used as annotation it is
marked *(MCP)*; uncertain names are described behaviorally instead.

---

## 1. Artifact identity

- Mod: `Baubles`, version `1.0.1.10`, MC `1.7.10`, author Azanor
  (`mcmod.info`; `baubles/common/Baubles.java` `@Mod` annotation).
- Hard Forge dependency: `required-after:Forge@[10.13.2,)`
  (`baubles/common/Baubles.java`) **[verified]**.
- The `baubles.api` package is a versioned Forge API:
  `@API(owner="Baubles", apiVersion="1.0.1.10", provides="Baubles|API")`
  (`baubles/api/package-info.java`) **[verified]**. Add-ons compile against
  `Baubles|API` 1.0.1.10.
- This version has **4 accessory slots** and **3 bauble types** — not the 7
  slots of the later 1.1.x line (see TL-004 "Version-critical fact").

## 2. Package map

| Package | Contents |
|---|---|
| `baubles.api` | `BaublesApi`, `BaubleType`, `IBauble`, `package-info` — the public contract |
| `baubles.common` | `Baubles` (mod entry), `CommonProxy` (GUI handler), `Config` |
| `baubles.common.container` | `InventoryBaubles` (IInventory), `SlotBauble`, `ContainerPlayerExpanded` |
| `baubles.common.event` | `EventHandlerEntity` (tick/death/load/save), `EventHandlerNetwork` (login sync), `KeyHandler` (hotkey) |
| `baubles.common.items` | `ItemRing` (example "Miner's Ring") |
| `baubles.common.lib` | `PlayerHandler` (per-player cache + file persistence), `CreativeTabBaubles` (unused in this version, see §12) |
| `baubles.common.network` | `PacketHandler`, `PacketOpenBaublesInventory`, `PacketOpenNormalInventory`, `PacketSyncBauble`, `AbstractPacket` (legacy), `PacketPipeline$2` (synthetic remnant) |
| `baubles.client` | `ClientProxy`, `gui/GuiPlayerExpanded`, `gui/GuiEvents`, `gui/GuiBaublesButton` |

`AbstractPacket` and `PacketPipeline$2` are vestigial: the active networking
uses Forge `SimpleNetworkWrapper` and none of the live packets extend
`AbstractPacket` **[verified]** — consistent with changelog 1.0.0.16 "switch to
SimpleNetworkWrapper".

## 3. Public API contract (`baubles.api`)

### 3.1 `BaublesApi.getBaubles(EntityPlayer) -> IInventory`

`baubles/api/BaublesApi.java`. A **reflection bridge**, not a direct call:

1. First call resolves `Class.forName("baubles.common.lib.PlayerHandler")` and
   caches `getMethod("getPlayerBaubles", EntityPlayer.class)` in a static
   `Method` field (lines 15–18).
2. Every call invokes that static method and casts the result to `IInventory`.
3. On any failure (class/method missing, invocation error) it logs
   `"[Baubles API] Could not invoke ... getPlayerBaubles"` to `FMLLog` and
   returns `null` **[verified]**.

Consequences **[verified from code; impact inferred]**:

- The API package is self-contained: add-ons can compile against
  `Baubles|API` without the implementation classes being present at compile
  time; at runtime a missing implementation degrades to `null`, and callers
  must null-check.
- The returned `IInventory` is the live per-player baubles inventory
  (`PlayerHandler.getPlayerBaubles`, §9) — callers can read all slots via
  `func_70301_a` (getStackInSlot, MCP) and `func_70302_i_` (getSizeInventory,
  MCP); size is always 4.
- The bridge exposes no typed bauble accessor: consumers must cast
  `getItem()` results to `IBauble` themselves.

### 3.2 `enum BaubleType { RING, AMULET, BELT }`

`baubles/api/BaubleType.java` — exactly three types in this version
**[verified]**. The enum's declaration order (RING, AMULET, BELT) is *not*
used for slot routing; routing is by explicit comparison (§5).

### 3.3 `interface IBauble`

`baubles/api/IBauble.java` — six methods, all taking the `ItemStack` plus an
`EntityLivingBase` bearer **[verified]**:

| Method | Contract as actually implemented |
|---|---|
| `getBaubleType(ItemStack)` | Slot routing key. Consulted in `InventoryBaubles.isItemValidForSlot` (`InventoryBaubles.java:142` region), `SlotBauble.func_75214_a` (isItemValid, MCP) (`SlotBauble.java:20–24`), and shift-click routing (`ContainerPlayerExpanded.java:121–156` region). Called on both sides wherever validation runs. |
| `onWornTick(ItemStack, EntityLivingBase)` | Driven every game tick for every equipped bauble by `EventHandlerEntity.playerTick` (`EventHandlerEntity.java:20–29`). Only ever invoked with `EntityPlayer` bearers (handler checks `instanceof EntityPlayer`) despite the `EntityLivingBase` signature **[verified]**. **No side guard** in the handler: fires on both logical sides for every ticking player entity (§7.3). |
| `onEquipped` / `onUnequipped` | Fired by inventory mutation, not by a dedicated event — see §7 firing map. Guarded by the `blockEvents` suppression flag. Not guaranteed to fire exactly once (§13 Q1). |
| `canEquip(ItemStack, EntityLivingBase)` | Item-side equip veto. Consulted in `isItemValidForSlot`, `SlotBauble.isItemValid`, shift-click routing, and `ItemRing` right-click auto-equip. |
| `canUnequip(ItemStack, EntityLivingBase)` | Item-side removal veto. Consulted **only** in `SlotBauble.func_82869_a` (canTake, MCP) (`SlotBauble.java:27–29`) — i.e. only for GUI slot take-out. Not consulted on death drops, programmatic `setInventorySlotContents`, or file load **[verified]**. |

There is no `getBaubleSlotInPlayerNBT`-style save hook and no client-render
hook in this API version **[verified]** — client-side visuals must be driven
by consumers from the synced inventory (§8).

## 4. Slot model

`InventoryBaubles` is a fixed `ItemStack[4]` (`InventoryBaubles.java:28`).
The index→type mapping is hard-coded at container construction
(`ContainerPlayerExpanded.java:55–58`) **[verified]**:

| Inventory index | BaubleType | GUI position (px in expanded GUI) |
|---|---|---|
| 0 | AMULET | (80, 8) |
| 1 | RING | (80, 26) |
| 2 | RING | (80, 44) |
| 3 | BELT | (80, 62) |

`isItemValidForSlot(i, stack)` (`InventoryBaubles.java:141–152`) enforces the
same mapping: stack must implement `IBauble`, pass `canEquip`, and its
`getBaubleType` must equal the slot's fixed type (0→AMULET, 1|2→RING,
3→BELT) **[verified]**.

Inventory surface notes **[verified]** (`InventoryBaubles.java`):

- `getInventoryStackLimit` (`func_70297_j_`) = 1; `SlotBauble.func_75219_a`
  (getSlotStackLimit, MCP) = 1.
- `isUseableByPlayer` always true; `getInventoryName` = `""`,
  `hasCustomInventoryName` false; `openInventory`/`closeInventory` no-ops.
- `markDirty` (`func_70296_d`) forwards to the **player's main inventory**
  `markDirty` — marking the baubles dirty schedules a player/world save, which
  in turn triggers the `SaveToFile` persistence hook (§9). This is the
  mechanism behind changelog 1.0.0.4 "allow other mods to force a save by
  marking the inventory as dirty" **[verified mechanism; changelog intent]**.
- `player` is held as a `WeakReference<EntityPlayer>` to avoid leaking player
  instances (§9 holds the strong reference via the username key).
- Any mutation (`func_70298_a`, `func_70299_a`) calls the attached container's
  `onCraftMatrixChanged` (`func_75130_a`) — used to force container resync —
  and then `syncSlotToClients(slot)` (§8).

Two removal paths differ in callback behavior **[verified]**:

- `func_70304_b(int)` (decrStackSize, MCP): removes without any
  `onUnequipped` callback.
- `func_70298_a(int slot, int amount)` (2-arg; part of the 1.7.10
  `IInventory` surface — bytecode-confirmed; commonly mapped as
  `getStackInSlotOnClosing` **[inferred name]**): removes and **does** fire
  `onUnequipped`.

## 5. GUI container: `ContainerPlayerExpanded`

Replaces the vanilla player inventory screen (GUI id 0; `Baubles.GUI = 0`,
`CommonProxy.getServerGuiElement`). Layout and container slot indices
**[verified]** (`ContainerPlayerExpanded.java:26–66`):

| Container slot idx | Content |
|---|---|
| 0 | crafting result |
| 1–4 | 2×2 crafting matrix |
| 5–8 | vanilla armor slots (anonymous `Slot` using `isValidArmor`) |
| 9 | bauble AMULET (`SlotBauble`) |
| 10, 11 | bauble RING ×2 |
| 12 | bauble BELT |
| 13–39 | main inventory 3×9 |
| 40–48 | hotbar |

Key behaviors:

- **Server-side aliasing**: on the server the container's `baubles` inventory
  *shares the `stackList` array reference* with the `PlayerHandler` instance
  (`ContainerPlayerExpanded.java:31`); on close, the server stores the
  container's inventory object back into `PlayerHandler`
  (`:89`, `setPlayerBaubles`). The persistent state is therefore the array,
  shared between handler and any open container **[verified]**.
- **Close cleanup**: crafting-matrix leftovers are returned to the player
  (`func_71019_a`, dropItem-like, MCP) and the result cleared
  (`:78–87`) — changelog 1.0.1.10 "now you won't lose items in bauble
  crafting grid when switching back to vanilla inventory" **[verified]**.
- **Never auto-closes**: `func_75145_c(EntityPlayer)` (canInteractWith-style
  check) returns `true` unconditionally (`:93`) **[verified]**.
- **Shift-click routing** (`func_82846_b`, transferStackInSlot, MCP;
  `:97–176`): armor items route to armor slots; `IBauble`s route by type —
  AMULET→9, RING→10 then 11 (first empty wins; ring routing additionally
  requires source slot > 11), BELT→12 — each gated on `getBaubleType` match
  **and** `canEquip` and target emptiness; otherwise vanilla-style inventory/
  hotbar shuffling via a private `mergeItemStack` copy (`:194–260`).
- **Merge guard**: `func_94530_a(ItemStack, Slot)` rejects merges whose target
  inventory is the crafting result (`:262–264`) **[verified]**.
- **Callback suppression**: the override of `Container.func_75131_a(ItemStack[])`
  (a bulk slot-write hook in the 1.7.10 `Container` surface; exact vanilla
  name not identified **[inferred]**) sets `baubles.blockEvents = true` before
  delegating to super (`:188–191`); `GuiPlayerExpanded` resets the flag to
  `false` every client screen tick (`GuiPlayerExpanded.java:29–32`). This is
  the changelog-1.0.1.3 "workaround for unnecessary calls to onEquipped and
  onUnequipped" **[verified mechanism; changelog intent]**.
- A private `unequipBauble(ItemStack)` hook exists but is **empty** — a
  vestige of the 1.0.1.5 "onUnequip called only once on shift-click" fix
  (`:185`, changelog) **[verified empty; history inferred]**.

## 6. Slot gating: `SlotBauble`

`baubles/common/container/SlotBauble.java` **[verified]**:

- `isItemValid` (`func_75214_a`): non-null, item implements `IBauble`,
  `getBaubleType(stack) == this.type`, and `canEquip(stack, player)` true.
- `canTake` (`func_82869_a`): contents' `canUnequip(stack, player)` must be
  true (unchecked cast — assumes validated contents are `IBauble`).
- Stack limit 1.

## 7. `IBauble` lifecycle firing map

All firing sites, from bytecode-confirmed call graph **[verified]**:

### 7.1 `onEquipped`

| Trigger | Site | Notes |
|---|---|---|
| Slot write (GUI drag/click, programmatic) | `InventoryBaubles.func_70299_a` (`:103–117`) | Only when `blockEvents == false` and incoming stack is `IBauble`. |
| Right-click auto-equip | `ItemRing.onItemRightClick` (`ItemRing.java:52–66`) | Calls `func_70299_a` (which fires the callback) **and then** `this.onEquipped(...)` again → double invocation (§13 Q1). |
| Client-side container sync | via vanilla `Container` slot updates calling `setInventorySlotContents` on the client's container inventory | **[inferred]** (standard 1.7.10 container sync path; no Baubles-side code). |

### 7.2 `onUnequipped`

| Trigger | Site | Notes |
|---|---|---|
| Slot overwritten/cleared | `InventoryBaubles.func_70299_a` (`:104–106`) | Skipped when `blockEvents` is set. Casts without `instanceof` — assumes validated `IBauble` contents. |
| Partial removal via 2-arg `func_70298_a` | `InventoryBaubles.java:66–95` | Fires on the removed piece. |
| Full removal via `func_70304_b` | — | **No callback** (asymmetry, §4). |
| Death drops | `dropItems`/`dropItemsAt` | **No callback** (§10). |
| File load | `readNBT` | **No callback** — loaded baubles never fire `onEquipped` (§9). |

### 7.3 `onWornTick`

`EventHandlerEntity.playerTick` on `LivingUpdateEvent`
(`EventHandlerEntity.java:20–29`): for every ticking `EntityPlayer`, iterate
all 4 slots, call `onWornTick` on each `IBauble`. **No `isRemote` guard** —
the handler runs on both logical sides, so in singleplayer each equipped
bauble receives `onWornTick` twice per game tick (server tick + client tick)
and on a dedicated server once per server tick plus once per client tick on
each tracking client **[verified from code; consequence inferred]**.
Implementations must be side-safe and idempotent (the example `ItemRing`
guards its effect with `isPotionActive` and its sound with `!isRemote`).

## 8. Client synchronization

### 8.1 Protocol

Channel: `SimpleNetworkWrapper` `"baubles"` (`PacketHandler.java`), messages
**[verified]**:

| Disc | Packet | Direction | Payload | Handler behavior |
|---|---|---|---|---|
| 0 | `PacketOpenBaublesInventory` | C→S | empty | Server opens GUI id 0 for the sender (`PacketOpenBaublesInventory.java`). |
| 1 | `PacketOpenNormalInventory` | C→S | empty | Server closes the current container (`onContainerClosed`) and swaps `player.openContainer` back to the vanilla inventory container (`PacketOpenNormalInventory.java`). |
| 2 | `PacketSyncBauble` | S→C | `byte slot`, `int playerId` (entity id), `ItemStack` (incl. null) | Client resolves the entity by id in the client world and writes `PlayerHandler.getPlayerBaubles(player).stackList[slot] = bauble` directly — **no lifecycle callbacks, no container update** (`PacketSyncBauble.java`). |

### 8.2 Triggers

- **Per-slot on every server-side mutation**: `InventoryBaubles.syncSlotToClients`
  (`:233–239`) is called from both removal paths and `func_70299_a`.
- **Full sync (all 4 slots)** on `PlayerEvent.LoadFromFile`
  (`EventHandlerEntity.java:71`) and on `PlayerLoggedInEvent` when the
  effective side is SERVER (`EventHandlerNetwork.java:14–25`) — the latter is
  the changelog-1.0.0.6 "secondary client sync event" fix **[verified
  mechanism; changelog intent]**.

### 8.3 Dedicated-server-only guard (quirk)

`syncSlotToClients` sends only when `Baubles.proxy.getClientWorld() == null`
(`InventoryBaubles.java:235`) — i.e. **only on a dedicated server**. In
singleplayer the proxy is `ClientProxy` and returns a client world, so
`PacketSyncBauble` is never sent, and no code path populates the client-side
`PlayerHandler` map (load is server-guarded; container aliasing is
server-guarded; the packet handler never runs). Consequence **[verified code
paths; impact inferred]**: in singleplayer, third-party client code calling
`BaublesApi.getBaubles()` sees an empty inventory; the expanded GUI itself is
still correct because it syncs through the vanilla container protocol.
Changelog 1.0.0.11 ("synced to all clients … allowing visual components")
shows the intent of this sync — the singleplayer hole is a defect, not a
design goal **[inferred]**.

## 9. Persistence: `PlayerHandler` + `.baub` files

### 9.1 In-memory cache

`baubles/common/lib/PlayerHandler.java`: a static
`HashMap<String username, InventoryBaubles>`, keyed by
`Entity.getCommandUsername` (`func_70005_c_`) — the **username, not the UUID**
**[verified]**. Changelog 1.0.1.8 documents the deliberate choice: UUID keys
broke offline-mode play; username keys break on rename, accepted as "the
lesser of two evils". `getPlayerBaubles` lazily creates an empty inventory;
`clearPlayerBaubles` evicts; `setPlayerBaubles` replaces (used on container
close). All file I/O is server-side only (`!world.isRemote` guard in
load/save) **[verified]**.

### 9.2 File format and save path

- Files live in the Forge event's `playerDirectory` (the save's player-data
  directory): `<username>.baub` (primary) and `<username>.baubback` (backup)
  (`EventHandlerEntity.getPlayerFile`, `:74–82`; suffix `dat` rejected).
- On save (`PlayerEvent.SaveToFile`, `EventHandlerEntity.java:93–99`):
  `PlayerHandler.savePlayerBaubles` first **copies the existing primary over
  the backup**, then writes fresh NBT to the primary; on write failure it logs
  and **deletes the corrupt primary** so the next load falls back to the
  backup (`PlayerHandler.java:76–101`) **[verified]**.
- On load (`PlayerEvent.LoadFromFile`, `EventHandlerEntity.java:40–72`):
  evict cache → resolve primary; if absent, migrate:
  1. a UUID-named `.baub` (produced by the 1.0.1.7-era build) is copied to
     the username name and the UUID file + its backup deleted;
  2. else a pre-1.7.10 legacy file at `<world>/players/<username>.baub`
     (`getLegacyFileFromPlayer`, `:84–91`) is copied in.
  Then `loadPlayerBaubles`: read primary; if missing, unreadable, or empty,
  read the backup and immediately re-save the primary from it
  (`PlayerHandler.java:34–74`). Finally a full 4-slot client sync is issued.
  **[verified]**
- NBT schema **[verified]** (`InventoryBaubles.saveNBT/readNBT`, `:157–190`):
  root compound with key `"Baubles.Inventory"` → `NBTTagList` of compounds,
  each `{ "Slot": byte, …ItemStack NBT… }`; read via
  `getTagList("Baubles.Inventory", 10)`. Slots are self-describing (sparse).
- `saveNBT(EntityPlayer)`/`readNBT(EntityPlayer)` overloads write the same
  schema into `player.getEntityData()` — the pre-1.0.1.0 "player data" format.
  In 1.0.1.10 **no caller invokes them** (changelog 1.0.1.0 removed the
  legacy-format fallback) **[verified unused]**.
- Loaded baubles do **not** fire `onEquipped` — `readNBT` writes `stackList`
  directly **[verified]**.

## 10. Death behavior

`EventHandlerEntity.playerDeath` on `PlayerDropsEvent`
(`EventHandlerEntity.java:32–39`) **[verified]**:

- Runs server-side only, and only when the `keepInventory` game rule is
  **false** (changelog 1.0.0.7 introduced the rule check).
- `InventoryBaubles.dropItemsAt(event.drops, player)` (`:214–232`): for each
  non-empty slot, create an `EntityItem` copy at the **dying player's
  position** (`y + eyeHeight`), pickup delay 40 ticks, random horizontal
  velocity (magnitude ≤ 0.5), `vy = 0.2`; append to `event.drops` (so other
  mods can still cancel/modify the drops), clear the slot, and sync it.
- No `onUnequipped` callback on death (§7.2).
- Changelog 1.0.1.9: "possible fix for baubles dropping near player spawn
  location instead of where he died" — the `dropItemsAt(…, entity)` variant
  taking an explicit position is the fix **[verified mechanism; history
  inferred]**. `dropItems(ArrayList)` (player-position variant) remains in the
  class but is not called by the death handler **[verified]**.

## 11. GUI and input integration

- **Hotkey**: `KeyBinding("Baubles Inventory", 48, "key.categories.inventory")`
  (`KeyHandler.java:15`). Default keycode 48 maps to LWJGL `KEY_B` — the 'B'
  hotkey **[inferred mapping; TL-004 also records 'b']**. On client
  `PlayerTickEvent` START, if the key is down **and** the client has focus
  (`Minecraft.field_71415_G`, `inGameHasFocus` **[inferred name]**), send
  `PacketOpenBaublesInventory` (`KeyHandler.java:22–28`). Pressing the same
  key inside the expanded GUI closes it (`GuiPlayerExpanded.keyTyped`,
  `:114–118`) — the hotkey toggles **[verified]**.
- **Toggle button**: `GuiEvents` injects a 10×10 `GuiBaublesButton` (id 55,
  ring icon from the GUI texture at (200/210, 48)) at `guiLeft+66, guiTop+9`
  into both `GuiInventory` and `GuiPlayerExpanded`
  (`GuiEvents.java:20–52`). Vanilla-inventory click → `PacketOpenBaublesInventory`
  (server opens the expanded GUI). Expanded-GUI click → client locally swaps
  to `GuiInventory` **and** sends `PacketOpenNormalInventory` so the server
  swaps containers in step (`:44–52`). Button label from lang keys
  `button.baubles` / `button.normal` (`assets/baubles/lang/en_US.lang`).
- **NEI offset**: if the player's main inventory is non-empty and
  `codechicken.nei.NEIClientConfig.isHidden()` (resolved by reflection,
  `:59–68`) returns true, the button x is shifted to compensate for the NEI
  panel — changelog 1.0.1.4 **[verified mechanism; changelog intent]**.
- **`GuiPlayerExpanded`** extends `InventoryEffectRenderer` (active potion
  effects render — relevant to changelog 1.0.1.7, the shortcut-ring shift bug
  with effects active **[inferred connection]**); background texture
  `baubles:textures/gui/expanded_inventory.png`; draws the empty-slot
  background (texture 200,0) for every slot with stack limit 1; renders the
  player model; keeps achievements/stats buttons; clears `blockEvents` each
  tick (§5).

## 12. Example item and bootstrap

- `ItemRing` ("Miner's Ring", lang `item.Ring.0.name`): `BaubleType.RING`;
  registered as `GameRegistry.registerItem(itemRing, "Ring", "Baubles")` in
  `Config.initialize` **[verified]**.
  - Right-click auto-equip (server-side): first slot where
    `isItemValidForSlot` passes → `setInventorySlotContents` (fires
    `onEquipped`), consume held stack unless the player is in creative, then a
    second explicit `onEquipped` call (§13 Q1) (`ItemRing.java:52–66`).
  - `onWornTick`: at meta 0, if the bearer lacks the potion
    `Potion.field_76422_e`, re-apply a 40-tick amplifier-0 ambient effect
    every tick → effectively permanent. The Miner's Ring's effect is Haste
    **[inferred — item name + community knowledge; the SRG field name is kept
    verbatim]** (`ItemRing.java:73–76`).
  - `onEquipped`: plays `random.orb` (server-side only); always-enchantment
    glint; `EnumRarity.rare`; `canEquip`/`canUnequip` always true.
  - **No crafting recipe**: `Config.initRecipe()` is empty — recipe removed in
    1.0.1.2 ("a bit op"), item kept as creative example **[verified empty;
    changelog intent]**.
- `CreativeTabBaubles` exists but is never instantiated or registered in this
  version; the ring is attached to a vanilla creative tab
  (`func_77637_a(CreativeTabs.field_78040_i)`, tab identity not determined
  **[inferred]**) **[verified unused]**.
- Mod bootstrap (`Baubles.java`): preInit loads config (no actual options
  exist in `Config` — only item registration), inits packets, registers
  `EventHandlerEntity` on `MinecraftForge.EVENT_BUS` and `EventHandlerNetwork`
  on the FML bus, then `proxy.registerHandlers()` (client: KeyHandler +
  GuiEvents). init: GUI handler id 0 + keybindings. postInit: empty
  `initRecipe` **[verified]**.

## 13. Quirks and defects (for the port: do not replicate blindly)

1. **Double `onEquipped` on right-click equip** — `ItemRing.onItemRightClick`
   fires the callback via `setInventorySlotContents` and again explicitly
   (`ItemRing.java:58` + `:63`) **[verified]**.
2. **`onWornTick` has no side guard** — runs on both logical sides (§7.3)
   **[verified]**.
3. **Singleplayer client-sync hole** — `PacketSyncBauble` never sent in SP;
   client-side `PlayerHandler` stays empty (§8.3) **[verified code paths]**.
4. **Callback asymmetry between removal methods** — `func_70304_b` silent,
   2-arg `func_70298_a` fires `onUnequipped` (§4) **[verified]**.
5. **`canUnequip` is only enforced through GUI slot take-out** — bypassed by
   death, programmatic writes, and loads (§3.3) **[verified]**.
6. **`blockEvents` is a client-tick-reset global flag** on the inventory —
   suppression is timing-based, not call-scoped (§5) **[verified]**.
7. **No lifecycle callbacks on load or death** — `onEquipped` never fires for
   baubles restored from disk; `onUnequipped` never fires on death (§7.2)
   **[verified]**.
8. **Username-keyed state** — rename loses baubles (accepted trade-off,
   changelog 1.0.1.8) **[verified]**.
9. **`func_70299_a` casts existing contents to `IBauble` without `instanceof`**
   when firing `onUnequipped` (`InventoryBaubles.java:105`) — safe only
   because validation guarantees the type; a non-validated write (e.g.
   `readNBT` into a shared array) could crash it **[verified code; risk
   inferred]**.
10. **Vestigial code shipped**: `AbstractPacket`, `PacketPipeline$2`,
    `unequipBauble()` (empty), `CreativeTabBaubles` (unused),
    `saveNBT(EntityPlayer)`/`readNBT(EntityPlayer)` (unused),
    `dropItems(ArrayList)` (uncalled) **[verified]**.

## 14. Verified vs inferred — summary

Everything above is **[verified]** against decompiled source and bytecode
unless marked **[inferred]**. The inferred items are: MCP name annotations
(`getStackInSlotOnClosing`, `inGameHasFocus`, `func_75131_a` identity), the
LWJGL keycode-48→'B' mapping, the Haste identity of `Potion.field_76422_e`,
the client-side container-sync firing of lifecycle callbacks, and the
changelog-derived design history. No runtime execution was performed; all
behavioral claims are static readings.
