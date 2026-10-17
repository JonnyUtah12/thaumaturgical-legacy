# Environment Audit — TL-001

Date: 2026-10-16 (EDT). Host: Linux (7.0.0-38-generic). All findings below were
produced by direct inspection in this session; nothing is assumed.

## Supplied artifacts (verified)

| Artifact | Path | Verification |
| --- | --- | --- |
| Thaumcraft | `/home/aaron/minecraft mods/Thaumcraft-1.7.10-4.2.3.5.jar` | ZIP/JAR, 12,565,807 bytes, 2136 entries, 942 `.class`. `mcmod.info`: modid `Thaumcraft`, version `1.7.10-4.2.3.5`, mcversion `1.7.10`, author Azanor, `dependencies: ["Baubles"]`. SHA-256 `3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be` (via REA `open_binary`). |
| Baubles | `/home/aaron/minecraft mods/Baubles-1.7.10-1.0.1.10.jar` | ZIP/JAR, 95,415 bytes, 69 entries. `mcmod.info`: modid `Baubles`, version `1.7.10-1.0.1.10`, author Azanor. SHA-256 `05ae8f11b8f9012ebebbf179227b010829739c6284e30da5dd3db220ee682f8a` (via REA `open_binary`). |
| NeoForge installer | `/home/aaron/minecraft mods/neoforge-21.1.259-installer.jar` | ZIP, 6,972,025 bytes. Embedded `version.json`: `"id": "neoforge-21.1.259"`, release type, releaseTime 2026-10-10. Installer `SimpleInstaller` 3.0.48. |

Neither original JAR was executed. Both remain unmodified.

## Toolchain

| Tool | Status |
| --- | --- |
| Java | **Temurin JDK 21.0.12.1+1 LTS at `~/opt/jdk21`** (includes `javap`). Not on PATH; used via absolute path. Matches the Java 21 requirement for MC 1.21.1. |
| Gradle | Not installed system-wide. The NeoForge MDK Gradle wrapper will bootstrap its own pinned Gradle (requires network). |
| Git | 2.53.0. Identity for this repo set to `JonnyUtah12 / jonnyutah12@users.noreply.github.com` (same as prior repos). |
| GitHub CLI | 2.46.0 present, **not authenticated** (see Blockers). |
| unzip / zip | Present. |
| Decompiler | **Vineflower 1.11.1** installed at `~/opt/tools/vineflower-1.11.1.jar` (Apache-2.0, from the official GitHub release; verified runnable under JDK 21). Verified working on both JARs. |
| REA plugin | Installed (`rea` Hermes plugin, `rea-agents@6.4.0` MCP). Tool discovery via `tool_search`/`tool_describe`/`tool_call` works. See `reverse-engineering/REA_WORKFLOW.md`. |
| Disk | 359 GB free on `/`. Sufficient. |
| Network | `api.github.com` reachable (HTTP 200). |

## Existing workspaces

- `/home/aaron/minecraft mods/thaumaturgical-legacy` did **not** exist before this
  audit; created and `git init`-ed (branch `main`) during TL-001.
- Unrelated repos (`~/pingtrace`, `~/pingtracev2`, `~/ping-tracer`) were not touched.
- No existing `thaumaturgical-legacy` GitHub repo check was possible without auth (TL-011).

## Blockers

1. **GitHub authentication missing.** No `gh` login, no `GH_TOKEN`/`GITHUB_TOKEN`
   env, no `~/.netrc`, no `~/.git-credentials`, no SSH private keys, no GitHub
   connector in the Hermes connector list. Blocks TL-012 (repo creation) and
   pushing. All local work (analysis, code, local git history) proceeds regardless.
   Resolution requires the user to authenticate (`gh auth login`) or provide a
   token through a supported channel; no secret was pasted into chat or logs.
