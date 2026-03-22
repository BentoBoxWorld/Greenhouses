# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Greenhouses is a BentoBox addon for Minecraft that lets players build glass structures to create custom biome greenhouses. Features include biome-specific plant growth, mob spawning, weather/snow simulation, and block erosion/conversion. Targets Spigot 1.21.5 and BentoBox 2.7.1+.

## Build Commands

```bash
mvn clean package              # Build JAR
mvn test                       # Run all tests
mvn test -Dtest=ClassName      # Run single test class
mvn test -Dtest=Class#method   # Run single test method
mvn clean verify               # Full build + analysis (used in CI)
```

**Requirements:** Java 21, Maven 3.x+

## Architecture

### Core Flow

`Greenhouses.java` (main addon) → initializes three managers:
- **GreenhouseManager** — orchestrates greenhouse CRUD, database persistence via BentoBox Database API
- **RecipeManager** — loads biome recipes from `biomes.yml` into `BiomeRecipe` objects
- **EcoSystemManager** — runs scheduled BukkitTasks for plant growth, mob spawning, block conversion, and greenhouse verification

### Key Classes

- **`greenhouse/BiomeRecipe`** — defines biome requirements (block composition, water/lava coverage, plants, mobs, conversions)
- **`managers/GreenhouseFinder`** — async greenhouse detection using `CompletableFuture` and `AsyncWorldCache` for non-blocking chunk scanning
- **`managers/GreenhouseMap`** — in-memory `HashMap<Island, List<Greenhouse>>` index
- **`data/Greenhouse`** — DataObject persisted to database; contains bounding box, biome recipe, broken status
- **`world/AsyncWorldCache`** — thread-safe `ChunkSnapshot` cache for async world access
- **`listeners/SnowTracker`** — rain-based snow simulation consuming water from hoppers

### Command Structure

Player commands registered under `/is greenhouse` (or gamemode equivalent):
- `make`, `remove`, `list`, `recipe`, `info`

Admin commands include `reload` and `info`.

### Configuration

- **`config.yml`** — tick intervals (plant/block/mob/eco), snow settings, allowed materials
- **`biomes.yml`** — biome recipe definitions with contents, plants, mobs, conversions, priorities
- **`locales/`** — 25+ language files

### Greenhouse Detection

`GreenhouseFinder.find()` returns a `CompletableFuture` scanning for rectangular glass structures. Validates roof, walls, floor, door, and hopper. Returns `GreenhouseResult` enum codes (~20 failure variants). Red glass positions cached for player error feedback.

## Testing

- **JUnit 4** with **PowerMock** (for Bukkit static mocking) and **Mockito**
- `ServerMocks.java` provides shared Bukkit server/registry mock setup
- Tests live in `src/test/java/world/bentobox/greenhouses/`
- Coverage via JaCoCo (excludes Material enum and synthetic fields)

## CI/CD

GitHub Actions runs on push to `develop`/`master` and PRs. Pipeline: JDK 21 → `mvn verify` → SonarCloud analysis → CodeMC artifact publish.

## Branching

- **develop** — active development
- **master** — releases (PRs target develop first)

---

## Modernization Backlog

Work through the stages below **in order**. Each stage should be a separate commit (or PR against `develop`). Run `mvn test` after every stage and fix any failures before moving on. All work targets the `develop` branch.

---

### Stage 1 — Bug Fixes

#### 1a. Fix Nether biome GUI text color bleed
- File: `src/main/java/world/bentobox/greenhouses/ui/panel/Panel.java`
- **Problem:** The Nether recipe entry uses a red color code that is never reset, causing all subsequent panel text to render red.
- **Fix:** Audit every text component built in `Panel.java` (and the Nether-related locale keys in `locales/en-US.yml`). Replace legacy `§` color codes with the Adventure component API (`Component.text(...).color(NamedTextColor.RED).append(Component.text(...).color(NamedTextColor.WHITE))`). Each component should be self-contained so colors cannot bleed into siblings.
- **Test:** Manually verify in-game (or add a unit test asserting the serialized component string contains a reset/close before the next item).

#### 1b. Fix tall flower (double-plant) top-half not being placed
- File: `src/main/java/world/bentobox/greenhouses/managers/EcoSystemManager.java`, method `growPlants()`
- **Problem:** After placing a tall plant (e.g. `TALL_GRASS`, `LARGE_FERN`, `SUNFLOWER`, `PEONY`, `ROSE_BUSH`, `LILAC`, `TALL_SEAGRASS`), only the bottom half (`Bisected.Half.BOTTOM`) is placed. The upper half block at `y+1` is never set.
- **Fix:** After placing a plant block, check if the placed `Material`'s `createBlockData()` implements `org.bukkit.block.data.Bisected`. If so:
  1. Set the placed block's `Bisected.Half` to `BOTTOM`.
  2. Compute `upperLocation = placedLocation.clone().add(0, 1, 0)`.
  3. Verify `upperLocation` is inside the greenhouse bounding box and the block there is `Material.AIR`.
  4. Place the same material at `upperLocation` with `Bisected.Half.TOP`.
- **Test:** Add a test in `EcoSystemManagerTest` that mocks a tall plant material and asserts two block-set calls (bottom + top).

---

### Stage 2 — Command System

#### 2a. Add plural alias `/is greenhouses`
- File: `src/main/java/world/bentobox/greenhouses/ui/user/UserCommand.java`
- **Problem:** The addon is called "greenhouses" everywhere but the command is `/is greenhouse` (singular).
- **Fix:** Add `"greenhouses"` to the alias list in the `super(...)` constructor call. Example:
  ```java
  super(addon, islandCommand, "greenhouse", "greenhouses", "gh");
  ```
- **Test:** Confirm `getAliases()` contains `"greenhouses"` in `UserCommandTest` (create this test class if it does not exist).

#### 2b. Restore `InfoCommand`, `ListCommand`, and `RecipeCommand`
- Files:
  - `src/main/java/world/bentobox/greenhouses/ui/user/InfoCommand.java`
  - `src/main/java/world/bentobox/greenhouses/ui/user/ListCommand.java`
  - `src/main/java/world/bentobox/greenhouses/ui/user/RecipeCommand.java`
  - `src/main/java/world/bentobox/greenhouses/ui/user/UserCommand.java`
- **Problem:** These three command classes exist but are commented out in `UserCommand`. The README documents them but they are not functional.
- **Fix:**
  - Implement `InfoCommand.execute()`: display the biome recipe name, block counts, and broken status of the greenhouse the player is currently standing in (use `GreenhouseMap.getGreenhouse(location)`). If the player is not in a greenhouse, send the `greenhouses.info.not-in-greenhouse` locale message.
  - Implement `ListCommand.execute()`: open the existing `Panel` GUI (same as the no-arg path in `MakeCommand`) so the player can browse all available recipes.
  - Implement `RecipeCommand.execute()`: accept an optional recipe-name argument and display that recipe's full requirements (block percentages, water/lava coverage, plants, mobs) as chat messages. If no argument, list available recipe names.
  - Uncomment all three registrations in `UserCommand`.
  - Add locale keys to `locales/en-US.yml` for any new messages.
- **Test:** Add `InfoCommandTest`, `ListCommandTest`, `RecipeCommandTest` covering at least: no-permission, player-not-in-greenhouse (Info), successful display paths.

#### 2c. Update CLAUDE.md architecture notes
- Update the "Command Structure" section of this file to reflect the restored commands and the new plural alias.

---

### Stage 3 — New Configuration Features

#### 3a. World allowlist
- Files:
  - `src/main/java/world/bentobox/greenhouses/Settings.java`
  - `src/main/java/world/bentobox/greenhouses/ui/user/MakeCommand.java`
  - `src/main/java/world/bentobox/greenhouses/managers/GreenhouseManager.java` (enum `GreenhouseResult`)
  - `src/main/resources/config.yml`
  - `src/main/resources/locales/en-US.yml`
- **Implementation:**
  1. Add to `Settings.java`:
     ```java
     @ConfigComment("List of world names where greenhouses are allowed. Leave empty to allow all worlds.")
     @ConfigEntry(path = "allowed-worlds")
     private List<String> allowedWorlds = new ArrayList<>();
     ```
  2. Add `WRONG_WORLD` to the `GreenhouseResult` enum in `GreenhouseManager.java`.
  3. In `MakeCommand.execute()`, before calling `makeGreenhouse()`, check:
     ```java
     List<String> allowed = addon.getSettings().getAllowedWorlds();
     if (!allowed.isEmpty() && !allowed.contains(player.getWorld().getName())) {
         // send WRONG_WORLD message, return true
     }
     ```
  4. Add `greenhouses.errors.wrong-world` locale key to `en-US.yml`.
  5. Document the setting in `config.yml` with a comment.
- **Test:** Add `MakeCommandTest` cases: world in allowlist (proceeds), world not in allowlist (blocked), empty allowlist (proceeds).

#### 3b. Per-player greenhouse limit
- Files: `Settings.java`, `MakeCommand.java`, `locales/en-US.yml`, `config.yml`
- **Implementation:**
  1. Add to `Settings.java`:
     ```java
     @ConfigComment("Maximum number of greenhouses a player can make. 0 = unlimited.")
     @ConfigEntry(path = "max-greenhouses-per-player")
     private int maxGreenhousesPerPlayer = 0;
     ```
  2. In `MakeCommand.execute()`, after the world check, count existing greenhouses on the player's island and compare:
     ```java
     int max = addon.getSettings().getMaxGreenhousesPerPlayer();
     if (max > 0) {
         long owned = addon.getGreenhouseManager().getMap()
             .getGreenhouses(island).stream()
             .filter(g -> /* owned by this player */)
             .count();
         if (owned >= max) {
             // send max-reached message, return true
         }
     }
     ```
  3. Add `greenhouses.errors.max-greenhouses` locale key (include `[max]` placeholder).
  4. Document in `config.yml`.
- **Test:** Add cases to `MakeCommandTest`: at limit (blocked with correct message), below limit (proceeds), limit = 0 (never blocked).

---

### Stage 4 — Test Framework Upgrade

#### 4a. Migrate from JUnit 4 + PowerMock to JUnit 5 + Mockito

- **Why:** PowerMock is effectively unmaintained and has known incompatibilities with the Java 21 module system. Mockito 5.x provides `mockStatic()` as a first-class replacement.
- **pom.xml changes:**
  1. Remove `junit:junit` and `powermock-*` dependencies.
  2. Add:
     ```xml
     <dependency>
       <groupId>org.junit.jupiter</groupId>
       <artifactId>junit-jupiter</artifactId>
       <version>5.11.0</version>
       <scope>test</scope>
     </dependency>
     ```
  3. Upgrade Mockito to `5.12.0` (remove the PowerMock-compatible `3.x` version).
  4. Update Surefire plugin to `3.3.0`:
     ```xml
     <plugin>
       <artifactId>maven-surefire-plugin</artifactId>
       <version>3.3.0</version>
     </plugin>
     ```
  5. Keep the existing `--add-opens` JVM args in Surefire config (still needed for Bukkit reflection).

- **Test migration pattern:**
  ```java
  // Before
  @RunWith(PowerMockRunner.class)
  @PrepareForTest({Bukkit.class})
  public class FooTest {
      @Before public void setUp() { PowerMockito.mockStatic(Bukkit.class); }
  }

  // After
  @ExtendWith(MockitoExtension.class)
  class FooTest {
      @BeforeEach void setUp() {
          // Use try-with-resources in each test method that needs static mocking, or
          // keep a MockedStatic field closed in @AfterEach
      }
  }
  ```
- Migrate all eight existing test classes one by one. Verify `mvn test` passes after each.
- Update this file's Testing section to reflect JUnit 5.

---

### Stage 5 — Coverage Expansion

Add tests for all currently untested classes. Aim for **≥ 80% line coverage** across the project (JaCoCo report: `target/site/jacoco/index.html`).

Priority order:

| Class | Key scenarios to cover |
|---|---|
| `GreenhouseMap` | `addGreenhouse`, `getGreenhouse`, `isOverlapping`, `removeGreenhouse`, `isInGreenhouse` |
| `GreenhouseGuard` | `onFlow` (in/out blocked by config), `onPistonPush`, `onPistonPull`, `onCreatureSpawn` |
| `IslandChangeEvents` | Island deleted → greenhouses removed |
| `SnowTracker` | Snow tick scheduling, rain on/off transitions |
| `Panel` + `PanelClick` | Inventory build, click routing to `MakeCommand` |
| `RecipeManager` | Happy-path YAML load, unknown material (warns + skips), unknown entity (warns + skips), >49 recipes (truncated) |
| `UserCommand` | Sub-command registration, default (no-arg) opens panel |

Use `ServerMocks.java` as the shared setup base (it already handles Registry and Tag mocking).

---

### Stage 6 — Code Cleanup & Polish

#### 6a. Extract magic numbers to constants
- `Roof.java`: replace `100` (max search blocks) with `private static final int MAX_SEARCH_RADIUS = 100;`
- `RecipeManager.java`: replace `49` with `public static final int MAX_RECIPES = 49;` and reference it from `Panel.java` too.
- `EcoSystemManager.java`: confirm `PLANTS_PER_BONEMEAL = 6` is already a constant (it is); consider moving it to `Settings` as a configurable value.

#### 6b. Apply modern Java idioms
- Replace `if/else if` chains on `String` biome names in `RecipeManager.processEntries()` with a `switch` expression.
- Replace `if (x instanceof Foo) { Foo f = (Foo) x; }` patterns with pattern-matching `instanceof`: `if (x instanceof Foo f) { }`.
- These are refactors only — behaviour must not change. Run the full test suite to confirm.

#### 6c. Add `@NonNull` / `@Nullable` annotations
- Add `org.eclipse.jdt.annotation` (provided scope, no runtime dep) to `pom.xml`.
- Annotate all public method parameters and return types in the `managers/` and `greenhouse/` packages.
- Fix any null-dereference warnings surfaced by the IDE / compiler as a result.

#### 6d. JavaDoc for complex algorithms
- `Roof.java`: class-level doc explaining the flood-fill expansion approach; method-level doc on `expandCoords()`.
- `Walls.java`: class-level doc explaining the `WallFinder` state machine; document the four-directional expansion.
- `GreenhouseFinder.java`: document the validation sequence (roof → walls → floor → door → hopper) and what each `GreenhouseResult` failure code means.
- `AsyncWorldCache.java`: document thread-safety guarantees and the snapshot lifecycle.

#### 6e. README update
- Correct the command table to match what is now actually implemented.
- Add a "Quick Start" section (install → add game mode to config → build glass structure → `/is greenhouse make`).
- Add Jenkins CI badge / link near the top.
- Remove the duplicate admin command entry.
