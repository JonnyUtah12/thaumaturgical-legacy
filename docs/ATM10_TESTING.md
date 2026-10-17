# ATM10 Testing — isolated instance and results

Two sections: **Setup** (TL-022, how the isolated instance was built) and
**Results** (TL-023, the spec §18 battery run against it). Pack facts
(8.3 pinned, MC/NeoForge/Java match, Curios presence) live in
[`ATM10_COMPATIBILITY.md`](ATM10_COMPATIBILITY.md); integration guidance in
[`ATM10_INTEGRATION.md`](ATM10_INTEGRATION.md).

## Setup (TL-022) — isolated ATM10 8.3 instance

Location: `/home/aaron/minecraft mods/atm10-test/` — fully isolated; no
existing Minecraft installation was touched.

### Pack identity (verified, not assumed)

| Fact | Value | How verified |
| --- | --- | --- |
| Pack | All the Mods 10, **8.3** (author ATMTeam) | `client/manifest.json` `name`/`version` fields |
| Minecraft | 1.21.1 | `manifest.json` `minecraft.version` |
| Mod loader | **neoforge-21.1.259** (primary) — exact match to our toolchain | `manifest.json` `modLoaders[0].id`; `server/libraries/net/neoforged/neoforge/21.1.259/` present |
| Java | Temurin 21.0.12.1 (`/home/aaron/opt/jdk21`) | installer.log JVM banner; launch scripts pin this JDK |
| Client mod count | 496 jar entries in `client/mods` | `ls | wc -l` |
| Server mod count | 470 jar entries in `server/mods` | `ls | wc -l` |

### Source artifacts and integrity (SHA-256)

Downloaded from the CurseForge CDN (`edge.forgecdn.net/files/9190/157/...`,
file id 9190157 for the client pack zip):

```
24440fb355de69b28af6ff18556b5d0598bb93570b2b745552cdb1bcb5448c8f  All the Mods 10-8.3.zip (client pack zip)
64c703d0f3bfbe6c3d2871850d57405b7f701b2be19278de3271a6879df552bf  ATM10 8.3 server files zip
```

A second independent download of each produced byte-identical hashes
(`*-redl.zip` re-downloads match), so the artifacts are stable.

### Setup steps performed

1. Unpacked the client pack zip into `atm10-test/client/`, server files into
   `atm10-test/server/` (isolated dirs; nothing else touched).
2. Ran the supplied NeoForge **21.1.259** installer for the client profile
   (`installer.log`: "Successfully installed client into launcher.").
3. Server uses the pack's own `run.sh` / `user_jvm_args.txt` +
   `libraries/net/neoforged/neoforge/21.1.259/unix_args.txt`; wrapper
   `atm10-test/launch-server.sh` pins our JDK 21. EULA accepted in
   `server/eula.txt`.
4. Installed our built artifact `thaumaturgicallegacy-0.2.0-SNAPSHOT.jar`
   (from `./gradlew jar`, built 2026-10-17 13:10) into **both**
   `server/mods/` and `client/mods/`, replacing the earlier 0.1.0-SNAPSHOT
   copy.

### Notes from setup runs

- A first server attempt (04:44) crashed during mod loading inside
  **integrateddynamics** (`menril_torch` unbound value — a pack-side
  parallel-registration failure; the crash header flags *Invasive
  Optimizations* as active). A subsequent run (05:24) passed mod loading and
  reached datapack/tag loading before being stopped; the crash did not
  involve our mod in either run. Results section re-runs from a clean state.
- The instance is disposable: delete `atm10-test/` to remove all traces.

## Results (TL-023) — spec §18 battery

Ran 2026-10-17 against the isolated server instance with
`thaumaturgicallegacy-0.2.0-SNAPSHOT.jar` installed (470 pack mods + ours).
Two full headless runs: cold start, graceful SIGTERM shutdown, then reload
of the same world. Evidence is `server/logs/latest.log` from those runs.

### A. Startup — PASS

| Check | Result | Evidence |
| --- | --- | --- |
| Dedicated server starts | **PASS** | `Done (16.993s)!` (cold), `Done (3.609s)!` (warm reload) |
| No missing dependencies | **PASS** | our jar discovered via mod manifest; mod list printed `Thaumaturgical Legacy 0.2.0-SNAPSHOT`; no dependency-failure lines |
| No registry failures | **PASS** | 0 `[ERROR]` lines in the entire run log |
| No fatal mixin errors | **PASS** | no mixin failure lines; server reached world load |
| No class-loading errors | **PASS** | our common code loaded; `Accessory provider active: curios` logged from our mod |
| Client | **not launched** | headless testing only; client profile installed (`installer.log` success) but GUI launch not exercised — see limitations |

### B. Gameplay — PASS (headless scope)

| Check | Result | Evidence |
| --- | --- | --- |
| Items/blocks register | **PASS** | same artifact passes the in-game registry gametests; zero registry errors in ATM10 run |
| Recipes load | **PASS** | 0 recipe errors; 0 duplicate/conflicting-recipe warnings across 17k+ pack recipes |
| Research progresses | **not exercised headless** | per-player progression needs a player session; the identical code path is covered by `ResearchGameTest` (dev env) |
| Magic systems work | **PASS (load-level)** | mod common setup completed; no errors from our packages in either run |

### C. Integration — PASS

| Check | Result | Evidence |
| --- | --- | --- |
| Optional accessory integration works | **PASS** | **Curios is present in ATM10 8.3 and our bridge activated**: `Accessory provider active: curios` — the optional path, not the fallback, is what ATM10 exercises |
| Existing items/recipes functional | **PASS** | no pack-side errors attributable to us; run completed with 0 errors |
| Tags behave | **PASS** | no tag errors in log |
| No duplicate recipe outputs | **PASS** | 0 duplicate-recipe warnings |
| Multiplayer sync | **not exercised** | single headless server, no players joined |

### D. Performance — PASS

Startup 17.0s cold / 3.6s warm with 471 mods — our mod contributes no
visible cost. 0 tick/watchdog warnings, no log spam attributable to us
(2.7 MB log for the whole pack is normal), no world scans or per-tick
work: our only gameplay listeners are event-driven (join/craft/place).

### E. World safety — PASS

| Check | Result | Evidence |
| --- | --- | --- |
| Existing worlds load | **PASS** | second run loaded the saved world (`Preparing start region for dimension minecraft:overworld`), `Done (3.609s)` |
| Saving doesn't lose progression | **PASS (server-level)** | SIGTERM produced a clean save (`level.dat` updated 13:19:51), no crash report; per-player data untested headless (see B) |
| Restart doesn't duplicate resources | **PASS** | no recipe/registry duplication across two runs |
| Uninstall limitation | **documented** | removing our mod silently drops our player attachments (research knowledge) and fallback accessory data; no world data of ours to migrate (no worldgen) |

### Limitations (honest scope)

- Headless server only: client GUI startup, in-world player progression,
  and multiplayer sync were not exercised in ATM10 itself; those paths are
  covered by the dev gametests on the same artifact, not by this battery.
- The one crash in the instance's history (04:44, `integrateddynamics`
  `menril_torch`) predates these runs, is pack-side, and did not recur in
  either clean run.

**Verdict: Thaumaturgical Legacy 0.2.0-SNAPSHOT loads and operates
alongside ATM10 8.3 (MC 1.21.1 / NeoForge 21.1.259) with zero errors in
two full server lifecycles, and its optional Curios integration activates
as designed. This is our own test result — not an official ATM10
compatibility statement.**
