package world.bentobox.greenhouses.ui.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager.GhResult;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
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
        if (user.getLocation() == null) {
            getAddon().logError("User had no location");
            return false;
        }
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
        ((Greenhouses)this.getAddon()).getManager().tryToMakeGreenhouse(location, br).thenAccept(result -> informUser(user, br, result));
        return true;
    }

    private boolean informUser(User user, BiomeRecipe br, GhResult result) {
        if (result.getResults().contains(GreenhouseResult.SUCCESS)) {
            // Success
            user.sendMessage("greenhouses.commands.user.make.success", "[biome]", result.getFinder().getGh().getBiomeRecipe().getFriendlyName());
            return true;
        }
        result.getResults().forEach(r -> user.sendMessage("greenhouses.commands.user.make.error." + r.name()));
        if (!result.getFinder().getRedGlass().isEmpty()) {
            // Show red glass
            result.getFinder().getRedGlass().forEach(rg -> user.getPlayer().sendBlockChange(rg.toLocation(user.getWorld()), Material.RED_STAINED_GLASS.createBlockData()));
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> result.getFinder().getRedGlass().stream().map(v -> v.toLocation(user.getWorld())).forEach(rg -> user.getPlayer().sendBlockChange(rg, rg.getBlock().getBlockData())), 120L);
        }
        if (br != null && result.getResults().contains(GreenhouseResult.FAIL_INSUFFICIENT_BLOCKS)) {
            result.getFinder().getGh().getMissingBlocks().forEach((k,v) -> user.sendMessage("greenhouses.commands.user.make.missing-blocks", "[material]", Util.prettifyText(k.toString()), TextVariables.NUMBER, String.valueOf(v)));
        }
        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(new ArrayList<>(this.getRecipes(user).keySet()));
    }

}
