package world.bentobox.greenhouses.ui.admin;

import java.util.List;

import org.bukkit.Location;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * Teleports the admin to a greenhouse, so that the output of the list command is actionable.
 *
 * @author tastybento
 */
class AdminTeleportCommand extends CompositeCommand {

    private Greenhouse gh;

    /**
     * @param parent - parent command
     */
    public AdminTeleportCommand(CompositeCommand parent) {
        super(parent, "tp", "teleport");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup() {
        this.setPermission("greenhouses.admin.tp");
        this.setOnlyPlayer(true);
        this.setParametersHelp("greenhouses.commands.admin.tp.parameters");
        this.setDescription("greenhouses.commands.admin.tp.description");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            this.showHelp(this, user);
            return false;
        }
        Greenhouses addon = this.getAddon();
        gh = addon.getManager().getGreenhouseById(args.get(0)).orElse(null);
        if (gh == null) {
            user.sendMessage("greenhouses.commands.admin.errors.unknown-id", "[id]", args.get(0));
            return false;
        }
        if (gh.getLocation() == null || gh.getLocation().getWorld() == null) {
            // Records with no world are exactly the ones that fail to load, so this is reachable
            user.sendMessage("greenhouses.commands.admin.errors.no-location", "[id]", AdminUtil.shortId(gh));
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Aim for the middle of the greenhouse floor rather than its minimum corner
        Location target = gh.getBoundingBox().getCenter().toLocation(gh.getLocation().getWorld());
        target.setY(gh.getFloorHeight() + 1D);
        Util.teleportAsync(user.getPlayer(), target).thenAccept(b -> user
                .sendMessage("greenhouses.commands.admin.tp.success", "[id]", AdminUtil.shortId(gh)));
        return true;
    }

}
