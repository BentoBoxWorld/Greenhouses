package world.bentobox.greenhouses.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * Runs the ecosystem for a greenhouse
 * @author tastybento
 *
 */
public class EcoSystemManager {

    private Greenhouses addon;
    private GreenhouseManager g;
    private BukkitTask plantTask;
    private BukkitTask mobTask;
    private BukkitTask blockTask;
    private BukkitTask ecoTask;

    public EcoSystemManager(Greenhouses addon, GreenhouseManager greenhouseManager) {
        this.addon = addon;
        this.g = greenhouseManager;
        setup();
    }

    private void setup() {
        // Kick off flower growing
        long plantTick = addon.getSettings().getPlantTick() * 60 * 20; // In minutes
        if (plantTick > 0) {
            addon.log("Kicking off flower growing scheduler every " + addon.getSettings().getPlantTick() + " minutes");
            plantTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(), () -> {
                g.getMap().getGreenhouses().forEach(gh -> growPlants(gh));
            }, 80L, plantTick);

        } else {
            addon.log("Flower growth disabled.");
        }

        // Kick block conversion growing
        long blockTick = addon.getSettings().getBlockTick() * 60 * 20; // In minutes

        if (blockTick > 0) {
            addon.log("Kicking off block conversion scheduler every " + addon.getSettings().getBlockTick() + " minutes");
            blockTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(), () -> {
                addon.log("Servicing greenhouse biome");


            }, 60L, blockTick);
        } else {
            addon.log("Block conversion disabled.");
        }
        // Kick off g/h verification
        long ecoTick = addon.getSettings().getEcoTick() * 60 * 20; // In minutes
        if (ecoTick > 0) {
            addon.log("Kicking off greenhouse verify scheduler every " + addon.getSettings().getEcoTick() + " minutes");
            ecoTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(), () -> {
                // Todo
            }, ecoTick, ecoTick);

        } else {
            addon.log("Greenhouse verification disabled.");
        }
        // Kick off mob population
        long mobTick = addon.getSettings().getMobTick() * 60 * 20; // In minutes
        if (mobTick > 0) {
            addon.log("Kicking off mob populator scheduler every " + addon.getSettings().getMobTick() + " minutes");
            mobTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(), () -> {
            }, 120L, mobTick);

        } else {
            addon.log("Mob disabled.");
        }

    }

    /**
     * Growns plants in the greenhouse
     * @param gh - greenhouse
     */
    private void growPlants(Greenhouse gh) {
        int bonemeal = getBoneMeal(gh);
        if (bonemeal > 0) {
            // Get a list of all available blocks
            setBoneMeal(gh, bonemeal - getAvailableBlocks(gh).stream().limit(bonemeal).mapToInt(bl -> gh.getBiomeRecipe().growPlant(bl) ? 1 : 0).sum());
        }

    }

    /**
     * Set a hopper's bone meal to this value
     * @param gh - greenhouse
     * @param value - value to set
     */
    private void setBoneMeal(Greenhouse gh, int value) {
        Hopper hopper = getHopper(gh);
        if (hopper != null) {
            hopper.getInventory().remove(Material.BONE_MEAL);
            hopper.getInventory().addItem(new ItemStack(Material.BONE_MEAL, value));
        }

    }

    /**
     * Get a list of the highest block inside the greenhouse
     * @param gh - greenhouse
     * @return List of blocks
     */
    private List<Block> getAvailableBlocks(Greenhouse gh) {
        List<Block> result = new ArrayList<>();
        for (int x = (int)gh.getFootprint().getMinX() + 1; x < (int)gh.getFootprint().getMaxX(); x++) {
            for (int z = (int)gh.getFootprint().getMinY() + 1; z < (int)gh.getFootprint().getMaxY(); z++) {
                for (int y = gh.getCeilingHeight() - 1; y >= gh.getFloorHeight(); y--) {
                    Block b = gh.getLocation().getWorld().getBlockAt(x, y, z);
                    if (!b.getType().equals(Material.AIR)) {
                        result.add(b);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private int getBoneMeal(Greenhouse gh) {
        Hopper hopper = getHopper(gh);
        if (hopper == null || !hopper.getInventory().contains(Material.BONE_MEAL)) {
            return 0;
        }
        return Arrays.stream(hopper.getInventory().getContents()).filter(Objects::nonNull)
                .filter(i -> i.getType().equals(Material.BONE_MEAL))
                .mapToInt(ItemStack::getAmount).sum();
    }

    private Hopper getHopper(Greenhouse gh) {
        if (gh.getRoofHopperLocation() == null) {
            return null;
        }
        // Check if there are any bonemeal in the hopper
        if (gh.getRoofHopperLocation().getBlock().getType() != Material.HOPPER) {
            gh.setRoofHopperLocation(null);
            return null;
        }
        return (Hopper)gh.getRoofHopperLocation().getBlock().getBlockData();
    }

    public void cancel() {
        plantTask.cancel();
        mobTask.cancel();
        blockTask.cancel();
        ecoTask.cancel();
    }

}
