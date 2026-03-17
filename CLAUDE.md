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
