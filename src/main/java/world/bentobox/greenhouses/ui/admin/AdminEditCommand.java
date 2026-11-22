package world.bentobox.greenhouses.ui.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.dialogs.Dialogs;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.ui.admin.editor.RecipeEditor;

/**
 * Opens the recipe editor: {@code edit} for the list of recipes, {@code edit <recipe>} to go
 * straight to one.
 *
 * @author tastybento
 */
class AdminEditCommand extends CompositeCommand {

    /**
     * @param parent - parent command
     */
    public AdminEditCommand(CompositeCommand parent) {
        super(parent, "edit");
    }

    @Override
    public void setup() {
        AdminUtil.setup(this, true);
        this.setOnlyPlayer(true);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // The editor's forms are dialogs
        if (!Dialogs.isSupported()) {
            user.sendMessage("greenhouses.editor.needs-dialogs");
            return false;
        }
        if (!args.isEmpty() && find(args.getFirst()).isEmpty()) {
            user.sendMessage("greenhouses.editor.unknown-recipe", "[key]", args.getFirst());
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        RecipeEditor editor = new RecipeEditor(getAddon(), user);
        if (args.isEmpty()) {
            editor.openList();
        } else {
            editor.edit(find(args.getFirst()).orElseThrow().getName());
        }
        return true;
    }

    private Optional<BiomeRecipe> find(String name) {
        return ((Greenhouses) getAddon()).getRecipes().getBiomeRecipes().stream()
                .filter(r -> r.getName().equalsIgnoreCase(name)).findFirst();
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> all) {
        List<String> args = all.subList(Math.min(getLevel(), all.size()), all.size());
        if (args.size() > 1) {
            return Optional.of(List.of());
        }
        List<String> names = new ArrayList<>();
        ((Greenhouses) getAddon()).getRecipes().getBiomeRecipes().forEach(r -> names.add(r.getName()));
        return Optional.of(Util.tabLimit(names, args.isEmpty() ? "" : args.getFirst()));
    }
}
