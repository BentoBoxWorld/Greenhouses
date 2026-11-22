package world.bentobox.greenhouses.ui.admin.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PagedPanel;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PickerPanel;
import world.bentobox.bentobox.api.panels.builders.PagedPanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.MobHabitat;
import world.bentobox.greenhouses.greenhouse.RecipeDraft;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.ConversionSpec;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.MobSpec;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.PlantSpec;
import world.bentobox.greenhouses.managers.RecipeCodec;
import world.bentobox.greenhouses.managers.RecipeManager;
import world.bentobox.greenhouses.managers.RecipeManager.SaveResult;
import world.bentobox.greenhouses.ui.panel.Lore;

/**
 * The admin recipe editor: a list of recipes, an editor for one recipe, and pages for its
 * required blocks, plants, mobs and block conversions. Changes go into the admin's
 * {@link EditSession} and reach biomes.yml only when saved.
 * <p>
 * Panels come from BentoBox's panel API, forms from its dialog API ({@link Forms}), and
 * choosing a block, mob or biome from its {@link PickerPanel}.
 *
 * @author tastybento
 */
public class RecipeEditor {

    private static final String K = "greenhouses.editor.";
    private static final String NUMBER = TextVariables.NUMBER;

    // Editor slots
    static final int ICON = 1;
    static final int GENERAL = 3;
    static final int BIOME = 4;
    static final int COVERAGE = 5;
    static final int MOB_LIMITS = 7;
    static final int BLOCKS = 19;
    static final int PLANTS = 21;
    static final int MOBS = 23;
    static final int CONVERSIONS = 25;
    static final int BACK = 45;
    static final int DISCARD = 47;
    static final int SAVE = 49;
    static final int DUPLICATE = 51;
    static final int DELETE = 53;

    /** Control columns on list pages */
    static final int LIST_BACK = 1;
    static final int LIST_ADD = 4;
    static final int LIST_INFO = 7;

    private final Greenhouses addon;
    private final User user;

    /**
     * @param addon the addon
     * @param user  the admin
     */
    public RecipeEditor(Greenhouses addon, User user) {
        this.addon = addon;
        this.user = user;
    }

    private RecipeManager recipes() {
        return addon.getRecipes();
    }

    private String t(String key, String... vars) {
        return user.getTranslation(K + key, vars);
    }

    /**
     * @param lore translated lines of lore, each possibly long or holding line breaks
     * @return the lines wrapped so tooltips stay narrow
     */
    static List<String> wrap(List<String> lore) {
        return lore.stream().flatMap(l -> Lore.wrapLegacy(l).stream()).toList();
    }

    private static PanelItem item(Material icon, String name, List<String> lore, PanelItem.ClickHandler click) {
        PanelItemBuilder b = new PanelItemBuilder().icon(icon).name(name).description(wrap(lore));
        if (click != null) {
            b.clickHandler(click);
        }
        return b.build();
    }

    private static PanelItem.ClickHandler run(Runnable r) {
        return (panel, u, click, slot) -> {
            r.run();
            return true;
        };
    }

    /**
     * @param key a recipe key
     * @return how many greenhouses use it
     */
    long greenhousesUsing(String key) {
        return addon.getManager().getMap().getGreenhouses().stream().filter(g -> key.equals(g.getBiomeRecipeName()))
                .count();
    }

    // ------------------------------------------------------------------------
    // Recipe list
    // ------------------------------------------------------------------------

    /**
     * Shows every recipe; clicking one edits it.
     */
    public void openList() {
        PagedPanelBuilder b = new PagedPanelBuilder().user(user).name(t("list.title"));
        Optional<EditSession> mine = EditSession.get(user.getUniqueId());
        for (BiomeRecipe r : recipes().getBiomeRecipes()) {
            List<String> lore = new ArrayList<>();
            lore.add(t("list.key", "[key]", r.getName()));
            lore.add(t("list.biome", "[biome]", Util.prettifyText(r.getBiome().getKey().getKey())));
            lore.add(t("list.priority", NUMBER, String.valueOf(r.getPriority())));
            lore.add(t("list.used", NUMBER, String.valueOf(greenhousesUsing(r.getName()))));
            if (mine.filter(s -> s.getKey().equals(r.getName()) && s.isDirty()).isPresent()) {
                lore.add(t("list.unsaved"));
            }
            lore.add(t("list.click-edit"));
            b.item(item(r.getIcon(), r.getFriendlyName(), lore, run(() -> edit(r.getName()))));
        }
        b.control(LIST_ADD, item(Material.WRITABLE_BOOK, t("list.new.name"), List.of(t("list.new.description")),
                run(this::newRecipe)));
        b.build();
    }

    /**
     * Starts editing a recipe, resuming unsaved changes to it, or asking first if there are
     * unsaved changes to a different one.
     *
     * @param key the recipe key
     */
    public void edit(String key) {
        Optional<EditSession> current = EditSession.get(user.getUniqueId());
        if (current.isPresent() && current.get().isDirty()) {
            if (current.get().getKey().equals(key)) {
                user.sendMessage(K + "resuming", "[name]", current.get().getDraft().getFriendlyName());
                open(current.get());
                return;
            }
            String other = current.get().getDraft().getFriendlyName();
            new DialogBuilder().title(user, K + "dialog.switch.title")
                    .body(user, K + "dialog.switch.body", "[name]", other)
                    .confirmation(DialogButton.of(user, K + "dialog.switch.discard", u -> startFresh(key)),
                            DialogButton.of(user, K + "dialog.switch.keep", u -> open(current.get())))
                    .build().show(user);
            return;
        }
        startFresh(key);
    }

    private void startFresh(String key) {
        Optional<RecipeDraft> d = recipes().getDraft(key);
        if (d.isEmpty()) {
            user.sendMessage(K + "unknown-recipe", "[key]", key);
            EditSession.end(user.getUniqueId());
            return;
        }
        open(EditSession.start(user.getUniqueId(), new EditSession(d.get(), recipes().getRevision(key), false)));
    }

    private void newRecipe() {
        new Forms.NewKey(user, null, k -> recipes().getDraft(k).isPresent(), key -> {
            // A new recipe needs a biome before anything else makes sense
            PickerPanel.biomes(user).title(t("pick.biome")).onPick(biome -> {
                RecipeDraft d = new RecipeDraft(key);
                d.setBiome(biome);
                d.setFriendlyName(Util.prettifyText(key));
                d.setIcon(PickerPanel.biomeIcon(biome));
                open(EditSession.start(user.getUniqueId(), new EditSession(d, RecipeManager.NEW_RECIPE, true)));
            }).onCancel(this::openList).open();
        }).show(() -> {
        }, this::openList);
    }

    // ------------------------------------------------------------------------
    // Recipe editor
    // ------------------------------------------------------------------------

    /**
     * Shows the editor for a session.
     *
     * @param s the session
     */
    public void open(EditSession s) {
        RecipeDraft d = s.getDraft();
        String title = t("title", "[name]", d.getFriendlyName(), "[unsaved]", s.isDirty() ? t("unsaved-mark") : "");
        PanelBuilder pb = new PanelBuilder().user(user).name(title).size(54);
        pb.item(ICON, item(d.getIcon(), t("icon.name"), List.of(t("click-change")),
                run(() -> Pick.item(user, t("pick.icon"), m -> change(s, () -> d.setIcon(m)), () -> open(s)))));
        pb.item(GENERAL, item(Material.NAME_TAG, t("general.name"), generalLore(d),
                run(() -> new Forms.General(user, d).show(() -> changed(s), () -> open(s)))));
        pb.item(BIOME, item(PickerPanel.biomeIcon(d.getBiome()), t("biome.name"),
                List.of(t("biome.value", "[biome]", Util.prettifyText(d.getBiome().getKey().getKey())), t("click-change")),
                run(() -> PickerPanel.biomes(user).title(t("pick.biome"))
                        .onPick(b -> change(s, () -> d.setBiome(b))).onCancel(() -> open(s)).open())));
        pb.item(COVERAGE, item(Material.WATER_BUCKET, t("coverage.name"), coverageLore(d),
                run(() -> new Forms.Coverage(user, d).show(() -> changed(s), () -> open(s)))));
        pb.item(MOB_LIMITS, item(Material.SPAWNER, t("mob-limits.name"), mobLimitLore(d),
                run(() -> new Forms.MobLimits(user, d).show(() -> changed(s), () -> open(s)))));
        pb.item(BLOCKS, item(Material.CHEST, t("blocks.name"), summary(d.getRequired().size()),
                run(() -> openBlocks(s, 0))));
        pb.item(PLANTS, item(Material.POPPY, t("plants.name"),
                summary(d.getPlants().size(), d.getPlants().stream().mapToDouble(PlantSpec::chance).sum()),
                run(() -> openPlants(s, 0))));
        pb.item(MOBS, item(Material.ZOMBIE_HEAD, t("mobs.name"),
                summary(d.getMobs().size(), d.getMobs().stream().mapToDouble(MobSpec::chance).sum()),
                run(() -> openMobs(s, 0))));
        pb.item(CONVERSIONS, item(Material.GRINDSTONE, t("conversions.name"), summary(d.getConversions().size()),
                run(() -> openConversions(s, 0))));
        pb.item(BACK, item(Material.OAK_DOOR, t("back.name"), List.of(t("back.description")), run(this::openList)));
        pb.item(DISCARD, item(Material.BARRIER, t("discard.name"), List.of(t("discard.description")),
                run(() -> discard(s))));
        List<String> problems = d.validate();
        List<String> saveLore = new ArrayList<>();
        if (!s.isDirty()) {
            saveLore.add(t("save.nothing"));
        } else if (problems.isEmpty()) {
            saveLore.add(t("save.description", NUMBER, String.valueOf(greenhousesUsing(d.getKey()))));
        } else {
            saveLore.add(t("save.problems"));
            problems.forEach(p -> saveLore.add(t("save.problem", "[problem]", p)));
        }
        pb.item(SAVE, item(problems.isEmpty() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, t("save.name"),
                saveLore, run(() -> save(s))));
        pb.item(DUPLICATE, item(Material.PAPER, t("duplicate.name"), List.of(t("duplicate.description")),
                run(() -> duplicate(s))));
        if (!s.isNew()) {
            pb.item(DELETE, item(Material.TNT, t("delete.name"),
                    List.of(t("delete.description", NUMBER, String.valueOf(greenhousesUsing(d.getKey())))),
                    run(() -> delete(s))));
        }
        pb.build();
    }

    private List<String> generalLore(RecipeDraft d) {
        List<String> lore = new ArrayList<>();
        lore.add(t("general.value-name", "[value]", d.getFriendlyName()));
        lore.add(t("general.value-priority", NUMBER, String.valueOf(d.getPriority())));
        lore.add(t("general.value-permission", "[value]", d.getPermission().isEmpty() ? t("none") : d.getPermission()));
        lore.add(t(d.getDescription().isEmpty() ? "general.no-description" : "general.has-description"));
        lore.add(t("click-change"));
        return lore;
    }

    private List<String> coverageLore(RecipeDraft d) {
        return List.of(coverageLine("water", d.getWaterCoverage()), coverageLine("lava", d.getLavaCoverage()),
                coverageLine("ice", d.getIceCoverage()), t("click-change"));
    }

    private String coverageLine(String what, int value) {
        String v = value < 0 ? t("coverage.unchecked") : value == 0 ? t("coverage.none") : t("coverage.at-least", NUMBER,
                String.valueOf(value));
        return t("coverage." + what, "[value]", v);
    }

    private List<String> mobLimitLore(RecipeDraft d) {
        List<String> lore = new ArrayList<>();
        lore.add(t("mob-limits.ground", NUMBER, String.valueOf(d.getMobLimit())));
        for (MobHabitat h : List.of(MobHabitat.WATER, MobHabitat.LAVA, MobHabitat.AIR)) {
            int v = d.getHabitatLimit(h);
            lore.add(t("mob-limits." + h.name().toLowerCase(java.util.Locale.ENGLISH), "[value]",
                    v == 0 ? t("mob-limits.shared") : String.valueOf(v)));
        }
        lore.add(t("mob-limits.max", "[value]", d.getMaxMobs() < 0 ? t("mob-limits.no-max") : String.valueOf(d.getMaxMobs())));
        lore.add(t("click-change"));
        return lore;
    }

    private List<String> summary(int count) {
        return List.of(t("entries", NUMBER, String.valueOf(count)), t("click-open"));
    }

    private List<String> summary(int count, double totalChance) {
        return List.of(t("entries", NUMBER, String.valueOf(count)),
                t("total-chance", "[value]", RecipeCodec.number(Math.round(totalChance * 1000) / 1000D)),
                t("click-open"));
    }

    private void change(EditSession s, Runnable r) {
        r.run();
        changed(s);
    }

    private void changed(EditSession s) {
        s.changed();
        open(s);
    }

    // ------------------------------------------------------------------------
    // Save, discard, duplicate, delete
    // ------------------------------------------------------------------------

    void save(EditSession s) {
        if (!s.isDirty()) {
            open(s);
            return;
        }
        SaveResult result = recipes().save(s.getDraft(), s.getRevision());
        switch (result.status()) {
        case SAVED -> {
            user.sendMessage(K + "saved", "[name]", s.getDraft().getFriendlyName(), NUMBER,
                    String.valueOf(greenhousesUsing(s.getKey())));
            // Carry on editing the saved recipe
            startFresh(s.getKey());
        }
        case INVALID -> {
            result.problems().forEach(p -> user.sendMessage(K + "save.problem", "[problem]", p));
            open(s);
        }
        case CONFLICT -> new DialogBuilder().title(user, K + "dialog.conflict.title")
                .body(user, K + "dialog.conflict.body", "[name]", s.getDraft().getFriendlyName())
                .button(DialogButton.of(user, K + "dialog.conflict.overwrite", u -> {
                    s.setRevision(recipes().getRevision(s.getKey()));
                    save(s);
                })).button(DialogButton.of(user, K + "dialog.conflict.theirs", u -> {
                    EditSession.end(user.getUniqueId());
                    startFresh(s.getKey());
                })).button(DialogButton.of(user, K + "dialog.cancel", u -> open(s))).build().show(user);
        case ERROR -> {
            user.sendMessage(K + "save-failed");
            open(s);
        }
        }
    }

    private void discard(EditSession s) {
        if (!s.isDirty()) {
            EditSession.end(user.getUniqueId());
            openList();
            return;
        }
        new DialogBuilder().title(user, K + "dialog.discard.title")
                .body(user, K + "dialog.discard.body", "[name]", s.getDraft().getFriendlyName())
                .confirmation(DialogButton.of(user, K + "dialog.discard.confirm", u -> {
                    EditSession.end(user.getUniqueId());
                    openList();
                }), DialogButton.of(user, K + "dialog.cancel", u -> open(s))).build().show(user);
    }

    private void duplicate(EditSession s) {
        new Forms.NewKey(user, s.getKey() + "_copy", k -> recipes().getDraft(k).isPresent(), key -> {
            RecipeDraft copy = RecipeDraft.copyAs(s.getDraft(), key);
            open(EditSession.start(user.getUniqueId(), new EditSession(copy, RecipeManager.NEW_RECIPE, true)));
        }).show(() -> {
        }, () -> open(s));
    }

    private void delete(EditSession s) {
        long used = greenhousesUsing(s.getKey());
        new DialogBuilder().title(user, K + "dialog.delete.title")
                .body(user, K + "dialog.delete.body", "[name]", s.getDraft().getFriendlyName(), NUMBER, String.valueOf(used))
                .confirmation(DialogButton.of(user, K + "dialog.delete.confirm", u -> {
                    if (recipes().delete(s.getKey())) {
                        EditSession.end(user.getUniqueId());
                        // Greenhouses of this type can no longer load; list them as unloaded
                        addon.getManager().reload();
                        user.sendMessage(K + "deleted", "[name]", s.getDraft().getFriendlyName(), NUMBER,
                                String.valueOf(used));
                        openList();
                    } else {
                        user.sendMessage(K + "save-failed");
                        open(s);
                    }
                }), DialogButton.of(user, K + "dialog.cancel", u -> open(s))).build().show(user);
    }

    // ------------------------------------------------------------------------
    // List pages
    // ------------------------------------------------------------------------

    private PagedPanelBuilder listPage(EditSession s, String what, int page, List<String> info, Runnable add) {
        return new PagedPanelBuilder().user(user).page(page)
                .name(t("list-title", "[what]", t(what + ".name"), "[name]", s.getDraft().getFriendlyName()))
                .control(LIST_BACK, item(Material.OAK_DOOR, t("back-to-recipe"), List.of(), run(() -> open(s))))
                .control(LIST_ADD, item(Material.LIME_DYE, t(what + ".add"), List.of(), run(add)))
                .control(LIST_INFO, item(Material.BOOK, t("help"), info, null));
    }

    private static int page(Panel panel) {
        return panel instanceof PagedPanel p ? p.getPage() : 0;
    }

    void openBlocks(EditSession s, int page) {
        Map<Material, Integer> req = s.getDraft().getRequired();
        PagedPanelBuilder b = listPage(s, "blocks", page, List.of(t("blocks.help")),
                () -> Pick.blockOrLiquid(user, t("pick.block"), m -> {
                    req.merge(m, 1, Integer::sum);
                    s.changed();
                    openBlocks(s, page);
                }, () -> openBlocks(s, page)));
        req.forEach((m, n) -> b.item(new PanelItemBuilder().icon(Pick.icon(m)).amount(Math.clamp(n, 1, 64))
                .name(t("blocks.entry", "[block]", Pick.name(m), NUMBER, String.valueOf(n)))
                .description(wrap(List.of(t("blocks.entry-help")))).clickHandler((panel, u, click, slot) -> {
                    blockClick(s, m, click, page(panel));
                    return true;
                }).build()));
        b.build();
    }

    /**
     * What a click on a required block does: left +1, right -1, shift to type a number, drop to remove.
     */
    void blockClick(EditSession s, Material m, ClickType click, int page) {
        Map<Material, Integer> req = s.getDraft().getRequired();
        switch (click) {
        case LEFT -> req.merge(m, 1, Integer::sum);
        case RIGHT -> req.computeIfPresent(m, (k, v) -> Math.max(1, v - 1));
        case SHIFT_LEFT, SHIFT_RIGHT -> {
            new Forms.Count(user, Pick.name(m), req.getOrDefault(m, 1), n -> req.put(m, n))
                    .show(() -> afterListChange(s, () -> openBlocks(s, page)), () -> openBlocks(s, page));
            return;
        }
        case DROP, CONTROL_DROP -> req.remove(m);
        default -> {
            return;
        }
        }
        afterListChange(s, () -> openBlocks(s, page));
    }

    void openPlants(EditSession s, int page) {
        List<PlantSpec> plants = s.getDraft().getPlants();
        PagedPanelBuilder b = listPage(s, "plants", page,
                List.of(t("plants.help"), t("total-chance", "[value]", total(plants.stream().mapToDouble(PlantSpec::chance)))),
                () -> addPlant(s, page));
        for (int i = 0; i < plants.size(); i++) {
            PlantSpec p = plants.get(i);
            int index = i;
            b.item(new PanelItemBuilder().icon(Pick.icon(p.plant())).name(Pick.name(p.plant()))
                    .description(wrap(List.of(t("chance", "[value]", RecipeCodec.number(p.chance())),
                            t("plants.grows-on", "[block]", Pick.name(p.growOn())), t("plants.entry-help"))))
                    .clickHandler((panel, u, click, slot) -> {
                        plantClick(s, index, click, page(panel));
                        return true;
                    }).build());
        }
        b.build();
    }

    void plantClick(EditSession s, int index, ClickType click, int page) {
        List<PlantSpec> plants = s.getDraft().getPlants();
        PlantSpec p = plants.get(index);
        Runnable back = () -> openPlants(s, page);
        switch (click) {
        case LEFT -> new Forms.Chance(user, Pick.name(p.plant()), p.chance(),
                c -> plants.set(index, new PlantSpec(p.plant(), c, p.growOn())))
                .show(() -> afterListChange(s, back), back);
        case RIGHT -> Pick.blockOrLiquid(user, t("pick.grows-on"), m -> {
            plants.set(index, new PlantSpec(p.plant(), p.chance(), m));
            afterListChange(s, back);
        }, back);
        case DROP, CONTROL_DROP -> {
            plants.remove(index);
            afterListChange(s, back);
        }
        default -> {
            // Nothing
        }
        }
    }

    private void addPlant(EditSession s, int page) {
        Runnable back = () -> openPlants(s, page);
        Pick.item(user, t("pick.plant"), plant -> Pick.blockOrLiquid(user, t("pick.grows-on"),
                on -> new Forms.Chance(user, Pick.name(plant), 10, c -> s.getDraft().getPlants()
                        .add(new PlantSpec(plant, c, on))).show(() -> afterListChange(s, back), back), back), back);
    }

    void openMobs(EditSession s, int page) {
        List<MobSpec> mobs = s.getDraft().getMobs();
        PagedPanelBuilder b = listPage(s, "mobs", page,
                List.of(t("mobs.help"), t("total-chance", "[value]", total(mobs.stream().mapToDouble(MobSpec::chance)))),
                () -> addMob(s, page));
        for (int i = 0; i < mobs.size(); i++) {
            MobSpec m = mobs.get(i);
            int index = i;
            List<String> lore = new ArrayList<>();
            lore.add(t("chance", "[value]", RecipeCodec.number(m.chance())));
            lore.add(t("mobs.spawns-on", "[block]", Pick.name(m.spawnOn())));
            if (m.data() != null) {
                lore.add(t("mobs.variant", "[value]", Util.prettifyText(m.data())));
            }
            lore.add(t("mobs.entry-help"));
            b.item(new PanelItemBuilder().icon(egg(m.type())).name(Util.prettifyText(m.type().name())).description(wrap(lore))
                    .clickHandler((panel, u, click, slot) -> {
                        mobClick(s, index, click, page(panel));
                        return true;
                    }).build());
        }
        b.build();
    }

    void mobClick(EditSession s, int index, ClickType click, int page) {
        List<MobSpec> mobs = s.getDraft().getMobs();
        MobSpec m = mobs.get(index);
        Runnable back = () -> openMobs(s, page);
        switch (click) {
        case LEFT -> new Forms.Mob(user, s.getDraft(), index).show(() -> afterListChange(s, back), back);
        case RIGHT -> Pick.blockOrLiquid(user, t("pick.spawns-on"), on -> {
            mobs.set(index, new MobSpec(m.type(), m.data(), m.chance(), on));
            afterListChange(s, back);
        }, back);
        case DROP, CONTROL_DROP -> {
            mobs.remove(index);
            afterListChange(s, back);
        }
        default -> {
            // Nothing
        }
        }
    }

    private void addMob(EditSession s, int page) {
        Runnable back = () -> openMobs(s, page);
        PickerPanel.entities(user).title(t("pick.mob")).onCancel(back).onPick(type -> Pick.blockOrLiquid(user,
                t("pick.spawns-on"), on -> new Forms.Chance(user, Util.prettifyText(type.name()), 10,
                        c -> s.getDraft().getMobs().add(new MobSpec(type, null, c, on)))
                        .show(() -> afterListChange(s, back), back), back)).open();
    }

    static Material egg(EntityType type) {
        Material egg = Material.matchMaterial(type.name() + "_SPAWN_EGG");
        return egg == null ? Material.NAME_TAG : egg;
    }

    void openConversions(EditSession s, int page) {
        List<ConversionSpec> list = s.getDraft().getConversions();
        PagedPanelBuilder b = listPage(s, "conversions", page, List.of(t("conversions.help")),
                () -> addConversion(s, page));
        for (int i = 0; i < list.size(); i++) {
            ConversionSpec c = list.get(i);
            int index = i;
            List<String> lore = new ArrayList<>();
            lore.add(t("conversions.to", "[block]", Pick.name(c.to())));
            lore.add(t("chance", "[value]", RecipeCodec.number(c.chance())));
            lore.add(c.adjacent() == null ? t("conversions.anywhere")
                    : t("conversions.next-to", "[block]", Pick.name(c.adjacent())));
            lore.add(t("conversions.entry-help"));
            b.item(new PanelItemBuilder().icon(Pick.icon(c.from())).name(Pick.name(c.from())).description(wrap(lore))
                    .clickHandler((panel, u, click, slot) -> {
                        conversionClick(s, index, click, page(panel));
                        return true;
                    }).build());
        }
        b.build();
    }

    void conversionClick(EditSession s, int index, ClickType click, int page) {
        List<ConversionSpec> list = s.getDraft().getConversions();
        ConversionSpec c = list.get(index);
        Runnable back = () -> openConversions(s, page);
        switch (click) {
        case LEFT -> new Forms.Chance(user, Pick.name(c.from()), c.chance(),
                v -> list.set(index, new ConversionSpec(c.from(), v, c.to(), c.adjacent())))
                .show(() -> afterListChange(s, back), back);
        case RIGHT -> Pick.blockOrLiquid(user, t("pick.convert-to"), m -> {
            list.set(index, new ConversionSpec(c.from(), c.chance(), m, c.adjacent()));
            afterListChange(s, back);
        }, back);
        case SHIFT_RIGHT -> Pick.blockOrLiquid(user, t("pick.next-to"), m -> {
            list.set(index, new ConversionSpec(c.from(), c.chance(), c.to(), m));
            afterListChange(s, back);
        }, back);
        case SHIFT_LEFT -> {
            list.set(index, new ConversionSpec(c.from(), c.chance(), c.to(), null));
            afterListChange(s, back);
        }
        case DROP, CONTROL_DROP -> {
            list.remove(index);
            afterListChange(s, back);
        }
        default -> {
            // Nothing
        }
        }
    }

    private void addConversion(EditSession s, int page) {
        Runnable back = () -> openConversions(s, page);
        Pick.blockOrLiquid(user, t("pick.convert-from"), from -> Pick.blockOrLiquid(user, t("pick.convert-to"),
                to -> new Forms.Chance(user, Pick.name(from), 10,
                        c -> s.getDraft().getConversions().add(new ConversionSpec(from, c, to, null)))
                        .show(() -> afterListChange(s, back), back), back), back);
    }

    private void afterListChange(EditSession s, Runnable reopen) {
        s.changed();
        reopen.run();
    }

    private static String total(java.util.stream.DoubleStream chances) {
        return RecipeCodec.number(Math.round(chances.sum() * 1000) / 1000D);
    }

    /**
     * @return the admin
     */
    @Nullable
    User getUser() {
        return user;
    }
}
