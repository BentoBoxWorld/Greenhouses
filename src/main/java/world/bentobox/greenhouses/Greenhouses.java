/**
 *
 */
package world.bentobox.greenhouses;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.greenhouses.listeners.GreenhouseEvents;
import world.bentobox.greenhouses.listeners.GreenhouseGuard;
import world.bentobox.greenhouses.listeners.SnowTracker;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.RecipeManager;
import world.bentobox.greenhouses.ui.user.UserCommand;

/**
 * @author tastybento
 *
 */
public class Greenhouses extends Addon {

    private GreenhouseManager manager;
    private Settings settings;
    private RecipeManager recipes;
    private List<World> activeWorlds = new ArrayList<>();

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.addons.Addon#onEnable()
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.saveResource("biomes.yml", false);
        settings = new Config<>(this, Settings.class).loadConfigObject();
        if (settings == null) {
            // Settings did no load correctly. Disable.
            logError("Settings did not load correctly - disabling Greenhouses - please check config.yml");
            this.setState(State.DISABLED);
            return;
        }
        // Load manager
        manager = new GreenhouseManager(this);
        // Load recipes
        recipes = new RecipeManager(this);

        // Register commands for AcidIsland and BSkyBlock
        getPlugin().getAddonsManager().getGameModeAddons().stream()
        .filter(gm -> gm.getDescription().getName().equals("AcidIsland") || gm.getDescription().getName().equals("BSkyBlock"))
        .forEach(gm ->  {
            // Register command
            gm.getPlayerCommand().ifPresent(playerCmd -> new UserCommand(this, playerCmd));
            // Store active world
            activeWorlds.add(gm.getOverWorld());
        });

        // Register listeners
        this.registerListener(new SnowTracker(this));
        this.registerListener(new GreenhouseEvents(this));
        this.registerListener(new GreenhouseGuard(this));

    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.addons.Addon#onDisable()
     */
    @Override
    public void onDisable() {
        if (manager != null) {
            manager.saveGreenhouses();
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
