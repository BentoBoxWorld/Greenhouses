package world.bentobox.greenhouses.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.BoundingBox;

import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.listeners.GreenhouseEvents;
import world.bentobox.greenhouses.listeners.GreenhouseGuard;
import world.bentobox.greenhouses.listeners.IslandChangeEvents;
import world.bentobox.greenhouses.listeners.SnowTracker;

public class GreenhouseManager implements Listener {

    /**
     * Result of greenhouse making
     *
     */
    public enum GreenhouseResult {
        FAIL_NO_ROOF,
        FAIL_BELOW,
        FAIL_BLOCKS_ABOVE,
        FAIL_HOLE_IN_WALL,
        FAIL_HOLE_IN_ROOF,
        FAIL_UNEVEN_WALLS,
        FAIL_BAD_ROOF_BLOCKS,
        FAIL_BAD_WALL_BLOCKS,
        FAIL_TOO_MANY_DOORS,
        FAIL_TOO_MANY_HOPPERS,
        FAIL_NO_WATER,
        FAIL_NO_LAVA,
        FAIL_NO_ICE,
        FAIL_INSUFFICIENT_WATER,
        FAIL_INSUFFICIENT_LAVA,
        FAIL_INSUFFICIENT_ICE,
        FAIL_NO_ISLAND,
        FAIL_OVERLAPPING,
        NULL,
        SUCCESS,
        FAIL_NO_RECIPE_FOUND,
        FAIL_INSUFFICIENT_BLOCKS,
        FAIL_NO_WORLD, FAIL_UNKNOWN_RECIPE
    }

    private final Greenhouses addon;
    // Greenhouses
    private final GreenhouseMap map;
    private final Database<Greenhouse> handler;
    private EcoSystemManager ecoMgr;

    public GreenhouseManager(Greenhouses addon) {
        this.addon = addon;
        handler = new Database<>(addon, Greenhouse.class);
        map = new GreenhouseMap(addon);
    }

    @EventHandler
    public void startManager(BentoBoxReadyEvent e) {
        loadGreenhouses();
        // Start ecosystems
        ecoMgr = new EcoSystemManager(addon, this);
        ecoMgr.setup();
        // Register listeners
        addon.registerListener(new SnowTracker(addon));
        addon.registerListener(new GreenhouseEvents(addon));
        addon.registerListener(new GreenhouseGuard(addon));
        addon.registerListener(new IslandChangeEvents(addon));
    }

    public GreenhouseMap getMap() {
        return map;
    }

    /**
     * Load all known greenhouses
     */
    private void loadGreenhouses() {
        map.clear();
        addon.log("Loading greenhouses...");
        List<Greenhouse> toBeRemoved = new ArrayList<>();
        handler.loadObjects().forEach(g -> {
            GreenhouseResult result = map.addGreenhouse(g);
            switch (result) {
            case FAIL_NO_ISLAND ->
            // Delete the failed greenhouse
            toBeRemoved.add(g);
            case FAIL_OVERLAPPING -> addon.logError("Greenhouse overlaps with another greenhouse. Skipping...");
            case NULL -> addon.logError("Null location of greenhouse. Cannot load. Skipping...");
            case SUCCESS -> activateGreenhouse(g);
            case FAIL_NO_WORLD -> addon.logError("Database contains greenhouse for a non-loaded world. Skipping...");
            case FAIL_UNKNOWN_RECIPE -> {
                addon.logError("Greenhouse uses a recipe that does not exist in the biomes.yml. Skipping...");
                addon.logError("Greenhouse Id " + g.getUniqueId());
            }
            default -> {
            }
            }
        });
        addon.log("Loaded " + map.getSize() + " greenhouses.");
        // Remove the old or outdated greenhouses
        toBeRemoved.forEach(handler::deleteObject);
    }

    /**
     * Removes the greenhouse from the world and resets biomes
     * @param gh - greenhouse
     */
    public void removeGreenhouse(Greenhouse gh) {
        handler.deleteObject(gh);
        map.removeGreenhouse(gh);
        if (gh.getOriginalBiome() == null) {
            addon.logError("Greenhouse had no original biome: " + gh.getLocation());
            return;
        }
        if (gh.getLocation() == null || gh.getLocation().getWorld() == null) {
            // Greenhouse is messed up. It's being deleted anyway.
            return;
        }
        addon.log("Returning biome to original state: " + gh.getOriginalBiome().toString());
        final BoundingBox bb = gh.getBoundingBox();
        for (int x = (int)bb.getMinX(); x<= (int)bb.getMaxX(); x+=4) {
            for (int z = (int)bb.getMinZ(); z<= (int)bb.getMaxZ(); z+=4) {
                for (int y = (int)bb.getMinY(); y<= (int)bb.getMaxY(); y+=4) {
                    // Set back to the original biome
                    gh.getLocation().getWorld().setBiome(x, y, z, gh.getOriginalBiome());
                }
            }
        }
    }


    /**
     * Checks that a greenhouse meets specs and makes it
     * If type is stated then only this specific type will be checked
     * @param location - location to start search from
     * @param greenhouseRecipe - recipe requested, or null for a best-effort search
     * @return - future greenhouse result {@link GhResult}
     */
    public CompletableFuture<GhResult> tryToMakeGreenhouse(Location location, BiomeRecipe greenhouseRecipe) {
        CompletableFuture<GhResult> r = new CompletableFuture<>();
        GreenhouseFinder finder = new GreenhouseFinder(addon);
        finder.find(location).thenAccept(resultSet -> {
            if (!resultSet.isEmpty()) {
                // Failure!
                r.complete(new GhResult().setFinder(finder).setResults(resultSet));
                return;
            }
            // Check if the greenhouse meets the requested recipe
            if (greenhouseRecipe != null) {
                checkRecipe(finder, greenhouseRecipe, resultSet).thenAccept(r::complete);
                return;
            }
            // Try ordered recipes
            findRecipe(finder).thenAccept(rs -> {
                resultSet.addAll(rs);
                r.complete(new GhResult().setFinder(finder).setResults(resultSet));
            });

        });
        return r;
    }

    /**
     * Tries to match the greenhouse to a recipe by going through all of them in order
     * @param finder - finder object
     */
    private CompletableFuture<Set<GreenhouseResult>> findRecipe(GreenhouseFinder finder) {
        CompletableFuture<Set<GreenhouseResult>> r = new CompletableFuture<>();
        // Get sorted list of all recipes
        List<BiomeRecipe> list = addon.getRecipes().getBiomeRecipes().stream().sorted().collect(Collectors.toList());
        findRecipe(r, list, finder);
        return r;
    }

    private void findRecipe(CompletableFuture<Set<GreenhouseResult>> r, List<BiomeRecipe> list,
            GreenhouseFinder finder) {
        if (list.isEmpty()) {
            r.complete(Collections.singleton(GreenhouseResult.FAIL_NO_RECIPE_FOUND));
            return;
        }
        BiomeRecipe br = list.get(0);
        list.remove(0);
        br.checkRecipe(finder.getGh()).thenAccept(results -> {
            if (results.isEmpty()) {
                r.complete(Collections.singleton(GreenhouseResult.SUCCESS));
            } else {
                findRecipe(r, list, finder);
            }
        });
    }

    /**
     * Checks to see if the greenhouse meets the designated recipe and returns the result
     * @param finder - finder object
     * @param greenhouseRecipe - recipe requested
     * @param resultSet - result set from finder
     * @return Greenhouse result
     */
    CompletableFuture<GhResult> checkRecipe(GreenhouseFinder finder, BiomeRecipe greenhouseRecipe, Set<GreenhouseResult> resultSet) {
        CompletableFuture<GhResult> r = new CompletableFuture<>();
        greenhouseRecipe.checkRecipe(finder.getGh()).thenAccept(rs -> {
            if (rs.isEmpty()) {
                // Success - set recipe and add to map
                finder.getGh().setBiomeRecipe(greenhouseRecipe);
                resultSet.add(map.addGreenhouse(finder.getGh()));
                activateGreenhouse(finder.getGh());
                handler.saveObjectAsync(finder.getGh());
                rs.addAll(resultSet);
            }
            GhResult recipe = new GhResult().setFinder(finder).setResults(rs);
            r.complete(recipe);
        });
        return r;
    }

    private void activateGreenhouse(Greenhouse gh) {
        Biome ghBiome = gh.getBiomeRecipe().getBiome();
        if (ghBiome == null) {
            addon.logError("Biome recipe error - no such biome for " + gh.getBiomeRecipe().getName());
            return;
        }
        final BoundingBox bb = gh.getBoundingBox();
        for (int x = (int)bb.getMinX(); x < bb.getMaxX(); x+=4) {
            for (int z = (int)bb.getMinZ(); z < bb.getMaxZ(); z+=4) {
                for (int y = (int)bb.getMinY(); y < bb.getMaxY(); y+=4) {
                    Objects.requireNonNull(gh.getWorld()).setBiome(x, y, z, ghBiome);
                }
            }
        }
    }

    /**
     * Result of the greenhouse make effort
     *
     */
    public static class GhResult {
        private Set<GreenhouseResult> results;
        private GreenhouseFinder finder;

        /**
         * @return the results
         */
        public Set<GreenhouseResult> getResults() {
            return results;
        }

        /**
         * @return the finder
         */
        public GreenhouseFinder getFinder() {
            return finder;
        }

        /**
         * @param results the results to set
         */
        GhResult setResults(Set<GreenhouseResult> results) {
            this.results = results;
            return this;
        }

        /**
         * @param finder the finder to set
         */
        GhResult setFinder(GreenhouseFinder finder) {
            this.finder = finder;
            return this;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "GhResult [results=" + results + ", finder=" + finder + "]";
        }


    }

    /**
     * @return the ecoMgr
     */
    public EcoSystemManager getEcoMgr() {
        return ecoMgr;
    }

    /**
     * Removes all greenhouses on island
     * @param island - island
     */
    public void removeGreenhouses(Island island) {
        map.getGreenhouses(island).forEach(handler::deleteObject);
        map.removeGreenhouses(island);

    }
}
