# Thaumaturgical Legacy — Kanban Board

The **live Hermes Kanban board** (board `default`, assignee `default`) is the
authoritative task tracker. This file is a human-readable index and status
snapshot; regenerate it when reporting milestones.

Status mapping: `todo` (gated/backlog) · `ready` · `running` (in progress) ·
`blocked` · `done`. Task bodies carry acceptance criteria, test requirements,
and affected files; completion notes and verification evidence live in each
task's completion metadata and comments.

## Task index (spec §11 numbering)

| ID | Title | Depends on |
| --- | --- | --- |
| TL-001 | Audit the development environment | — |
| TL-002 | Verify REA and its supported analysis tools | — |
| TL-003 | Inspect the Thaumcraft artifact | TL-001 |
| TL-004 | Inspect the Baubles artifact | TL-001 |
| TL-005 | Establish the Java decompilation workflow | TL-001 |
| TL-006 | Investigate licensing and asset provenance | — |
| TL-007 | Document Thaumcraft mechanics | TL-003, TL-005 |
| TL-008 | Document Baubles mechanics | TL-004, TL-005 |
| TL-009 | Map the Thaumcraft–Baubles interaction | TL-007, TL-008 |
| TL-010 | Design the modern accessory architecture | TL-006 |
| TL-011 | Verify GitHub authentication and repository status | — |
| TL-012 | Create the GitHub repository | TL-011 |
| TL-013 | Initialize the NeoForge workspace | — |
| TL-014 | Configure versioning and changelogs | — |
| TL-015 | Establish automated builds and tests | — |
| TL-016 | Implement the initial magic framework | — |
| TL-017 | Implement the research system | — |
| TL-018 | Implement initial magical crafting | — |
| TL-019 | Implement accessory support / optional integration | TL-010, TL-016 |
| TL-020 | Complete the first playable gameplay loop | TL-017, TL-018 |
| TL-021 | Investigate the selected ATM10 release | — |
| TL-022 | Establish an isolated ATM10 test environment | TL-015, TL-021 |
| TL-023 | Test startup and gameplay with ATM10 | TL-020, TL-022 |
| TL-024 | Resolve identified compatibility issues | TL-023 |
| TL-025 | Prepare the first release | — |
| TL-026 | Prepare distribution documentation | TL-025 |
| TL-027 | Prepare an ATM10 integration proposal | TL-024, TL-026 |

## Snapshot — 2026-10-17 (after v0.1.0 release)

- **DONE:** TL-001…TL-008, TL-011…TL-016, TL-021, TL-025
- **IN PROGRESS / READY (dispatcher-active):** TL-009, TL-010, TL-014*,
  TL-015*, TL-017, TL-018, TL-022, TL-026 (*retrospective cards — work already
  verified in repo; workers close them with evidence)
- **GATED (todo):** TL-019, TL-020, TL-023, TL-024, TL-027

Milestones: v0.1.0 (vis-magic core) released 2026-10-17. Next: 0.2.0 with
research + crafting + accessories (TL-017/018/019/020), then ATM10 test
battery (TL-022/023/024) and distribution docs (TL-026/027).

Note: some commit messages from worker sessions used the spec §20 numbering
(e.g. "TL-009" for the playable system). The table above (spec §11) is the
canonical numbering for this board.
