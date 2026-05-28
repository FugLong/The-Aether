# Fabric 26.1.2 port — findings, roadblocks, and plan

**Goal:** Run **The Aether** on **Minecraft 26.1.2**, **Fabric first** (26.1+ range ideal; NeoForge secondary).

**Primary upstream:** `The-Aether-Team/The-Aether` — `1.21.1-develop-fabric`  
**This fork work:** performance fixes ([#2850](https://github.com/The-Aether-Team/The-Aether/issues/2850)) + eventual 26.1.2 port branch.

---

## 1. Executive summary

| Track | Status | Notes |
|-------|--------|-------|
| **1.21.1 Fabric perf** | Done (fork) | Branch `perf/2850-fabric-1.21.1`, commit `fix(perf): … (#2850)` — addresses Spark tick hotspots on servers you can run **today**. |
| **26.1.2 Fabric port** | **Blocked** | Missing Maven artifacts + JDK 25 + non-trivial API port after deps resolve. |
| **26.1.2 NeoForge port** | Partially unblocked | Nitrogen + Cumulus NeoForge **26.1.2** exist on Maven; Accessories embedded via `jarJar` on Neo — still a large port. |

**Bottom line:** You cannot “just bump `gradle.properties`” for Fabric 26.1.2. You need **JDK 25**, **published or locally built Nitrogen (Fabric)**, **published or locally built Accessories (Fabric)**, then an **Aether compile-fix pass** for 26.1 API changes.

---

## 2. What we investigated

### 2.1 Minecraft / toolchain

- **Target game version:** `26.1.2` (user goal; support `>=26.1` in `minecraft_version_range` when porting).
- **Fabric Loader (26.1 target):** `0.19.2` (from WIP `gradle.properties` experiments).
- **Fabric API (26.1 target):** `0.149.1+26.1.2`.
- **Loom:** `1.13-SNAPSHOT` (resolves to **1.13.6** in practice) in root `build.gradle`.
- **Java for compile/run (26.1.2):** Minecraft 26.1 requires **Java 25**. Loom fails early on JDK 21/24:
  - `Minecraft 26.1.2 requires Java 25 but Gradle is using 21`
- **`fabric.mod.json` today:** `"java": ">=21"` on **1.21.1** branch — must be raised to `>=25` when porting.
- **`build.gradle` today:** `java.toolchain.languageVersion = JavaLanguageVersion.of(21)` — must become **25** for 26.1.2.
- **Parchment (today):** pinned to MC **1.21** (`neogradle.subsystems.parchment.minecraftVersion=1.21`) — likely needs a **26.1-compatible Parchment** release or official-only mappings for 26.1.

### 2.2 Host JDK inventory (dev machine, May 2026)

`/usr/libexec/java_home -V` reported:

| JDK | Vendor |
|-----|--------|
| 21.0.8 | Oracle (`jdk-21.jdk`) |
| 21.0.8 | Temurin (`temurin-21.jdk`) |
| 24.0.2 | Oracle (`jdk-24.jdk`) |

**Not present:** JDK 17, JDK 25. Install **Temurin 25** or **Oracle JDK 25** before attempting a 26.1.2 compile.

### 2.3 Core library dependencies (Fabric)

Declared in `build.gradle` (Fabric branch):

```gradle
include modImplementation("com.aetherteam.nitrogen:nitrogen_internals:${project.nitrogen_version}")
include modImplementation("com.aetherteam.cumulus:cumulus_menus:${project.cumulus_version}")
include modImplementation("io.wispforest:accessories-fabric:${project.accessories_version}+${project.minecraft_version}")
```

Maven repos:

- `https://packages.aether-mod.net/Nitrogen`
- `https://packages.aether-mod.net/Cumulus`
- `https://maven.wispforest.io` / `releases`

**HTTP probe results (May 2026):**

| Artifact | URL pattern | Result |
|----------|-------------|--------|
| `nitrogen_internals-26.1.2-1.3.5-neoforge` | packages.aether-mod.net | **Exists** (302) |
| `nitrogen_internals-26.1.2-1.3.5-fabric` | packages.aether-mod.net | **404** |
| `nitrogen_internals-1.21.1-1.1.24-fabric` | packages.aether-mod.net | **Exists** (302) |
| `cumulus_menus-26.1.2-2.0.15-fabric` | packages.aether-mod.net | **Exists** (302) |
| `accessories-fabric` + `26.1.2` | maven.wispforest.io | **Not found** (404 on guessed coordinates) |

**1.21.1 Fabric versions (current `origin/1.21.1-develop-fabric`):**

| Property | Value |
|----------|--------|
| `minecraft_version` | `1.21.1` |
| `fabric_loader_version` | `0.16.5` |
| `fabric_version` | `0.103.0+1.21.1` |
| `nitrogen_version` | `1.21.1-1.1.22-beta.2-fabric` |
| `cumulus_version` | `1.21.1-2.0.8-fabric` |
| `accessories_version` | `1.1.0-beta.48` (resolves to `…+1.21.1`) |

**26.1.2 target versions (planned, not all published for Fabric):**

| Property | Target |
|----------|--------|
| `minecraft_version` | `26.1.2` |
| `minecraft_version_range` | `>=26.1` |
| `fabric_loader_version` | `0.19.2` |
| `fabric_version` | `0.149.1+26.1.2` |
| `nitrogen_version` | `26.1.2-1.3.5-fabric` ❌ |
| `cumulus_version` | `26.1.2-2.0.15-fabric` ✅ |
| `accessories_version` | TBD (no known `+26.1.2` artifact) ❌ |

### 2.4 NeoForge vs Fabric dependency model (important)

| Library | Fabric (1.21.1 / target) | NeoForge (upstream 1.21.1-develop) |
|---------|--------------------------|--------------------------------------|
| **Nitrogen** | `include modImplementation` — bundled into jar via Loom include | `jarJar` embed |
| **Cumulus** | `include modImplementation` | `jarJar` embed |
| **Accessories** | **Separate mod** — `accessories-fabric:version+mc` — players need Accessories **or** you ship a fat jar that includes it | **`jarJar` embed** `accessories-neoforge` — players do **not** install Accessories separately |

**Implication:** Fabric 26.1.2 **must** have a working **accessories-fabric** build for 26.1.2 (or composite-build it). You cannot copy NeoForge’s embedded JAR name/coordinates onto Fabric.

### 2.5 How deeply each dependency is wired into Aether

Rough import surface in `src/main/java` (Fabric tree):

| Dependency | Import usage | Role |
|------------|--------------|------|
| **Nitrogen** | **~180+ files** | Event buses (`EntityTickEvents`, `LevelEvents`, `PlayerTickEvents`, living entity events), `INBTSynchable` attachments sync, custom recipe serializers, placement-ban recipes, networking (`PacketDistributor`), registry helpers, mixin hooks (`nitrogen_fabric$…`), data components, loot modifiers, structure processors, JEI/REI category glue, etc. |
| **Accessories** | **~40 files** | Gloves/capes/pendants/rings, `AccessoriesCapability`, equipment slots, accessory screen, mob accessory drops, mixins (`MobMixin`, `ArmorStandMixin`), Phoenix glove checks, dispenser behaviors, renderers. |
| **Cumulus** | **~7 files** | Title screen / menu API (`AetherMenus` entrypoint `cumulus:menu_initializers`), music/menu hooks. |

**“Write out” / replace verdict:**

- **Nitrogen:** Not realistic — it is infrastructure, not a thin adapter.
- **Accessories:** Not realistic — core item/combat/UI systems depend on Wisp Forest API.
- **Cumulus:** Do **not** replace — only version-bump (artifact exists for 26.1.2 Fabric).

### 2.6 Nitrogen (library context)

- Repo: [The-Aether-Team/Nitrogen](https://github.com/The-Aether-Team/Nitrogen) (public for team visibility; team describes it as internal maintenance library).
- Supports Forge / NeoForge / Fabric; published via **GitHub Packages** → `packages.aether-mod.net`.
- Default branch (Mar 2026): `1.21.4-develop`; releases heavily track Minecraft minors.
- **NeoForge 26.1.2** artifacts appear on Maven; **Fabric 26.1.2** artifact was **not** found at time of investigation.
- Aether Fabric still relies on Nitrogen **mixins** injecting tick events — performance work moved listeners to `PlayerTickEvents` and early exits, but Nitrogen remains required at runtime.

### 2.7 Accessories (library context)

- Source: [wisp-forest/accessories](https://github.com/wisp-forest/accessories) (LGPL).
- Fabric coordinate: `io.wispforest:accessories-fabric:${version}+${minecraft_version}`.
- `fabric.mod.json` declares hard dependency: `"accessories": "*"`.
- All glove-based armor set checks go through `EquipmentUtil` → `AccessoriesCapability` when `require_gloves` config is true.

### 2.8 Optional / compile-only mods (not hard blockers)

These are `modCompileOnly` / `modLocalRuntime` — nice for dev, not required to produce a minimal jar:

- JEI, REI, Jade, Lootr, ModMenu, Debugutils, Porting-Lib remnants, Architectury, Cloth Config.

Each will need **26.1.2-compatible versions** for full dev testing, but they should not prevent a first successful `compileJava` once core trio (Nitrogen, Cumulus, Accessories) resolves.

---

## 3. Performance work (issue #2850) — separate from version port

### 3.1 Problem (from Spark)

With **no players in the Aether**, server still spent large tick fractions on:

- `aetherFabric$levelTickEvents` (~18%)
- `entityTickEvents` (~7%)

Root cause: Fabric listeners hooked via Nitrogen **per-entity** / **per-level** events, running armor scans and player logic for **all** loaded living entities and **all** dimensions.

### 3.2 Fixes implemented (Fabric 1.21.1)

Branch: **`perf/2850-fabric-1.21.1`** (based on `origin/1.21.1-develop-fabric`).

| Area | Change |
|------|--------|
| `ArmorAbilityListener` | `PlayerTickEvents` instead of `EntityTickEvents`; players only; `mayHaveAbilityArmor()` gate |
| `AetherPlayerListener` | `PlayerTickEvents`; direct `Player` callback |
| `DimensionListener` | Level tick only when dimension effects match Aether; portal neighbor check skips non-water |
| `DimensionHooks` | Eternal day / travel timer early exits |
| `AetherPlayerAttachment` | Client/server split in `onUpdate`; life-shard modifier only when count changes |
| Armor abilities | Piece-based checks; Phoenix particle throttle; Neptune Depth Strider fix |
| `EntityHooks` / `EntityListener` | Aercloud launch only in Aether; only when passenger |
| `AudioHooks` | Music manager skips outside Aether (unless boss music) |
| `EquipmentUtil` | `wearsValkyrie/Neptune/Phoenix/Gravitite` piece helpers |
| `GravititeArmor` | Skip full-set scan on jump if no gravitite piece |

**14 Java files** — no `gradle.properties` change on perf branch.

### 3.3 NeoForge perf (not yet committed on a branch)

`git stash list` contains:

`stash@{0}: On 1.21.1-develop: perf+neo fixes`

Same logical fixes adapted to NeoForge events (`PlayerTickEvent`, etc.). Apply to `1.21.1-develop` when desired.

### 3.4 26.1.2 branch state

Local branch **`26.1.2-develop-fabric`** exists but **does not** have committed gradle bumps (only experiments in working tree earlier). Perf changes should be **merged/cherry-picked** onto 26.1.2 after the port compiles.

---

## 4. Roadblocks (ordered)

### R1 — JDK 25 missing on dev machine

- **Symptom:** Loom refuses to set up Minecraft 26.1.2.
- **Fix:** Install JDK 25; `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`; set toolchain to 25 in `build.gradle`.

### R2 — Nitrogen Fabric 26.1.2 not on Maven

- **Symptom:** Gradle dependency resolution failure for `nitrogen_internals-26.1.2-1.3.5-fabric`.
- **Fix options:**
  1. Wait for The Aether Team to publish (fastest if they respond).
  2. Clone Nitrogen, checkout/port Fabric 26.1.2, `includeBuild` or `publishToMavenLocal`.
  3. Use composite build in `settings.gradle` pointing at local Nitrogen clone.

### R3 — Accessories Fabric 26.1.2 not on Maven

- **Symptom:** Cannot resolve `accessories-fabric:…+26.1.2`.
- **Fix options:** Same pattern — Wisp Forest release, or fork/port + composite build / mavenLocal.

### R4 — Minecraft / API delta (1.21.1 → 26.1.2)

After R1–R3, expect compile errors:

- Attachment API (`getAttachedOrCreate` vs older patterns).
- Registry / holder API changes.
- Fabric API breaking changes.
- Mixin target renames (official mappings).
- `fabric.mod.json` dependency ranges, Java requirement.

### R5 — Mappings / Parchment

Current Parchment pin is **1.21**. May need updated Parchment for 26.1 or rely on Mojang official mappings only until Parchment catches up.

### R6 — Ecosystem mods (soft)

JEI, REI, Jade, etc. for 26.1.2 — update when doing full QA, not for first compile.

### R7 — Upstream coordination

Nitrogen README states the library is primarily for **The Aether Team** internal use. Outside forks depend on their publish cadence or maintaining a fork of Nitrogen.

---

## 5. What will **not** work

| Bad idea | Why |
|----------|-----|
| Use 1.21.1 Nitrogen/Accessories jars on MC 26.1.2 | Binary/API mismatch |
| Strip Nitrogen and “reimplement later” | ~180 files; events, sync, recipes, networking |
| Strip Accessories on Fabric | Hard dep in `fabric.mod.json`; gloves/slots everywhere |
| Compile 26.1.2 with Java 21/24 | Loom hard-fails |
| Assume NeoForge jarJar deps work on Fabric | Different artifacts and loader |
| Skip Accessories and ship anyway | Runtime crash / missing mod |

---

## 6. Recommended path to goal (Fabric 26.1.2)

### Phase 0 — Shippable now

- [x] Perf on **1.21.1 Fabric** (`perf/2850-fabric-1.21.1`).
- [ ] Run server jar from fork build; validate Spark profile improved.

### Phase 1 — Environment

1. Install **JDK 25**.
2. Create branch `26.1.2-develop-fabric` from latest `1.21.1-develop-fabric` (or merge perf first).
3. Bump `gradle.properties`:
   - `minecraft_version=26.1.2`
   - `minecraft_version_range=>=26.1`
   - Fabric loader/API versions for 26.1.2
   - `cumulus_version=26.1.2-2.0.15-fabric`
   - `nitrogen_version=26.1.2-1.3.5-fabric` (when available)
   - `accessories_version=<TBD>+26.1.2`
4. Set Java toolchain **25** in `build.gradle`.
5. Update `fabric.mod.json` `"java": ">=25"`.

### Phase 2 — Unblock dependencies

**Option A — Wait / ask upstream (lowest risk)**  
Open issue on `The-Aether-Team/The-Aether` or team Discord:

- Request publish of `nitrogen_internals-26.1.2-*-fabric`.
- Ask Wisp Forest / team for `accessories-fabric` for 26.1.2.

**Option B — Composite builds (best for active fork)**  

`settings.gradle`:

```gradle
includeBuild('../Nitrogen') {
    dependencySubstitution {
        substitute module('com.aetherteam.nitrogen:nitrogen_internals') using project(':')
    }
}
// Similar for Accessories when cloned
```

- Clone Nitrogen + Accessories beside The-Aether.
- Port Fabric subprojects to 26.1.2 using NeoForge 26.1.2 / Nitrogen `1.21.1-fabric` as references.
- `./gradlew build` in each sibling until artifacts substitute cleanly.

**Option C — mavenLocal**  
Publish Nitrogen/Accessories to `~/.m2/repository` from local clones; keep Aether `build.gradle` unchanged aside from versions.

### Phase 3 — Port Aether code

1. `./gradlew compileJava` and fix errors in batches:
   - Attachments / networking
   - Events package
   - Mixins (run `genSources` / refresh mappings)
   - Client-only vs server splits
2. Cherry-pick **perf/2850** commits onto 26.1.2 branch (resolve conflicts).
3. Run client + server smoke tests: portal, dimension tick, armor abilities, accessories GUI.

### Phase 4 — QA & release

1. Update optional mod versions (JEI, etc.).
2. Multiplayer test with Spark — confirm #2850 profile improvement holds on 26.1.2.
3. Tag release `26.1.2-x.x.x-fabric` on fork; PR upstream if desired.

### Phase 5 — NeoForge (optional secondary)

Upstream NeoForge 26.1.2 has Nitrogen + Cumulus published; Accessories embedded. Port path is parallel but loader-specific event code differs — use stashed `perf+neo fixes` as starting point.

---

## 7. Effort estimates (rough)

| Task | Effort |
|------|--------|
| JDK install + gradle bump | Hours |
| Upstream publishes both Fabric libs | **0** dev days (waiting) |
| Composite build + port Nitrogen Fabric 26.1.2 | 2–5 days (if source branch exists) |
| Port Accessories Fabric 26.1.2 | 2–5 days |
| Aether compile-fix pass | 3–10 days (depends on 26.1 churn) |
| QA + multiplayer perf validation | 2–5 days |
| Rewrite deps away | **Weeks–months** — not advised |

---

## 8. Local branches / artifacts reference

| Branch | Purpose |
|--------|---------|
| `origin/1.21.1-develop-fabric` | Upstream Fabric baseline |
| `perf/2850-fabric-1.21.1` | Perf fix for #2850 (1.21.1) |
| `26.1.2-develop-fabric` | Local WIP name for port (no committed 26.1 gradle yet) |
| `1.21.1-develop` | NeoForge baseline |
| `stash@{0}` | NeoForge perf (`perf+neo fixes`) |

---

## 9. Key file references

| File | Relevance |
|------|-----------|
| `gradle.properties` | MC version, loader, Nitrogen/Cumulus/Accessories versions |
| `build.gradle` | Loom, dependencies, Java toolchain, `include modImplementation` |
| `settings.gradle` | Composite builds (future) |
| `src/main/resources/fabric.mod.json` | Loader deps, Java version, Cumulus entrypoint |
| `docs/PORTING_26.1.md` | This document |

---

## 10. Decision log

| Decision | Rationale |
|----------|-----------|
| Fabric first | User priority |
| Do not inline Nitrogen/Accessories | Surface area too large |
| Ship 1.21.1 perf before 26.1.2 | De-risks player pain while deps catch up |
| Prefer composite build over copying JARs | Legal, maintainable, matches team workflow |
| `PlayerTickEvents` over `EntityTickEvents` | Removes per-entity mixin callback cost for player-only logic |

---

*Last updated: May 2026 — re-verify Maven URLs and upstream branches before starting Phase 2.*
