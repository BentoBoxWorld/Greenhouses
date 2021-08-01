package world.bentobox.greenhouses.ui.user;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;

/**
 * Command to remove a greenhouse
 * @author tastybento
 *
 */
class RemoveCommand extends CompositeCommand {

    /**
     * @param parent - parent command
     */
    public RemoveCommand(CompositeCommand parent) {
        super(parent, "remove");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup() {
        this.setPermission("greenhouses.player");
        this.setOnlyPlayer(true);
        this.setDescription("greenhouses.commands.user.remove.description");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (user.getLocation() == null) {
            getAddon().logError("User had no location");
            return false;
        }
        // Check flag
        if (!getIslands().getIslandAt(user.getLocation()).map(i -> i.isAllowed(user, Greenhouses.GREENHOUSES)).orElse(false)) {
            user.sendMessage("greenhouses.errors.no-rank");
            return false;
        }
        Greenhouses addon = this.getAddon();
        // Remove greenhouse if it exists
        if (!addon.getManager().getMap().getGreenhouse(user.getLocation()).map(gh -> {
            user.sendMessage("general.success");
            addon.getManager().removeGreenhouse(gh);
            return true;
        }).orElse(false)) {
            user.sendMessage("greenhouses.errors.not-inside");
            return false;
        }
        return true;
    }

}
