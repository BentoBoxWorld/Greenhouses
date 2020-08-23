package world.bentobox.greenhouses.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
        FAIL_INSUFFICIENT_BLOCKS
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
            case FAIL_NO_ISLAND:
                // Delete the failed greenhouse
                toBeRemoved.add(g);
                break;
            case FAIL_OVERLAPPING:
            case NULL:
                addon.logError(result.name());
                break;
            case SUCCESS:
                activateGreenhouse(g);
                break;
            default:
                break;

            }
        });
        addon.log("Loaded " + map.getSize() + " greenhouses.");
        // Remove the old or outdated greenhouses
        toBeRemoved.forEach(handler::deleteObject);
    }

    /**
     * Saves all the greenhouses to database
     */
    public void saveGreenhouses() {
        addon.log("Saving greenhouses...");
        map.getGreenhouses().forEach(handler::saveObjectAsync);
    }

    /**
     * Removes the greenhouse from the world and resets biomes
     * @param g - greenhouse
     */
    public void removeGreenhouse(Greenhouse g) {
        handler.deleteObject(g);
        map.removeGreenhouse(g);
        addon.log("Returning biome to original state: " + g.getOriginalBiome().toString());
        for (int x = (int)g.getBoundingBox().getMinX(); x<= (int)g.getBoundingBox().getMaxX(); x+=4) {
            for (int z = (int)g.getBoundingBox().getMinZ(); z<= (int)g.getBoundingBox().getMaxZ(); z+=4) {
                for (int y = (int)g.getBoundingBox().getMinY(); y<= (int)g.getBoundingBox().getMaxY(); y+=4) {
                    // Set back to the original biome
                    g.getLocation().getWorld().setBiome(x, y, z, g.getOriginalBiome());
                }
            }
        }
    }


    /**
     * Checks that a greenhouse meets specs and makes it
     * If type is stated then only this specific type will be checked
     * @param location - location to start search from
     * @param greenhouseRecipe - recipe requested, or null for a best-effort search
     * @return - greenhouse result {@link GhResult}
     */
    public GhResult tryToMakeGreenhouse(Location location, BiomeRecipe greenhouseRecipe) {
        GreenhouseFinder finder = new GreenhouseFinder();
        Set<GreenhouseResult> resultSet = finder.find(location);
        if (!resultSet.isEmpty()) {
            // Failure!
            return new GhResult().setFinder(finder).setResults(resultSet);
        }
        // Check if the greenhouse meets the requested recipe
        if (greenhouseRecipe != null) {
            resultSet = greenhouseRecipe.checkRecipe(finder.getGh());
            if (resultSet.isEmpty()) {
                // Success - set recipe and add to map
                finder.getGh().setBiomeRecipe(greenhouseRecipe);
                resultSet.add(map.addGreenhouse(finder.getGh()));
                activateGreenhouse(finder.getGh());
                handler.saveObjectAsync(finder.getGh());
            }
            return new GhResult().setFinder(finder).setResults(resultSet);
        }

        // Try ordered recipes
        resultSet.add(addon.getRecipes().getBiomeRecipes().stream().sorted()
                .filter(r -> r.checkRecipe(finder.getGh()).isEmpty()).findFirst()
                .map(r -> {
                    // Success - set recipe and add to map
                    finder.getGh().setBiomeRecipe(r);
                    activateGreenhouse(finder.getGh());
                    handler.saveObjectAsync(finder.getGh());
                    return map.addGreenhouse(finder.getGh());
                }).orElse(GreenhouseResult.FAIL_NO_RECIPE_FOUND));
        return new GhResult().setFinder(finder).setResults(resultSet);
    }

    private void activateGreenhouse(Greenhouse gh) {
        for (int x = (int)gh.getBoundingBox().getMinX(); x < gh.getBoundingBox().getMaxX(); x+=4) {
            for (int z = (int)gh.getBoundingBox().getMinZ(); z < gh.getBoundingBox().getMaxZ(); z+=4) {
                for (int y = (int)gh.getBoundingBox().getMinY(); y < gh.getBoundingBox().getMaxY(); y+=4) {
                    gh.getWorld().setBiome(x, y, z, gh.getBiomeRecipe().getBiome());
                }
            }
        }
    }

    /**
     * Result of the greenhouse make effort
     *
     */
    public class GhResult {
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
