package world.bentobox.greenhouses.ui.admin;

import java.util.List;

import org.bukkit.Location;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
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

    @Override
    public void setup() {
        AdminUtil.setup(this, true);
        this.setOnlyPlayer(true);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        gh = AdminUtil.requireOne(this, user, args);
        if (gh == null) {
            return false;
        }
        if (gh.getLocation() == null || gh.getLocation().getWorld() == null) {
            // Records with no world are exactly the ones that fail to load, so this is reachable
            user.sendMessage("greenhouses.commands.admin.errors.no-location", "[id]", AdminUtil.shortId(gh));
            return false;
        }
        return true;
    }

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
