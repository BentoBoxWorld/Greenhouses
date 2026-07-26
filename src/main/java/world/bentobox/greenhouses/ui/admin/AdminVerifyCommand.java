package world.bentobox.greenhouses.ui.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

/**
 * Re-runs recipe verification against the world for one or all greenhouses and reports which no
 * longer meet their recipe. Without this, a greenhouse only gets re-checked on the eco tick.
 *
 * @author tastybento
 */
class AdminVerifyCommand extends CompositeCommand {

    private List<Greenhouse> toCheck;

    /**
     * @param parent - parent command
     */
    public AdminVerifyCommand(CompositeCommand parent) {
        super(parent, "verify", "check");
    }

    @Override
    public void setup() {
        AdminUtil.setup(this, true);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            Greenhouse gh = AdminUtil.byId(this, user, args.get(0));
            if (gh == null) {
                return false;
            }
            toCheck = List.of(gh);
            return true;
        }
        Greenhouses addon = this.getAddon();
        toCheck = new ArrayList<>(addon.getManager().getMap().getGreenhouses());
        if (toCheck.isEmpty()) {
            user.sendMessage("greenhouses.commands.admin.list.none");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        user.sendMessage("greenhouses.commands.admin.verify.checking", TextVariables.NUMBER,
                String.valueOf(toCheck.size()));
        // Recipe checks run asynchronously, so report each result as it arrives
        toCheck.forEach(gh -> gh.getBiomeRecipe().checkRecipe(gh).thenAccept(results -> report(user, gh, results)));
        return true;
    }

    private void report(User user, Greenhouse gh, Set<GreenhouseResult> results) {
        if (results.isEmpty()) {
            user.sendMessage("greenhouses.commands.admin.verify.ok", "[id]", AdminUtil.shortId(gh), "[xyz]",
                    AdminUtil.xyz(gh));
            return;
        }
        user.sendMessage("greenhouses.commands.admin.verify.broken", "[id]", AdminUtil.shortId(gh), "[xyz]",
                AdminUtil.xyz(gh), "[reasons]",
                results.stream().map(GreenhouseResult::name).sorted().collect(java.util.stream.Collectors.joining(", ")));
        gh.getMissingBlocks().forEach((m, count) -> user.sendMessage(
                "greenhouses.commands.user.make.missing-blocks", "[material]", m.name(), "[number]",
                String.valueOf(count)));
    }

}
