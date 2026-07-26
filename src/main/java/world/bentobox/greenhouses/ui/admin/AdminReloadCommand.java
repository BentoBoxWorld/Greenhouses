package world.bentobox.greenhouses.ui.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;

/**
 * Reloads the biome recipes and re-reads the greenhouses from the database, without a restart.
 *
 * @author tastybento
 */
class AdminReloadCommand extends CompositeCommand {

    /**
     * @param parent - parent command
     */
    public AdminReloadCommand(CompositeCommand parent) {
        super(parent, "reload");
    }

    @Override
    public void setup() {
        AdminUtil.setup(this, false);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Greenhouses addon = this.getAddon();
        // Recipes first - reloading greenhouses resolves each one's recipe by name
        addon.getRecipes().reload();
        addon.getManager().reload();
        user.sendMessage("greenhouses.commands.admin.reload.success", TextVariables.NUMBER,
                String.valueOf(addon.getManager().getMap().getSize()));
        int unloaded = addon.getManager().getUnloaded().size();
        if (unloaded > 0) {
            user.sendMessage("greenhouses.commands.admin.reload.unloaded", TextVariables.NUMBER,
                    String.valueOf(unloaded));
        }
        return true;
    }

}
