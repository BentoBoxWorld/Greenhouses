package world.bentobox.greenhouses.greenhouse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;

import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

public class BiomeRecipe implements Comparable<BiomeRecipe> {
    private Greenhouses plugin;
    private Biome type;
    private Material icon; // Biome icon for control panel
    private int priority;
    private String name;
    private String friendlyName;

    private final List<BlockFace> ADJ_BLOCKS = Arrays.asList( BlockFace.DOWN, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.WEST);

    // Content requirements
    // Material, Type, Qty. There can be more than one type of material required
    private Map<Material, Integer> requiredBlocks = new HashMap<>();
    // Plants
    private TreeMap<Double, GreenhousePlant> plantTree = new TreeMap<>();

    // Mobs
    // Entity Type, Material to Spawn on, Probability
    private TreeMap<Double, GreenhouseMob> mobTree = new TreeMap<>();

    // Conversions
    // Original Material, Original Type, New Material, New Type, Probability
    private Map<Material, GreenhouseBlockConversions> conversionBlocks = new HashMap<>();

    private int mobLimit;
    private int waterCoverage;
    private int iceCoverage;
    private int lavaCoverage;

    private String permission = "";
    private Random random = new Random();
    private Map<Material, Integer> missingBlocks;

    /**
     * @param type
     * @param priority
     */
    public BiomeRecipe(Greenhouses addon, Biome type, int priority) {
        this.plugin = addon;
        this.type = type;
        this.priority = priority;
        //addon.logger(3,"" + type.toString() + " priority " + priority);
        mobLimit = 9; // Default
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
        //plugin.logger(1,"   " + convChance + "% chance for " + Util.prettifyText(oldMaterial.toString()) + " to convert to " + Util.prettifyText(newMaterial.toString()));
    }


    /**
     * @param mobType
     * @param mobProbability
     * @param mobSpawnOn
     */
    public void addMobs(EntityType mobType, int mobProbability, Material mobSpawnOn) {
        //plugin.logger(1,"   " + mobProbability + "% chance for " + Util.prettifyText(mobType.toString()) + " to spawn on " + Util.prettifyText(mobSpawnOn.toString())+ ".");
        double probability = ((double)mobProbability/100);
        double lastProb = mobTree.isEmpty() ? 0D : mobTree.lastKey();
        // Add up all the probabilities in the list so far
        if ((1D - lastProb) >= probability) {
            // Add to probability tree
            mobTree.put(lastProb + probability, new GreenhouseMob(mobType, mobSpawnOn));
        } else {
            plugin.logError("Mob chances add up to > 100% in " + type.toString() + " biome recipe! Skipping " + mobType.toString());
        }
    }

    /**
     * Creates a list of plants that can grow, the probability and what they must grow on.
     * Data is drawn from the file biomes.yml
     * @param plantMaterial - plant type
     * @param plantProbability - probability of growing
     * @param plantGrowOn - material on which it must grow
     */
    public void addPlants(Material plantMaterial, int plantProbability, Material plantGrowOn) {
        double probability = ((double)plantProbability/100);
        // Add up all the probabilities in the list so far
        double lastProb = plantTree.isEmpty() ? 0D : plantTree.lastKey();
        if ((1D - lastProb) >= probability) {
            // Add to probability tree
            plantTree.put(lastProb + probability, new GreenhousePlant(plantMaterial, plantGrowOn));
        } else {
            plugin.logError("Plant chances add up to > 100% in " + type.toString() + " biome recipe! Skipping " + plantMaterial.toString());
        }
        //plugin.logger(1,"   " + plantProbability + "% chance for " + Util.prettifyText(plantMaterial.toString()) + " to grow on " + Util.prettifyText(plantGrowOn.toString()));
    }

    /**
     * @param blockMaterial
     * @param blockQty
     */
    public void addReqBlocks(Material blockMaterial, int blockQty) {
        requiredBlocks.put(blockMaterial, blockQty);
        //plugin.logger(1,"   " + blockMaterial + " x " + blockQty);
    }

    // Check required blocks
    /**
     * Checks greenhouse meets recipe requirements. If player is not null, a explanation of
     * any failures will be provided.
     * @return true if meet this biome recipe.
     */
    public Set<GreenhouseResult> checkRecipe(Greenhouse gh) {
        Set<GreenhouseResult> result = new HashSet<>();
        long area = gh.getArea();
        Map<Material, Integer> blockCount = new HashMap<>();
        // Look through the greenhouse and count what is in there
        for (int y = gh.getFloorHeight(); y< gh.getCeilingHeight();y++) {
            for (int x = (int) (gh.getFootprint().getMinX()+1); x < gh.getFootprint().getMaxX(); x++) {
                for (int z = (int) (gh.getFootprint().getMinY()+1); z < gh.getFootprint().getMaxY(); z++) {
                    Block b = gh.getWorld().getBlockAt(x, y, z);
                    if (!b.getType().equals(Material.AIR)) {
                        blockCount.putIfAbsent(b.getType(), 0);
                        blockCount.merge(b.getType(), 1, Integer::sum);
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
        //plugin.logger(3,"water req=" + waterCoverage + " lava req=" + lavaCoverage + " ice req="+iceCoverage);
        //plugin.logger(3,"waterRatio=" + waterRatio + " lavaRatio=" + lavaRatio + " iceRatio="+iceRatio);


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
        missingBlocks = requiredBlocks.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() - blockCount.getOrDefault(e.getKey(), 0)));
        // Remove any entries that are 0 or less
        missingBlocks.values().removeIf(v -> v <= 0);
        return result;
    }

    /**
     * @param b
     */
    public void convertBlock(Block b) {
        //plugin.logger(3,"try to convert block");
        GreenhouseBlockConversions bc = conversionBlocks.get(b.getType());
        if (bc == null || random.nextDouble() > bc.getProbability()) {
            return;
        }
        // Check if the block is in the right area, up, down, n,s,e,w
        if (ADJ_BLOCKS.stream().map(b::getRelative).map(Block::getType).anyMatch(m -> bc.getLocalMaterial() == null || m == bc.getLocalMaterial())) {
            // Convert!
            //plugin.logger(3,"Convert block");
            b.setType(bc.getNewMaterial());
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
     * @return a mob that can spawn in the greenhouse
     */
    public Optional<GreenhouseMob> getRandomMob() {
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
        if (bl.getType() != Material.AIR) {
            return false;
        }
        return getRandomPlant().map(p -> {
            if (bl.getY() != 0 && p.getPlantGrownOn().map(m -> m.equals(bl.getRelative(BlockFace.DOWN).getType())).orElse(true)) {
                bl.setType(p.getPlantMaterial());
                return true;
            }
            return false;
        }).orElse(false);
    }

    /**
     * @param set the friendly name
     */
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }
    /**
     * @param icecoverage the icecoverage to set
     */
    public void setIcecoverage(int icecoverage) {
        if (icecoverage == 0) {
            //plugin.logger(1,"   No Ice Allowed");
        } else if (icecoverage > 0) {
            //plugin.logger(1,"   Ice > " + icecoverage + "%");
        }
        this.iceCoverage = icecoverage;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(Material icon) {
        this.icon = icon;
    }

    /**
     * @param lavaCoverage the lavaCoverage to set
     */
    public void setLavacoverage(int lavacoverage) {
        if (lavacoverage == 0) {
            //plugin.logger(1,"   No Lava Allowed");
        } else if (lavacoverage > 0) {
            //plugin.logger(1,"   Lava > " + lavacoverage + "%");
        }
        this.lavaCoverage = lavacoverage;
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
     * @param waterCoverage the waterCoverage to set
     */
    public void setWatercoverage(int watercoverage) {
        if (watercoverage == 0) {
            //plugin.logger(1,"   No Water Allowed");
        } else if (watercoverage > 0) {
            //plugin.logger(1,"   Water > " + watercoverage + "%");
        }
        this.waterCoverage = watercoverage;
    }

    /**
     * @return the missingBlocks
     */
    public Map<Material, Integer> getMissingBlocks() {
        return missingBlocks;
    }

    @Override
    public int compareTo(BiomeRecipe o) {
        return Integer.compare(o.getPriority(), this.getPriority());
    }

}
