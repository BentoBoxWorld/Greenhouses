package world.bentobox.greenhouses.managers;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import world.bentobox.bentobox.database.Database;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;

public class GreenhouseManager {

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
        FAIL_NO_RECIPE_FOUND
    }

    private Greenhouses addon;
    // Greenhouses
    private GreenhouseMap map;
    private Database<Greenhouse> handler;

    public GreenhouseManager(Greenhouses addon) {
        this.addon = addon;
        handler = new Database<>(addon, Greenhouse.class);
        map = new GreenhouseMap(addon);
        loadGreenhouses();
        // Start ecosystems
        new EcoSystemManager(addon, this);
    }

    public GreenhouseMap getMap() {
        return map;
    }

    /**
     * Load all known greenhouses
     */
    public void loadGreenhouses() {
        addon.log("Loading greenhouses...");
        handler.loadObjects().forEach(g -> {
            GreenhouseResult result = map.addGreenhouse(g);
            switch (result) {
            case FAIL_NO_ISLAND:
            case FAIL_OVERLAPPING:
            case NULL:
                addon.logError(result.name());
                break;
            case SUCCESS:
                break;
            default:
                break;

            }
        });
        addon.log("Loaded greenhouses.");
    }

    /**
     * Saves all the greenhouses to database
     */
    public void saveGreenhouses() {
        addon.log("Saving greenhouses...");
        map.getGreenhouses().forEach(handler::saveObject);
    }

    /**
     * Removes the greenhouse from the world and resets biomes
     * @param g
     */
    public void removeGreenhouse(Greenhouse g) {
        map.removeGreenhouse(g);
        addon.log("Returning biome to original state: " + g.getOriginalBiome().toString());
        if (g.getOriginalBiome().equals(Biome.NETHER) || g.getOriginalBiome().equals(Biome.DESERT)
                || g.getOriginalBiome().equals(Biome.DESERT_HILLS)) {
            for (int x = (int)g.getFootprint().getMinX(); x<= (int)g.getFootprint().getMaxX(); x++) {
                for (int z = (int)g.getFootprint().getMinY(); z<= (int)g.getFootprint().getMinY(); z++) {
                    // Set back to the original biome
                    g.getLocation().getWorld().setBiome(x, z, g.getOriginalBiome());
                    for (int y = g.getFloorHeight(); y< g.getCeilingHeight(); y++) {
                        Block b = g.getLocation().getWorld().getBlockAt(x, y, z);
                        // Remove any water
                        if (b.getType().equals(Material.WATER) || b.getType().equals(Material.BLUE_ICE)
                                || b.getType().equals(Material.FROSTED_ICE)
                                || b.getType().equals(Material.ICE) || b.getType().equals(Material.PACKED_ICE)
                                || b.getType().equals(Material.SNOW) || b.getType().equals(Material.SNOW_BLOCK)) {
                            // Evaporate it
                            b.setType(Material.AIR);
                            b.getWorld().spawnParticle(Particle.SMOKE_LARGE, b.getLocation(), 5);
                        }
                    }
                }
            }
        }
    }


    /**
     * Checks that a greenhouse meets specs and makes it
     * If type is stated then only this specific type will be checked
     * @param user
     * @param greenhouseRecipe
     * @return
     */
    public GhResult tryToMakeGreenhouse(Location location, BiomeRecipe greenhouseRecipe) {
        addon.log("Player location is " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
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
                map.addGreenhouse(finder.getGh());
            }
            return new GhResult().setFinder(finder).setResults(resultSet);
        }

        // Try ordered recipes
        resultSet.add(addon.getRecipes().getBiomeRecipes().stream().sorted()
                .filter(r -> r.checkRecipe(finder.getGh()).isEmpty()).findFirst()
                .map(r -> {
                    // Success - set recipe and add to map
                    finder.getGh().setBiomeRecipe(r);
                    return map.addGreenhouse(finder.getGh());
                }).orElse(GreenhouseResult.FAIL_NO_RECIPE_FOUND));
        return new GhResult().setFinder(finder).setResults(resultSet);
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
        public GhResult setResults(Set<GreenhouseResult> results) {
            this.results = results;
            return this;
        }

        /**
         * @param finder the finder to set
         */
        public GhResult setFinder(GreenhouseFinder finder) {
            this.finder = finder;
            return this;
        }


    }
}
