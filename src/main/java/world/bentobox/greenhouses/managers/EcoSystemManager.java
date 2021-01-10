package world.bentobox.greenhouses.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * Runs the ecosystem for a greenhouse
 * @author tastybento
 *
 */
public class EcoSystemManager {

    private static final int PLANTS_PER_BONEMEAL = 6;
    private static final String MINUTES = " minutes";
    private final Greenhouses addon;
    private final GreenhouseManager g;
    private BukkitTask plantTask;
    private BukkitTask mobTask;
    private BukkitTask blockTask;
    private BukkitTask ecoTask;

    public EcoSystemManager(Greenhouses addon, GreenhouseManager greenhouseManager) {
        this.addon = addon;
        this.g = greenhouseManager;
    }

    /**
     * Kick off schedulers
     */
    void setup() {
        // Kick off flower growing
        long plantTick = addon.getSettings().getPlantTick() * 60 * 20L; // In minutes
        if (plantTick > 0) {
            addon.log("Kicking off flower growing scheduler every " + addon.getSettings().getPlantTick() + MINUTES);
            plantTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(), () -> g.getMap().getGreenhouses().forEach(this::growPlants), 80L, plantTick);
        } else {
            addon.log("Flower growth disabled.");
        }

        // Kick block conversion growing
        long blockTick = addon.getSettings().getBlockTick() * 60 * 20l; // In minutes

        if (blockTick > 0) {
            addon.log("Kicking off block conversion scheduler every " + addon.getSettings().getBlockTick() + MINUTES);
            blockTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(), () -> g.getMap().getGreenhouses().forEach(this::convertBlocks), 60L, blockTick);
        } else {
            addon.log("Block conversion disabled.");
        }
        // Kick off g/h verification
        long ecoTick = addon.getSettings().getEcoTick() * 60 * 20L; // In minutes
        if (ecoTick > 0) {
            addon.log("Kicking off greenhouse verify scheduler every " + addon.getSettings().getEcoTick() + MINUTES);
            ecoTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(), () -> g.getMap().getGreenhouses().forEach(this::verify), ecoTick, ecoTick);

        } else {
            addon.log("Greenhouse verification disabled.");
        }
        // Kick off mob population
        long mobTick = addon.getSettings().getMobTick() * 60 * 20L; // In minutes
        if (mobTick > 0) {
            addon.log("Kicking off mob populator scheduler every " + addon.getSettings().getMobTick() + MINUTES);
            mobTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(), () -> g.getMap().getGreenhouses().forEach(this::addMobs), 120L, mobTick);
        } else {
            addon.log("Mob disabled.");
        }

    }

    private void convertBlocks(Greenhouse gh) {
        if(!gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMaxX()) >> 4, ((int) gh.getBoundingBox().getMaxZ()) >> 4) || !gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMinX()) >> 4, ((int) gh.getBoundingBox().getMinZ()) >> 4)){
            return;
        }
        for (double x = gh.getInternalBoundingBox().getMinX(); x < gh.getInternalBoundingBox().getMaxX(); x++) {
            for (double z = gh.getInternalBoundingBox().getMinZ(); z < gh.getInternalBoundingBox().getMaxZ(); z++) {
                for (double y = gh.getInternalBoundingBox().getMaxY() - 1; y >= gh.getBoundingBox().getMinY() && y > 0; y--) {
                    Block b = gh.getWorld().getBlockAt(NumberConversions.floor(x), NumberConversions.floor(y), NumberConversions.floor(z)).getRelative(BlockFace.DOWN);
                    if (!b.isEmpty()) gh.getBiomeRecipe().convertBlock(gh, b);
                }
            }
        }
    }

    private void verify(Greenhouse gh) {
        if(!gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMaxX()) >> 4, ((int) gh.getBoundingBox().getMaxZ()) >> 4) || !gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMinX()) >> 4, ((int) gh.getBoundingBox().getMinZ()) >> 4)){
            //addon.log("Skipping verify for unloaded greenhouse at " + gh.getLocation());
            return;
        }
        if (!gh.getBiomeRecipe().checkRecipe(gh).isEmpty()) {
            addon.log("Greenhouse failed verification at " + gh.getLocation());
            g.removeGreenhouse(gh);
        }
    }

    private void addMobs(Greenhouse gh) {
        if(!gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMaxX()) >> 4, ((int) gh.getBoundingBox().getMaxZ()) >> 4) || !gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMinX()) >> 4, ((int) gh.getBoundingBox().getMinZ()) >> 4)){
            //addon.log("Skipping addmobs for unloaded greenhouse at " + gh.getLocation());
            return;
        }
        if (gh.getBiomeRecipe().noMobs()) {
            return;
        }
        // Check greenhouse chunks are loaded
        for (double blockX = gh.getBoundingBox().getMinX(); blockX < gh.getBoundingBox().getMaxX(); blockX+=16) {
            for (double blockZ = gh.getBoundingBox().getMinZ(); blockZ < gh.getBoundingBox().getMaxZ(); blockZ+=16) {
                int chunkX = (int)(blockX / 16);
                int chunkZ = (int)(blockZ / 16);
                if (!gh.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                    return;
                }
            }
        }
        // Count the entities in the greenhouse
        long sum = gh.getWorld().getEntities().stream()
                .filter(e -> gh.getBiomeRecipe().getMobTypes().contains(e.getType()))
                .filter(e -> gh.contains(e.getLocation())).count();
        // Get the blocks in the greenhouse where spawning could occur
        List<Block> list = new ArrayList<>(getAvailableBlocks(gh, false));
        Collections.shuffle(list, new Random(System.currentTimeMillis()));
        Iterator<Block> it = list.iterator();
        // Check if the greenhouse is full
        while (it.hasNext() && (sum == 0 || gh.getArea() / sum >= gh.getBiomeRecipe().getMobLimit())) {
            // Spawn something if chance says so
            if (gh.getBiomeRecipe().spawnMob(it.next())) {
                // Add a mob to the sum in the greenhouse
                sum++;
            }
        }
    }

    /**
     * Grow plants in the greenhouse
     * @param gh - greenhouse
     */
    private void growPlants(Greenhouse gh) {
        if(!gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMaxX()) >> 4, ((int) gh.getBoundingBox().getMaxZ()) >> 4) || !gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMinX()) >> 4, ((int) gh.getBoundingBox().getMinZ()) >> 4)){
            //addon.log("Skipping growplants for unloaded greenhouse at " + gh.getLocation());
            return;
        }
        int bonemeal = getBoneMeal(gh);
        if (bonemeal > 0) {
            // Get a list of all available blocks
            int plantsGrown = getAvailableBlocks(gh, true).stream().limit(bonemeal).mapToInt(bl -> gh.getBiomeRecipe().growPlant(bl) ? 1 : 0).sum();
            if (plantsGrown > 0) {
                setBoneMeal(gh, bonemeal - (int)Math.ceil((double)plantsGrown / PLANTS_PER_BONEMEAL ));
            }

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
     * Get a list of the lowest level blocks inside the greenhouse. May be air, liquid or plants.
     * These blocks sit just above solid blocks
     * @param gh - greenhouse
     * @param ignoreliquid - true if liquid blocks should be treated like air blocks
     * @return List of blocks
     */
    public List<Block> getAvailableBlocks(Greenhouse gh, boolean ignoreLiquid) {
        List<Block> result = new ArrayList<>();
        for (double x = gh.getInternalBoundingBox().getMinX(); x < gh.getInternalBoundingBox().getMaxX(); x++) {
            for (double z = gh.getInternalBoundingBox().getMinZ(); z < gh.getInternalBoundingBox().getMaxZ(); z++) {
                for (double y = gh.getInternalBoundingBox().getMaxY() - 1; y >= gh.getBoundingBox().getMinY(); y--) {
                    Block b = gh.getWorld().getBlockAt(NumberConversions.floor(x), NumberConversions.floor(y), NumberConversions.floor(z));
                    if (!(b.isEmpty() || (ignoreLiquid && b.isLiquid()))
                            && (b.getRelative(BlockFace.UP).isEmpty()
                                    || (b.getRelative(BlockFace.UP).isPassable() && !b.isLiquid())
                                    || (ignoreLiquid && b.isLiquid() && b.getRelative(BlockFace.UP).isPassable()))) {
                        result.add(b.getRelative(BlockFace.UP));
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
        return (Hopper)gh.getRoofHopperLocation().getBlock().getState();
    }

    /**
     * Cancel all the scheduled tasks
     */
    public void cancel() {
        plantTask.cancel();
        mobTask.cancel();
        blockTask.cancel();
        ecoTask.cancel();
    }

}
