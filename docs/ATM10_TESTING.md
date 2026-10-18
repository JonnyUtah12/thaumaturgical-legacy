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

Downloaded from the CurseForge CDN (`edge.forgecdn.net/files/9190/157/`,
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

- A first server attempt (04:44) crashed during mod loading. See
  *Limitations* for the corrected post-mortem (it involved both a
  pack-side integrateddynamics failure and a caught secondary error from
  our then-old 0.1.0 jar). A subsequent run (05:24) passed mod loading and
  reached datapack/tag loading before being stopped. Results section
  re-runs from a clean state with the 0.2.0 jar.
- The instance is disposable: delete `atm10-test/` to remove all traces.

## Results (TL-023) — spec §18 battery

Ran 2026-10-17 against the isolated instance with
`thaumaturgicallegacy-0.2.0-SNAPSHOT.jar` installed (470 pack mods + ours
on the server, 496 + ours on the client). Evidence is the instance's own
logs (`server/logs/`, rotated `*.log.gz`, client stdout logs,
`client/crash-reports/`) cross-checked against the kernel journal.

**Test-harness note (reproducibility).** The automation worker runs inside
a 4 GiB systemd memory cgroup; Minecraft processes must be started outside
it (`systemd-run --user --scope -p MemoryMax=…`). Earlier "SIGKILL"
failures in this task's history were that cgroup's OOM killer, not the
mod. The host has 14 GiB RAM: the full client (~7.2 GiB RSS) and the
server (~3.4 GiB RSS) do not fit simultaneously with headroom — this is
why the live multiplayer join (section C) could not be completed.

### A. Startup — PASS

| Check | Result | Evidence |
| --- | --- | --- |
| Dedicated server starts | **PASS** | `Done (16.993s)!` cold (13:17), `Done (3.609s)!` warm reload (13:23); three more warm reloads 20:36 (2.8s), 21:40 (3.0s), 22:15 (2.6s) |
| No missing dependencies | **PASS** | our jar discovered via mod manifest; mod list printed `Thaumaturgical Legacy 0.2.0-SNAPSHOT`; zero dependency-failure lines in any run |
| No registry failures from our mod | **PASS (ours)** | the two cited runs contain 136/132 ERROR lines — **all pack-side**: mekmm loot tables ×83, config-timing EventBus (Mekanism/TwilightForest/Aether/Reliquary) ×32, DISTXFORM client-class-on-server ×7, allthemodium ItemStack keys ×4, Bumblezone trades ×4, DataMapLoader (enderio/reliquary fluids) ×2, Curios slot parse for another mod's slot (`rsinsertexportupgrade`) ×2, pamhc2crops tag ×1, dungeons_arise advancements ×1. **Zero mention `thaumaturgicallegacy`** |
| No fatal mixin errors | **PASS** | zero mixin ERROR lines in either run (only optional-class WARNs from other mods) |
| No class-loading errors from our mod | **PASS** | `Thaumaturgical Legacy 0.2.0-SNAPSHOT loading (NeoForge, MC 1.21.1)` + `Accessory provider active: curios` logged in every run |
| Client starts | **PASS** | headless (Xvfb + llvmpipe software GL, pack-default `-Xmx8G`): reached title screen — `Sound engine started`, `OpenAL initialized`, ModernFix `Game took 180.308 seconds to start` (software rendering; not our mod's cost). Our mod logged `Thaumaturgical Legacy client setup complete`. 47 ERROR lines in the client run, all pack-side (same EventBus/GeckoLib/asset classes), zero ours |

### B. Gameplay — PASS (headless scope)

| Check | Result | Evidence |
| --- | --- | --- |
| Items/blocks register | **PASS** | same artifact passes the in-game registry gametests; zero registry errors in ATM10 runs |
| Recipes load | **PASS** | 0 recipe errors; 0 duplicate/conflicting-recipe warnings across the pack's recipe set |
| Research progresses | **not exercised in ATM10** | per-player progression needs an in-game session; the identical code path is covered by `ResearchGameTest` (dev env) |
| Magic systems work | **PASS (load-level)** | common + client setup completed on both sides; no errors from our packages in any run |
| Item models | **FIXED (TL-024)** | original run: client log `Unable to load model: 'thaumaturgicallegacy:item/vis_tap' … FileNotFoundException: thaumaturgicallegacy:models/item/vis_tap.json` (file absent from sources/jar; cosmetic). Fixed by adding the block-item model; re-tested 22:51/23:13 runs below — 85 missing-model warnings in the client run, **zero** mention `thaumaturgicallegacy` |

### C. Integration — PASS

| Check | Result | Evidence |
| --- | --- | --- |
| Optional accessory integration works | **PASS** | **Curios is present in ATM10 8.3 and our bridge activated on both server and client**: `Accessory provider active: curios` — the optional path, not the fallback, is what ATM10 exercises |
| Existing items/recipes functional | **PASS** | no pack-side errors attributable to us; runs completed with zero errors mentioning our mod |
| Tags behave | **PASS (ours)** | 1 pack-side tag error (pamhc2crops `minecraft:tags/item/rabbit_food`); zero tag errors from our mod |
| No duplicate recipe outputs | **PASS** | 0 duplicate-recipe warnings |
| Multiplayer sync | **not verified on this host** | four join attempts (server up + client `--quickPlayMultiplayer localhost:25565`) all ended in kernel/cgroup OOM kills while the client loaded resources: client footprint ~7.2 GiB + server ~3.4 GiB exceeds the 14 GiB host. No join completed; no crash report from our code in any attempt (the one JVM heap OOM was during pack resource loading at `-Xmx4G`). Sync logic is covered by dev gametests, not by this battery |

### D. Performance — PASS

Server startup 17.0s cold / 2.6–3.6s warm with 471 mods — our mod
contributes no visible cost. **0 tick/watchdog warnings in the cited
runs**; the only `Can't keep up` stalls all day (×1 each) occurred in two
server runs that had a client loading concurrently — host memory
thrashing, not our mod. No log spam attributable to us. Our only gameplay
listeners are event-driven (join/craft/place); no world scans or per-tick
work.

### E. World safety — PASS

| Check | Result | Evidence |
| --- | --- | --- |
| Existing worlds load | **PASS** | the saved world reloaded in every warm run (`Preparing start region…` then `Done`), 2.6–3.6s |
| Saving doesn't lose progression | **PASS (server-level)** | SIGTERM produced clean saves three times (`level.dat` updated 13:25, 22:08, 22:37); no crash reports from any 0.2.0 run; per-player data untested headless (see B/C) |
| Restart doesn't duplicate resources | **PASS** | no recipe/registry duplication across five server lifecycles |
| Uninstall limitation | **documented** | removing our mod silently drops our player attachments (research knowledge) and fallback accessory data; no world data of ours to migrate (no worldgen) |

### Defects found (input for TL-024) — all resolved

1. Missing item model `assets/thaumaturgicallegacy/models/item/vis_tap.json`
   — Vis Tap rendered as the missing-model placeholder in the client.
   Root cause: the `vis_tap` BlockItem was registered
   (`ModItems.registerSimpleBlockItem`) but no item model was ever added to
   sources — the block model (`models/block/vis_tap.json`) and blockstate
   exist, and the other three items have item models; vanilla logs the WARN
   and substitutes the placeholder. Cosmetic, no functional impact.
   Fix: added `models/item/vis_tap.json` (`parent:
   thaumaturgicallegacy:block/vis_tap`) — the standard block-item model.

### Fix re-test (TL-024) — 2026-10-17, same isolated instance

Rebuilt `thaumaturgicallegacy-0.2.0-SNAPSHOT.jar` with the model file
(sha256 `406e4887…7aef157`, identical bytes deployed to `client/mods` and
`server/mods`) and re-ran both sides:

- **Server** (22:49–22:51, scope `MemoryMax=11G`): jar discovered via mod
  manifest, `Thaumaturgical Legacy 0.2.0-SNAPSHOT loading` +
  `Accessory provider active: curios`, `Done (3.090s)!` warm reload; 177
  ERROR lines, **zero** mentioning our mod; SIGTERM → exit 143, clean
  `level.dat` save at 22:51, no OOM (scope peak 7.3G).
- **Client** (23:11–23:13, headless Xvfb + llvmpipe, `-Xmx8G`, scope
  `MemoryMax=12G` + swap): our mod loaded, Curios bridge active,
  `Thaumaturgical Legacy client setup complete`, title screen reached
  (`Sound engine started`, ModernFix `Game took 159.809 seconds`). 79
  ERROR lines, zero ours. **85 `Unable to load model` warnings — all
  pack-side (silentgear, potionsmaster, etc.); zero mention
  `thaumaturgicallegacy`.** The defect's exact signature from the 22:28
  baseline run (`Unable to load model: 'thaumaturgicallegacy:item/vis_tap'`)
  is gone.
- Harness note (adds to the one above): scope memory must account for
  page cache inside the cgroup — the client scope peaked at **11.2G**
  (a 10G scope with `MemorySwapMax=0` was OOM-killed at 22:55 during
  resource loading; the successful run used `MemoryMax=12G` +
  `MemorySwapMax=3G`). The server peaked at 7.3G during AlmostUnified
  recipe unification (a 6G scope OOM-killed at 22:43).

No other defects were found by the battery, so none needed fixing; the
pack-side error inventory (136/132 server, 47→79 client per run) belongs
to the pack, not to us, and is itemized in section A.

### Limitations (honest scope)

- Headless client (Xvfb + llvmpipe): title screen reached, but no in-game
  play session was driven; in-world progression and live multiplayer sync
  were not exercised in ATM10 itself (host memory, see C). Those paths are
  covered by the dev gametests on the same artifact, not by this battery.
- The 04:44 crash (old **0.1.0** jar): the fatal cause was pack-side
  (integrateddynamics `menril_torch` unbound value under parallel
  registration; crash header flags *Invasive Optimizations*). Our 0.1.0
  jar additionally logged a **caught secondary** `ExceptionInInitializerError`
  (`Cannot register new entries to DeferredRegister after RegisterEvent
  has been fired`) — a cascade of the same parallel-loading failure.
  Neither error recurred in any of the six 0.2.0-jar runs.
- The `memorysettings` pack mod shows a modal dialog when the heap is
  below its configured minimum (5 850 MB on this system), which blocks
  headless client startup invisibly. Use the pack-default `-Xmx8G`, or set
  `disableWarnings=true` in `client/config/memorysettings.json` (we set
  this during low-heap join attempts; it is a test-instance config only).

**Verdict: Thaumaturgical Legacy 0.2.0-SNAPSHOT loads and operates
alongside ATM10 8.3 (MC 1.21.1 / NeoForge 21.1.259) on both the dedicated
server (five lifecycles, plus the TL-024 re-test) and the client (title
screen reached, plus the TL-024 re-test), with zero errors attributable to
our mod, and its optional Curios integration activates as designed on both
sides. The one cosmetic defect found (missing `vis_tap` item model) was
fixed in TL-024 and verified gone by re-running both sides against the
same instance. Live multiplayer sync remains unverified on this host due
to its 14 GiB memory ceiling. This is our own test result — not an
official ATM10 compatibility statement.**
