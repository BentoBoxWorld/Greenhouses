package world.bentobox.greenhouses.ui.user;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
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
            new Panel((Greenhouses)this.getAddon()).ShowPanel(user);
            return true;
        }
        return makeGreenhouse(user, null);
    }
    
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
        GhResult result = ((Greenhouses)this.getAddon()).getManager().tryToMakeGreenhouse(location, br);

        if (result.getResults().contains(GreenhouseResult.SUCCESS)) {
            // Success
            user.sendMessage("greenhouses.commands.user.make.success", "[biome]", result.getFinder().getGh().getBiomeRecipe().getFriendlyName());
            return true;
        }
        result.getResults().forEach(r -> user.sendMessage("greenhouses.commands.user.make.error." + r.name()));
        if (!result.getFinder().getRedGlass().isEmpty()) {
            // Show red glass
            result.getFinder().getRedGlass().forEach(rg -> user.getPlayer().sendBlockChange(rg, Material.RED_STAINED_GLASS.createBlockData()));
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> result.getFinder().getRedGlass().forEach(rg -> user.getPlayer().sendBlockChange(rg, rg.getBlock().getBlockData())), 120L);
        }
        return true;
    }
    
}
