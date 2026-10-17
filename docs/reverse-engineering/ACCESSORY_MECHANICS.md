# Accessory Mechanics — Behavioral Spec (from Baubles 1.0.1.10) — TL-008

Purpose: a mechanics-level specification of what the Baubles 1.7.10-1.0.1.10
artifact actually does, distilled for a modern (NeoForge 21.x) implementation.
Companion to `BAUBLES_API_ANALYSIS.md` (full API contract, quirks list, and
citations); this document states the *behavior* and marks, per mechanic, what
to preserve and what to modernize.

Evidence: decompiled sources at
`/home/aaron/.hermes/cache/scratch/decompiled/baubles/` + bytecode cross-check
+ embedded `changelog.txt`. Labels: **[verified]** = direct code reading;
**[inferred]** = interpretation or changelog-derived intent. Citations use
decompiled file paths; see the API analysis for line-level detail.

Scope note: this describes the 1.0.1.10 behavior (4 slots, 3 types). The
7-slot "amulet/belt/head/charm" feature set on CurseForge belongs to the later
1.1.x line and is **not** specified here (TL-004 version-critical fact).

---

## A. Core invariants (preserve as-is)

### A1. Server-authoritative accessory state
The server owns the accessory inventory; the client holds a synced mirror.
Every mutation happens server-side and is pushed to clients
(`InventoryBaubles.syncSlotToClients`, `PacketSyncBauble`). **[verified]**
→ Modern: keep server authority; use the modern network payload/attachment
sync instead of a bespoke packet.

### A2. Fixed slot set with hard type routing
4 slots: index 0 = AMULET, 1–2 = RING, 3 = BELT; three types total
(`ContainerPlayerExpanded.java:55–58`, `InventoryBaubles.java:28`). A slot
accepts only items whose declared `BaubleType` matches it. **[verified]**
→ Modern: preserve the *model* (typed slots, item declares its type); the
slot count/type set is a design choice for our mod — Thaumcraft 4-era
add-ons of this era expect exactly this 4-slot layout, so match it if we want
drop-in compatibility with the analyzed artifact's ecosystem.

### A3. One item per accessory slot
Stack limit 1 everywhere (inventory, slot, and merge logic). Accessory items
never stack. **[verified]**

### A4. Item-side vetoes
Two item hooks gate interaction:
- `canEquip(stack, bearer)` — consulted at **every entry point**: slot
  validation, drag/click, shift-click routing, right-click auto-equip,
  `isItemValidForSlot`. **[verified]**
- `canUnequip(stack, bearer)` — consulted **only** when taking an item out of
  an accessory slot through the GUI. Bypassed by death, programmatic writes,
  and loading. **[verified]**
→ Modern: preserve `canEquip` at all entry points. Decide explicitly whether
`canUnequip` should also gate death/forced removal (Baubles does not; see D3).

### A5. Validation is enforced at every insertion path, not just the GUI
`isItemValidForSlot` duplicates the slot's type+canEquip check so that
non-GUI writers (mods via `getBaubles`) get the same rules; the GUI slot
adds `canTake`. **[verified]**
→ Modern: put the rule in the inventory/container layer once, not in three
places.

## B. Lifecycle semantics (preserve, with fixes)

### B1. Equip / unequip callbacks
Items implementing the accessory interface receive `onEquipped` /
`onUnequipped` when they enter/leave a slot through normal inventory
mutation (`InventoryBaubles.func_70299_a`, guarded by a suppression flag).
**[verified]**

Required semantics for a modern port:
- Fire exactly once per transition. Baubles violates this (double-fire on
  right-click equip; suppression via a global flag reset on client ticks —
  API analysis §13 Q1/Q6). **[verified defect]**
- Do **not** fire on deserialization/load (Baubles doesn't) — state
  restoration is not an equip event. **[verified behavior; keep]**
- Decide death explicitly: Baubles drops accessories **without**
  `onUnequipped` (API analysis §10). Items that must clean up bearer state
  (potions, attributes) need a removal hook that *does* run on death drops.
  **[inferred requirement; mark as a port decision]**

### B2. Tick callback for worn accessories
`onWornTick(stack, bearer)` is driven every game tick for every equipped
accessory, from a living-update event handler (`EventHandlerEntity.playerTick`).
**[verified]**

Required semantics:
- Server-side ticking is the source of truth for gameplay effects.
- Baubles fires it on **both** logical sides with no guard; example items
  must be side-safe (the Miner's Ring guards its sound with `!isRemote` and
  its effect with an `isPotionActive` check). **[verified]**
- Modern: fire server-side; if client-side visuals need ticking, provide a
  separate, explicitly client-side hook. Document idempotency requirements.

### B3. Bearer type
The tick driver only ever passes players (checks `instanceof EntityPlayer`)
even though the interface accepts `EntityLivingBase`. Accessories are a
player-only concept in this version. **[verified]**

## C. State, persistence, and death (preserve behavior, modernize storage)

### C1. Per-player persistent accessory inventory
State survives death, logout, and world reload. Baubles stores it in
per-player files `<username>.baub` + `<username>.baubback` backup, written on
the player-save event, with corruption fallback to the backup and immediate
re-save after a backup restore (`PlayerHandler`, `EventHandlerEntity`).
**[verified]**
→ Modern: store on the player itself (NeoForge data attachment / entity
saved-data) instead of side-channel files; keep the *durability* properties:
write-time backup, corrupt-file recovery, migration path for older formats.
The username-vs-UUID keying problem (changelog 1.0.1.8) disappears with
attachment storage.

### C2. Dirty-flag save scheduling
Marking the accessory inventory dirty schedules a player save
(`markDirty` forwards to the main inventory). Other mods can force a save
this way (changelog 1.0.0.4). **[verified mechanism]**
→ Modern: attachments are saved with the player automatically; preserve the
"mutate → gets saved" guarantee, drop the manual flag.

### C3. Death contract
On death with `keepInventory` **off**: all equipped accessories are dropped as
items at the death location (pickup delay 40 ticks, small random scatter),
participating in the standard drop event so other mods can intercept them;
slots are cleared and synced. With `keepInventory` on: accessories are kept.
**[verified]** (changelog 1.0.0.7 rule check, 1.0.0.14 "prevent loss of
non-baubles items on death", 1.0.1.9 drop-at-death-location fix.)
→ Modern: preserve exactly this — drop into the drop event (not manual
`entityDropItem`), respect the gamerule, drop at death position.

### C4. Client mirror sync
Per-slot sync on every server-side mutation; full 4-slot sync on player join
(login and world load). Sync writes the mirror directly (no callbacks).
**[verified]** Baubles' sync is dedicated-server-only, leaving the
singleplayer client mirror empty (API analysis §8.3) — a hole, not a design.
**[verified defect]**
→ Modern: sync in all environments (singleplayer included).

### C5. Read access for other mods
A stable, typed accessor for "the accessory inventory of this player" is the
single most-used part of the API (`BaublesApi.getBaubles`). Baubles ships it
as a reflection bridge so add-ons compile without the implementation; a null
return means "not loaded". **[verified]**
→ Modern: replace with a first-party API + capability/attachment lookup;
keep the guarantee that consumers get the *live* inventory, and a defined
"not present" behavior.

## D. Interaction surfaces (preserve UX, re-implement on modern UI)

### D1. Inventory screen integration
A small toggle button on the vanilla inventory screen opens an expanded
player screen (crafting grid + armor + accessory slots + player model +
potion effects); the reverse button returns to the vanilla screen, with the
server swapping containers in lockstep (packets 0/1). **[verified]**
→ Modern: a single screen or a proper tabbed UI; the two-container swap
hack (closing one container, forcing `openContainer` back) is a 1.7.10
artifact to drop.

### D2. Hotkey toggle
Default key 'B' (keycode 48 **[inferred mapping]**) opens the accessory
screen when no GUI has focus, and closes it when pressed inside it.
Registerable via the controls menu. **[verified]**

### D3. Shift-click auto-equip routing
Shift-clicking an accessory from inventory routes it by type to its slot
(amulet→1st amulet slot, ring→first empty ring slot, belt→belt slot),
gated on type match + `canEquip` + target empty. **[verified]**
→ Preserve; it is the primary equip UX.

### D4. Right-click quick-equip (example item)
The bundled example ring equips on right-click, consuming the held stack
unless in creative. This is item behavior, not framework behavior — Baubles
provides no framework helper for it. **[verified]**
→ Modern: offer a shared "equip on right-click" helper for accessory items.

### D5. Third-party UI coexistence
The toggle button position adapts when NEI (Not Enough Items) is present,
via reflection (`GuiEvents.isNeiHidden`). **[verified]**
→ Modern: use layout-aware GUI hooks instead of mod-name reflection.

## E. Explicitly do NOT carry over

(Defects catalogued in API analysis §13; summarized as port decisions.)

1. Reflection-based API bridge → typed API + capability lookup.
2. Static `HashMap<username, inventory>` cache → player-attached data.
3. Side-channel `.baub`/`.baubback` files → player save data (keep backup/
   recovery *properties*, not the file layout).
4. `blockEvents` global suppression flag → call-scoped event suppression or
   a proper event bus.
5. Double-fired / asymmetric lifecycle callbacks → exactly-once, symmetric
   equip/unequip; explicit decision on death and load.
6. Unguarded both-sides `onWornTick` → server-side gameplay tick, separate
   client visual hook.
7. Singleplayer client-sync hole → sync everywhere.
8. `canUnequip` enforced only in GUI take-out → define its full scope.
9. Vestigial classes (`AbstractPacket`, `PacketPipeline`, unused tabs/
   overloads) → don't port.
10. Username-keyed persistence → UUID/attachment keyed (rename-safe).

## F. Traceability

| Spec item | Primary evidence (decompiled paths) |
|---|---|
| A1–A3, C4 | `common/container/InventoryBaubles.java`, `common/network/PacketSyncBauble.java`, `common/container/SlotBauble.java` |
| A4, A5 | `common/container/SlotBauble.java`, `common/container/InventoryBaubles.java` (`func_94041_b`), `common/container/ContainerPlayerExpanded.java` (`func_82846_b`) |
| B1 | `common/container/InventoryBaubles.java` (`func_70299_a`, `func_70298_a`), `common/items/ItemRing.java` |
| B2, B3 | `common/event/EventHandlerEntity.java` (`playerTick`) |
| C1, C2 | `common/lib/PlayerHandler.java`, `common/event/EventHandlerEntity.java` (`playerLoad`/`playerSave`), `changelog.txt` 1.0.0.4/1.0.0.10/1.0.1.8 |
| C3 | `common/event/EventHandlerEntity.java` (`playerDeath`), `InventoryBaubles.dropItemsAt`, `changelog.txt` 1.0.0.7/1.0.0.14/1.0.1.9 |
| C5 | `api/BaublesApi.java`, `api/package-info.java` |
| D1–D3 | `client/gui/GuiEvents.java`, `client/gui/GuiPlayerExpanded.java`, `common/event/KeyHandler.java`, `common/network/PacketOpen*.java`, `ContainerPlayerExpanded.func_82846_b` |
| D4 | `common/items/ItemRing.java` (`onItemRightClick`) |
| E | API analysis §13 (all citations there) |
