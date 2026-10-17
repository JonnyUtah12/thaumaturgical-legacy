# Research System (v0.2) — Design Record

Status: implemented on `main` (TL-017 research progression, TL-018 research-gated
crafting). Target: Minecraft 1.21.1 / NeoForge 21.1.259, project 0.2.0-SNAPSHOT.

Grounded in the verified Thaumcraft 4 research model documented in
`docs/reverse-engineering/CORE_MECHANICS.md` §7 (all TC claims below cite it).

## What TC does (verified) and what we replicate

| TC 4.2.3.5 behavior (CORE_MECHANICS.md §7) | Replicated? | Modern form |
| --- | --- | --- |
| Knowledge is **per-player** (`PlayerKnowledge` on the player entity) | yes | `PlayerKnowledge` in a NeoForge **player data attachment** (`ModAttachments.RESEARCH`) — save/load, `/data` access and client sync come from the loader |
| Entries have **prerequisites** forming a tree | yes | `ResearchCatalog` definitions with `Set<ResearchId> prerequisites`; completion refused unless all prerequisites are known |
| Entries **auto-unlock on join** and on **world triggers** (crafting, right-clicking) | yes (join, craft, place) | `PlayerLoggedInEvent` → First Sight; `ItemCraftedEvent` → Wandwright; `BlockEvent.EntityPlaceEvent` → Tapping the Flow |
| Knowledge **gates recipes and spells** | yes, two mechanisms | spells: hard gate in `ThaumicWandItem` (research query); recipes: **knowledge item** (see below) |
| Research **currency** (aspect pools) spent on hex-note puzzles | **no** | deliberately out of scope for 0.2; the RE docs mark the puzzle UI as presentation, not mechanics |
| Knowledge **survives death** | yes | attachment `copyOnDeath()` |

## Architecture

```
magic/research/            pure logic, no Minecraft types (same rule as magic/)
  ResearchId               enum of entries + translation keys
  ResearchDefinition       record: prerequisites + unlocked spells
  ResearchCatalog          static tree (EnumMap), spellsUnlockedBy(known)
  PlayerKnowledge          completed EnumSet; canComplete/complete/completeRaw
event/ResearchEvents       the ONLY game-touching layer: subscribes triggers,
                           grants rewards, announces, answers isSpellKnown
registration/ModAttachments RESEARCH attachment: enum-name list codec,
                           sync(fromCodec), copyOnDeath
items/ThaumicWandItem      cast gate + cycle restricted to known spells
```

The trigger methods are split (`onCrafted(Player, ItemStack)` etc.) so the
gametest drives the exact production path without a login session.

## Recipe gating: knowledge item, not per-player recipe hook

TC checks a research key against the **acting player** inside its custom
recipe type. NeoForge 21.1 has no per-player recipe gate for vanilla
crafting: `Recipe#canSynthesize` is not consulted by the vanilla container
menu, and a custom RecipeType + `RecipeManager` check would have to bypass
the vanilla recipe book UI to be reliable.

Decision (TL-018): the **Vis Tap recipe requires a Research Note item** —
granted exactly once as the Wandwright completion reward. The gate is
therefore enforced by vanilla crafting itself (you cannot craft without the
item), survives `/gamerule keepInventory`, shows up in the recipe UI, and
needs no network or container surgery. Cost: the note is spendable/losable —
acceptable for 0.2 and documented as a known limitation.

## Spell gate

`ResearchEvents.isSpellKnown(player, spell)`: Firebolt always; every other
spell requires its unlocking entry. Enforced at cast time (defensive: stacks
can carry ordinals from older worlds or `/give`) and in the shift-click
cycle (only known spells are selectable). The attachment's `sync` keeps the
client-side cycle list consistent.

## Tree (v0.2)

```
First Sight (join) ──► Wandwright (craft wand; reward: Research Note)
                            └──► Tapping the Flow (place Vis Tap; unlocks Frost Lance)
```

A fresh player can craft the wand and cast Firebolt immediately (0.1.0 loop
unchanged); Frost Lance now requires the full loop — the progression step the
mission brief asks for ("use that tool to unlock further progression").

## Persistence & multiplayer

- Attachment serialization: lenient `Codec.STRING.listOf()` over enum names —
  unknown ids dropped on load (forward-compatible with newer saves;
  `Codec.enumCodec` does not exist on this DFU version, so the codec is
  hand-rolled).
- Synced to clients via `ByteBufCodecs.fromCodec` — the wand cycle reads the
  same state client-side.
- `copyOnDeath()` — knowledge survives death (TC model).
- Server-authoritative: only server-side triggers mutate; `setData` marks the
  save dirty and pushes the sync.

## Testing

- Unit: `PlayerKnowledgeTest` (prereq gating, idempotence, stable order),
  `ResearchCatalogTest` (acyclicity, entry point, spell unlocks).
- GameTest: `ResearchGameTest.researchChainGatesSpellsAndGrantsNote` —
  attachment-backed flow: locked → skip refused → join → craft trigger →
  note reward count == 1 → place trigger → Frost Lance unlocked → save-codec
  NBT round-trip equality + forward-tolerance on unknown ids (the unit-test
  classpath is deliberately Minecraft-free, so the codec assertions live in
  the gametest where the real runtime exists).

## Known limitations / next steps

- The note is a consumable item; losing it loses the recipe gate (re-crafting
  the wand does not re-grant it — Wandwright completes once). A future version
  can re-grant on demand or move to a custom RecipeType with a real
  per-player check.
- No research UI/GUI; chat announcements only.
- Aspect-pool currency and the note-puzzle presentation are not modeled.
