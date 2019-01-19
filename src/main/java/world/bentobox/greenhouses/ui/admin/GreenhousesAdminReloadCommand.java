/**
 *
 */
package world.bentobox.greenhouses.ui.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
public class GreenhousesAdminReloadCommand extends CompositeCommand {

    /**
     * @param addon
     * @param label
     * @param aliases
     */
    public GreenhousesAdminReloadCommand(CompositeCommand parent) {
        super(parent, "reload");
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
        // TODO Auto-generated method stub
        return false;
    }

}
