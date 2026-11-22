package world.bentobox.greenhouses.ui.admin;

import static world.bentobox.greenhouses.ui.panel.Items.item;
import static world.bentobox.greenhouses.ui.panel.Items.run;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.dialogs.DialogField;
import world.bentobox.bentobox.api.dialogs.DialogField.Choice.Option;
import world.bentobox.bentobox.api.dialogs.DialogResponse;
import world.bentobox.bentobox.api.dialogs.Dialogs;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.builders.PagedPanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.build.GreenhousePlan;
import world.bentobox.greenhouses.build.GreenhousePlan.Mode;
import world.bentobox.greenhouses.build.GreenhousePlan.Size;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.ui.admin.editor.RecipeEditor;

/**
 * The admin hub, opened by the admin command with no arguments: one place to reach the recipe
 * editor, the greenhouse browser and the other admin commands. Each button shows only if the
 * admin has that command's permission, and runs the command itself, so the checks and messages
 * are the same as typing it.
 *
 * @author tastybento
 */
public class AdminHub {

    private static final String K = "greenhouses.hub.";

    static final int RECIPES = 10;
    static final int GREENHOUSES = 12;
    static final int BUILD = 14;
    static final int VERIFY = 16;
    static final int RELOAD = 22;
    static final int CLOSE = 26;

    private final CompositeCommand admin;
    private final User user;
    private final Greenhouses addon;

    /**
     * @param admin the admin command, whose sub-commands the buttons run
     * @param user  the admin
     */
    public AdminHub(CompositeCommand admin, User user) {
        this.admin = admin;
        this.user = user;
        this.addon = admin.getAddon();
    }

    private String t(String key, String... vars) {
        return user.getTranslation(K + key, vars);
    }

    /**
     * @param label a sub-command label
     * @return the sub-command, if the admin may use it
     */
    Optional<CompositeCommand> allowed(String label) {
        return admin.getSubCommand(label).filter(c -> user.hasPermission(c.getPermission()));
    }

    /**
     * Runs a sub-command as if the admin had typed it.
     *
     * @param label the sub-command
     * @param args  its arguments
     */
    void call(String label, String... args) {
        allowed(label).ifPresent(c -> c.call(user, c.getLabel(), List.of(args)));
    }

    /**
     * Opens the hub.
     */
    public void open() {
        PanelBuilder pb = new PanelBuilder().user(user).size(27).name(t("title"));
        int loaded = addon.getManager().getMap().getSize();
        int unloaded = addon.getManager().getUnloaded().size();
        boolean dialogs = Dialogs.isSupported();
        if (dialogs && allowed("edit").isPresent()) {
            pb.item(RECIPES, item(Material.WRITABLE_BOOK, t("recipes.name"), List.of(t("recipes.description",
                    TextVariables.NUMBER, String.valueOf(addon.getRecipes().getBiomeRecipes().size()))),
                    run(() -> new RecipeEditor(addon, user).backTo(this::open).openList())));
        }
        if (allowed("list").isPresent()) {
            List<String> lore = new java.util.ArrayList<>();
            lore.add(t("greenhouses.description", TextVariables.NUMBER, String.valueOf(loaded)));
            if (unloaded > 0) {
                lore.add(t("greenhouses.unloaded", TextVariables.NUMBER, String.valueOf(unloaded)));
            }
            pb.item(GREENHOUSES, item(Material.GLASS, t("greenhouses.name"), lore,
                    run(() -> new GreenhouseBrowser(admin, user, this::open).open(0))));
        }
        if (dialogs && allowed("build").isPresent()) {
            pb.item(BUILD, item(Material.BRICKS, t("build.name"), List.of(t("build.description")),
                    run(this::pickBuildRecipe)));
        }
        if (allowed("verify").isPresent()) {
            pb.item(VERIFY, item(Material.SPYGLASS, t("verify.name"), List.of(t("verify.description")), run(() -> {
                user.closeInventory();
                call("verify");
            })));
        }
        if (allowed("reload").isPresent()) {
            pb.item(RELOAD, item(Material.CLOCK, t("reload.name"), List.of(t("reload.description")),
                    run(dialogs ? this::confirmReload : this::reload)));
        }
        pb.item(CLOSE, item(Material.OAK_DOOR, t("close"), List.of(), run(user::closeInventory)));
        pb.build();
    }

    private void confirmReload() {
        new DialogBuilder().title(user, K + "reload.confirm-title").body(user, K + "reload.confirm-body")
                .confirmation(DialogButton.of(user, K + "reload.confirm", u -> reload()),
                        DialogButton.of(user, K + "cancel", u -> open()))
                .build().show(user);
    }

    private void reload() {
        call("reload");
        open();
    }

    // ------------------------------------------------------------------------
    // Build here
    // ------------------------------------------------------------------------

    /**
     * Lists the recipes to build a test greenhouse for.
     */
    void pickBuildRecipe() {
        PagedPanelBuilder b = new PagedPanelBuilder().user(user).name(t("build.pick-title"));
        addon.getRecipes().getBiomeRecipes().stream().sorted(Comparator.comparing(BiomeRecipe::getName))
                .forEach(r -> {
                    Size size = GreenhousePlan.defaultSize(r);
                    b.item(item(r.getIcon(), r.getFriendlyName(), List.of(t("build.key", "[key]", r.getName()),
                            t("build.default-size", "[size]", size.toString()), t("build.click")),
                            run(() -> buildForm(r, size, Mode.KEEP))));
                });
        b.control(1, item(Material.OAK_DOOR, t("back"), List.of(), run(this::open)));
        b.build();
    }

    private void buildForm(BiomeRecipe r, Size size, Mode mode) {
        List<Option> modes = List.of(Option.of(user, "keep", K + "build.keep"),
                Option.of(user, "clear", K + "build.clear"));
        new DialogBuilder().title(user, K + "build.title", "[name]", r.getFriendlyName())
                .body(user, K + "build.body")
                .input(DialogField.number(user, "width", K + "build.width", GreenhousePlan.MIN_WIDTH,
                        GreenhousePlan.MAX_WIDTH, size.width()).step(1))
                .input(DialogField.number(user, "depth", K + "build.depth", GreenhousePlan.MIN_WIDTH,
                        GreenhousePlan.MAX_WIDTH, size.depth()).step(1))
                .input(DialogField.number(user, "height", K + "build.height", GreenhousePlan.MIN_HEIGHT,
                        GreenhousePlan.MAX_HEIGHT, size.height()).step(1))
                .input(DialogField.choice(user, "mode", K + "build.mode", modes, mode.name().toLowerCase(java.util.Locale.ENGLISH)))
                .button(DialogButton.submit(user, K + "build.go", (u, resp) -> build(r, resp)))
                .button(DialogButton.of(user, K + "cancel", u -> pickBuildRecipe()))
                .build().show(user);
    }

    /**
     * Builds a test greenhouse where the admin stands, from the build dialog's answers.
     *
     * @param r    the recipe
     * @param resp the answers
     */
    void build(BiomeRecipe r, DialogResponse resp) {
        Size d = GreenhousePlan.defaultSize(r);
        int w = Math.clamp(resp.getInt("width").orElse(d.width()), GreenhousePlan.MIN_WIDTH, GreenhousePlan.MAX_WIDTH);
        int dp = Math.clamp(resp.getInt("depth").orElse(d.depth()), GreenhousePlan.MIN_WIDTH, GreenhousePlan.MAX_WIDTH);
        int h = Math.clamp(resp.getInt("height").orElse(d.height()), GreenhousePlan.MIN_HEIGHT, GreenhousePlan.MAX_HEIGHT);
        String mode = resp.getChoice("mode").orElse("keep");
        admin.getSubCommand("build").filter(AdminBuildCommand.class::isInstance).map(AdminBuildCommand.class::cast)
                .ifPresent(c -> c.buildNow(user, List.of(r.getName(), w + "x" + dp + "x" + h, mode)));
    }
}
