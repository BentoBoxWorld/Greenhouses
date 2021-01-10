package world.bentobox.greenhouses;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.RecipeManager;
import world.bentobox.greenhouses.ui.admin.AdminCmd;
import world.bentobox.greenhouses.ui.user.UserCommand;

/**
 * @author tastybento
 *
 */
public class Greenhouses extends Addon {

    private GreenhouseManager manager;
    private Settings settings;
    private RecipeManager recipes;
    private final List<World> activeWorlds = new ArrayList<>();
    public static final Flag GREENHOUSES = new Flag.Builder("GREENHOUSE", Material.GREEN_STAINED_GLASS)
            .mode(Mode.BASIC)
            .type(Type.PROTECTION).build();
    private static Greenhouses instance;
    private final Config<Settings> config;

    public static Greenhouses getInstance() {
        return instance;
    }

    /**
     * Constructor
     */
    public Greenhouses() {
        super();
        instance = this;
        config = new Config<>(this, Settings.class);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.addons.Addon#onEnable()
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.saveResource("biomes.yml", false);
        settings = config.loadConfigObject();
        if (settings == null) {
            // Settings did no load correctly. Disable.
            logError("Settings did not load correctly - disabling Greenhouses - please check config.yml");
            this.setState(State.DISABLED);
            return;
        }
        config.saveConfigObject(settings);
        // Load recipes
        recipes = new RecipeManager(this);
        // Load manager
        manager = new GreenhouseManager(this);
        // Clear
        this.activeWorlds.clear();
        // Register commands for
        getPlugin().getAddonsManager().getGameModeAddons().stream()
        .filter(gm -> settings.getGameModes().stream().anyMatch(gm.getDescription().getName()::equalsIgnoreCase))
        .forEach(gm ->  {
            // Register command
            gm.getPlayerCommand().ifPresent(playerCmd -> new UserCommand(this, playerCmd));
            gm.getAdminCommand().ifPresent(playerCmd -> new AdminCmd(this, playerCmd));
            // Log
            this.log("Hooking into " + gm.getDescription().getName());
            // Store active world
            activeWorlds.add(gm.getOverWorld());
        });
        if (this.activeWorlds.isEmpty()) {
            this.logError("Greenhouses could not hook into any game modes! Check config.yml");
            this.setState(State.DISABLED);
        } else {
            // Register greenhouse manager
            this.registerListener(manager);
            // Register protection flag with BentoBox
            getPlugin().getFlagsManager().registerFlag(this, GREENHOUSES);
        }
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.addons.Addon#onDisable()
     */
    @Override
    public void onDisable() {
        if (manager != null) {
            manager.saveGreenhouses();
            if (manager.getEcoMgr() != null) manager.getEcoMgr().cancel();
        }
    }

    /**
     * @return the manager
     */
    public GreenhouseManager getManager() {
        return manager;
    }

    /**
     * @return Settings
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * @return the recipes
     */
    public RecipeManager getRecipes() {
        return recipes;
    }

    public List<World> getActiveWorlds() {
        return activeWorlds;
    }

}
