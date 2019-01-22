package world.bentobox.greenhouses.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

public class GreenhouseMap {

    private Greenhouses addon;
    private Map<Island, List<Greenhouse>> greenhouses = new HashMap<>();

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
     * Try to get greenhouse at location
     * @param location - location
     * @return Optional greenhouse or empty
     */
    public Optional<Greenhouse> getGreenhouse(Location location) {
        return getXZGreenhouse(location).filter(g -> location.getBlockY() <= g.getCeilingHeight() && location.getBlockY() >= g.getFloorHeight());
    }

    private Optional<Greenhouse> getXZGreenhouse(Location location) {
        return addon.getIslands().getIslandAt(location)
                .filter(i -> greenhouses.containsKey(i))
                .map(i -> {
                    for (Greenhouse gh : greenhouses.get(i)) {
                        Bukkit.getLogger().info("Trying " + location);
                        Bukkit.getLogger().info(gh.toString());
                        if (gh.contains(location)) {
                            Bukkit.getLogger().info("inside gh");
                            return Optional.of(gh);
                        }
                    }
                    Bukkit.getLogger().info("None found");
                    return null;
                }).orElse(Optional.empty());
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
     * Check if location is above a greenhouse
     * @param location - location
     * @return true if above a known greenhouse
     */
    public boolean isAboveGreenhouse(Location location) {
        return getXZGreenhouse(location).map(g -> location.getBlockY() > g.getCeilingHeight()).orElse(false);
    }

    private boolean isOverlapping(Greenhouse greenhouse) {
        return addon.getIslands().getIslandAt(greenhouse.getLocation()).map(i -> {
            greenhouses.putIfAbsent(i, new ArrayList<>());
            return greenhouses.get(i).stream().anyMatch(g -> g.getFootprint().intersects(greenhouse.getFootprint()));
        }).orElse(false);

    }

    /**
     * Removes the greenhouse from the map
     * @param greenhouse - greenhouse
     */
    public void removeGreenhouse(Greenhouse greenhouse) {
        addon.getIslands().getIslandAt(greenhouse.getLocation()).ifPresent(i -> {
            greenhouses.putIfAbsent(i, new ArrayList<>());
            greenhouses.get(i).remove(greenhouse);
        });
    }

    /**
     * @return a list of all the Greenhouses
     */
    public List<Greenhouse> getGreenhouses() {
        return greenhouses.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }
}
