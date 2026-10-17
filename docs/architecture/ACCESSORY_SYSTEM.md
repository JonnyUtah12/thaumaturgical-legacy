# Accessory System Architecture (TL-010)

Status: **decided** (2026-10-17). This is the design record; implementation is
TL-019. No code ships with this decision.

## Decision

**Hybrid: a core-standalone accessory layer with an optional Curios
integration.**

- The core mod has **zero hard dependencies** and always works without Curios.
- The **primary integration is Curios** (the accessory framework ATM10 8.3
  actually ships — TL-021). When Curios is present, our accessories live in a
  dedicated Curios slot type and use Curios' UI, persistence, sync, and
  lifecycle events natively.
- When Curios is **absent**, a deliberately minimal built-in fallback keeps
  accessories equippable (one hidden charm slot, right-click equip/unequip,
  no GUI). The fallback is a compatibility shim, not a second framework.
- All gameplay effects (vis discount, runic shielding, hover) are written
  against our own "worn accessories" query, never against Curios types, so
  effects behave identically under either provider.

## Verified facts

Everything below was verified against real artifacts this session, not memory.

**Pack facts** (docs/ATM10_COMPATIBILITY.md, TL-021): ATM10 8.3 ships
`curios-neoforge-9.5.1+1.21.1.jar`; Curios consumers in-pack include All The
Arcanist Gear, Bauble-Heart Canisters, and a KubeJS script. No Baubles, no
Accessories-mod jar. Guidance on record: register **dedicated slot types**,
do not reuse generic `ring`/`belt` slots.

**Curios 9.5.1+1.21.1 API surface** — verified via `javap` against the actual
pack jar (`/home/aaron/minecraft mods/atm10-test/server/mods/
curios-neoforge-9.5.1+1.21.1.jar`):

| Concern | Verified API |
| --- | --- |
| Slot type registration | **Data-driven in 9.5.1** (corrected at TL-019 implementation): slot types load from datapack files `<namespace>:curios/slots/<slot-id>.json` (`CuriosSlotManager`, a `SimpleJsonResourceReloadListener` over `curios/slots`; slot id = the file path, so a dotted id like `thaumaturgicallegacy.charm` is one legal path segment; `size` in the JSON is the default per-entity size — `SlotHelper.createSlots` reads `ISlotType.getSize()`). The programmatic `CuriosApi.getSlotHelper().addSlotType(ISlotType)` (`SlotType.Builder` with `icon/order/size/...`) is deprecated-for-removal and the slot helper does not exist until `ServerAboutToStartEvent` — common-setup `enqueueWork` registration NPEs (observed). `Curios.setup` only builds legacy IMC slot types. |
| Presets | `SlotTypePreset`: `head, necklace, back, body, bracelet, hands, ring, belt, charm, curio`. |
| Entity→slot granting | Also data-driven: `<namespace>:curios/entities/<name>.json` with `{"entities": ["minecraft:player"], "slots": ["<slot-id>"]}` (`CuriosEntityManager`; optional `replace`, `conditions`). A registered slot type is granted to an entity type only through this — `CuriosApi.getEntitySlots(LivingEntity)` resolves per-EntityType from these files, and `isStackValid` on a custom slot id passes only when the entity actually has that slot. |
| Item→slot routing | Item **tags**: `CuriosTags.createItemTag(id)` builds tag `curios:<id>` (bytecode-verified: `ResourceLocation.fromNamespaceAndPath("curios", id)`). A dotted custom id is a single legal path segment. |
| Item behavior | `ICurioItem` (onEquip/onUnequip/canEquip/canUnequip/curioTick/onEquipFromUse/canEquipFromUse/getAttributeModifiers/curioBreak/getDropRule/writeSyncData/readSyncData/canSync) — **or** `CuriosApi.registerCurio(Item, ICurioItem)`, which attaches behavior without the item class implementing any Curios type. This is the load-safety mechanism for optional integration. |
| Access | `CuriosApi.getCuriosInventory(LivingEntity) → Optional<ICuriosItemHandler>`; `ICuriosHelper.findFirstCurio/findCurios`; NeoForge `EntityCapability` `CuriosCapability.INVENTORY` / `ITEM_HANDLER`, `ItemCapability` `ITEM` (modern NeoForge capabilities — not Forge caps, not attachments). |
| Events | `CurioChangeEvent`, `CurioCanEquipEvent`, `CurioCanUnequipEvent`, `CurioDropsEvent`, `DropRulesEvent`, `CurioAttributeModifierEvent`, `SlotModifiersUpdatedEvent`. |
| Slot unlocking | `CuriosApi.addSlotModifier(Multimap|ItemStack, slotId, RL, amount, op)` — the idiomatic "+N slots" grant. |
| Pack interop | `curios-common.toml` has a `slots = []` config — pack makers can create/resize slot types without code. |
| Requirements | Curios mods.toml requires NeoForge `[21.1.60,)` — our 21.1.259 satisfies it. License LGPL-3.0-or-later; we use it as an external dependency only, no code vendored. |

**Originals** (docs/reverse-engineering/): Baubles 1.0.1.10 is 4-slot/3-type,
CC BY-NC-SA — model may inform, no code reuse. The behavioral spec
(ACCESSORY_MECHANICS.md, TL-008) fixes the contracts we must preserve:
server authority, item-side `canEquip`/`canUnequip` vetoes, exactly-once
lifecycle callbacks, death-drop semantics under `keepInventory`, full
client sync including singleplayer. Thaumcraft accessory items to model:
runic set (amulet/ring/girdle), vis amulet (TC pays vis from it first —
CORE_MECHANICS.md), hover girdle.

## Options considered

| Criterion | 1. Optional Curios only (feature gated on presence) | 2. Custom accessory system | 3. **Hybrid (chosen)** |
| --- | --- | --- | --- |
| Compatibility (ATM10 8.3) | native | coexists; two accessory UIs side by side | native in-pack |
| Multiplayer reliability | Curios-proven | we own sync (risk) | Curios-proven; fallback sync is one tiny payload |
| Persistence | Curios (UUID-keyed, death-safe) | we own it (attachment + death copy + corruption edge cases) | Curios; fallback uses vanilla player-save attachments |
| Modpack interop | full (config, KubeJS, slot modifiers, pack gear) | none — invisible to pack tooling | full |
| Maintainability | smallest surface | largest: GUI, keybind, config, sync, death, migration | bridge + bounded shim |
| Performance | negligible (capability-based) | negligible | negligible |
| Install simplicity | core JAR standalone, but **feature silently absent** without Curios | single JAR, full feature | single JAR, full feature everywhere; best experience with Curios (already in-pack) |

## Rationale

1. The hard constraint ("core mod MUST work without Curios") is satisfiable by
   all three options; what separates them is what happens to the *feature*
   without Curios. Option 1 deletes it. Thaumcraft's identity is
   accessory-centric — the vis amulet sits inside the vis loop itself — so a
   silently missing feature in standalone installs is a product defect, not a
   graceful degradation.
2. In the target environment (ATM10), Curios is already installed. Re-using it
   buys the UI, per-slot sync, join sync, death handling, persistence,
   pack-maker config, and interop with existing pack gear at zero maintenance
   cost — exactly the surfaces the Baubles spec catalogues as defect-prone
   (ACCESSORY_MECHANICS.md §E).
3. The verified `CuriosApi.registerCurio(Item, ICurioItem)` makes the hybrid
   clean: our item classes never reference Curios types; the bridge is a
   separate class set loaded only when `ModList.get().isLoaded("curios")`.
   No mixin tricks, no reflection.
4. The fallback's cost is bounded *by design*: one attachment-backed slot,
   right-click equip (the pattern the Baubles spec D4 already endorses as
   item behavior), no GUI/keybind in 0.2.0. It exists so the feature is
   never absent, not to compete with Curios.

## Architecture

```
com.jonnyutah12.thaumaturgicallegacy.accessory
  ├─ core (pure, no MC/Curios types): effect descriptors, cost-modifier rules
  ├─ ThaumicAccessory (our marker on items: slot kind + effect hooks)
  ├─ WornAccessories (single query: equipped accessories for a player)
  ├─ provider SPI: AccessoryProvider { query, validate, equip/unequip }
  ├─ FallbackProvider  (always loadable; player AttachmentType)
  └─ curios/ bridge    (loaded only when Curios present)
```

- **Provider selection at startup, exactly one active.** With Curios: the
  bridge registers item behavior via `registerCurio` (the charm slot type
  itself is a data file, not code); the fallback provider is inert (no
  double-equip, no duplicate state). Without Curios: fallback provider active.
- **Effects consume `WornAccessories` only.** The vis-cost discount hooks the
  `SpellCatalog` cost path (mirroring TC's "pay from the vis amulet first"),
  runic shielding hooks damage events, hover hooks movement — none imports
  `top.theillusivec4.curios.*`.
- **Dedicated slot type**: id `thaumaturgicallegacy.charm` (display "Thaumic
  Charm"), size 1, our icon, item tag `curios:thaumaturgicallegacy.charm`.
  Deliberately not `ring`/`necklace`/`belt` presets: avoids crowding slots
  shared with Arcanist Gear and heart canisters (TL-021 conflict note 2) and
  lets us gate extra slots by research via `CuriosApi.addSlotModifier` —
  "+1 charm slot" as a research reward is a progression mechanic the preset
  slots cannot express.
- **Lifecycle contracts** (Baubles spec → Curios mechanics, all native):
  server authority (Curios owns the handler), `canEquip`/`canUnequip`
  (ICurioItem), worn-tick (`curioTick`, server-side), persistence
  (player save, UUID-keyed), death (`CurioDropsEvent`/`DropRulesEvent`
  honoring `keepInventory`), join + per-slot sync (Curios), third-party read
  access (`getCuriosInventory` / `findFirstCurio`).
- **Fallback contracts** (mirror the same spec, minimal):
  `AttachmentType` on `Player` with a serializer; copy on death via
  `PlayerEvent.Clone`; death drops honor `keepInventory`; one S2C sync
  payload on join and on change (server-authoritative; client mirror is
  display-only); right-click equip/unequip with the same validation as
  `canEquip`.
- **Build wiring**: `compileOnly` + `localRuntime` Curios dependency (the
  pattern already scaffolded in build.gradle's commented JEI example — do not
  bundle), plus `[[dependencies.thaumaturgicallegacy]] modId="curios",
  type="optional", ordering="AFTER", side="BOTH"` in mods.toml.

## Rejected options

1. **Hard Curios dependency.** Violates the core-standalone constraint;
   standalone installs would need a second download for a feature of ours.
2. **Custom accessory system only.** Full control, but we would own the whole
   defect-prone surface the Baubles spec catalogues (GUI, keybind, config,
   sync, death, migration) while duplicating a framework the target pack
   already ships — two accessory UIs, two persistence systems, zero interop
   with pack tooling. Contradicts TL-021's recorded guidance to build on
   Curios.
3. **Optional Curios integration with no fallback** (feature simply absent
   without Curios). Cheapest option and loader-safe, but it makes our
   signature items (vis-amulet analogue, runic pieces) dead weight in
   standalone installs. Rejected on product grounds; the hybrid's shim costs
   little more.
4. **Reusing preset slots (`necklace`/`ring`/`belt`) instead of a dedicated
   slot.** Matches TC's layout but re-creates slot crowding with in-pack
   Curios gear and cannot express research-gated slot unlocks. Rejected in
   favor of `thaumaturgicallegacy.charm`.

## Consequences / TL-019 checklist

- Implement per the layout above; TL-019's acceptance list (slot definition,
  validation, insertion/removal, persistence, equipped effects, death/respawn,
  client/server sync, no-Curios startup gametest) applies unchanged.
- Verified at implementation (TL-019): tag file naming for the dotted slot id
  works as a single path segment (`data/curios/tags/item/thaumaturgicallegacy.charm.json`);
  slot registration is data-driven (`data/thaumaturgicallegacy/curios/slots/thaumaturgicallegacy.charm.json`,
  `size: 1` is the default size — see the corrected API row above) AND the slot
  must be granted to players via `data/thaumaturgicallegacy/curios/entities/charm.json`
  (both files required; observed: without the entities file the slot registers
  but `isStackValid`/insertion never see it);
  `addSlotModifier` on a custom slot id is exercised later (research reward).
- ATM10 end-to-end check stays in TL-023 (isolated test env already has the
  Curios jar).

## Explicitly deferred

Cosmetic rendering of worn accessories (`ICurioRenderer`/`CuriosRendererRegistry`),
a fallback GUI/keybind, a second provider for the Accessories mod, and any
TC-style multi-slot layout. None blocks 0.2.0.

## Clean-room compliance

Curios is an external dependency (LGPL-3.0-or-later), declared `compileOnly`
and never bundled; no Curios code is copied. Baubles (CC BY-NC-SA) and
Thaumcraft (All Rights Reserved) contribute **behavioral models only** — no
code, assets, lang strings, or numbers; item names are ours, consistent with
docs/REVERSE_ENGINEERING_POLICY.md.
