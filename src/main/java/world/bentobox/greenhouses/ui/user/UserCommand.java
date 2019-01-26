package world.bentobox.greenhouses.ui.user;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;

/**
 * @author tastybento
 *
 */
public class UserCommand extends CompositeCommand {

    /**
     * @param gh - addon
     * @param parent - parent command
     */
    public UserCommand(Greenhouses gh, CompositeCommand parent) {
        super(gh, parent, "greenhouse", "gh");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup() {
        this.setPermission("greenhouses.player");
        this.setOnlyPlayer(true);
        this.setParametersHelp("greenhouses.command.parameters");
        this.setDescription("greenhouses.command.description");

        //new InfoCommand(this);
        //new ListCommand(this);
        new MakeCommand(this);
        //new RecipeCommand(this);
        new RemoveCommand(this);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.showHelp(this, user);
        return true;
    }

}
