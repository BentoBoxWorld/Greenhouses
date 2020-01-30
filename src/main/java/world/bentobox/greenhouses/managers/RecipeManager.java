package world.bentobox.greenhouses.managers;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;

public class RecipeManager {

    private static final int MAXIMUM_INVENTORY_SIZE = 49;
    private final Greenhouses addon;
    private static final List<BiomeRecipe> biomeRecipes = new ArrayList<>();

    public RecipeManager(Greenhouses addon) {
        this.addon = addon;
        try {
            loadBiomeRecipes();
        } catch (Exception e) {
            addon.logError(e.getMessage());
        }
    }

    /**
     * Get BiomeRecipe by name
     * @param name - name
     * @return Optional BiomeRecipe found
     */
    public static Optional<BiomeRecipe> getBiomeRecipies(String name) {
        return biomeRecipes.stream().filter(r -> r.getName().equals(name)).findFirst();
    }

    /**
     * Loads all the biome recipes from the file biomes.yml.
     * @throws InvalidConfigurationException - bad YAML
     * @throws IOException - io exception
     */
    private void loadBiomeRecipes() throws IOException, InvalidConfigurationException {
        biomeRecipes.clear();
        YamlConfiguration biomeConfig = new YamlConfiguration();
        File biomeFile = new File(addon.getDataFolder(), "biomes.yml");
        if (!biomeFile.exists()) {
            addon.logError("No biomes.yml file!");
            addon.saveResource("biomes.yml", true);
        }
        biomeConfig.load(biomeFile);
        if (!biomeConfig.isConfigurationSection("biomes")) {
            addon.logError("biomes.yml file is missing, empty or corrupted. Delete and reload plugin again!");
            return;
        }
        ConfigurationSection biomeSection = biomeConfig.getConfigurationSection("biomes");
        // Loop through all the entries
        for (String type: biomeSection.getValues(false).keySet()) {
            processEntries(type, biomeSection);
            // Check maximum number
            if (biomeRecipes.size() == MAXIMUM_INVENTORY_SIZE) {
                addon.logWarning("Cannot load any more biome recipies - limit is " + MAXIMUM_INVENTORY_SIZE);
                break;
            }
        }
        addon.log("Loaded " + biomeRecipes.size() + " biome recipes.");
    }

    private void processEntries(String biomeType, ConfigurationSection biomeSection) {
        try {
            ConfigurationSection biomeRecipeConfig = biomeSection.getConfigurationSection(biomeType);
            Biome thisBiome;
            if (biomeRecipeConfig.contains("biome")) {
                // Try and get the biome via the biome setting
                thisBiome = Biome.valueOf(biomeRecipeConfig.getString("biome").toUpperCase());
            } else {
                // Old style, where type was the biome name
                thisBiome = Biome.valueOf(biomeType);
            }
            int priority = biomeRecipeConfig.getInt("priority", 0);

            // Create the biome recipe
            BiomeRecipe biomeRecipe = getBiomeRecipe(biomeRecipeConfig, biomeType, thisBiome, priority);

            // Set the needed blocks
            ConfigurationSection reqContents = biomeRecipeConfig.getConfigurationSection("contents");
            if (reqContents != null) {
                for (String rq : reqContents.getKeys(false)) {
                    parseReqBlock(biomeRecipe, rq, reqContents);
                }
            }

            // Load plants
            loadPlants(biomeRecipeConfig, biomeRecipe);

            // Load mobs!
            loadMobs(biomeRecipeConfig, biomeRecipe);

            // Load block conversions
            loadBlockConversions(biomeRecipeConfig, biomeRecipe);

            // Add the recipe to the list
            biomeRecipes.add(biomeRecipe);
        } catch (Exception e) {
            addon.logError("Problem loading biome recipe - skipping! " + e.getMessage());
            StringBuilder validBiomes = new StringBuilder();
            for (Biome biome : Biome.values()) {
                validBiomes.append(" ").append(biome.name());
            }
            addon.logError("Valid biomes are " + validBiomes);
        }

    }

    private BiomeRecipe getBiomeRecipe(ConfigurationSection biomeRecipeConfig, String biomeType, Biome thisBiome, int priority) {
        BiomeRecipe b = new BiomeRecipe(addon, thisBiome, priority);
        // Set the name
        b.setName(biomeType);
        if (addon.getSettings().isStartupLog()) addon.log("Adding biome recipe for " + biomeType);
        // Set the permission
        b.setPermission(biomeRecipeConfig.getString("permission",""));
        // Set the icon
        b.setIcon(Material.valueOf(biomeRecipeConfig.getString("icon", "SAPLING")));
        b.setFriendlyName(ChatColor.translateAlternateColorCodes('&', biomeRecipeConfig.getString("friendlyname", Util.prettifyText(biomeType))));
        // A value of zero on these means that there must be NO coverage, e.g., desert. If the value is not present, then the default is -1
        b.setWatercoverage(biomeRecipeConfig.getInt("watercoverage",-1));
        b.setLavacoverage(biomeRecipeConfig.getInt("lavacoverage",-1));
        b.setIcecoverage(biomeRecipeConfig.getInt("icecoverage",-1));
        b.setMobLimit(biomeRecipeConfig.getInt("moblimit", 9));
        return b;
    }

    private void loadPlants(ConfigurationSection biomeRecipeConfig, BiomeRecipe b) {
        ConfigurationSection temp = biomeRecipeConfig.getConfigurationSection("plants");
        // # Plant Material: Probability in %:Block Material on what they grow
        if (temp != null) {
            HashMap<String,Object> plants = (HashMap<String,Object>)temp.getValues(false);
            for (Entry<String, Object> s: plants.entrySet()) {
                Material plantMaterial = Material.valueOf(s.getKey());
                String[] split = ((String)s.getValue()).split(":");
                int plantProbability = Integer.parseInt(split[0]);
                Material plantGrowOn = Material.valueOf(split[1]);
                b.addPlants(plantMaterial, plantProbability, plantGrowOn);
            }
        }

    }

    private void loadBlockConversions(ConfigurationSection biomeRecipeConfig, BiomeRecipe b) {
        ConfigurationSection conversionSec = biomeRecipeConfig.getConfigurationSection("conversions");
        if (conversionSec != null) {
            for (String oldMat : conversionSec.getKeys(false)) {
                try {
                    Material oldMaterial = Material.valueOf(oldMat.toUpperCase());
                    String conversions = conversionSec.getString(oldMat);
                    if (!conversions.isEmpty()) {
                        String[] split = conversions.split(":");
                        int convChance = Integer.parseInt(split[0]);
                        Material newMaterial = Material.valueOf(split[1]);
                        Material localMaterial = Material.valueOf(split[2]);
                        b.addConvBlocks(oldMaterial, newMaterial, convChance, localMaterial);
                    }
                } catch (Exception e) {
                    addon.logError("Could not parse " + oldMat);
                }

            }
        }
    }

    private void loadMobs(ConfigurationSection biomeRecipeConfig, BiomeRecipe b) {
        ConfigurationSection temp = biomeRecipeConfig.getConfigurationSection("mobs");
        // Mob EntityType: Probability:Spawn on Material
        if (temp != null) {
            ((HashMap<String,Object>)temp.getValues(false)).entrySet().forEach(s -> parseMob(s,b));
        }

    }

    private void parseMob(Entry<String, Object> s, BiomeRecipe b) {
        try {
            EntityType mobType = EntityType.valueOf(s.getKey().toUpperCase());
            String[] split = ((String)s.getValue()).split(":");
            int mobProbability = Integer.parseInt(split[0]);
            Material mobSpawnOn = Material.valueOf(split[1]);
            b.addMobs(mobType, mobProbability, mobSpawnOn);
        } catch (Exception e) {
            addon.logError("Could not parse " + s.getKey());
        }
    }

    private void parseReqBlock(BiomeRecipe b, String rq, ConfigurationSection reqContents) {
        try {
            b.addReqBlocks(Material.valueOf(rq.toUpperCase()), reqContents.getInt(rq));
        } catch(Exception e) {
            addon.logError("Could not parse required block " + rq);
        }
    }

    /**
     * @return the biomeRecipes
     */
    public List<BiomeRecipe> getBiomeRecipes() {
        return biomeRecipes;
    }


}
