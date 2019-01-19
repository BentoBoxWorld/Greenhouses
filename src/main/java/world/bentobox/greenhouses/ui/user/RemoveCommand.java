/**
 *
 */
package world.bentobox.greenhouses.ui.user;

import java.util.List;

import org.bukkit.ChatColor;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.Greenhouse;
import world.bentobox.greenhouses.ui.Locale;

/**
 * @author tastybento
 *
 */
public class RemoveCommand extends CompositeCommand {

    /**
     * @param parent
     * @param label
     * @param aliases
     */
    public RemoveCommand(CompositeCommand parent) {
        super(parent, "make");
        // TODO Auto-generated constructor stub
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
        final Greenhouse greenhouseNow = ((Greenhouses)getAddon()).getInGreenhouse(user);
        if (greenhouseNow != null) {
            if (greenhouseNow.getOwner().equals(user.getUniqueId())) {
                user.sendMessage(ChatColor.RED + Locale.errorremoving);
                plugin.removeGreenhouse(greenhouseNow);
                return true;
            }
            user.sendMessage(ChatColor.RED + Locale.errornotyours);
        } else {
            user.sendMessage(ChatColor.RED + Locale.errornotinside);
        }
        return true;

    }

}
