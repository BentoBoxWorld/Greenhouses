package world.bentobox.greenhouses.ui.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;

/**
 * Root of the Greenhouses admin command tree, registered under the game mode's admin command,
 * e.g. {@code /bsbadmin greenhouses}.
 *
 * @author tastybento
 */
public class AdminCommand extends CompositeCommand {

    /**
     * @param addon - addon
     * @param parent - the game mode's admin command
     */
    public AdminCommand(Greenhouses addon, CompositeCommand parent) {
        super(addon, parent, "greenhouses", "greenhouse", "gh");
    }

    @Override
    public void setup() {
        this.setPermission("greenhouses.admin");
        this.setOnlyPlayer(false);
        this.setDescription("greenhouses.commands.admin.description");

        new AdminListCommand(this);
        new AdminInfoCommand(this);
        new AdminDeleteCommand(this);
        new AdminTeleportCommand(this);
        new AdminVerifyCommand(this);
        new AdminReloadCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        return showHelp(this, user);
    }

}
