package world.bentobox.greenhouses.greenhouse;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Piglin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.google.common.base.Enums;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.world.AsyncWorldCache;

public class BiomeRecipe implements Comparable<BiomeRecipe> {
    private static final String CHANCE_FOR = "% chance for ";
    private Greenhouses addon;
    private Biome type;
    private Material icon; // Biome icon for control panel
    private int priority;
    private String name;
    private String friendlyName;

    private static final List<BlockFace> ADJ_BLOCKS = Arrays.asList( BlockFace.DOWN, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.WEST);

    // Content requirements
    // Material, Type, Qty. There can be more than one type of material required
    private final Map<Material, Integer> requiredBlocks = new EnumMap<>(Material.class);
    // Plants
    private final TreeMap<Double, GreenhousePlant> plantTree = new TreeMap<>();

    // Mobs
    // Entity Type, Material to Spawn on, Probability
    private final TreeMap<Double, GreenhouseMob> mobTree = new TreeMap<>();

    // Conversions
    // Original Material, Original Type, New Material, New Type, Probability
    //private final Map<Material, GreenhouseBlockConversions> conversionBlocks = new EnumMap<>(Material.class);
    private final Multimap<Material, GreenhouseBlockConversions> conversionBlocks = ArrayListMultimap.create();

    private int mobLimit;
    private int waterCoverage;
    private int iceCoverage;
    private int lavaCoverage;

    private String permission = "";
    private final Random random = new Random();


    /**
     * Create a degenerate recipe with nothing in it
     */
    public BiomeRecipe() {}

    /**
     * @param type - biome
     * @param priority - priority (higher is better)
     */
    public BiomeRecipe(Greenhouses addon, Biome type, int priority) {
        this.addon = addon;
        this.type = type;
        this.priority = priority;
        mobLimit = 9; // Default
    }

    private void startupLog(String message) {
        if (addon.getSettings().isStartupLog()) addon.log(message);
    }

    /**
     * @param oldMaterial - material that will convert
     * @param newMaterial - what it will convert to
     * @param convChance - percentage chance
     * @param localMaterial - what material must be next to it for conversion to happen
     */
    public void addConvBlocks(Material oldMaterial, Material newMaterial, double convChance, Material localMaterial) {
        double probability = Math.min(convChance/100 , 1D);
        conversionBlocks.put(oldMaterial, new GreenhouseBlockConversions(oldMaterial, newMaterial, probability, localMaterial));
        startupLog("   " + convChance + CHANCE_FOR + Util.prettifyText(oldMaterial.toString()) + " to convert to " + Util.prettifyText(newMaterial.toString()));
    }


    /**
     * @param mobType - entity type
     * @param mobProbability - relative probability
     * @param mobSpawnOn - material to spawn on
     * @return true if add is successful
     */
    public boolean addMobs(EntityType mobType, double mobProbability, Material mobSpawnOn) {
        startupLog("   " + mobProbability + CHANCE_FOR + Util.prettifyText(mobType.toString()) + " to spawn on " + Util.prettifyText(mobSpawnOn.toString())+ ".");
        double probability = mobProbability/100;
        double lastProb = mobTree.isEmpty() ? 0D : mobTree.lastKey();
        // Add up all the probabilities in the list so far
        if ((1D - lastProb) >= probability) {
            // Add to probability tree
            mobTree.put(lastProb + probability, new GreenhouseMob(mobType, mobSpawnOn));
            return true;
        } else {
            addon.logError("Mob chances add up to > 100% in " + type.toString() + " biome recipe! Skipping " + mobType);
            return false;
        }
    }

    /**
     * Creates a list of plants that can grow, the probability and what they must grow on.
     * Data is drawn from the file biomes.yml
     * @param plantMaterial - plant type
     * @param plantProbability - probability of growing
     * @param plantGrowOn - material on which it must grow
     * @return true if add is successful
     */
    public boolean addPlants(Material plantMaterial, double plantProbability, Material plantGrowOn) {
        double probability = plantProbability/100;
        // Add up all the probabilities in the list so far
        double lastProb = plantTree.isEmpty() ? 0D : plantTree.lastKey();
        if ((1D - lastProb) >= probability) {
            // Add to probability tree
            plantTree.put(lastProb + probability, new GreenhousePlant(plantMaterial, plantGrowOn));
        } else {
            addon.logError("Plant chances add up to > 100% in " + type.toString() + " biome recipe! Skipping " + plantMaterial.toString());
            return false;
        }
        startupLog("   " + plantProbability + CHANCE_FOR + Util.prettifyText(plantMaterial.toString()) + " to grow on " + Util.prettifyText(plantGrowOn.toString()));
        return true;
    }

    /**
     * @param blockMaterial - block material
     * @param blockQty - number of blocks required
     */
    public void addReqBlocks(Material blockMaterial, int blockQty) {
        requiredBlocks.put(blockMaterial, blockQty);
        startupLog("   " + blockMaterial + " x " + blockQty);
    }

    /**
     * Checks greenhouse meets recipe requirements.
     * @return GreenhouseResult - result
     */
    public CompletableFuture<Set<GreenhouseResult>> checkRecipe(Greenhouse gh) {
        CompletableFuture<Set<GreenhouseResult>> r = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> checkRecipeAsync(r, gh));
        return r;

    }

    /**
     * Check greenhouse meets recipe requirements. Expected to be run async.
     * @param r - future to complete when done
     * @param gh - greenhouse
     * @return set of results from the check
     */
    private Set<GreenhouseResult> checkRecipeAsync(CompletableFuture<Set<GreenhouseResult>> r, Greenhouse gh) {
        AsyncWorldCache cache = new AsyncWorldCache(gh.getWorld());
        Set<GreenhouseResult> result = new HashSet<>();
        long area = gh.getArea();
        Map<Material, Integer> blockCount = new EnumMap<>(Material.class);

        // Look through the greenhouse and count what is in there
        for (int y = gh.getFloorHeight(); y< gh.getCeilingHeight();y++) {
            for (int x = (int) (gh.getBoundingBox().getMinX()+1); x < gh.getBoundingBox().getMaxX(); x++) {
                for (int z = (int) (gh.getBoundingBox().getMinZ()+1); z < gh.getBoundingBox().getMaxZ(); z++) {
                    Material t = cache.getBlockType(x, y, z);
                    if (!t.equals(Material.AIR)) {
                        blockCount.putIfAbsent(t, 0);
                        blockCount.merge(t, 1, Integer::sum);
                    }
                }
            }
        }
        // Calculate % water, ice and lava ratios
        double waterRatio = (double)blockCount.getOrDefault(Material.WATER, 0)/area * 100;
        double lavaRatio = (double)blockCount.getOrDefault(Material.LAVA, 0)/area * 100;
        int ice = blockCount.entrySet().stream().filter(en -> en.getKey().equals(Material.ICE)
                || en.getKey().equals(Material.BLUE_ICE)
                || en.getKey().equals(Material.PACKED_ICE))
                .mapToInt(Map.Entry::getValue).sum();
        double iceRatio = (double)ice/(double)area * 100;

        // Check required ratios - a zero means none of these are allowed, e.g.desert has no water
        if (waterCoverage == 0 && waterRatio > 0) {
            result.add(GreenhouseResult.FAIL_NO_WATER);
        }
        if (lavaCoverage == 0 && lavaRatio > 0) {
            result.add(GreenhouseResult.FAIL_NO_LAVA);
        }
        if (iceCoverage == 0 && iceRatio > 0) {
            result.add(GreenhouseResult.FAIL_NO_ICE);
        }
        if (waterCoverage > 0 && waterRatio < waterCoverage) {
            result.add(GreenhouseResult.FAIL_INSUFFICIENT_WATER);
        }
        if (lavaCoverage > 0 && lavaRatio < lavaCoverage) {
            result.add(GreenhouseResult.FAIL_INSUFFICIENT_LAVA);
        }
        if (iceCoverage > 0 && iceRatio < iceCoverage) {
            result.add(GreenhouseResult.FAIL_INSUFFICIENT_ICE);
        }
        // Compare to the required blocks
        Map<Material, Integer> missingBlocks = requiredBlocks.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() - blockCount.getOrDefault(e.getKey(), 0)));
        // Remove any entries that are 0 or less
        missingBlocks.values().removeIf(v -> v <= 0);
        if (!missingBlocks.isEmpty()) {
            result.add(GreenhouseResult.FAIL_INSUFFICIENT_BLOCKS);
            gh.setMissingBlocks(missingBlocks);
        }
        // Return to main thread to complete
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> r.complete(result));
        return result;
    }

    /**
     * Check if block should be converted
     * @param b - block to check
     */
    public void convertBlock(Block b) {
        Material bType  = b.getType();
        // Check if there is a block conversion for this block, as while the rest of the method wont do anything if .get() returns nothing anyway it still seems to be quite expensive
        if(conversionBlocks.keySet().contains(bType)) {
            for(GreenhouseBlockConversions conversion_option : conversionBlocks.get(bType)) {

                // Roll the dice before bothering with checking the surrounding block as I think it's more common for greenhouses to be filled with convertable blocks and thus this dice roll wont be "wasted"
                if(ThreadLocalRandom.current().nextDouble() < conversion_option.probability()) {
                    // Check if any of the adjacent blocks matches the required LocalMaterial, if there are any required LocalMaterials
                    if(conversion_option.localMaterial() != null) {
                        for(BlockFace adjacent_block : ADJ_BLOCKS) {
                            if(b.getRelative(adjacent_block).getType() == conversion_option.localMaterial()) {
                                b.setType(conversion_option.newMaterial());
                                break;
                            }
                        }
                    } else {
                        b.setType(conversion_option.newMaterial());
                    }
                }
            }
        }
    }

    /**
     * @return the type
     */
    public Biome getBiome() {
        return type;
    }

    /**
     * @return true if there are blocks to convert for this biome
     */
    public boolean getBlockConvert() {
        return !conversionBlocks.isEmpty();
    }

    /**
     * @return the friendly name
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * @return the iceCoverage
     */
    public int getIceCoverage() {
        return iceCoverage;
    }

    /**
     * @return the icon
     */
    public Material getIcon() {
        return icon;
    }

    /**
     * @return the lavaCoverage
     */
    public int getLavaCoverage() {
        return lavaCoverage;
    }

    /**
     * @return the mobLimit
     */
    public int getMobLimit() {
        return mobLimit;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the permission
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Spawn a mob on block b if it makes sense and random change suggests it
     * @param b - block
     * @return true if a mob was spawned
     */
    public boolean spawnMob(Block b) {
        if (b.getY() == 0) {
            return false;
        }
        // Center spawned mob
        Location spawnLoc = b.getLocation().clone().add(new Vector(0.5, 0, 0.5));
        return getRandomMob()
                // Check if the spawn on block matches, if it exists
                .filter(m -> Optional.of(m.mobSpawnOn()).map(b.getRelative(BlockFace.DOWN).getType()::equals).orElse(true))
                // If spawn occurs, check if it can fit inside greenhouse
                .map(m -> {
                    Entity entity = b.getWorld().spawnEntity(spawnLoc, m.mobType());
                    if (entity != null) {
                        preventZombie(entity);
                        return addon
                                .getManager()
                                .getMap()
                                .getGreenhouse(b.getLocation()).map(gh -> {
                                    BoundingBox interior = gh.getBoundingBox().clone();
                                    interior.expand(-1, -1, -1);
                                    if (!interior.contains(entity.getBoundingBox())) {
                                        entity.remove();
                                        return false;
                                    }
                                    return true;
                                }).orElse(false);
                    }
                    return false;
                }).orElse(false);

    }

    /**
     * Prevent hoglins and piglins from zombifying if they spawn in the overworld
     * @param entity - spawned entity
     */
    private void preventZombie(Entity entity) {
        if (!entity
                .getWorld()
                .getEnvironment()
                .equals(Environment.NORMAL) ||
                !Enums.getIfPresent(EntityType.class, "PIGLIN")
                .isPresent()) {
            return;
        }

        if (entity instanceof Piglin p) {
            p.setImmuneToZombification(true);
            return;
        }
        if (entity instanceof Hoglin h) {
            h.setImmuneToZombification(true);
        }
    }

    /**
     * @return a mob that can spawn in the greenhouse
     */
    private Optional<GreenhouseMob> getRandomMob() {
        // Return a random mob that can spawn in the biome or empty
        Double key = mobTree.ceilingKey(random.nextDouble());
        return key == null ? Optional.empty() : Optional.ofNullable(mobTree.get(key));
    }

    private Optional<GreenhousePlant> getRandomPlant() {
        // Grow a random plant that can grow
        double r = random.nextDouble();
        Double key = plantTree.ceilingKey(r);
        return key == null ? Optional.empty() : Optional.ofNullable(plantTree.get(key));
    }

    /**
     * @return a list of blocks that are required for this recipe
     */
    public List<String> getRecipeBlocks() {
        return requiredBlocks.entrySet().stream().map(en -> Util.prettifyText(en.getKey().toString()) + " x " + en.getValue()).collect(Collectors.toList());
    }

    /**
     * @return the waterCoverage
     */
    public int getWaterCoverage() {
        return waterCoverage;
    }

    /**
     * Plants a plant on block bl if it makes sense.
     * @param bl - block
     * @return true if successful
     */
    public boolean growPlant(Block bl) {
        if (!bl.isEmpty()) {
            return false;
        }
        return getRandomPlant().map(p -> {
            if (bl.getY() != 0 && Optional.of(p.plantGrownOn()).map(m -> m.equals(bl.getRelative(BlockFace.DOWN).getType())).orElse(false)) {
                BlockData dataBottom = p.plantMaterial().createBlockData();
                if (dataBottom instanceof Bisected) {
                    ((Bisected) dataBottom).setHalf(Bisected.Half.BOTTOM);
                    BlockData dataTop = p.plantMaterial().createBlockData();
                    ((Bisected) dataTop).setHalf(Bisected.Half.TOP);
                    if (bl.getRelative(BlockFace.UP).getType().equals(Material.AIR)) {
                        bl.setBlockData(dataBottom, false);
                        bl.getRelative(BlockFace.UP).setBlockData(dataTop, false);
                    } else {
                        return false; // No room
                    }
                } else {
                    bl.setBlockData(dataBottom, false);
                }
                bl.getWorld().spawnParticle(Particle.SNOWBALL, bl.getLocation(), 10, 2, 2, 2);
                return true;
            }
            return false;
        }).orElse(false);
    }

    /**
     * @param friendlyName - set the friendly name
     */
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }
    /**
     * @param iceCoverage the ice coverage to set
     */
    public void setIcecoverage(int iceCoverage) {
        if (iceCoverage == 0) {
            startupLog("   No Ice Allowed");
        } else if (iceCoverage > 0) {
            startupLog("   Ice > " + iceCoverage + "%");
        }
        this.iceCoverage = iceCoverage;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(Material icon) {
        this.icon = icon;
    }

    /**
     * @param lavaCoverage the lava coverage to set
     */
    public void setLavacoverage(int lavaCoverage) {
        if (lavaCoverage == 0) {
            startupLog("   No Lava Allowed");
        } else if (lavaCoverage > 0) {
            startupLog("   Lava > " + lavaCoverage + "%");
        }
        this.lavaCoverage = lavaCoverage;
    }

    /**
     * @param mobLimit the mobLimit to set
     */
    public void setMobLimit(int mobLimit) {
        this.mobLimit = mobLimit;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param permission the permission to set
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * @param type the type to set
     */
    public void setType(Biome type) {
        this.type = type;
    }

    /**
     * @param waterCoverage the water coverage to set
     */
    public void setWatercoverage(int waterCoverage) {
        if (waterCoverage == 0) {
            startupLog("   No Water Allowed");
        } else if (waterCoverage > 0) {
            startupLog("   Water > " + waterCoverage + "%");
        }
        this.waterCoverage = waterCoverage;
    }

    @Override
    public int compareTo(BiomeRecipe o) {
        return Integer.compare(o.getPriority(), this.getPriority());
    }


    /**
     * @return true if this recipe has no mobs that may spawn
     */
    public boolean noMobs() {
        return mobTree.isEmpty();
    }

    /**
     * @return the mob types that may spawn due to this recipe
     */
    public Set<EntityType> getMobTypes() {
        return mobTree.values().stream().map(GreenhouseMob::mobType).collect(Collectors.toSet());
    }


}
