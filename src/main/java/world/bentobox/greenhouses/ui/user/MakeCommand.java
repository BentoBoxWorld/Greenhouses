package world.bentobox.greenhouses.ui.user;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.managers.GreenhouseManager.GhResult;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

/**
 * @author tastybento
 *
 */
class MakeCommand extends CompositeCommand {

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
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {

        // TODO Check permission
        // Find the physical the greenhouse
        Location location = user.getLocation().add(new Vector(0,1,0));
        // Check if there's a gh here already
        if (((Greenhouses)this.getAddon()).getManager().getMap().getGreenhouse(location).isPresent()) {
            user.sendRawMessage("You are in a greenhouse already!" );
            return true;
        }
        GhResult result = ((Greenhouses)this.getAddon()).getManager().tryToMakeGreenhouse(location, null);

        if (result.getResults().contains(GreenhouseResult.SUCCESS)) {
            // Success
            user.sendMessage("general.success");
            user.sendRawMessage(result.getFinder().getGh().getBiomeRecipe().getName());
            return true;
        }
        result.getResults().forEach(r -> sendErrorMessage(user, r));
        if (!result.getFinder().getRedGlass().isEmpty()) {
            // Show red glass
            result.getFinder().getRedGlass().forEach(rg -> user.getPlayer().sendBlockChange(rg, Material.RED_STAINED_GLASS.createBlockData()));
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> result.getFinder().getRedGlass().forEach(rg -> user.getPlayer().sendBlockChange(rg, rg.getBlock().getBlockData())), 120L);
        }
        return true;
    }

    private void sendErrorMessage(User user, GreenhouseResult r) {
        user.sendRawMessage(r.name());
        switch (r) {
        case FAIL_BAD_ROOF_BLOCKS:
            break;
        case FAIL_BAD_WALL_BLOCKS:
            break;
        case FAIL_BELOW:
            break;
        case FAIL_BLOCKS_ABOVE:
            break;
        case FAIL_HOLE_IN_ROOF:
            break;
        case FAIL_HOLE_IN_WALL:
            break;
        case FAIL_NO_ROOF:
            break;
        case FAIL_TOO_MANY_DOORS:
            break;
        case FAIL_TOO_MANY_HOPPERS:
            break;
        case FAIL_UNEVEN_WALLS:
            break;
        case FAIL_INSUFFICIENT_ICE:
            break;
        case FAIL_INSUFFICIENT_LAVA:
            break;
        case FAIL_INSUFFICIENT_WATER:
            break;
        case FAIL_NO_ICE:
            break;
        case FAIL_NO_LAVA:
            break;
        case FAIL_NO_WATER:
            break;
        default:
            break;
        }
    }
}
