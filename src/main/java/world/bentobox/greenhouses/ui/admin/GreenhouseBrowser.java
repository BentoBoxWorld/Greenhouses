package world.bentobox.greenhouses.ui.admin;

import static world.bentobox.greenhouses.ui.panel.Items.item;
import static world.bentobox.greenhouses.ui.panel.Items.page;
import static world.bentobox.greenhouses.ui.panel.Items.run;
import static world.bentobox.greenhouses.ui.panel.Items.wrap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.dialogs.DialogField;
import world.bentobox.bentobox.api.dialogs.Dialogs;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PagedPanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager.UnloadedGreenhouse;

/**
 * A paged list of every greenhouse, loaded or not, for admins. Clicks run the admin commands
 * on the greenhouse: teleport, verify, info and delete.
 *
 * @author tastybento
 */
public class GreenhouseBrowser {

    private static final String K = "greenhouses.browser.";

    static final int BACK = 1;
    static final int FILTER = 4;

    private final CompositeCommand admin;
    private final User user;
    private final Greenhouses addon;
    private final Runnable back;
    /** Only greenhouses on islands owned by this player, or all if null */
    private @Nullable String owner;

    /**
     * @param admin the admin command, whose sub-commands the clicks run
     * @param user  the admin
     * @param back  what Back does
     */
    public GreenhouseBrowser(CompositeCommand admin, User user, Runnable back) {
        this.admin = admin;
        this.user = user;
        this.addon = admin.getAddon();
        this.back = back;
    }

    private String t(String key, String... vars) {
        return user.getTranslation(K + key, vars);
    }

    private boolean may(String label) {
        return admin.getSubCommand(label).filter(c -> user.hasPermission(c.getPermission())).isPresent();
    }

    private void call(String label, String id) {
        admin.getSubCommand(label).filter(c -> user.hasPermission(c.getPermission()))
                .ifPresent(c -> c.call(user, c.getLabel(), List.of(id)));
    }

    /**
     * @param owner only show greenhouses on this player's islands, or null for all
     * @return this browser
     */
    GreenhouseBrowser owner(@Nullable String owner) {
        this.owner = owner == null || owner.isBlank() ? null : owner.strip();
        return this;
    }

    /**
     * Opens the browser.
     *
     * @param page the page to show
     */
    public void open(int page) {
        String title = owner == null ? t("title") : t("title-owner", "[owner]", owner);
        PagedPanelBuilder b = new PagedPanelBuilder().user(user).name(title).page(page);
        List<Greenhouse> loaded = new ArrayList<>(addon.getManager().getMap().getGreenhouses());
        loaded.removeIf(gh -> owner != null && !AdminUtil.ownerName(addon, gh).equalsIgnoreCase(owner));
        loaded.sort(Comparator.comparing((Greenhouse gh) -> AdminUtil.ownerName(addon, gh).toLowerCase())
                .thenComparing(gh -> String.valueOf(gh.getBiomeRecipeName())));
        loaded.forEach(gh -> b.item(entry(gh, null)));
        // Records that failed to load have no island listing, so they show whatever the filter
        if (owner == null) {
            addon.getManager().getUnloaded().forEach(u -> b.item(entry(u.greenhouse(), u)));
        }
        if (b.getItems().isEmpty()) {
            b.item(item(Material.BARRIER, t("none"), List.of(), null));
        }
        b.control(BACK, item(Material.OAK_DOOR, t("back"), List.of(), run(back)));
        if (Dialogs.isSupported()) {
            List<String> lore = new ArrayList<>();
            lore.add(owner == null ? t("filter.all") : t("filter.only", "[owner]", owner));
            lore.add(t("filter.click"));
            b.control(FILTER, item(Material.PLAYER_HEAD, t("filter.name"), lore, run(this::askOwner)));
        }
        b.build();
    }

    /**
     * @param gh       a greenhouse
     * @param unloaded why it did not load, or null if it is loaded
     * @return its entry
     */
    PanelItem entry(Greenhouse gh, @Nullable UnloadedGreenhouse unloaded) {
        String id = gh.getUniqueId();
        BiomeRecipe recipe = unloaded == null ? gh.getBiomeRecipe() : null;
        Material icon = recipe == null ? Material.BARRIER : recipe.getIcon();
        String name = recipe == null ? String.valueOf(gh.getBiomeRecipeName()) : recipe.getFriendlyName();
        List<String> lore = new ArrayList<>();
        lore.add(t("id", "[id]", AdminUtil.shortId(gh)));
        lore.add(t("owner", "[owner]", AdminUtil.ownerName(addon, gh)));
        lore.add(t("where", "[world]", AdminUtil.worldName(gh), "[xyz]", AdminUtil.xyz(gh)));
        if (unloaded != null) {
            lore.add(t("unloaded", "[reason]", unloaded.reason().name()));
        } else if (gh.isBroken()) {
            lore.add(t("broken"));
        } else {
            lore.add(t("ok"));
        }
        boolean hasWorld = gh.getLocation() != null && gh.getLocation().getWorld() != null;
        boolean tp = hasWorld && may("tp");
        boolean verify = unloaded == null && may("verify");
        boolean info = may("info");
        boolean delete = Dialogs.isSupported() && may("delete");
        if (tp) {
            lore.add(t("click.tp"));
        }
        if (verify) {
            lore.add(t("click.verify"));
        }
        if (info) {
            lore.add(t("click.info"));
        }
        if (delete) {
            lore.add(t("click.delete"));
        }
        return new PanelItemBuilder().icon(icon).name(name).description(wrap(lore))
                .clickHandler((panel, u, click, slot) -> {
                    if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
                        if (delete) {
                            confirmDelete(gh, page(panel));
                        }
                    } else if (click.isShiftClick()) {
                        if (info) {
                            user.closeInventory();
                            call("info", id);
                        }
                    } else if (click.isRightClick()) {
                        if (verify) {
                            user.closeInventory();
                            call("verify", id);
                        }
                    } else if (tp) {
                        user.closeInventory();
                        call("tp", id);
                    }
                    return true;
                }).build();
    }

    private void confirmDelete(Greenhouse gh, int page) {
        new DialogBuilder().title(user, K + "delete.title")
                .body(user, K + "delete.body", "[id]", AdminUtil.shortId(gh), "[owner]", AdminUtil.ownerName(addon, gh),
                        "[xyz]", AdminUtil.xyz(gh))
                .confirmation(DialogButton.of(user, K + "delete.confirm", u -> {
                    delete(gh);
                    open(page);
                }), DialogButton.of(user, K + "cancel", u -> open(page)))
                .build().show(user);
    }

    /**
     * Deletes a greenhouse record and says so.
     *
     * @param gh the greenhouse
     */
    void delete(Greenhouse gh) {
        if (addon.getManager().deleteById(gh.getUniqueId())) {
            user.sendMessage("greenhouses.commands.admin.delete.success", "[id]", AdminUtil.shortId(gh));
        } else {
            user.sendMessage("greenhouses.commands.admin.errors.unknown-id", "[id]", gh.getUniqueId());
        }
    }

    private void askOwner() {
        new DialogBuilder().title(user, K + "filter.title")
                .input(DialogField.text(user, "owner", K + "filter.label", Optional.ofNullable(owner).orElse(""))
                        .maxLength(16))
                .button(DialogButton.submit(user, K + "filter.apply",
                        (u, r) -> owner(r.getText("owner").orElse(null)).open(0)))
                .button(DialogButton.of(user, K + "cancel", u -> open(0)))
                .build().show(user);
    }
}
