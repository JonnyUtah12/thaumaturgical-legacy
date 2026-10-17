# REA Workflow for Java Mod JARs — TL-002

Date: 2026-10-16. REA plugin: Hermes `rea` plugin, MCP server `rea-agents@6.4.0`
(stdio over npx). Bundled skill: `reverse-engineer-anything` (v37).

## Tool-discovery workflow actually used

1. `skill_view('agent-plugin-rea-c4599aab:reverse-engineer-anything')` — routing rules.
2. `tool_search(["rea open binary archive inspect", "rea artifact inventory findings"])`
   → candidate tools.
3. `tool_describe(["mcp__rea__open_binary", "mcp__rea__inspect_artifact"])` — real schemas.
4. `tool_call` invocations recorded below.

No tool names or arguments were invented; every call used the described schema.

## What REA did on these JARs (observed)

| Operation | Tool | Result (observed) |
| --- | --- | --- |
| Open + validate archive | `open_binary(path=…)` | Both JARs: `format: "zip"`, `kind: "archive"`, SHA-256 digests (recorded in ENVIRONMENT_AUDIT.md). |
| Full artifact inventory | `inspect_artifact(integrity_policy="record-and-continue")` | Complete content-addressed artifact graph for Baubles JAR: 69 members, per-member digests, integrity observations, evidence IDs (e.g. `ev_904776d627e5…`, inspection `ai_04380c4e892a…`). Response 81.6 KB, retained on disk by the runtime. |

## What REA does **not** do here (verified limitation)

REA's archive route provides inventory/graph/integrity evidence; it did **not**
decompile Java `.class` files into source for these targets, and no Java
decompiler backend was advertised by the session's tool list. Therefore Java
source analysis was performed with **Vineflower 1.11.1** (Apache-2.0, official
GitHub release, run under the local JDK 21) and `javap` where bytecode-level
inspection sufficed. Statements about decompiled source in this project are
attributed to Vineflower, never to REA.

## Reproducible commands

```bash
# REA (via Hermes tool bridge): open_binary + inspect_artifact — see table above.

# Full decompilation (output kept OUTSIDE the repository, in the scratch dir):
~/opt/jdk21/bin/java -jar ~/opt/tools/vineflower-1.11.1.jar \
  "/home/aaron/minecraft mods/Baubles-1.7.10-1.0.1.10.jar" decompiled/baubles
~/opt/jdk21/bin/java -jar ~/opt/tools/vineflower-1.11.1.jar -e \
  "/home/aaron/minecraft mods/Thaumcraft-1.7.10-4.2.3.5.jar" decompiled/thaumcraft
# Result: 26 Java files (Baubles), 859 Java files (Thaumcraft; inner classes merged).

# Bytecode inspection (no decompiler):
~/opt/jdk21/bin/javap -p -c <class>
```

## Evidence discipline

- Every inventory claim cites either a member listing (`unzip -l`), a REA
  evidence ID, or a decompiled file path under `decompiled/`.
- Decompiled output is an interpretation of bytecode; important behaviors are
  cross-checked against resource files (JSON/lang/config) or multiple classes.
- Neither original JAR was executed at any point.
