package world.bentobox.greenhouses.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

public class GreenhouseMap {

    private final Greenhouses addon;
    private final Map<Island, List<Greenhouse>> greenhouses = new HashMap<>();

    /**
     * @param addon - addon
     */
    public GreenhouseMap(Greenhouses addon) {
        this.addon = addon;
    }

    /**
     * Try to add a greenhouse
     * @param greenhouse - greenhouse object
     * @return result {@link GreenhouseResult}
     */
    public GreenhouseResult addGreenhouse(Greenhouse greenhouse) {
        // Validation checks
        if (greenhouse.getBiomeRecipe().getBiome() == null) {
            return GreenhouseResult.FAIL_UNKNOWN_RECIPE;
        }
        if (greenhouse.getWorld() == null) {
            return GreenhouseResult.FAIL_NO_WORLD;
        }
        if (greenhouse.getLocation() == null) {
            return GreenhouseResult.NULL;
        }
        return addon.getIslands().getIslandAt(greenhouse.getLocation()).map(i -> {
            greenhouses.putIfAbsent(i, new ArrayList<>());
            // Check if overlapping
            if (!isOverlapping(greenhouse)) {
                greenhouses.get(i).add(greenhouse);
                return GreenhouseResult.SUCCESS;
            } else {
                return GreenhouseResult.FAIL_OVERLAPPING;
            }
        }).orElse(GreenhouseResult.FAIL_NO_ISLAND);
    }

    /**
     * Clear the greenhouse map
     */
    public void clear() {
        greenhouses.clear();
    }

    /**
     * Try to get greenhouse at location
     * @param location - location
     * @return Optional greenhouse or empty
     */
    public Optional<Greenhouse> getGreenhouse(Location location) {
        return getXZGreenhouse(location).filter(g -> location.getBlockY() <= g.getCeilingHeight() && location.getBlockY() >= g.getFloorHeight());
    }

    private Optional<Greenhouse> getXZGreenhouse(Location location) {
        return addon.getIslands().getIslandAt(location)
                .filter(greenhouses::containsKey).flatMap(i -> greenhouses.get(i).stream().filter(g -> g.contains(location)).findFirst());
    }

    /**
     * Check if location is inside a greenhouse
     * @param location - location
     * @return true if inside a greenhouse
     */
    public boolean inGreenhouse(Location location) {
        return getGreenhouse(location).isPresent();
    }

    /**
     * Check if a location is in a specific greenhouse
     * @param gh - greenhouse
     * @param location - location to check
     * @return true if inside
     */
    public boolean inGreenhouse(Greenhouse gh, Location location) {
        return getGreenhouse(location).map(gh::equals).orElse(false);
    }

    /**
     * Check if location is above a greenhouse
     * @param location - location
     * @return true if above a known greenhouse
     */
    public boolean isAboveGreenhouse(Location location) {
        return getXZGreenhouse(location).map(g -> location.getBlockY() > g.getCeilingHeight()).orElse(false);
    }

    private boolean isOverlapping(Greenhouse greenhouse) {
        return greenhouse.getLocation() != null && addon.getIslands().getIslandAt(greenhouse.getLocation()).map(i -> {
            greenhouses.putIfAbsent(i, new ArrayList<>());
            return greenhouses.get(i).stream().anyMatch(g ->
            g.getLocation().getWorld().equals(greenhouse.getLocation().getWorld()) &&
            g.getBoundingBox().overlaps(greenhouse.getBoundingBox()));
        }).orElse(false);

    }

    /**
     * Removes the greenhouse from the map
     * @param greenhouse - greenhouse
     */
    protected void removeGreenhouse(Greenhouse greenhouse) {
        if (greenhouse.getLocation() != null) {
            addon.getIslands().getIslandAt(greenhouse.getLocation()).ifPresent(i -> {
                if (greenhouses.containsKey(i)) greenhouses.get(i).remove(greenhouse);
            });
        }
    }

    /**
     * @param island island
     */
    public void removeGreenhouses(Island island) {
        greenhouses.remove(island);
    }


    /**
     * @return a list of all the Greenhouses
     */
    public List<Greenhouse> getGreenhouses() {
        return greenhouses.values().stream().flatMap(List::stream).toList();
    }

    /**
     * Get all greenhouses on island
     * @param island - island
     * @return list of islands or empty list
     */
    public List<Greenhouse> getGreenhouses(Island island) {
        return greenhouses.getOrDefault(island, Collections.emptyList());
    }

    /**
     * @return number of greenhouses loaded
     */
    public int getSize() {
        return greenhouses.values().stream().mapToInt(List::size).sum();
    }


}
