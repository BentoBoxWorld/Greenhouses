package world.bentobox.greenhouses.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.NumberConversions;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;

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
        long blockTick = addon.getSettings().getBlockTick() * 60 * 20L; // In minutes

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
        final World world = gh.getWorld();
        final BoundingBox bb = gh.getBoundingBox();
        if(world == null || gh.getLocation() == null || gh.getLocation().getWorld() == null
                || !gh.getLocation().getWorld().isChunkLoaded(((int) bb.getMaxX()) >> 4, ((int) bb.getMaxZ()) >> 4)
                || !gh.getLocation().getWorld().isChunkLoaded(((int) bb.getMinX()) >> 4, ((int) bb.getMinZ()) >> 4)){
            return;
        }
        final BoundingBox ibb = gh.getInternalBoundingBox();
        int ghMinX = NumberConversions.floor(ibb.getMinX());
        int ghMaxX = NumberConversions.floor(ibb.getMaxX());
        int ghMinY = NumberConversions.floor(gh.getBoundingBox().getMinY()); // Note: this gets the floor
        int ghMaxY = NumberConversions.floor(ibb.getMaxY());
        int ghMinZ = NumberConversions.floor(ibb.getMinZ());
        int ghMaxZ = NumberConversions.floor(ibb.getMaxZ());
        BiomeRecipe biomeRecipe = gh.getBiomeRecipe();

        for (int x = ghMinX; x < ghMaxX; x++) {
            for (int z = ghMinZ; z < ghMaxZ; z++) {
                for (int y = ghMinY; y < ghMaxY; y++) {
                    Block b = world.getBlockAt(x, y, z);

                    if(!b.isEmpty()) {
                        biomeRecipe.convertBlock(b);
                    }
                }
            }
        }
    }

    private void verify(Greenhouse gh) {
        if(gh.getLocation() == null || gh.getLocation().getWorld() == null
                || !gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMaxX()) >> 4, ((int) gh.getBoundingBox().getMaxZ()) >> 4)
                || !gh.getLocation().getWorld().isChunkLoaded(((int) gh.getBoundingBox().getMinX()) >> 4, ((int) gh.getBoundingBox().getMinZ()) >> 4)){
            // Skipping verify for unloaded greenhouse
            return;
        }
        gh.getBiomeRecipe().checkRecipe(gh).thenAccept(rs -> {
            if (!rs.isEmpty()) {
                addon.log("Greenhouse failed verification at " + gh.getLocation());
                g.removeGreenhouse(gh);
            }
        });

    }

    /**
     * Try to spawn mobs in a greenhouse
     * @param gh greenhouse
     * @return true if mobs were spawned, false if not
     */
    boolean addMobs(Greenhouse gh) {
        final BoundingBox bb = gh.getBoundingBox();
        if(gh.getLocation() == null || gh.getLocation().getWorld() == null || gh.getWorld() == null
                || !gh.getLocation().getWorld().isChunkLoaded(((int) bb.getMaxX()) >> 4, ((int) bb.getMaxZ()) >> 4)
                || !gh.getLocation().getWorld().isChunkLoaded(((int) bb.getMinX()) >> 4, ((int) bb.getMinZ()) >> 4)){
            // Skipping addmobs for unloaded greenhouse
            return false;
        }
        if (gh.getBiomeRecipe().noMobs()) {
            return false;
        }
        // Check greenhouse chunks are loaded
        for (double blockX = bb.getMinX(); blockX < bb.getMaxX(); blockX+=16) {
            for (double blockZ = bb.getMinZ(); blockZ < bb.getMaxZ(); blockZ+=16) {
                int chunkX = (int)(blockX / 16);
                int chunkZ = (int)(blockZ / 16);
                if (!gh.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        // Count the entities in the greenhouse
        long sum = gh.getWorld().getEntities().stream()
                .filter(e -> gh.getBiomeRecipe().getMobTypes().contains(e.getType()))
                .filter(e -> gh.contains(e.getLocation())).count();
        // Get the blocks in the greenhouse where spawning could occur
        List<GrowthBlock> list = new ArrayList<>(getAvailableBlocks(gh, false));
        Collections.shuffle(list, new Random(System.currentTimeMillis()));
        Iterator<GrowthBlock> it = list.iterator();
        // Check if the greenhouse is full
        if (gh.getBiomeRecipe().getMaxMob() > -1 && sum >= gh.getBiomeRecipe().getMaxMob()) {
            return false;
        }
        while (it.hasNext() && (sum == 0 || gh.getArea() / sum >= gh.getBiomeRecipe().getMobLimit())) {
            // Spawn something if chance says so
            if (gh.getBiomeRecipe().spawnMob(it.next().block())) {
                // Add a mob to the sum in the greenhouse
                sum++;
            }
        }
        return sum > 0;
    }

    /**
     * Grow plants in the greenhouse
     * @param gh - greenhouse
     */
    private void growPlants(Greenhouse gh) {
        final BoundingBox bb = gh.getBoundingBox();
        if (gh.getLocation() == null || gh.getLocation().getWorld() == null
                || !gh.getLocation().getWorld().isChunkLoaded(((int) bb.getMaxX()) >> 4, ((int) bb.getMaxZ()) >> 4) || !gh.getLocation().getWorld().isChunkLoaded(((int) bb.getMinX()) >> 4, ((int) bb.getMinZ()) >> 4)){
            //Skipping growplants for unloaded greenhouse
            return;
        }
        int bonemeal = getBoneMeal(gh);
        if (bonemeal > 0) {
            // Get a list of all available blocks
            List<GrowthBlock> list = getAvailableBlocks(gh, false);
            Collections.shuffle(list);
            int plantsGrown = list.stream().limit(bonemeal).mapToInt(bl -> gh.getBiomeRecipe().growPlant(bl, false) ? 1 : 0).sum();
            // Underwater plants
            list = getAvailableBlocks(gh, true);
            Collections.shuffle(list);
            plantsGrown += list.stream().limit(bonemeal).mapToInt(bl -> gh.getBiomeRecipe().growPlant(bl, true) ? 1 : 0).sum();
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

    public record GrowthBlock(Block block, Boolean floor) {}

    /**
     * Get a list of the lowest level blocks inside the greenhouse. May be air, liquid or plants.
     * These blocks sit just above solid blocks. Leaves are ignored too.
     * @param gh - greenhouse
     * @param ignoreLiquid - true if liquid blocks should be treated like air blocks
     * @return List of blocks
     */
    protected List<GrowthBlock> getAvailableBlocks(Greenhouse gh, boolean ignoreLiquid) {
        final BoundingBox bb = gh.getBoundingBox();
        final BoundingBox ibb = gh.getInternalBoundingBox();
        List<GrowthBlock> result = new ArrayList<>();
        if (gh.getWorld() == null) return result;
        for (double x = ibb.getMinX(); x < ibb.getMaxX(); x++) {
            for (double z = ibb.getMinZ(); z < ibb.getMaxZ(); z++) {
                for (double y = ibb.getMaxY() - 1; y >= bb.getMinY(); y--) {
                    Block b = gh.getWorld().getBlockAt(NumberConversions.floor(x), NumberConversions.floor(y), NumberConversions.floor(z));
                    if (checkBlock(result, b, ignoreLiquid)) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    private boolean checkBlock(List<GrowthBlock> result, Block b, boolean ignoreLiquid) {
        // Check floor blocks
        if (!ignoreLiquid) {
            // Check ceiling blocks
            if (b.isEmpty() && !b.getRelative(BlockFace.UP).isEmpty()) {
                result.add(new GrowthBlock(b, false));
            }
            if (!b.isEmpty() && !Tag.LEAVES.isTagged(b.getType())
                    && (b.getRelative(BlockFace.UP).isEmpty()
                            || b.getRelative(BlockFace.UP).isPassable()
                            || Tag.LEAVES.isTagged(b.getRelative(BlockFace.UP).getType())
                            )
                    ) {
                result.add(new GrowthBlock(b.getRelative(BlockFace.UP), true));
                return true;
            }
        } else {
            if (!b.isEmpty() && !b.isLiquid() && b.getRelative(BlockFace.UP).isLiquid()) {
                result.add(new GrowthBlock(b.getRelative(BlockFace.UP), true));
                return true;
            }
        }
        return false;
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

    /**
     * Get the hopper
     * @param gh greenhouse
     * @return hopper block or null if it does not exist
     */
    private Hopper getHopper(Greenhouse gh) {
        // Check if the hopper block is still a hopper
        if (gh.getRoofHopperLocation() == null || !gh.getRoofHopperLocation().getBlock().getType().equals(Material.HOPPER)) {
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
