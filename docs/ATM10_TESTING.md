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

_(run recorded below; nothing claimed without a passing check)_
