package world.bentobox.greenhouses.greenhouse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.ui.Locale;

public class BiomeRecipe {
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

    /**
     * @param type
     * @param priority
     */
    public BiomeRecipe(Greenhouses plugin, Biome type, int priority) {
        this.plugin = plugin;
        this.type = type;
        this.priority = priority;
        plugin.logger(3,"" + type.toString() + " priority " + priority);
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
        plugin.logger(1,"   " + convChance + "% chance for " + Util.prettifyText(oldMaterial.toString()) + " to convert to " + Util.prettifyText(newMaterial.toString()));
    }


    /**
     * @param mobType
     * @param mobProbability
     * @param mobSpawnOn
     */
    public void addMobs(EntityType mobType, int mobProbability, Material mobSpawnOn) {
        plugin.logger(1,"   " + mobProbability + "% chance for " + Util.prettifyText(mobType.toString()) + " to spawn on " + Util.prettifyText(mobSpawnOn.toString())+ ".");
        double probability = ((double)mobProbability/100);
        // Add up all the probabilities in the list so far
        if ((1D - mobTree.lastKey()) >= probability) {
            // Add to probability tree
            mobTree.put(mobTree.lastKey() + probability, new GreenhouseMob(mobType, mobSpawnOn));
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
        if ((1D - plantTree.lastKey()) >= probability) {
            // Add to probability tree
            plantTree.put(plantTree.lastKey() + probability, new GreenhousePlant(plantMaterial, plantGrowOn));
        } else {
            plugin.logError("Plant chances add up to > 100% in " + type.toString() + " biome recipe! Skipping " + plantMaterial.toString());
        }
        plugin.logger(1,"   " + plantProbability + "% chance for " + Util.prettifyText(plantMaterial.toString()) + " to grow on " + Util.prettifyText(plantGrowOn.toString()));
    }

    /**
     * @param blockMaterial
     * @param blockQty
     */
    public void addReqBlocks(Material blockMaterial, int blockQty) {
        requiredBlocks.put(blockMaterial, blockQty);
        plugin.logger(1,"   " + blockMaterial + " x " + blockQty);
    }

    // Check required blocks
    /**
     * Checks greenhouse meets recipe requirements. If player is not null, a explanation of
     * any failures will be provided.
     * @param pos1
     * @param pos2
     * @param player
     * @return true if a cube defined by pos1 and pos2 meet this biome recipe.
     */
    public boolean checkRecipe(Location pos1, Location pos2, Player player) {
        plugin.logger(3,"Checking for biome " + type.toString());
        long area = (pos2.getBlockX()-pos1.getBlockX()-1) * (pos2.getBlockZ()-pos1.getBlockZ()-1);
        plugin.logger(3,"area =" + area);
        plugin.logger(3,"Pos1 = " + pos1.toString());
        plugin.logger(3,"Pos1 = " + pos2.toString());
        boolean pass = true;
        Map<Material, Integer> blockCount = new HashMap<>();
        // Look through the greenhouse and count what is in there
        for (int y = pos1.getBlockY(); y<pos2.getBlockY();y++) {
            for (int x = pos1.getBlockX()+1;x<pos2.getBlockX();x++) {
                for (int z = pos1.getBlockZ()+1;z<pos2.getBlockZ();z++) {
                    Block b = pos1.getWorld().getBlockAt(x, y, z);
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
        plugin.logger(3,"water req=" + waterCoverage + " lava req=" + lavaCoverage + " ice req="+iceCoverage);
        plugin.logger(3,"waterRatio=" + waterRatio + " lavaRatio=" + lavaRatio + " iceRatio="+iceRatio);


        // Check required ratios - a zero means none of these are allowed, e.g.desert has no water
        if (waterCoverage == 0 && waterRatio > 0) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipenowater);
            }
            pass=false;
        }
        if (lavaCoverage == 0 && lavaRatio > 0) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipenolava);
            }
            pass=false;
        }
        if (iceCoverage == 0 && iceRatio > 0) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipenoice);
            }
            pass=false;
        }
        if (waterCoverage > 0 && waterRatio < waterCoverage) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipewatermustbe.replace("[coverage]", String.valueOf(waterCoverage)));
            }
            pass=false;
        }
        if (lavaCoverage > 0 && lavaRatio < lavaCoverage) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipelavamustbe.replace("[coverage]", String.valueOf(lavaCoverage)));
            }
            pass=false;

        }
        if (iceCoverage > 0 && iceRatio < iceCoverage) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipeicemustbe.replace("[coverage]", String.valueOf(iceCoverage)));
            }
            pass=false;
        }
        // Compare to the required blocks
        Map<Material, Integer> missingBlocks = requiredBlocks.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() - blockCount.getOrDefault(e.getKey(), 0)));
        // Remove any entries that are 0 or less
        missingBlocks.values().removeIf(v -> v <= 0);
        if (!missingBlocks.isEmpty()) {
            pass = false;
        }
        missingBlocks.forEach((k,v) -> player.sendMessage(ChatColor.RED + Locale.recipemissing + " " + Util.prettifyText(k.toString()) + " x " + v));
        return pass;
    }

    /**
     * @param b
     */
    public void convertBlock(Block b) {
        plugin.logger(3,"try to convert block");
        GreenhouseBlockConversions bc = conversionBlocks.get(b.getType());
        if (bc == null || random.nextDouble() > bc.getProbability()) {
            return;
        }
        // Check if the block is in the right area, up, down, n,s,e,w
        if (ADJ_BLOCKS.stream().map(b::getRelative).map(Block::getType).anyMatch(m -> bc.getLocalMaterial() == null || m == bc.getLocalMaterial())) {
            // Convert!
            plugin.logger(3,"Convert block");
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
        Double key = plantTree.ceilingKey(random.nextDouble());
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
     * @param bl
     * @return
     */
    public void growPlant(Block bl) {
        if (bl.getType() != Material.AIR) {
            return;
        }
        getRandomPlant().ifPresent(p -> {
            if (bl.getY() != 0 && p.getPlantGrownOn().map(m -> m.equals(bl.getRelative(BlockFace.DOWN).getType())).orElse(true)) {
                bl.setType(p.getPlantMaterial());
            }
        });
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
            plugin.logger(1,"   No Ice Allowed");
        } else if (icecoverage > 0) {
            plugin.logger(1,"   Ice > " + icecoverage + "%");
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
            plugin.logger(1,"   No Lava Allowed");
        } else if (lavacoverage > 0) {
            plugin.logger(1,"   Lava > " + lavacoverage + "%");
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
            plugin.logger(1,"   No Water Allowed");
        } else if (watercoverage > 0) {
            plugin.logger(1,"   Water > " + watercoverage + "%");
        }
        this.waterCoverage = watercoverage;
    }

}
