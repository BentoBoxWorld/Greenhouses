package world.bentobox.greenhouses.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.NumberConversions;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.GreenhouseMob;
import world.bentobox.greenhouses.greenhouse.MobHabitat;

/**
 * Runs the ecosystem for a greenhouse
 * @author tastybento
 *
 */
public class EcoSystemManager {

    private static final int PLANTS_PER_BONEMEAL = 6;
    private static final String MINUTES = " minutes";
    private static final String TICKS = " ticks";
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

        // Mob spawning and block conversion sample a few random spots every sampleTicks rather
        // than scanning every block in one go (issue #121). The fraction is chosen so that, on
        // average, each spot still gets one chance every blocktick / mobtick minutes.
        long sampleTicks = Math.max(1, addon.getSettings().getSampleTicks());
        // Kick block conversion growing
        long blockTick = addon.getSettings().getBlockTick() * 60 * 20L; // In minutes

        if (blockTick > 0) {
            addon.log("Kicking off block conversion scheduler every " + addon.getSettings().getBlockTick() + MINUTES
                    + ", sampled every " + sampleTicks + TICKS);
            double fraction = Math.min(1D, (double) sampleTicks / blockTick);
            blockTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(),
                    () -> g.getMap().getGreenhouses().forEach(gh -> convertBlocks(gh, fraction)), 60L, sampleTicks);
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
            addon.log("Kicking off mob populator scheduler every " + addon.getSettings().getMobTick() + MINUTES
                    + ", sampled every " + sampleTicks + TICKS);
            double fraction = Math.min(1D, (double) sampleTicks / mobTick);
            mobTask = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(),
                    () -> g.getMap().getGreenhouses().forEach(gh -> addMobs(gh, fraction)), 120L, sampleTicks);
        } else {
            addon.log("Mob disabled.");
        }

    }

    /**
     * Give a random sample of the blocks in the greenhouse a chance to convert. Each call picks,
     * on average, {@code fraction} of the greenhouse's volume, so calling this every
     * {@code fraction * blocktick} ticks gives every block one chance per blocktick on average -
     * the same rate as the old full scan, without doing the whole scan in one tick (issue #121).
     * @param gh - greenhouse
     * @param fraction - fraction of the greenhouse's blocks to sample, 0 to 1
     * @return number of blocks sampled
     */
    int convertBlocks(Greenhouse gh, double fraction) {
        if (gh.getBiomeRecipe() == null || !gh.getBiomeRecipe().getBlockConvert()) {
            return 0;
        }
        final BoundingBox ibb = gh.getInternalBoundingBox();
        int ghMinX = NumberConversions.floor(ibb.getMinX());
        int ghMaxX = NumberConversions.floor(ibb.getMaxX());
        int ghMinY = NumberConversions.floor(gh.getBoundingBox().getMinY()); // Note: this gets the floor
        int ghMaxY = NumberConversions.floor(ibb.getMaxY());
        int ghMinZ = NumberConversions.floor(ibb.getMinZ());
        int ghMaxZ = NumberConversions.floor(ibb.getMaxZ());
        int sizeX = ghMaxX - ghMinX;
        int sizeY = ghMaxY - ghMinY;
        int sizeZ = ghMaxZ - ghMinZ;
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return 0;
        }
        int samples = sampleCount(sizeX * sizeY * sizeZ, fraction, ThreadLocalRandom.current().nextDouble());
        // Only check the chunks when there is something to do - most calls sample nothing
        if (samples == 0 || !chunksLoaded(gh, gh.getBoundingBox())) {
            return 0;
        }
        final World world = gh.getWorld();
        BiomeRecipe biomeRecipe = gh.getBiomeRecipe();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < samples; i++) {
            Block b = world.getBlockAt(ghMinX + rand.nextInt(sizeX), ghMinY + rand.nextInt(sizeY), ghMinZ + rand.nextInt(sizeZ));
            if (!b.isEmpty()) {
                biomeRecipe.convertBlock(b);
            }
        }
        return samples;
    }

    /**
     * How many items to sample from a population so that, on average, {@code fraction} of it is
     * sampled. The whole part is always sampled; the remainder is sampled with that probability.
     * @param population - number of items
     * @param fraction - fraction to sample, 0 to 1
     * @param roll - random number in [0, 1)
     * @return number to sample
     */
    static int sampleCount(int population, double fraction, double roll) {
        if (population <= 0 || fraction <= 0) {
            return 0;
        }
        double expected = population * Math.min(1D, fraction);
        int whole = (int) expected;
        return whole + (roll < expected - whole ? 1 : 0);
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
     * Try to spawn mobs on a random sample of the greenhouse's floor columns. Each call picks, on
     * average, {@code fraction} of the columns and tries every spawn spot in them, so calling this
     * every {@code fraction * mobtick} ticks gives every spot one chance per mobtick on average -
     * the same rate as the old wave, but spread out so mobs arrive gradually instead of filling
     * the greenhouse in one go (issue #121).
     * @param gh greenhouse
     * @param fraction - fraction of the greenhouse's columns to sample, 0 to 1
     * @return true if any mobs were spawned, false if not
     */
    boolean addMobs(Greenhouse gh, double fraction) {
        if (gh.getBiomeRecipe() == null || gh.getBiomeRecipe().noMobs()) {
            return false;
        }
        final BoundingBox bb = gh.getBoundingBox();
        final BoundingBox ibb = gh.getInternalBoundingBox();
        int minX = NumberConversions.floor(ibb.getMinX());
        int minZ = NumberConversions.floor(ibb.getMinZ());
        int sizeX = NumberConversions.ceil(ibb.getMaxX()) - minX;
        int sizeZ = NumberConversions.ceil(ibb.getMaxZ()) - minZ;
        if (sizeX <= 0 || sizeZ <= 0) {
            return false;
        }
        int samples = sampleCount(sizeX * sizeZ, fraction, ThreadLocalRandom.current().nextDouble());
        // Only check chunks and count entities when there is something to do
        if (samples == 0 || !chunksLoaded(gh, bb)) {
            return false;
        }
        final BiomeRecipe recipe = gh.getBiomeRecipe();
        // Count the mobs in the greenhouse, by habitat - only look at the greenhouse, not the whole world
        MobCounts counts = new MobCounts(gh, recipe);
        gh.getWorld().getNearbyEntities(bb, e -> recipe.getMobTypes().contains(e.getType()) && gh.contains(e.getLocation()))
        .forEach(e -> counts.add(e.getType()));
        int spawned = 0;
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < samples && counts.anyRoom(); i++) {
            List<GrowthBlock> spots = new ArrayList<>();
            getAvailableBlocks(gh, minX + rand.nextInt(sizeX), minZ + rand.nextInt(sizeZ), false, spots);
            Collections.shuffle(spots);
            for (GrowthBlock spot : spots) {
                if (!counts.anyRoom()) {
                    break;
                }
                // Spawn something if chance says so and its habitat has room
                GreenhouseMob[] chosen = new GreenhouseMob[1];
                if (recipe.spawnMob(spot.block(), m -> {
                    chosen[0] = m;
                    return counts.hasRoom(m.mobType());
                })) {
                    counts.add(chosen[0] == null ? null : chosen[0].mobType());
                    spawned++;
                }
            }
        }
        return spawned > 0;
    }

    /**
     * The mobs in a greenhouse, counted by the habitat whose limit they use (issue #8). Ground
     * mobs are limited by floor area, water and lava mobs by the number of water or lava blocks,
     * and flying mobs by the internal volume. The recipe's maxmobs caps the total.
     */
    static class MobCounts {
        private final Greenhouse gh;
        private final BiomeRecipe recipe;
        private final Map<MobHabitat, Long> counts = new EnumMap<>(MobHabitat.class);
        private final Set<MobHabitat> pools = EnumSet.noneOf(MobHabitat.class);
        private long total;

        MobCounts(Greenhouse gh, BiomeRecipe recipe) {
            this.gh = gh;
            this.recipe = recipe;
            recipe.getMobTypes().forEach(t -> pools.add(pool(t)));
            if (pools.isEmpty()) {
                pools.add(MobHabitat.GROUND);
            }
        }

        /**
         * The habitat a mob type counts against. Water and lava fall back to the ground until
         * the greenhouse's liquid has been measured, which happens at the first recipe check.
         */
        MobHabitat pool(@Nullable EntityType type) {
            MobHabitat h = type == null ? MobHabitat.GROUND : recipe.getMobPool(type);
            if (h == null || space(h) == null) {
                return MobHabitat.GROUND;
            }
            return h;
        }

        @Nullable
        private Integer space(MobHabitat h) {
            return switch (h) {
            case WATER -> gh.getWaterBlocks();
            case LAVA -> gh.getLavaBlocks();
            case AIR -> gh.getInternalVolume();
            default -> gh.getArea();
            };
        }

        void add(@Nullable EntityType type) {
            counts.merge(pool(type), 1L, Long::sum);
            total++;
        }

        /**
         * Whether one more mob of this type may spawn. The absolute maxmobs cap is checked on
         * every spawn, not just once (issue #127), alongside the habitat's density limit.
         */
        boolean hasRoom(@Nullable EntityType type) {
            return underCap() && hasRoom(pool(type));
        }

        boolean anyRoom() {
            return underCap() && pools.stream().anyMatch(this::hasRoom);
        }

        private boolean underCap() {
            int maxMob = recipe.getMaxMob();
            return maxMob <= -1 || total < maxMob;
        }

        private boolean hasRoom(MobHabitat h) {
            int space = Objects.requireNonNullElse(space(h), 0);
            long sum = counts.getOrDefault(h, 0L);
            // The limit is blocks per mob, so one more fits if every mob, the new one included,
            // still has that many blocks. A habitat with any space always gets its first mob.
            return space > 0 && (sum == 0 || (sum + 1) * recipe.getMobLimit(h) <= space);
        }
    }

    /**
     * Checks that the greenhouse has a world and that every chunk it covers is loaded. Mobs
     * cannot be spawned into unloaded chunks.
     * @param gh - greenhouse
     * @param bb - the greenhouse's bounding box
     * @return true if the greenhouse can be worked on
     */
    private boolean chunksLoaded(Greenhouse gh, BoundingBox bb) {
        if (gh.getLocation() == null || gh.getLocation().getWorld() == null || gh.getWorld() == null
                || !gh.getLocation().getWorld().isChunkLoaded(((int) bb.getMaxX()) >> 4, ((int) bb.getMaxZ()) >> 4)
                || !gh.getLocation().getWorld().isChunkLoaded(((int) bb.getMinX()) >> 4, ((int) bb.getMinZ()) >> 4)) {
            // Skipping addmobs for unloaded greenhouse
            return false;
        }
        for (double blockX = bb.getMinX(); blockX < bb.getMaxX(); blockX += 16) {
            for (double blockZ = bb.getMinZ(); blockZ < bb.getMaxZ(); blockZ += 16) {
                if (!gh.getWorld().isChunkLoaded((int) (blockX / 16), (int) (blockZ / 16))) {
                    return false;
                }
            }
        }
        return true;
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
            final BoundingBox internalBb = gh.getInternalBoundingBox();
            // Get a list of all available blocks
            List<GrowthBlock> list = getAvailableBlocks(gh, false);
            Collections.shuffle(list);
            int plantsGrown = list.stream().limit(bonemeal).mapToInt(bl -> gh.getBiomeRecipe().growPlant(bl, false, internalBb) ? 1 : 0).sum();
            // Underwater plants
            list = getAvailableBlocks(gh, true);
            Collections.shuffle(list);
            plantsGrown += list.stream().limit(bonemeal).mapToInt(bl -> gh.getBiomeRecipe().growPlant(bl, true, internalBb) ? 1 : 0).sum();
            if (plantsGrown > 0) {
                setBoneMeal(gh, bonemeal - (int)Math.ceil((double)plantsGrown / PLANTS_PER_BONEMEAL ));
            }

        }

    }

    /**
     * Set a hopper's bone meal to this value. If the value is zero or less the bone meal is
     * simply removed; an ItemStack with an amount of zero is rejected by newer server APIs.
     * @param gh - greenhouse
     * @param value - value to set
     */
    protected void setBoneMeal(Greenhouse gh, int value) {
        Hopper hopper = getHopper(gh);
        if (hopper == null) {
            return;
        }
        hopper.getInventory().remove(Material.BONE_MEAL);
        if (value > 0) {
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
        final BoundingBox ibb = gh.getInternalBoundingBox();
        List<GrowthBlock> result = new ArrayList<>();
        if (gh.getWorld() == null) return result;
        // Block coordinates, so iterate as ints rather than incrementing doubles
        for (int x = NumberConversions.floor(ibb.getMinX()); x < ibb.getMaxX(); x++) {
            for (int z = NumberConversions.floor(ibb.getMinZ()); z < ibb.getMaxZ(); z++) {
                getAvailableBlocks(gh, x, z, ignoreLiquid, result);
            }
        }
        return result;
    }

    /**
     * Add the available blocks in one column of the greenhouse to the result, top to bottom.
     * @param gh - greenhouse
     * @param x - block x
     * @param z - block z
     * @param ignoreLiquid - true if liquid blocks should be treated like air blocks
     * @param result - list to add to
     */
    private void getAvailableBlocks(Greenhouse gh, int x, int z, boolean ignoreLiquid, List<GrowthBlock> result) {
        final BoundingBox bb = gh.getBoundingBox();
        final BoundingBox ibb = gh.getInternalBoundingBox();
        for (int y = NumberConversions.floor(ibb.getMaxY()) - 1; y >= bb.getMinY(); y--) {
            Block b = gh.getWorld().getBlockAt(x, y, z);
            checkBlock(result, b, ignoreLiquid);
        }
    }

    private void checkBlock(List<GrowthBlock> result, Block b, boolean ignoreLiquid) {
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
            }
        } else {
            if (!b.isEmpty() && !b.isLiquid() && b.getRelative(BlockFace.UP).isLiquid()) {
                result.add(new GrowthBlock(b.getRelative(BlockFace.UP), true));
            }
        }
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
        // A task is null if its tick is set to 0 (disabled)
        for (BukkitTask task : new BukkitTask[] {plantTask, mobTask, blockTask, ecoTask}) {
            if (task != null) {
                task.cancel();
            }
        }
    }

}
