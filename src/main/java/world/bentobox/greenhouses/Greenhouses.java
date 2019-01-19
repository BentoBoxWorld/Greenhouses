package world.bentobox.greenhouses;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.Ecosystem;
import world.bentobox.greenhouses.greenhouse.Greenhouse;
import world.bentobox.greenhouses.greenhouse.Roof;
import world.bentobox.greenhouses.greenhouse.Walls;
import world.bentobox.greenhouses.listeners.GreenhouseEvents;
import world.bentobox.greenhouses.listeners.GreenhouseGuard;
import world.bentobox.greenhouses.listeners.JoinLeaveEvents;
import world.bentobox.greenhouses.ui.ControlPanel;
import world.bentobox.greenhouses.ui.GreenhouseCmd;
import world.bentobox.greenhouses.ui.Locale;
import world.bentobox.greenhouses.ui.admin.AdminCmd;
import world.bentobox.greenhouses.util.MetricsLite;
import world.bentobox.greenhouses.util.Util;
import world.bentobox.greenhouses.util.VaultHelper;

/**
 * This plugin simulates greenhouses in Minecraft. It enables players to build biomes inside
 * glass houses. Each biome is different and can spawn plants and animals. The recipe for each
 * biome is determined by a configuration file.b
 * @author tastybento
 */
public class Greenhouses extends Addon {
    // Maximum size that the Minecraft inventory can be in items before going weird
    private static final int MAXIMUM_INVENTORY_SIZE = 49;

    // Players object
    public PlayerCache players;

    // Greenhouses
    private HashSet<Greenhouse> greenhouses = new HashSet<Greenhouse>();
    private HashMap<UUID, HashSet<Greenhouse>> playerhouses = new HashMap<UUID, HashSet<Greenhouse>>();

    // Offline Messages
    private HashMap<UUID, List<String>> messages = new HashMap<UUID, List<String>>();
    private YamlConfiguration messageStore;

    // Ecosystem object and random number generator
    private Ecosystem eco = new Ecosystem(this);
    // Tasks
    private BukkitTask plantTask;
    private BukkitTask mobTask;
    private BukkitTask blockTask;
    private BukkitTask ecoTask;

    // Biomes
    private List<BiomeRecipe> biomeRecipes = new ArrayList<BiomeRecipe>();
    private ControlPanel biomeInv;
    // Debug level (0 = none, 1 = important ones, 2 = level 2, 3 = level 3
    private List<String> debug = new ArrayList<String>();

    /**
     * Loads all the biome recipes from the file biomes.yml.
     */
    public void loadBiomeRecipes() {
        biomeRecipes.clear();
        YamlConfiguration biomes = loadYamlFile("biomes.yml");
        ConfigurationSection biomeSection = biomes.getConfigurationSection("biomes");
        if (biomeSection == null) {
            getLogger().severe("biomes.yml file is missing, empty or corrupted. Delete and reload plugin again!");
            return;
        }

        // Loop through all the entries
        for (String type: biomeSection.getValues(false).keySet()) {
            logger(1,"Loading "+type + " biome recipe:");
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
                    BiomeRecipe b = new BiomeRecipe(this, thisBiome,priority);
                    // Set the name
                    b.setName(type);
                    // Set the permission
                    b.setPermission(biomeRecipe.getString("permission",""));
                    // Set the icon
                    b.setIcon(Material.valueOf(biomeRecipe.getString("icon", "SAPLING")));
                    b.setFriendlyName(ChatColor.translateAlternateColorCodes('&', biomeRecipe.getString("friendlyname", "")));
                    // A value of zero on these means that there must be NO coverage, e.g., desert. If the value is not present, then the default is -1
                    b.setWatercoverage(biomeRecipe.getInt("watercoverage",-1));
                    b.setLavacoverage(biomeRecipe.getInt("lavacoverage",-1));
                    b.setIcecoverage(biomeRecipe.getInt("icecoverage",-1));
                    b.setMobLimit(biomeRecipe.getInt("moblimit", 9));
                    // Set the needed blocks
                    String contents = biomeRecipe.getString("contents", "");
                    logger(3,"contents = '" + contents + "'");
                    if (!contents.isEmpty()) {
                        String[] split = contents.split(" ");
                        // Format is MATERIAL: Qty or MATERIAL: Type:Quantity
                        for (String s : split) {
                            // Split it again
                            String[] subSplit = s.split(":");
                            if (subSplit.length > 1) {
                                Material blockMaterial = Material.valueOf(subSplit[0]);
                                // TODO: Need to parse these inputs better. INTS and Strings
                                //logger(3,"subsplit = " + subSplit);
                                //logger(3,"subsplit length = " + subSplit.length);
                                int blockType = 0;
                                int blockQty = 0;
                                if (subSplit.length == 2) {
                                    blockQty = Integer.valueOf(subSplit[1]);
                                    blockType = -1; // anything okay
                                } else if (subSplit.length == 3) {
                                    //logger(3,"subsplit[1] = " + subSplit[1]);
                                    //logger(3,"subsplit[2] = " + subSplit[2]);
                                    //logger(3,"subsplit[1] value = " + Integer.valueOf(subSplit[1]));
                                    //logger(3,"subsplit[2] value = " + Integer.valueOf(subSplit[2]));
                                    blockType = Integer.valueOf(subSplit[1]);
                                    blockQty = Integer.valueOf(subSplit[2]);
                                }
                                b.addReqBlocks(blockMaterial, blockType, blockQty);
                            } else {
                                getLogger().warning("Block material " + s + " has no associated qty in biomes.yml " + type);
                            }
                        }
                    }

                    // Load plants
                    // # Plant Material: Probability in %:Block Material on what they grow:Plant Type(optional):Block Type(Optional)
                    ConfigurationSection temp = biomes.getConfigurationSection("biomes." + type + ".plants");
                    if (temp != null) {
                        HashMap<String,Object> plants = (HashMap<String,Object>)temp.getValues(false);
                        if (plants != null) {
                            for (String s: plants.keySet()) {
                                //logger(1, "Plant = " + s);
                                Material plantMaterial = null;
                                int plantType = 0;
                                if (s.contains(":")) {
                                    String[] split = s.split(":");
                                    if (split.length == 2) {
                                        plantMaterial = Material.valueOf(split[0]);
                                        plantType = Integer.valueOf(split[1]);
                                    }
                                } else {
                                    plantMaterial = Material.valueOf(s);
                                }
                                //logger(1, "Plant = " + plantMaterial);
                                String[] split = ((String)plants.get(s)).split(":");
                                //logger(1, "Split length = " + split.length);
                                int plantProbability = Integer.valueOf(split[0]);
                                Material plantGrowOn = Material.valueOf(split[1]);
                                if (split.length == 3) {
                                    //logger(1, "Split legth is ==3");
                                    plantType = Integer.valueOf(split[2]);
                                    //logger(1, "plant type = " + plantType);
                                }
                                b.addPlants(plantMaterial, plantType, plantProbability, plantGrowOn);
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
                                // TODO: Currently not used
                                int mobSpawnOnType = 0;
                                if (split.length == 3) {
                                    mobSpawnOnType = Integer.valueOf(split[2]);
                                }
                                b.addMobs(mobType, mobProbability, mobSpawnOn);
                            }
                        }
                    }
                    // Load block conversions
                    String conversions = biomeSection.getString(type + ".conversions", "");
                    logger(3,"conversions = '" + conversions + "'");
                    if (!conversions.isEmpty()) {
                        String[] split = conversions.split(" ");
                        for (String s : split) {
                            // Split it again
                            String[] subSplit = s.split(":");
                            // After this is split, there must be 5 entries!
                            Material oldMaterial = null;
                            int oldType = 0;
                            Material newMaterial = null;
                            int newType = 0;
                            Material localMaterial = null;
                            int localType = 0;
                            int convChance;
                            oldMaterial = Material.valueOf(subSplit[0]);
                            oldType = Integer.valueOf(subSplit[1]);
                            convChance = Integer.valueOf(subSplit[2]);
                            newMaterial = Material.valueOf(subSplit[3]);
                            newType = Integer.valueOf(subSplit[4]);
                            if (subSplit.length == 7) {
                                localMaterial = Material.valueOf(subSplit[5]);
                                localType = Integer.valueOf(subSplit[6]);
                            }
                            b.addConvBlocks(oldMaterial, oldType, newMaterial, newType, convChance, localMaterial, localType);
                        }
                    }
                    // Add the recipe to the list
                    biomeRecipes.add(b);
                }
            } catch (Exception e) {
                logger(1,"Problem loading biome recipe - skipping!");
                String validBiomes = "";
                for (Biome biome : Biome.values()) {
                    validBiomes = validBiomes + " " + biome.name();
                }
                logger(1,"Valid biomes are " + validBiomes);
                e.printStackTrace();
            }

            // Check maximum number
            if (biomeRecipes.size() == MAXIMUM_INVENTORY_SIZE) {
                getLogger().warning("Cannot load any more biome recipies - limit is 49!");
                break;
            }

        }
        logger(1,"Loaded " + biomeRecipes.size() + " biome recipes.");
    }


    /**
     * @return the biomeRecipes
     */
    public List<BiomeRecipe> getBiomeRecipes() {
        return biomeRecipes;
    }


    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    public void loadPluginConfig() {
        try {
            getConfig();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        // Get the localization strings
        getLocale();
        Locale.generalnotavailable = ChatColor.translateAlternateColorCodes('&', getLocale().getString("general.notavailable", "Greenhouses are not available in this world"));
        Locale.generalgreenhouses = ChatColor.translateAlternateColorCodes('&', getLocale().getString("general.greenhouses", "Greenhouses"));
        Locale.generalbiome = ChatColor.translateAlternateColorCodes('&', getLocale().getString("general.biome", "Biome"));
        Locale.generalowner = ChatColor.translateAlternateColorCodes('&', getLocale().getString("general.owner", "Owner"));
        Locale.helphelp = ChatColor.translateAlternateColorCodes('&', getLocale().getString("help.help", "help"));
        Locale.helpmake = ChatColor.translateAlternateColorCodes('&', getLocale().getString("help.make", "Tries to make a greenhouse"));
        Locale.helpremove = ChatColor.translateAlternateColorCodes('&', getLocale().getString("help.remove", "Removes a greenhouse that you are standing in if you are the owner"));
        Locale.helpinfo = ChatColor.translateAlternateColorCodes('&', getLocale().getString("help.info", "Shows info on the greenhouse you and general info"));
        Locale.helplist = ChatColor.translateAlternateColorCodes('&', getLocale().getString("help.list", "Lists all the greenhouse biomes that can be made"));
        Locale.helpopengui = ChatColor.translateAlternateColorCodes('&', getLocale().getString("help.opengui", "Opens the Greenhouse GUI"));
        Locale.helprecipe = ChatColor.translateAlternateColorCodes('&', getLocale().getString("help.recipe", "Tells you how to make greenhouse biome"));
        Locale.listtitle = ChatColor.translateAlternateColorCodes('&', getLocale().getString("list.title", "[Greenhouse Biome Recipes]"));
        Locale.listinfo = ChatColor.translateAlternateColorCodes('&', getLocale().getString("list.info", "Use /greenhouse recipe <number> to see details on how to make each greenhouse"));
        Locale.errorunknownPlayer = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.unknownPlayer", "That player is unknown."));
        Locale.errornoPermission = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.noPermission", "You don't have permission to use that command!"));
        Locale.errorcommandNotReady = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.commandNotReady", "You can't use that command right now."));
        Locale.errorofflinePlayer = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.offlinePlayer", "That player is offline or doesn't exist."));
        Locale.errorunknownCommand = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.unknownCommand", "Unknown command."));
        Locale.errormove = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.move", "Move to a greenhouse you own first."));
        Locale.errornotowner = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.notowner", "You must be the owner of this greenhouse to do that."));
        Locale.errorremoving = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.removing", "Removing greenhouse!"));
        Locale.errornotyours = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.notyours", "This is not your greenhouse!"));
        Locale.errornotinside = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.notinside", "You are not in a greenhouse!"));
        Locale.errortooexpensive = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.tooexpensive", "You cannot afford [price]" ));
        Locale.erroralreadyexists = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.alreadyexists", "Greenhouse already exists!"));
        Locale.errornorecipe = ChatColor.translateAlternateColorCodes('&', getLocale().getString("error.norecipe", "This does not meet any greenhouse recipe!"));
        Locale.messagesenter = ChatColor.translateAlternateColorCodes('&', getLocale().getString("messages.enter", "Entering [owner]'s [biome] greenhouse!"));
        Locale.messagesleave = ChatColor.translateAlternateColorCodes('&', getLocale().getString("messages.leave", "Now leaving [owner]'s greenhouse."));
        Locale.messagesyouarein = ChatColor.translateAlternateColorCodes('&', getLocale().getString("messages.youarein", "You are now in [owner]'s [biome] greenhouse!"));
        Locale.messagesremoved = ChatColor.translateAlternateColorCodes('&', getLocale().getString("messages.removed", "This greenhouse is no more..."));
        Locale.messagesremovedmessage = ChatColor.translateAlternateColorCodes('&', getLocale().getString("messages.removedmessage", "A [biome] greenhouse of yours is no more!"));
        Locale.messagesecolost = ChatColor.translateAlternateColorCodes('&', getLocale().getString("messages.ecolost", "Your greenhouse at [location] lost its eco system and was removed."));
        Locale.infotitle = ChatColor.translateAlternateColorCodes('&', getLocale().getString("info.title", "&A[Greenhouse Construction]"));
        Locale.infoinstructions = getLocale().getStringList("info.instructions");
        Locale.infoinfo = ChatColor.translateAlternateColorCodes('&', getLocale().getString("info.info", "[Greenhouse Info]"));
        Locale.infonone = ChatColor.translateAlternateColorCodes('&', getLocale().getString("info.none", "None"));
        Locale.infowelcome = ChatColor.translateAlternateColorCodes('&', getLocale().getString("info.welcome","&BWelcome! Click here for instructions"));
        Locale.infonomore = ChatColor.translateAlternateColorCodes('&', getLocale().getString("info.nomore", "&4You cannot build any more greenhouses!"));
        Locale.infoonemore = ChatColor.translateAlternateColorCodes('&', getLocale().getString("info.onemore","&6You can build one more greenhouse."));
        Locale.infoyoucanbuild = ChatColor.translateAlternateColorCodes('&', getLocale().getString("info.youcanbuild","&AYou can builds up to [number] more greenhouses!"));
        Locale.infounlimited = ChatColor.translateAlternateColorCodes('&', getLocale().getString("info.unlimited","&AYou can build an unlimited number of greenhouses!"));
        Locale.recipehint = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.hint", "Use /greenhouse list to see a list of recipe numbers!"));
        Locale.recipewrongnumber = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.wrongnumber", "Recipe number must be between 1 and [size]"));
        Locale.recipetitle = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.title", "[[biome] recipe]"));
        Locale.recipenowater = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.nowater", "No water allowed."));
        Locale.recipenoice = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.noice", "No ice allowed."));
        Locale.recipenolava = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.nolava", "No lava allowed."));
        Locale.recipewatermustbe = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.watermustbe", "Water > [coverage]% of floor area."));
        Locale.recipeicemustbe = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.icemustbe", "Ice blocks > [coverage]% of floor area."));
        Locale.recipelavamustbe = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.lavamustbe", "Lava > [coverage]% of floor area."));
        Locale.recipeminimumblockstitle = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.minimumblockstitle", "[Minimum blocks required]"));
        Locale.lineColor = ChatColor.translateAlternateColorCodes('&', ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.linecolor", "&f")));
        Locale.recipenootherblocks = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.nootherblocks", "No other blocks required."));
        Locale.recipemissing = ChatColor.translateAlternateColorCodes('&', getLocale().getString("recipe.missing", "Greenhouse is missing"));
        Locale.eventbroke = ChatColor.translateAlternateColorCodes('&', getLocale().getString("event.broke", "You broke this greenhouse! Reverting biome to [biome]!"));
        Locale.eventfix = ChatColor.translateAlternateColorCodes('&', getLocale().getString("event.fix", "Fix the greenhouse and then make it again."));
        Locale.eventcannotplace = ChatColor.translateAlternateColorCodes('&', getLocale().getString("event.cannotplace", "Blocks cannot be placed above a greenhouse!"));
        Locale.eventpistonerror = ChatColor.translateAlternateColorCodes('&', getLocale().getString("event.pistonerror", "Pistons cannot push blocks over a greenhouse!"));
        Locale.createnoroof = ChatColor.translateAlternateColorCodes('&', getLocale().getString("create.noroof", "There seems to be no roof!"));
        Locale.createmissingwall = ChatColor.translateAlternateColorCodes('&', getLocale().getString("create.missingwall", "A wall is missing!"));
        Locale.createnothingabove = ChatColor.translateAlternateColorCodes('&', getLocale().getString("create.nothingabove", "There can be no blocks above the greenhouse!"));
        Locale.createholeinroof = ChatColor.translateAlternateColorCodes('&', getLocale().getString("create.holeinroof", "There is a hole in the roof or it is not flat!"));
        Locale.createholeinwall = ChatColor.translateAlternateColorCodes('&', getLocale().getString("create.holeinwall", "There is a hole in the wall or they are not the same height all the way around!"));
        Locale.createhoppererror = ChatColor.translateAlternateColorCodes('&', getLocale().getString("create.hoppererror", "Only one hopper is allowed in the walls or roof."));
        Locale.createdoorerror = ChatColor.translateAlternateColorCodes('&', getLocale().getString("create.doorerror", "You cannot have more than 4 doors in the greenhouse!"));
        Locale.createsuccess = ChatColor.translateAlternateColorCodes('&', getLocale().getString("create.success", "You successfully made a [biome] biome greenhouse!"));
        Locale.adminHelpreload = ChatColor.translateAlternateColorCodes('&', getLocale().getString("adminHelp.reload", "reload configuration from file."));
        Locale.adminHelpinfo = ChatColor.translateAlternateColorCodes('&', getLocale().getString("adminHelp.info", "provides info on the greenhouse you are in"));
        Locale.reloadconfigReloaded = ChatColor.translateAlternateColorCodes('&', getLocale().getString("reload.configReloaded", "Configuration reloaded from file."));
        Locale.admininfoerror = ChatColor.translateAlternateColorCodes('&', getLocale().getString("admininfo.error", "Greenhouse info only available in-game"));
        Locale.admininfoerror2 = ChatColor.translateAlternateColorCodes('&', getLocale().getString("admininfo.error2", "Put yourself in a greenhouse to see info."));
        Locale.admininfoflags = ChatColor.translateAlternateColorCodes('&', getLocale().getString("admininfo.flags", "[Greenhouse Flags]"));
        Locale.newsheadline = ChatColor.translateAlternateColorCodes('&', getLocale().getString("news.headline", "[Greenhouse News]"));
        Locale.controlpaneltitle = ChatColor.translateAlternateColorCodes('&', getLocale().getString("controlpanel.title", "&AGreenhouses"));
        Locale.limitslimitedto = ChatColor.translateAlternateColorCodes('&', getLocale().getString("limits.limitedto","Permissions limit you to [limit] greenhouses so [number] were removed."));
        Locale.limitsnoneallowed = ChatColor.translateAlternateColorCodes('&', getLocale().getString("limits.noneallowed", "Permissions do not allow you any greenhouses so [number] were removed."));


        // Assign settings
        this.debug = getConfig().getStringList("greenhouses.debug");
        Settings.allowFlowIn = getConfig().getBoolean("greenhouses.allowflowin", false);
        Settings.allowFlowOut = getConfig().getBoolean("greenhouses.allowflowout", false);
        // Other settings
        Settings.worldName = getConfig().getStringList("greenhouses.worldName");
        if (Settings.worldName.isEmpty()) {
            Settings.worldName.add("world");
        }
        logger(1,"Greenhouse worlds are: " + Settings.worldName );
        Settings.snowChanceGlobal = getConfig().getDouble("greenhouses.snowchance", 0.5D);
        Settings.snowDensity = getConfig().getDouble("greenhouses.snowdensity", 0.1D);
        Settings.snowSpeed = getConfig().getLong("greenhouses.snowspeed", 30L);
        Settings.iceInfluence = getConfig().getInt("greenhouses.iceinfluence", 125);
        Settings.ecoTick = getConfig().getInt("greenhouses.ecotick", 30);
        Settings.mobTick = getConfig().getInt("greenhouses.mobtick", 20);
        Settings.plantTick = getConfig().getInt("greenhouses.planttick", 5);
        Settings.blockTick = getConfig().getInt("greenhouses.blocktick", 10);

        logger(3,"Snowchance " + Settings.snowChanceGlobal);
        logger(3,"Snowdensity " + Settings.snowDensity);
        logger(3,"Snowspeed " + Settings.snowSpeed);

        // Max greenhouse settings
        Settings.maxGreenhouses = getConfig().getInt("greenhouses.maxgreenhouses",-1);
        Settings.deleteExtras = getConfig().getBoolean("greenhouses.deleteextras", false);

    }

    /*
     * (non-Javadoc)
     *
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        saveGreenhouses();
        // Reset biomes back
        /*
	for (Greenhouse g: plugin.getGreenhouses()) {
	    try {
		g.endBiome();
	    } catch (Exception e) {}
	}*/
        try {
            // Remove players from memory
            greenhouses.clear();
            playerhouses.clear();
            players.removeAllPlayers();
            saveMessages();
        } catch (final Exception e) {
            addon.getLogger().severe("Something went wrong saving files!");
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        // instance of this plugin
        addon = this;
        saveDefaultConfig();
        saveDefaultLocale();
        // Metrics
        new MetricsLite(this);
        // Economy
        if (!VaultHelper.setupEconomy()) {
            getLogger().severe("Could not set up economy!");
        }
        // Set up player's cache
        players = new PlayerCache(this);
        loadPluginConfig();
        loadBiomeRecipes();
        biomeInv = new ControlPanel(this);
        // Set up commands for this plugin
        getCommand("greenhouse").setExecutor(new GreenhouseCmd(this,players));
        getCommand("gadmin").setExecutor(new AdminCmd(this,players));
        // Register events that this plugin uses
        registerEvents();
        // Load messages
        loadMessages();

        // Kick off a few tasks on the next tick
        getServer().getScheduler().runTask(addon, new Runnable() {

            @Override
            public void run() {
                final PluginManager manager = Bukkit.getServer().getPluginManager();
                if (manager.isPluginEnabled("Vault")) {
                    Greenhouses.getAddon().logger(1,"Trying to use Vault for permissions...");
                    if (!VaultHelper.setupPermissions()) {
                        getLogger().severe("Cannot link with Vault for permissions! Disabling plugin!");
                        manager.disablePlugin(Greenhouses.getAddon());
                    } else {
                        logger(1,"Success!");
                    };
                }
                // Load greenhouses
                loadGreenhouses();
            }
        });
        ecoTick();
    }

    public void ecoTick() {
        // Cancel any old schedulers
        if (plantTask != null)
            plantTask.cancel();
        if (blockTask != null)
            blockTask.cancel();
        if (mobTask != null)
            mobTask.cancel();
        if (ecoTask != null)
            ecoTask.cancel();

        // Kick off flower growing
        long plantTick = Settings.plantTick * 60 * 20; // In minutes
        if (plantTick > 0) {
            logger(1,"Kicking off flower growing scheduler every " + Settings.plantTick + " minutes");
            plantTask = getServer().getScheduler().runTaskTimer(addon, new Runnable() {

                @Override
                public void run() {
                    for (Greenhouse g : getGreenhouses()) {
                        logger(3,"Servicing greenhouse biome : " + g.getBiome().toString());
                        //checkEco();
                        try {
                            g.growFlowers();
                        } catch (Exception e) {
                            getLogger().severe("Problem found with greenhouse during growing flowers. Skipping...");
                            if (addon.getDebug().contains("3")) {
                                e.printStackTrace();
                            }
                        }
                        //g.populateGreenhouse();
                    }
                }
            }, 80L, plantTick);

        } else {
            logger(1,"Flower growth disabled.");
        }

        // Kick off flower growing
        long blockTick = Settings.blockTick * 60 * 20; // In minutes

        if (blockTick > 0) {
            logger(1,"Kicking off block conversion scheduler every " + Settings.blockTick + " minutes");
            blockTask = getServer().getScheduler().runTaskTimer(addon, new Runnable() {

                @Override
                public void run() {
                    for (Greenhouse g : getGreenhouses()) {
                        try {
                            g.convertBlocks();
                        } catch (Exception e) {
                            getLogger().severe("Problem found with greenhouse during block conversion. Skipping...");
                            getLogger().severe("[Greenhouse info]");
                            getLogger().severe("Owner: " + g.getOwner());
                            getLogger().severe("Location " + g.getPos1().toString() + " to " + g.getPos2().toString());
                            e.printStackTrace();
                        }

                        logger(3,"Servicing greenhouse biome : " + g.getBiome().toString());
                    }
                }
            }, 60L, blockTick);
        } else {
            logger(1,"Block conversion disabled.");
        }
        // Kick off g/h verification
        long ecoTick = Settings.plantTick * 60 * 20; // In minutes
        if (ecoTick > 0) {
            logger(1,"Kicking off greenhouse verify scheduler every " + Settings.ecoTick + " minutes");
            ecoTask = getServer().getScheduler().runTaskTimer(addon, new Runnable() {

                @Override
                public void run() {
                    try {
                        checkEco();
                    } catch (Exception e) {
                        getLogger().severe("Problem found with greenhouse during eco check. Skipping...");
                        if (addon.getDebug().contains("3")) {
                            e.printStackTrace();
                        }
                    }

                    //}
                }
            }, ecoTick, ecoTick);

        } else {
            logger(1,"Greenhouse verification disabled.");
        }
        // Kick off mob population
        long mobTick = Settings.mobTick * 60 * 20; // In minutes
        if (mobTick > 0) {
            logger(1,"Kicking off mob populator scheduler every " + Settings.plantTick + " minutes");
            mobTask = getServer().getScheduler().runTaskTimer(addon, new Runnable() {

                @Override
                public void run() {
                    for (Greenhouse g : getGreenhouses()) {
                        g.populateGreenhouse();
                    }
                }
            }, 120L, mobTick);

        } else {
            logger(1,"Mob disabled.");
        }
    }


    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {
        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;
        //Bukkit.logger(1,"Random number = " + randomNum);
        return randomNum;
    }


    /**
     * Load all known greenhouses
     */
    protected void loadGreenhouses() {
        // Load all known greenhouses
        // Clear them first
        greenhouses.clear();
        // Check for updated file
        greenhouseFile = new File(this.getDataFolder(),"greenhouses.yml");
        greenhouseConfig = new YamlConfiguration();
        File playersFolder = new File(this.getDataFolder(),"players");
        // See if the new file exists or not, if not make it
        if (!greenhouseFile.exists() && !playersFolder.exists()) {
            // Brand new install
            logger(1,"Creating new greenhouse.yml file");
            greenhouseConfig.createSection("greenhouses");
            try {
                greenhouseConfig.save(greenhouseFile);
            } catch (IOException e) {
                logger(1,"Could not save greenhouse.yml file!");
                // Could not save
                e.printStackTrace();
            }
        } else if (!greenhouseFile.exists() && playersFolder.exists()) {
            logger(1,"Converting from old greenhouse storage to new greenhouse storage");
            ConfigurationSection greenhouseSection = greenhouseConfig.createSection("greenhouses");
            int greenhouseNum = 0;
            // Load all the players
            for (final File f : playersFolder.listFiles()) {
                // Need to remove the .yml suffix
                String fileName = f.getName();
                if (fileName.endsWith(".yml")) {
                    try {
                        logger(1,"Converting " + fileName.substring(0, fileName.length() - 4));
                        final UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                        if (playerUUID == null) {
                            getLogger().warning("Player file contains erroneous UUID data.");
                            getLogger().warning("Looking at " + fileName.substring(0, fileName.length() - 4));
                        }
                        //new Players(this, playerUUID);
                        YamlConfiguration playerInfo = new YamlConfiguration();
                        playerInfo.load(f);
                        // Copy over greenhouses
                        ConfigurationSection myHouses = playerInfo.getConfigurationSection("greenhouses");
                        if (myHouses != null) {
                            // Get a list of all the greenhouses
                            for (String key : myHouses.getKeys(false)) {
                                try {
                                    // Copy over the info
                                    greenhouseSection.set(greenhouseNum + ".owner", playerUUID.toString());
                                    greenhouseSection.set(greenhouseNum + ".playerName", playerInfo.getString("playerName",""));
                                    greenhouseSection.set(greenhouseNum + ".pos-one", playerInfo.getString("greenhouses." + key + ".pos-one",""));
                                    greenhouseSection.set(greenhouseNum + ".pos-two", playerInfo.getString("greenhouses." + key + ".pos-two",""));
                                    greenhouseSection.set(greenhouseNum + ".originalBiome", playerInfo.getString("greenhouses." + key + ".originalBiome", "PLAINS"));
                                    greenhouseSection.set(greenhouseNum + ".greenhouseBiome", playerInfo.getString("greenhouses." + key + ".greenhouseBiome", "PLAINS"));
                                    greenhouseSection.set(greenhouseNum + ".roofHopperLocation", playerInfo.getString("greenhouses." + key + ".roofHopperLocation"));
                                    greenhouseSection.set(greenhouseNum + ".farewellMessage", playerInfo.getString("greenhouses." + key + ".flags.farewellMessage",""));
                                    greenhouseSection.set(greenhouseNum + ".enterMessage", playerInfo.getString("greenhouses." + key + ".flags.enterMessage",""));
                                } catch (Exception e) {
                                    addon.getLogger().severe("Problem copying player files");
                                    e.printStackTrace();
                                }
                                greenhouseNum++;
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            // Save the greenhouse file
            try {
                greenhouseConfig.save(greenhouseFile);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (greenhouseFile.exists()){
            // Load greenhouses from new file
            try {
                greenhouseConfig.load(greenhouseFile);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (greenhouseConfig.isConfigurationSection("greenhouses")) {
            ConfigurationSection myHouses = greenhouseConfig.getConfigurationSection("greenhouses");
            if (myHouses != null) {
                // Get a list of all the greenhouses
                for (String key : myHouses.getKeys(false)) {
                    logger(3,"Loading greenhouse #" + key);
                    try {
                        String playerName = myHouses.getString(key + ".playerName", "");
                        // Load all the values
                        Location pos1 = getLocationString(myHouses.getString(key + ".pos-one"));
                        Location pos2 = getLocationString(myHouses.getString(key + ".pos-two"));
                        UUID owner = UUID.fromString(myHouses.getString(key + ".owner"));
                        logger(3,"File pos1: " + pos1.toString());
                        logger(3,"File pos1: " + pos2.toString());
                        if (pos1 != null && pos2 !=null) {
                            // Check if this greenhouse already exists
                            if (!checkGreenhouseIntersection(pos1, pos2)) {
                                Greenhouse g = new Greenhouse(this, pos1, pos2, owner);
                                logger(3,"Greenhouse pos1: " + g.getPos1().toString());
                                logger(3,"Greenhouse pos2: " + g.getPos2().toString());
                                // Set owner name
                                g.setPlayerName(playerName);
                                logger(3,"Owner is " + playerName);
                                // Set biome
                                String oBiome = myHouses.getString(key + ".originalBiome", "PLAINS");
                                // Do some conversions
                                Biome originalBiome = convertBiome(oBiome);
                                g.setOriginalBiome(originalBiome);
                                logger(3,"original biome = " + oBiome + " converted = " + originalBiome);
                                String gBiome = myHouses.getString(key + ".greenhouseBiome", "PLAINS");
                                Biome greenhouseBiome = convertBiome(gBiome);
                                if (greenhouseBiome == null) {
                                    greenhouseBiome = Biome.PLAINS;
                                }
                                logger(3,"greenhouse biome = " + gBiome + " converted = " + greenhouseBiome);
                                String recipeName = myHouses.getString(key + ".greenhouseRecipe", "");
                                boolean success = false;
                                // Check to see if this biome has a recipe
                                // Try by name first
                                for (BiomeRecipe br : getBiomeRecipes()) {
                                    if (br.getName().equalsIgnoreCase(recipeName)) {
                                        success = true;
                                        g.setBiomeRecipe(br);
                                        break;
                                    }
                                }
                                // Fall back to biome
                                if (!success) {
                                    for (BiomeRecipe br : getBiomeRecipes()) {
                                        if (br.getBiome().equals(greenhouseBiome)) {
                                            success = true;
                                            g.setBiomeRecipe(br);
                                            break;
                                        }
                                    }
                                }
                                // Check to see if it was set properly
                                if (!success) {
                                    getLogger().warning("*****************************************");
                                    getLogger().warning("WARNING: No known recipe for biome " + greenhouseBiome.toString());
                                    getLogger().warning("[Greenhouse info]");
                                    getLogger().warning("Owner: " + playerName + " UUID:" + g.getOwner());
                                    getLogger().warning("Location :" + g.getPos1().getWorld().getName() + " " + g.getPos1().getBlockX() + "," + g.getPos1().getBlockZ());
                                    getLogger().warning("Greenhouse will be removed next eco-tick!");
                                    getLogger().warning("*****************************************");
                                }
                                // Start the biome
                                g.startBiome(false);
                                Location hopperLoc = getLocationString(myHouses.getString(key + ".roofHopperLocation"));
                                if (hopperLoc != null) {
                                    g.setRoofHopperLocation(hopperLoc);
                                }
                                // Load farewell and hello messages
                                g.setEnterMessage(myHouses.getString(key +".enterMessage",(Locale.messagesenter.replace("[owner]", playerName )).replace("[biome]", Util.prettifyText(gBiome))));
                                g.setFarewellMessage(myHouses.getString(key +".farewellMessage",Locale.messagesleave.replace("[owner]", playerName)));
                                // Add to the cache
                                addGHToPlayer(owner, g);
                            }
                        } else {
                            getLogger().severe("Problem loading greenhouse with locations " + myHouses.getString(key + ".pos-one") + " and " + myHouses.getString(key + ".pos-two") + " skipping.");
                            getLogger().severe("Has this world been deleted?");
                        }
                    } catch (Exception e) {
                        getLogger().severe("Problem loading greenhouse file");
                        e.printStackTrace();
                    }

                }
                logger(3,"Loaded " + addon.getGreenhouses().size() + " greenhouses.");
            }
        }

        logger(1,"Loaded " + getGreenhouses().size() + " greenhouses.");
    }

    public void addGHToPlayer(UUID owner, Greenhouse g) {
        HashSet<Greenhouse> storedhouses = null;

        if (playerhouses.get(owner) != null) {
            storedhouses = playerhouses.get(owner);
            playerhouses.remove(owner);
        } else {
            storedhouses = new HashSet<Greenhouse>();
        }

        storedhouses.add(g);
        greenhouses.add(g);
        playerhouses.put(owner, storedhouses);
    }

    public void removeGHFromPlayer(UUID owner, Greenhouse g) {
        HashSet<Greenhouse> storedhouses = null;

        if (playerhouses.get(owner) != null) {
            storedhouses = playerhouses.get(owner);
            playerhouses.remove(owner);
        } else {
            storedhouses = new HashSet<Greenhouse>();
        }

        storedhouses.remove(g);
        greenhouses.remove(g);
        playerhouses.put(owner, storedhouses);
    }


    /**
     * Converts biomes to known biomes if required
     * @param oBiome
     * @return
     */
    private Biome convertBiome(String oBiome) {
        if (addon.getServer().getVersion().contains("(MC: 1.8") || addon.getServer().getVersion().contains("(MC: 1.7")) {
            try {
                return Biome.valueOf(oBiome);
            } catch (Exception e) {
                getLogger().severe("Could not identify Biome " + oBiome + " setting to PLAINS - may destroy greenhouse");
                return Biome.PLAINS;
            }
        } else {
            // May need to convert Biome
            if (oBiome.equalsIgnoreCase("COLD_TAIGA")) {
                getLogger().warning("Converting Cold Taiga biome to 1.9 Taiga Cold");
                return Biome.TAIGA_COLD;
            }
            if (oBiome.equalsIgnoreCase("FLOWER_FOREST")) {
                getLogger().warning("Converting Flower Forest biome to 1.9 Forest");
                return Biome.FOREST;
            }
            if (oBiome.equalsIgnoreCase("BEACH")) {
                getLogger().warning("Converting Beach biome to 1.9 Beaches");
                return Biome.BEACHES;
            }
            String test = oBiome;

            while (!test.isEmpty()) {
                for (Biome biome: Biome.values()) {
                    if (biome.name().contains(test)) {
                        if (!biome.name().equals(test)) {
                            getLogger().warning("Converting " + oBiome + " biome to 1.9 " + biome.name() + " - may destroy greenhouse.");
                        }
                        return biome;
                    }
                }
                test = test.substring(0, test.length() - 1);
            }
        }
        getLogger().severe("Could not identify Biome " + oBiome + " setting to PLAINS - may destroy greenhouse");
        return Biome.PLAINS;
    }


    /**
     * Registers events
     */
    public void registerEvents() {
        final PluginManager manager = getServer().getPluginManager();
        // Greenhouse Protection events
        manager.registerEvents(new GreenhouseGuard(this), this);
        // Listen to greenhouse change events
        manager.registerEvents(new GreenhouseEvents(this), this);
        // Events for when a player joins or leaves the server
        manager.registerEvents(new JoinLeaveEvents(this, players), this);
        // Biome CP
        manager.registerEvents(biomeInv, this);
        // Weather event
        manager.registerEvents(eco, this);
    }


    // Localization
    /**
     * Saves the locale.yml file if it does not exist
     */
    public void saveDefaultLocale() {
        if (localeFile == null) {
            localeFile = new File(getDataFolder(), "locale.yml");
        }
        if (!localeFile.exists()) {
            saveResource("locale.yml", false);
        }
    }

    /**
     * Reloads the locale file
     */
    public void reloadLocale() {
        if (localeFile == null) {
            saveDefaultLocale();
        }
        locale = YamlConfiguration.loadConfiguration(localeFile);
    }

    /**
     * @return locale FileConfiguration object
     */
    public FileConfiguration getLocale() {
        if (locale == null) {
            reloadLocale();
        }
        return locale;
    }



    /*
    public void saveLocale() {
	if (locale == null || localeFile == null) {
	    return;
	}
	try {
	    getLocale().save(localeFile);
	} catch (IOException ex) {
	    getLogger().severe("Could not save config to " + localeFile);
	}
    }
     */
    /**
     * Sets a message for the player to receive next time they login
     * @param player
     * @param message
     * @return true if player is offline, false if online
     */
    public boolean setMessage(UUID playerUUID, String message) {
        logger(3,"received message - " + message);
        Player player = getServer().getPlayer(playerUUID);
        // Check if player is online
        if (player != null) {
            if (player.isOnline()) {
                //player.sendMessage(message);
                return false;
            }
        }
        // Player is offline so store the message

        List<String> playerMessages = messages.get(playerUUID);
        if (playerMessages != null) {
            playerMessages.add(message);
        } else {
            playerMessages = new ArrayList<String>(Arrays.asList(message));
        }
        messages.put(playerUUID, playerMessages);
        return true;
    }

    public List<String> getMessages(UUID playerUUID) {
        List<String> playerMessages = messages.get(playerUUID);
        if (playerMessages != null) {
            // Remove the messages
            messages.remove(playerUUID);
        } else {
            // No messages
            playerMessages = new ArrayList<String>();
        }
        return playerMessages;
    }

    public boolean saveMessages() {
        logger(1,"Saving offline messages...");
        try {
            // Convert to a serialized string
            final HashMap<String,Object> offlineMessages = new HashMap<String,Object>();
            for (UUID p : messages.keySet()) {
                offlineMessages.put(p.toString(),messages.get(p));
            }
            // Convert to YAML
            messageStore.set("messages", offlineMessages);
            saveYamlFile(messageStore, "messages.yml");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadMessages() {
        logger(1,"Loading offline messages...");
        try {
            messageStore = loadYamlFile("messages.yml");
            if (messageStore.getConfigurationSection("messages") == null) {
                messageStore.createSection("messages"); // This is only used to create
            }
            HashMap<String,Object> temp = (HashMap<String, Object>) messageStore.getConfigurationSection("messages").getValues(true);
            for (String s : temp.keySet()) {
                List<String> messageList = messageStore.getStringList("messages." + s);
                if (!messageList.isEmpty()) {
                    messages.put(UUID.fromString(s), messageList);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @return the greenhouses
     */
    public HashSet<Greenhouse> getGreenhouses() {
        return greenhouses;
    }

    /**
     *
     * @param uuid
     * @return Cached player greenhouses
     */
    /*
    public HashSet<Greenhouse> getPlayerGHouse(UUID uuid) {
    	if (playerhouses.containsKey(uuid)) {
    		return playerhouses.get(uuid);
    	}
    	return null;
    }
     */

    /**
     * @param greenhouses the greenhouses to set
     */
    public void setGreenhouses(HashSet<Greenhouse> greenhouses) {
        this.greenhouses = greenhouses;
    }

    /**
     * @return the playerhouses
     */
    public HashMap<UUID, HashSet<Greenhouse>> getPlayerhouses() {
        return playerhouses;
    }


    /**
     * Clears the greenhouses list
     */
    public void clearGreenhouses() {
        this.greenhouses.clear();
    }

    /**
     * Checks if a greenhouse defined by the corner points pos1 and pos2 overlaps any known greenhouses
     * @param pos1
     * @param pos2
     * @return
     */
    public boolean checkGreenhouseIntersection(Location pos1, Location pos2) {
        // Create a 2D rectangle of this
        Rectangle2D.Double rect = new Rectangle2D.Double();
        rect.setFrameFromDiagonal(pos1.getX(), pos1.getZ(), pos2.getX(), pos2.getZ());
        Rectangle2D.Double testRect = new Rectangle2D.Double();
        // Create a set of rectangles of current greenhouses
        for (Greenhouse d: greenhouses) {
            testRect.setFrameFromDiagonal(d.getPos1().getX(), d.getPos1().getZ(),d.getPos2().getX(),d.getPos2().getZ());
            if (rect.intersects(testRect)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a location is inside a greenhouse (3D space)
     * @param location
     * @return Greenhouse or null if none
     */
    public Greenhouse getInGreenhouse(Location location) {
        for (Greenhouse g : greenhouses) {
            //logger(3,"greenhouse check");
            if (g.insideGreenhouse(location)) {
                return g;
            }
        }
        // This location is not in a greenhouse
        return null;
    }

    /**
     * Checks if the location is on the greenhouse
     * @param location
     * @return the greenhouse that this is above
     */
    public Greenhouse aboveAGreenhouse(Location location) {
        for (Greenhouse g : greenhouses) {
            //logger(3,"greenhouse check");
            if (g.aboveGreenhouse(location)) {
                return g;
            }
        }
        // This location is above in a greenhouse
        return null;
    }

    /**
     * Removes the greenhouse from the world and resets biomes
     * @param g
     */
    public void removeGreenhouse(Greenhouse g) {
        //players.get(g.getOwner());
        // Remove the greenhouse
        greenhouses.remove(g);
        // Remove the greenhouse from the owner's count (only does anything if they are online)
        players.decGreenhouseCount(g.getOwner());
        // Stop any eco action
        eco.remove(g);
        logger(3,"Returning biome to original state: " + g.getOriginalBiome().toString());
        //g.setBiome(g.getOriginalBiome()); // just in case
        if (g.getOriginalBiome().equals(Biome.HELL) || g.getOriginalBiome().equals(Biome.DESERT)
                || g.getOriginalBiome().equals(Biome.DESERT_HILLS)) {
            // Remove any water
            for (int y = g.getPos1().getBlockY(); y< g.getPos2().getBlockY();y++) {
                for (int x = g.getPos1().getBlockX();x<=g.getPos2().getBlockX();x++) {
                    for (int z = g.getPos1().getBlockZ();z<=g.getPos2().getBlockZ();z++) {
                        Block b = g.getPos1().getWorld().getBlockAt(x, y, z);
                        if (b.getType().equals(Material.WATER) || b.getType().equals(Material.STATIONARY_WATER)
                                || b.getType().equals(Material.ICE) || b.getType().equals(Material.PACKED_ICE)
                                || b.getType().equals(Material.SNOW) || b.getType().equals(Material.SNOW_BLOCK)) {
                            // Evaporate it
                            b.setType(Material.AIR);
                            //ParticleEffects.SMOKE_LARGE.send(Bukkit.getOnlinePlayers(),b.getLocation(),0D,0D,0D,1F,5,20);
                            b.getWorld().spawnParticle(Particle.SMOKE_LARGE, b.getLocation(), 5);
                            //ParticleEffect.SMOKE_LARGE.display(0F,0F,0F, 0.1F, 5, b.getLocation(), 30D);
                        }
                    }
                }
            }
        }
        g.endBiome();
        boolean ownerOnline = false;
        if (!ownerOnline) {
            setMessage(g.getOwner(), Locale.messagesremovedmessage.replace("[biome]", Util.prettifyText(g.getBiome().toString())) + " [" + g.getPos1().getBlockX() + "," + g.getPos1().getBlockZ() + "]");
        }
        /*
	// Set the biome
	for (int y = g.getPos1().getBlockY(); y< g.getPos2().getBlockY();y++) {

	    for (int x = g.getPos1().getBlockX()+1;x<g.getPos2().getBlockX();x++) {
		for (int z = g.getPos1().getBlockZ()+1;z<g.getPos2().getBlockZ();z++) {
		    g.getPos1().getWorld().getBlockAt(x, y, z).setBiome(g.getOriginalBiome());
		}
	    }
	}
	int minx = Math.min(g.getPos1().getChunk().getX(), g.getPos2().getChunk().getX());
	int maxx = Math.max(g.getPos1().getChunk().getX(), g.getPos2().getChunk().getX());
	int minz = Math.min(g.getPos1().getChunk().getZ(), g.getPos2().getChunk().getZ());
	int maxz = Math.max(g.getPos1().getChunk().getZ(), g.getPos2().getChunk().getZ());
	for (int x = minx; x < maxx + 1; x++) {
	    for (int z = minz; z < maxz+1;z++) {
		world.refreshChunk(x,z);
	    }
	}
         */

    }


    /**
     * Checks that each greenhouse is still viable
     */
    public void checkEco() {
        // Run through each greenhouse
        logger(3,"started eco check");
        // Check all the greenhouses to see if they still meet the g/h recipe
        List<Greenhouse> onesToRemove = new ArrayList<Greenhouse>();
        for (Greenhouse g : getGreenhouses()) {
            logger(3,"Testing greenhouse owned by " + g.getOwner().toString());
            if (!g.checkEco()) {
                // The greenhouse failed an eco check - remove it
                onesToRemove.add(g);
            }
        }
        for (Greenhouse gg : onesToRemove) {
            // Check if player is online
            Player owner = addon.getServer().getPlayer(gg.getOwner());
            if (owner == null)  {
                // TODO messages.ecolost
                setMessage(gg.getOwner(), Locale.messagesecolost.replace("[location]", Greenhouses.getStringLocation(gg.getPos1())));
            } else {
                owner.sendMessage(ChatColor.RED + Locale.messagesecolost.replace("[location]", Greenhouses.getStringLocation(gg.getPos1())));
            }

            logger(1,"Greenhouse at " + Greenhouses.getStringLocation(gg.getPos1()) + " lost its eco system and was removed.");
            logger(1,"Greenhouse biome was " + Util.prettifyText(gg.getBiome().toString()) + " - reverted to " + Util.prettifyText(gg.getOriginalBiome().toString()));
            //UUID ownerUUID = gg.getOwner();
            removeGreenhouse(gg);
            removeGHFromPlayer(owner.getUniqueId(), gg);
            //players.save(ownerUUID);
        }
        saveGreenhouses();
    }

    public Inventory getRecipeInv(Player player) {
        return biomeInv.getPanel(player);
    }


    /**
     * Checks that a greenhouse meets specs and makes it
     * @param player
     * @return the Greenhouse object
     */
    public Greenhouse tryToMakeGreenhouse(final Player player) {
        return tryToMakeGreenhouse(player, null);
    }
    /**
     * Checks that a greenhouse meets specs and makes it
     * If type is stated then only this specific type will be checked
     * @param player
     * @param greenhouseRecipe
     * @return
     */
    @SuppressWarnings("deprecation")
    public Greenhouse tryToMakeGreenhouse(final Player player, BiomeRecipe greenhouseRecipe) {
        if (greenhouseRecipe != null) {
            // Do an immediate permissions check of the biome recipe if the type is declared
            if (!greenhouseRecipe.getPermission().isEmpty()) {
                if (!VaultHelper.checkPerm(player, greenhouseRecipe.getPermission())) {
                    player.sendMessage(ChatColor.RED + Locale.errornoPermission);
                    logger(2,"no permssions to use this biome");
                    return null;
                }
            }
            String name = greenhouseRecipe.getFriendlyName();
            if (name.isEmpty()) {
                name = Util.prettifyText(greenhouseRecipe.getBiome().name()) + " biome";
            }
            player.sendMessage(ChatColor.GOLD + "Trying to make a " + name + " greenhouse...");
        }
        // Proceed to check the greenhouse
        final Location location = player.getLocation().add(new Vector(0,1,0));
        logger(3,"Player location is " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
        final Biome originalBiome = location.getBlock().getBiome();
        final World world = location.getWorld();

        // we have the height above this location where a roof block is
        // Check the sides

        // Now look along the roof until we find the dimensions of the roof
        Roof roof = new Roof(this, location);
        if (!roof.isRoofFound()) {
            player.sendMessage(ChatColor.RED + Locale.createnoroof);
            logger(3,"Roof not found with roof check");
            return null;
        }
        // Check that the player is under the roof
        if (!(player.getLocation().getBlockX() > roof.getMinX() && player.getLocation().getBlockX() <= roof.getMaxX()
                && player.getLocation().getBlockZ() > roof.getMinZ() && player.getLocation().getBlockZ() <= roof.getMaxZ())) {
            logger(3,"Player does not appear to be inside the greenhouse");
            logger(3,"Player location " + player.getLocation());
            logger(3,"Roof minx = " + roof.getMinX() + " maxx = " + roof.getMaxX());
            logger(3,"Roof minz = " + roof.getMinZ() + " maxz = " + roof.getMaxZ());
            player.sendMessage(ChatColor.RED + Locale.errornotinside);
            return null;
        }
        // Now see if the walls match the roof - they may not
        Walls walls = new Walls(this, player, roof);
        // Roof is never smaller than walls, but walls can be smaller than the roof
        int maxX = walls.getMaxX();
        int minX = walls.getMinX();
        int maxZ = walls.getMaxZ();
        int minZ = walls.getMinZ();

        logger(3,"minx = " + minX);
        logger(3,"maxx = " + maxX);
        logger(3,"minz = " + minZ);
        logger(3,"maxz = " + maxZ);
        // Now check again to see if the floor really is the floor and the walls follow the rules
        // Counts
        int wallDoors = 0;
        // Hoppers
        int ghHopper = 0;
        // Air
        boolean airHoles = false;
        // Other blocks
        boolean otherBlocks = false;
        // Ceiling issue
        boolean inCeiling = false;
        // Blocks above the greenhouse
        boolean blocksAbove = false;
        // The y height where other blocks were found
        // If this is the bottom layer, the player has most likely uneven walls
        int otherBlockLayer = -1;
        int wallBlockCount = 0;
        Location roofHopperLoc = null;
        Set<Location> redGlass = new HashSet<Location>();
        int y = 0;
        for (y = world.getMaxHeight(); y >= walls.getFloor(); y--) {
            Set<Location> redLayer = new HashSet<Location>();
            int doorCount = 0;
            int hopperCount = 0;
            boolean airHole = false;
            boolean otherBlock = false;
            boolean blockAbove = false;
            wallBlockCount = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location thisBlock = new Location(world, x, y, z);
                    Material blockType = world.getBlockAt(x, y, z).getType();
                    // Checking above greenhouse - no blocks allowed
                    if (y > roof.getHeight()) {
                        //Greenhouses.logger(3,"DEBUG: Above greenhouse");
                        // We are above the greenhouse
                        if ((world.getEnvironment().equals(Environment.NORMAL) || world.getEnvironment().equals(Environment.THE_END))
                                && blockType != Material.AIR) {
                            blockAbove = true;
                            redLayer.add(thisBlock);
                        }
                    } else {
                        //Greenhouses.logger(3,"DEBUG: In greenhouse");
                        // Check just the walls
                        if (y == roof.getHeight() || x == minX || x == maxX || z == minZ || z== maxZ) {
                            //Greenhouses.logger(3,"DEBUG: Checking " + x + " " + y + " " + z);
                            // Doing string check for DOOR allows all 1.8 doors to be covered even if the server is not 1.8
                            if ((y != roof.getHeight() && !WALLBLOCKS.contains(blockType.name()) && !blockType.toString().contains("DOOR"))
                                    || (y == roof.getHeight() && !roof.isRoofBlock(blockType) && !blockType.toString().contains("DOOR"))) {
                                logger(2,"DEBUG: bad block found at  " + x + "," + y+ "," + z + " " + blockType);
                                if (blockType == Material.AIR) {
                                    airHole = true;
                                    if (y == roof.getHeight()) {
                                        inCeiling = true;
                                    }
                                } else {
                                    otherBlock = true;
                                }
                                redLayer.add(thisBlock);
                            } else {
                                wallBlockCount++;
                                // A string comparison is used to capture 1.8+ door types without stopping pre-1.8
                                // servers from working
                                if (blockType.toString().contains("DOOR")) {
                                    doorCount++;
                                    // If we already have 8 doors add these blocks to the red list
                                    if (wallDoors == 8) {
                                        redLayer.add(thisBlock);
                                    }
                                }
                                if (blockType.equals(Material.HOPPER)) {
                                    hopperCount++;
                                    if (ghHopper > 0) {
                                        // Problem! Add extra hoppers to the red glass list
                                        redLayer.add(thisBlock);
                                    } else {
                                        // This is the first hopper
                                        roofHopperLoc = thisBlock.clone();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (wallBlockCount == 0 && y < roof.getHeight()) {
                // This is the floor
                break;
            } else {
                wallBlockCount = 0;
                wallDoors += doorCount;
                ghHopper += hopperCount;
                if (airHole) {
                    airHoles = true;
                }
                if (otherBlock) {
                    otherBlocks = true;
                    if (otherBlockLayer < 0) {
                        otherBlockLayer = y;
                    }
                }
                if (blockAbove) {
                    blocksAbove = true;
                }
                // Collect the holes
                redGlass.addAll(redLayer);
            }
        }
        logger(3,"Floor is at height y = " + y);
        // Check that the player is vertically in the greenhouse
        if (player.getLocation().getBlockY() <= y) {
            player.sendMessage(ChatColor.RED + Locale.errornotinside);
            return null;
        }
        if (!redGlass.isEmpty()) {
            // Show errors
            if (blocksAbove) {
                player.sendMessage(ChatColor.RED + Locale.createnothingabove);
            }
            if (airHoles & !inCeiling) {
                player.sendMessage(ChatColor.RED + Locale.createholeinwall);
            } else if (airHoles & inCeiling) {
                player.sendMessage(ChatColor.RED + Locale.createholeinroof);
            }
            //Greenhouses.logger(3,"DEBUG: otherBlockLayer = " + otherBlockLayer);
            if (otherBlocks && otherBlockLayer == y + 1) {
                player.sendMessage(ChatColor.RED + "Walls must be even all the way around");
            } else if (otherBlocks && otherBlockLayer == roof.getHeight()) {
                player.sendMessage(ChatColor.RED + "Roof blocks must be glass, glowstone, doors or a hopper.");
            } else if (otherBlocks) {
                player.sendMessage(ChatColor.RED + "Wall blocks must be glass, glowstone, doors or a hopper.");
            }
            if (wallDoors > 8) {
                player.sendMessage(ChatColor.RED + Locale.createdoorerror);
            }
            if (ghHopper > 1) {
                player.sendMessage(ChatColor.RED + Locale.createhoppererror);
            }
            // Display the red glass and then erase it
            for (Location loc: redGlass) {
                player.sendBlockChange(loc,Material.STAINED_GLASS,(byte)14);
            }
            final Set<Location> original = redGlass;
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {

                @Override
                public void run() {
                    for (Location loc: original) {
                        player.sendBlockChange(loc,loc.getBlock().getType(),loc.getBlock().getData());
                    }
                }}, 120L);
            return null;
        }
        //player.sendMessage(ChatColor.GREEN + "Seems ok");

        Location insideOne = new Location(world,minX,walls.getFloor(),minZ);
        Location insideTwo = new Location(world,maxX,roof.getHeight(),maxZ);
        BiomeRecipe winner = null;
        // Now check for the greenhouse biomes
        if (greenhouseRecipe != null) {
            if (greenhouseRecipe.checkRecipe(insideOne, insideTwo, player)) {
                winner = greenhouseRecipe;
            } else {
                return null;
            }
        }
        if (winner == null) {
            // Loop through biomes to see which ones match
            // Int is the priority. Higher priority ones win
            int priority = 0;
            for (BiomeRecipe r : addon.getBiomeRecipes()) {
                // Only check ones that this player has permission to use
                if (r.getPermission().isEmpty() || (!r.getPermission().isEmpty() && VaultHelper.checkPerm(player, r.getPermission()))) {
                    // Only check higher priority ones
                    if (r.getPriority()>priority) {
                        player.sendMessage(ChatColor.GOLD + "Trying " + Util.prettifyText(r.getBiome().toString()));
                        if (r.checkRecipe(insideOne, insideTwo, null)) {
                            player.sendMessage(ChatColor.GOLD + "Maybe...");
                            winner = r;
                            priority = r.getPriority();
                        } else {
                            player.sendMessage(ChatColor.GOLD + "No.");
                        }
                    }
                } else {
                    addon.logger(2, "No permission for " + player.getName() + " to make " + r.getBiome().toString());
                    //player.sendMessage(ChatColor.RED + "No permission for " + r.getType().toString());
                }
            }
        }

        if (winner != null) {
            logger(3,"biome winner is " + winner.getFriendlyName());
            Greenhouse g = new Greenhouse(this, insideOne, insideTwo, player.getUniqueId());
            g.setOriginalBiome(originalBiome);
            g.setBiomeRecipe(winner);
            String friendlyName = Util.prettifyText(winner.getBiome().toString());
            if (!winner.getFriendlyName().isEmpty()) {
                friendlyName = winner.getFriendlyName();
            }
            g.setPlayerName(player.getName());
            g.setEnterMessage((Locale.messagesenter.replace("[owner]", player.getDisplayName())).replace("[biome]", friendlyName));
            g.setFarewellMessage((Locale.messagesleave.replace("[owner]", player.getDisplayName())).replace("[biome]", friendlyName));
            // Store the roof hopper location so it can be tapped in the future
            if (ghHopper == 1) {
                g.setRoofHopperLocation(roofHopperLoc);
            }
            g.startBiome(false);
            player.sendMessage(ChatColor.GREEN + Locale.createsuccess.replace("[biome]", friendlyName));
            players.incGreenhouseCount(player);
            // Store this greenhouse
            greenhouses.add(g);
            // Find everyone who is in this greenhouse and tell them they are in a greenhouse now
            for (Player p : getServer().getOnlinePlayers()) {
                if (g.insideGreenhouse(p.getLocation())) {
                    if (!p.equals(player)) {
                        p.sendMessage((Locale.messagesyouarein.replace("[owner]", player.getDisplayName())).replace("[biome]", friendlyName));
                    }
                }
            }
            return g;
        }
        return null;
    }

    /**
     * Saves all the greenhouses to greenhouse.yml
     */
    public void saveGreenhouses() {
        logger(2,"Saving greenhouses...");
        ConfigurationSection greenhouseSection = greenhouseConfig.createSection("greenhouses");
        // Get a list of all the greenhouses
        int greenhouseNum = 0;
        for (Greenhouse g: greenhouses) {
            try {
                // Copy over the info
                greenhouseSection.set(greenhouseNum + ".owner", g.getOwner().toString());
                greenhouseSection.set(greenhouseNum + ".playerName", g.getPlayerName());
                greenhouseSection.set(greenhouseNum + ".pos-one", getStringLocation(g.getPos1()));
                greenhouseSection.set(greenhouseNum + ".pos-two", getStringLocation(g.getPos2()));
                greenhouseSection.set(greenhouseNum + ".originalBiome", g.getOriginalBiome().toString());
                greenhouseSection.set(greenhouseNum + ".greenhouseBiome", g.getBiome().toString());
                greenhouseSection.set(greenhouseNum + ".greenhouseRecipe", g.getBiomeRecipe().getName());
                greenhouseSection.set(greenhouseNum + ".roofHopperLocation", getStringLocation(g.getRoofHopperLocation()));
                greenhouseSection.set(greenhouseNum + ".farewellMessage", g.getFarewellMessage());
                greenhouseSection.set(greenhouseNum + ".enterMessage", g.getEnterMessage());
            } catch (Exception e) {
                addon.getLogger().severe("Problem copying player files");
                e.printStackTrace();
            }
            greenhouseNum++;
        }
        try {
            greenhouseConfig.save(greenhouseFile);
        } catch (IOException e) {
            getLogger().severe("Could not save greenhouse.yml!");
            e.printStackTrace();
        }
    }

    /**
     * General purpose logger to reduce console spam
     * @param level
     * @param info
     */
    public void logger(int level, String info) {
        if (debug.contains("0")) {
            return;
        }
        if (debug.contains("1") && level == 1) {
            Bukkit.getLogger().info(info);
        } else if (debug.contains(String.valueOf(level))){
            Bukkit.getLogger().info("DEBUG ["+level+"]:"+info);
        }
    }


    /**
     * @return the debug
     */
    public List<String> getDebug() {
        return debug;
    }

    /**
     * Returns the maximum number of greenhouses this player can make
     * @param player
     * @return number of greenhouses or -1 to indicate unlimited
     */
    public int getMaxGreenhouses(Player player) {
        // -1 is unimited
        int maxGreenhouses = Settings.maxGreenhouses;
        for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
            if (perms.getPermission().startsWith("greenhouses.limit")) {
                logger(2,"Permission is = " + perms.getPermission());
                try {
                    int max = Integer.valueOf(perms.getPermission().split("greenhouses.limit.")[1]);
                    if (max > maxGreenhouses) {
                        maxGreenhouses = max;
                    }
                } catch (Exception e) {} // Do nothing
            }
            // Do some sanity checking
            if (maxGreenhouses < 0) {
                maxGreenhouses = -1;
            }
        }
        return maxGreenhouses;
    }
}
