# Asset Provenance — TL-006

Every asset in this repository must have one of these provenances:

| Category | Policy |
| --- | --- |
| Textures / models | Authored for this project (original pixel art by/for this project). No texture extracted from Thaumcraft or Baubles JARs. |
| Sounds | Original or explicitly freely-licensed (record source + license here). None from original JARs. |
| Lang / text | Written for this project. Item names describe our own content; no copied lang strings. |
| Code | 100% independent implementation (see REVERSE_ENGINEERING_POLICY.md). |
| Data (recipes, research JSON) | Authored for this project; gameplay numbers may be *informed* by documented original behavior but are our own values. |

## Register

(To be filled as assets are added. Format: `path | origin | author | license`.)

- (none yet — no assets committed as of this document)

## Explicit exclusions

- `Thaumcraft-1.7.10-4.2.3.5.jar` and `Baubles-1.7.10-1.0.1.10.jar` are never
  committed, bundled, or redistributed.
- Decompiled sources and extracted originals live only in the scratch directory
  outside this repository.
