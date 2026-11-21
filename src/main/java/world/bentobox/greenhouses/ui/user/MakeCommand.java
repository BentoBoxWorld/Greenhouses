package world.bentobox.greenhouses.ui.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.ui.GreenhouseResults;
import world.bentobox.greenhouses.ui.panel.Panel;

/**
 * Command to try to make a greenhouse
 * @author tastybento
 *
 */
class MakeCommand extends CompositeCommand  {

    /**
     * @param parent - parent command
     */
    public MakeCommand(CompositeCommand parent) {
        super(parent, "make");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup() {
        this.setPermission("greenhouses.player");
        this.setOnlyPlayer(true);
        this.setParametersHelp("greenhouses.commands.user.make.parameters");
        this.setDescription("greenhouses.commands.user.make.description");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!Util.getWorld(user.getWorld()).equals(getWorld())) {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }
        if (args.isEmpty()) {
            new Panel(this.getAddon()).showPanel(user);
            return true;
        }
        // Check recipe given matches
        BiomeRecipe br = getRecipe(user, args.get(0));
        if (br == null) {
            user.sendMessage("greenhouses.commands.user.make.unknown-recipe");
            user.sendMessage("greenhouses.commands.user.make.try-these");
            getRecipes(user).forEach((k,v) -> user.sendMessage("greenhouses.commands.user.make.recipe-format", TextVariables.NAME, v.getName()));
            return false;
        }
        return makeGreenhouse(user, br);
    }

    /**
     * Get a recipe for user
     * @param user - user
     * @param arg - given string
     * @return recipe or null if unknown
     */
    private BiomeRecipe getRecipe(User user, String arg) {
        return getRecipes(user).get(arg);
    }
    /**
     * Get a string list of recipes the player has permission to use
     * @param user - user
     * @return list
     */
    private Map<String, BiomeRecipe> getRecipes(User user) {
        return ((Greenhouses)getAddon()).getRecipes().getBiomeRecipes().stream()
                .filter(br -> user.hasPermission(br.getPermission()))
                .collect(Collectors.toMap(BiomeRecipe::getName, br -> br));
    }

    /**
     * @param user - user
     * @param br  requested biome recipe, or null to try anything
     * @return true if successful
     */
    private boolean makeGreenhouse(User user, BiomeRecipe br) {
        // Check flag
        if (!getIslands().getIslandAt(user.getLocation()).map(i -> i.isAllowed(user, Greenhouses.GREENHOUSES)).orElse(false)) {
            user.sendMessage("greenhouses.errors.no-rank");
            return false;
        }
        // Find the physical the greenhouse
        Location location = user.getLocation().add(new Vector(0,1,0));
        // Check if there's a gh here already
        if (((Greenhouses)this.getAddon()).getManager().getMap().getGreenhouse(location).isPresent()) {
            user.sendMessage("greenhouses.commands.user.make.error.already");
            return false;
        }
        // Try to make the greenhouse
        ((Greenhouses)this.getAddon()).getManager().tryToMakeGreenhouse(location, br).thenAccept(result -> GreenhouseResults.report(user, br, result, getPlugin()));
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(new ArrayList<>(this.getRecipes(user).keySet()));
    }

}
