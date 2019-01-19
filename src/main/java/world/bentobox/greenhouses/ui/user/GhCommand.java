/**
 *
 */
package world.bentobox.greenhouses.ui.user;

import java.util.List;

import org.bukkit.ChatColor;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.greenhouse.Greenhouse;
import world.bentobox.greenhouses.ui.Locale;

/**
 * @author tastybento
 *
 */
public class GhCommand extends CompositeCommand {

    /**
     * @param parent
     * @param label
     * @param aliases
     */
    public GhCommand(CompositeCommand parent) {
        super(parent, "greenhouse", "gh");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup() {
        this.setPermission("greenhouses.command");
        this.setOnlyPlayer(true);
        this.setParametersHelp("greenhouses.command.parameters");
        this.setDescription("greenhouses.command.description");

        new InfoCommand(this);
        new ListCommand(this);
        new MakeCommand(this);
        new RecipeCommand(this);
        new RemoveCommand(this);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        final Greenhouse greenhouseInNow = players.getInGreenhouse(player);
        if (greenhouseInNow==null || greenhouseInNow.getOwner().equals(playerUUID)) {
            player.openInventory(plugin.getRecipeInv(player));
            return true;
        } else {
            player.sendMessage(ChatColor.RED + Locale.errornotowner);
            return true;
        }
    }

}
