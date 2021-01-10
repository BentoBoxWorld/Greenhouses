package world.bentobox.greenhouses.ui.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;

/**
 * @author tastybento
 *
 */
class GreenhousesAdminInfoCommand extends CompositeCommand {

    private Greenhouses addon = Greenhouses.getInstance();
    private static final Set<Material> transparent = new HashSet<>();
    {
        transparent.add(Material.AIR);
        transparent.add(Material.GLASS);
    }
    /**
     * @param parent - parent user command, e.g, /island
     */
    public GreenhousesAdminInfoCommand(CompositeCommand parent) {
        super(parent, "info");

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
        Location l = user.getPlayer().getLineOfSight(transparent, 5).get(0).getLocation();
        addon.getManager().getMap().getGreenhouse(l).ifPresent(gh ->
        {
            addon.log("There are " + addon.getManager().getEcoMgr().getAvailableBlocks(gh, false).size());
            addon.getManager().getEcoMgr().getAvailableBlocks(gh, false).forEach(b -> user.getPlayer().sendBlockChange(b.getLocation(), Material.CYAN_STAINED_GLASS.createBlockData()));
        });
        return true;
    }


}
