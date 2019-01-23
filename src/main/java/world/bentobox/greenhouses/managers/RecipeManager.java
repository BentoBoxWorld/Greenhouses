package world.bentobox.greenhouses.managers;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private Greenhouses addon;
    private static List<BiomeRecipe> biomeRecipes = new ArrayList<>();

    public RecipeManager(Greenhouses addon) {
        this.addon = addon;
        try {
            loadBiomeRecipes();
        } catch (Exception e) {
            addon.logError(e.getMessage());
            // TODO Auto-generated catch block
            e.printStackTrace();
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
     * @throws InvalidConfigurationException
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void loadBiomeRecipes() throws FileNotFoundException, IOException, InvalidConfigurationException {
        biomeRecipes.clear();
        YamlConfiguration biomes = new YamlConfiguration();
        File biomeFile = new File(addon.getDataFolder(), "biomes.yml");
        if (!biomeFile.exists()) {
            addon.logError("No biomes.yml file!");
            addon.saveResource("biomes.yml", true);
        }
        biomes.load(biomeFile);
        ConfigurationSection biomeSection = biomes.getConfigurationSection("biomes");
        if (biomeSection == null) {
            addon.logError("biomes.yml file is missing, empty or corrupted. Delete and reload plugin again!");
            return;
        }

        // Loop through all the entries
        for (String type: biomeSection.getValues(false).keySet()) {
            try {
                ConfigurationSection biomeRecipe = biomeSection.getConfigurationSection(type);
                Biome thisBiome = null;
                if (biomeRecipe.contains("biome")) {
                    // Try and get the biome via the biome setting
                    thisBiome = Biome.valueOf(biomeRecipe.getString("biome").toUpperCase());
                } else {
                    // Old style, where type was the biome name
                    thisBiome = Biome.valueOf(type);
                }
                if (thisBiome != null) {
                    int priority = biomeRecipe.getInt("priority", 0);
                    BiomeRecipe b = new BiomeRecipe(addon, thisBiome,priority);
                    // Set the name
                    b.setName(type);
                    // Set the permission
                    b.setPermission(biomeRecipe.getString("permission",""));
                    // Set the icon
                    b.setIcon(Material.valueOf(biomeRecipe.getString("icon", "SAPLING")));
                    b.setFriendlyName(ChatColor.translateAlternateColorCodes('&', biomeRecipe.getString("friendlyname", Util.prettifyText(type))));
                    // A value of zero on these means that there must be NO coverage, e.g., desert. If the value is not present, then the default is -1
                    b.setWatercoverage(biomeRecipe.getInt("watercoverage",-1));
                    b.setLavacoverage(biomeRecipe.getInt("lavacoverage",-1));
                    b.setIcecoverage(biomeRecipe.getInt("icecoverage",-1));
                    b.setMobLimit(biomeRecipe.getInt("moblimit", 9));
                    // Set the needed blocks
                    ConfigurationSection reqContents = biomeRecipe.getConfigurationSection("contents");
                    if (reqContents != null) {
                        for (String rq : reqContents.getKeys(false)) {
                            Material mat = Material.valueOf(rq.toUpperCase());
                            if (mat != null) {
                                b.addReqBlocks(mat, reqContents.getInt(rq));
                            } else {
                                addon.logError("Could not parse required block " + rq);
                            }
                        }
                    }
                    // Load plants
                    // # Plant Material: Probability in %:Block Material on what they grow
                    ConfigurationSection temp = biomes.getConfigurationSection("biomes." + type + ".plants");
                    if (temp != null) {
                        HashMap<String,Object> plants = (HashMap<String,Object>)temp.getValues(false);
                        if (plants != null) {
                            for (String s: plants.keySet()) {
                                //logger(1, "Plant = " + s);
                                Material plantMaterial = Material.valueOf(s);

                                //logger(1, "Plant = " + plantMaterial);
                                String[] split = ((String)plants.get(s)).split(":");
                                //logger(1, "Split length = " + split.length);
                                int plantProbability = Integer.valueOf(split[0]);
                                Material plantGrowOn = Material.valueOf(split[1]);

                                b.addPlants(plantMaterial, plantProbability, plantGrowOn);
                            }
                        }
                    }
                    // Load mobs!
                    // Mob EntityType: Probability:Spawn on Material
                    temp = biomes.getConfigurationSection("biomes." + type + ".mobs");
                    if (temp != null) {
                        HashMap<String,Object> mobs = (HashMap<String,Object>)temp.getValues(false);
                        if (mobs != null) {
                            for (String s: mobs.keySet()) {
                                EntityType mobType = EntityType.valueOf(s);
                                String[] split = ((String)mobs.get(s)).split(":");
                                int mobProbability = Integer.valueOf(split[0]);
                                Material mobSpawnOn = Material.valueOf(split[1]);
                                b.addMobs(mobType, mobProbability, mobSpawnOn);
                            }
                        }
                    }
                    // Load block conversions
                    ConfigurationSection conversionSec = biomeSection.getConfigurationSection(type + ".conversions");
                    if (conversionSec != null) {
                        for (String oldMat : conversionSec.getKeys(false)) {
                            Material oldMaterial = Material.valueOf(oldMat);
                            if (oldMaterial == null) {
                                addon.logError("Could not parse " + oldMat);
                                break;
                            }
                            String conversions = conversionSec.getString(oldMat);

                            //logger(3,"conversions = '" + conversions + "'");
                            if (!conversions.isEmpty()) {
                                String[] split = conversions.split(":");
                                int convChance = Integer.valueOf(split[0]);
                                Material newMaterial = Material.valueOf(split[1]);
                                Material localMaterial = Material.valueOf(split[2]);
                                b.addConvBlocks(oldMaterial, newMaterial, convChance, localMaterial);
                            }
                        }
                    }


                    // Add the recipe to the list
                    biomeRecipes.add(b);
                }
            } catch (Exception e) {
                //logger(1,"Problem loading biome recipe - skipping!");
                String validBiomes = "";
                for (Biome biome : Biome.values()) {
                    validBiomes = validBiomes + " " + biome.name();
                }
                //logger(1,"Valid biomes are " + validBiomes);
                e.printStackTrace();
            }

            // Check maximum number
            if (biomeRecipes.size() == MAXIMUM_INVENTORY_SIZE) {
                addon.logWarning("Cannot load any more biome recipies - limit is 49!");
                break;
            }

        }
        addon.log("Loaded " + biomeRecipes.size() + " biome recipes.");
    }

    /**
     * @return the biomeRecipes
     */
    public List<BiomeRecipe> getBiomeRecipes() {
        return biomeRecipes;
    }


}
