package world.bentobox.greenhouses.ui.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * Deletes a greenhouse record by unique ID, whether or not it loaded successfully. This is the
 * supported way to clear out records that are skipped at startup - for example a greenhouse that
 * overlaps another - without hand-editing the database.
 *
 * @author tastybento
 */
class AdminDeleteCommand extends ConfirmableCommand {

    private Greenhouse gh;

    /**
     * @param parent - parent command
     */
    public AdminDeleteCommand(CompositeCommand parent) {
        super(parent, "delete");
    }

    @Override
    public void setup() {
        AdminUtil.setup(this, true);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        gh = AdminUtil.requireOne(this, user, args);
        return gh != null;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Greenhouses addon = this.getAddon();
        // Deleting a greenhouse cannot be undone, so always confirm
        this.askConfirmation(user, user.getTranslation("greenhouses.commands.admin.delete.confirm", "[id]",
                AdminUtil.shortId(gh), "[owner]", AdminUtil.ownerName(addon, gh), "[xyz]", AdminUtil.xyz(gh)), () -> {
                    if (addon.getManager().deleteById(gh.getUniqueId())) {
                        user.sendMessage("greenhouses.commands.admin.delete.success", "[id]", AdminUtil.shortId(gh));
                    } else {
                        user.sendMessage("greenhouses.commands.admin.errors.unknown-id", "[id]", gh.getUniqueId());
                    }
                });
        return true;
    }

}
