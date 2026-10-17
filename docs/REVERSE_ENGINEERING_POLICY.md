# Reverse-Engineering Policy — TL-006

## What we may and may not do with the original artifacts

### Thaumcraft 4.2.3.5 (Azanor)

- **No license grant found.** The JAR contains no LICENSE/NOTICE file; the
  decompiled API classes carry no license headers; the CurseForge project page
  (extracted 2026-10-16) shows no explicit license. Treat as **All Rights
  Reserved / closed source**.
- Permitted: static analysis of the artifact we possess; documenting observed
  structure and behavior; using that documentation as a specification for an
  independent implementation.
- Not permitted (absent explicit permission): committing the JAR, publishing
  decompiled source, redistributing textures/models/sounds/lang strings,
  repackaging any part of it.

### Baubles 1.7.10-1.0.1.10 (Azanor / chicken_bones)

- **License: CC BY-NC-SA 3.0 Unported** — stated in the official repository
  README (`github.com/Azanor/Baubles`, fetched 2026-10-16).
- NC + SA are incompatible with a permissively-licensed standalone mod intended
  for broad modpack distribution. Therefore we do **not** vendor or adapt
  Baubles code or assets; we implement an independent accessory system informed
  by the documented API contract.

## Decisions

1. **Independent implementation only.** Thaumaturgical Legacy is written from
   scratch against documented behavior. No decompiled class is a source input to
   any committed file. Decompiled output stays outside this repository
   (`scratch/decompiled/`) and is cited as analysis evidence only.
2. **New project license: MIT.** Our code has no derivative relationship to the
   originals (no copied code/assets), so MIT maximizes modpack compatibility
   (ATM10-style packs) and matches the user's prior projects.
3. **Names**: "Thaumcraft" and "Baubles" are used only descriptively
   (compatibility/policy text), never as mod names, mod ids, or branding.
4. **If permission is later clarified** (e.g. asset authorization from
   Azanor), asset reuse would be reconsidered and recorded in
   `ASSET_PROVENANCE.md`. Until then: zero original assets.

## Uncertainties recorded

- Thaumcraft 4's exact legal terms are not published; the All-Rights-Reserved
  treatment is the conservative default, not a confirmed statement by the author.
- Baubles' CC BY-NC-SA applies to the 1.7.10 source repo; the JAR itself embeds
  no license text (verified: no LICENSE entry in the archive).
