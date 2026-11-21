package world.bentobox.greenhouses.ui.admin.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.bukkit.entity.EntityType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.dialogs.DialogField;
import world.bentobox.bentobox.api.dialogs.DialogField.Choice.Option;
import world.bentobox.bentobox.api.dialogs.DialogResponse;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.greenhouse.MobHabitat;
import world.bentobox.greenhouses.greenhouse.RecipeDraft;
import world.bentobox.greenhouses.managers.RecipeCodec;

/**
 * The dialog forms of the recipe editor. Each form knows its fields and how to apply what was
 * entered to a draft; applying checks everything first and changes nothing if anything is
 * wrong, so the form can be shown again with the problems listed.
 *
 * @author tastybento
 */
public final class Forms {

    static final String KEY = "greenhouses.editor.";
    private static final String F = KEY + "form.";

    private Forms() {
        // Utility class
    }

    /**
     * A form: some fields, and what to do with the answers.
     */
    public abstract static class Form {
        protected final User user;

        /**
         * @param user the admin filling it in
         */
        protected Form(User user) {
            this.user = user;
        }

        /**
         * @return the locale key of the title
         */
        protected abstract String title();

        /**
         * @return the fields, filled in with the current values
         */
        protected abstract List<DialogField> fields();

        /**
         * Applies the answers if they are all valid.
         *
         * @param r the answers
         * @return what is wrong, translated for the user; empty if the answers were applied
         */
        public abstract List<String> apply(DialogResponse r);

        /**
         * Shows the form.
         *
         * @param onDone   run after valid answers have been applied
         * @param onCancel run if the admin cancels
         */
        public void show(Runnable onDone, Runnable onCancel) {
            show(List.of(), onDone, onCancel);
        }

        private void show(List<String> problems, Runnable onDone, Runnable onCancel) {
            DialogBuilder b = new DialogBuilder().title(user, title());
            problems.forEach(p -> b.body(Util.parseMiniMessageOrLegacy(user.getTranslation(F + "problem", "[problem]", p))));
            fields().forEach(b::input);
            b.button(DialogButton.submit(user, F + "ok", (u, r) -> {
                List<String> wrong = apply(r);
                if (wrong.isEmpty()) {
                    onDone.run();
                } else {
                    show(wrong, onDone, onCancel);
                }
            })).button(DialogButton.of(user, F + "cancel", u -> onCancel.run()));
            b.build().show(user);
        }

        protected String t(String key, String... vars) {
            return user.getTranslation(F + key, vars);
        }
    }

    // ------------------------------------------------------------------------
    // Reading answers
    // ------------------------------------------------------------------------

    /**
     * @param r     answers
     * @param key   field key
     * @param probs where to add a problem if it is not a valid percentage
     * @param what  what the field is, for the problem
     * @param user  the user, for translating
     * @return the chance, if valid
     */
    static Optional<Double> chance(DialogResponse r, String key, List<String> probs, String what, User user) {
        String text = r.getText(key).orElse("").trim().replace("%", "").replace(',', '.');
        try {
            double d = Double.parseDouble(text);
            if (d > 0 && d <= 100) {
                return Optional.of(d);
            }
        } catch (NumberFormatException e) {
            // Reported below
        }
        probs.add(user.getTranslation(F + "bad-chance", "[what]", what, "[value]", text));
        return Optional.empty();
    }

    static int intOf(DialogResponse r, String key, int fallback) {
        return r.getInt(key).orElse(fallback);
    }

    // ------------------------------------------------------------------------
    // Forms
    // ------------------------------------------------------------------------

    /**
     * Name, description, priority and permission.
     */
    public static class General extends Form {
        private final RecipeDraft d;

        public General(User user, RecipeDraft d) {
            super(user);
            this.d = d;
        }

        @Override
        protected String title() {
            return F + "general.title";
        }

        @Override
        protected List<DialogField> fields() {
            return List.of(DialogField.text(user, "name", F + "general.name", d.getFriendlyName()).maxLength(128).width(300),
                    DialogField.text(user, "description", F + "general.description", d.getDescription()).maxLength(512)
                            .multiline(5).width(300),
                    DialogField.number(user, "priority", F + "general.priority", 0, 100, d.getPriority()).step(1),
                    DialogField.text(user, "permission", F + "general.permission", d.getPermission()).maxLength(64)
                            .width(300));
        }

        @Override
        public List<String> apply(DialogResponse r) {
            List<String> probs = new ArrayList<>();
            String name = r.getText("name").orElse("").strip();
            String permission = r.getText("permission").orElse("").strip();
            if (name.isEmpty()) {
                probs.add(t("general.no-name"));
            }
            if (permission.contains(" ")) {
                probs.add(t("general.bad-permission"));
            }
            if (probs.isEmpty()) {
                d.setFriendlyName(name);
                d.setDescription(r.getText("description").orElse("").strip());
                d.setPriority(intOf(r, "priority", d.getPriority()));
                d.setPermission(permission);
            }
            return probs;
        }
    }

    /** Coverage choices */
    static final String UNCHECKED = "unchecked";
    static final String NONE = "none";
    static final String AT_LEAST = "atleast";

    /**
     * Water, lava and ice coverage: each not checked, not allowed, or at least a percentage.
     */
    public static class Coverage extends Form {
        private final RecipeDraft d;

        public Coverage(User user, RecipeDraft d) {
            super(user);
            this.d = d;
        }

        @Override
        protected String title() {
            return F + "coverage.title";
        }

        @Override
        protected List<DialogField> fields() {
            List<DialogField> f = new ArrayList<>();
            addCoverage(f, "water", d.getWaterCoverage());
            addCoverage(f, "lava", d.getLavaCoverage());
            addCoverage(f, "ice", d.getIceCoverage());
            return f;
        }

        private void addCoverage(List<DialogField> f, String what, int value) {
            String mode = value < 0 ? UNCHECKED : value == 0 ? NONE : AT_LEAST;
            List<Option> options = List.of(Option.of(user, UNCHECKED, F + "coverage." + UNCHECKED),
                    Option.of(user, NONE, F + "coverage." + NONE), Option.of(user, AT_LEAST, F + "coverage." + AT_LEAST));
            f.add(DialogField.choice(user, what + "_mode", F + "coverage." + what, options, mode).width(250));
            f.add(DialogField.number(user, what, F + "coverage.percent", 1, 100, Math.max(1, value)).step(1).width(250));
        }

        @Override
        public List<String> apply(DialogResponse r) {
            List<String> probs = new ArrayList<>();
            int water = coverage(r, "water", d.getWaterCoverage());
            int lava = coverage(r, "lava", d.getLavaCoverage());
            int ice = coverage(r, "ice", d.getIceCoverage());
            if (Math.max(0, water) + Math.max(0, lava) + Math.max(0, ice) > 100) {
                probs.add(t("coverage.too-much"));
            } else {
                d.setWaterCoverage(water);
                d.setLavaCoverage(lava);
                d.setIceCoverage(ice);
            }
            return probs;
        }

        private static int coverage(DialogResponse r, String what, int current) {
            return switch (r.getChoice(what + "_mode").orElse("")) {
            case UNCHECKED -> RecipeDraft.UNSET;
            case NONE -> 0;
            case AT_LEAST -> Math.clamp(intOf(r, what, Math.max(1, current)), 1, 100);
            default -> current;
            };
        }
    }

    /**
     * Blocks per mob for each habitat, and the most mobs at once.
     */
    public static class MobLimits extends Form {
        private final RecipeDraft d;

        public MobLimits(User user, RecipeDraft d) {
            super(user);
            this.d = d;
        }

        @Override
        protected String title() {
            return F + "mobs.title";
        }

        @Override
        protected List<DialogField> fields() {
            return List.of(
                    DialogField.number(user, "ground", F + "mobs.ground", 1, 100, d.getMobLimit()).step(1).width(250),
                    DialogField.number(user, "water", F + "mobs.water", 0, 200, d.getHabitatLimit(MobHabitat.WATER))
                            .step(1).width(250),
                    DialogField.number(user, "lava", F + "mobs.lava", 0, 200, d.getHabitatLimit(MobHabitat.LAVA))
                            .step(1).width(250),
                    DialogField.number(user, "air", F + "mobs.air", 0, 200, d.getHabitatLimit(MobHabitat.AIR)).step(1)
                            .width(250),
                    DialogField.number(user, "max", F + "mobs.max", -1, 200, d.getMaxMobs()).step(1).width(250));
        }

        @Override
        public List<String> apply(DialogResponse r) {
            d.setMobLimit(Math.max(1, intOf(r, "ground", d.getMobLimit())));
            d.setHabitatLimit(MobHabitat.WATER, Math.max(0, intOf(r, "water", d.getHabitatLimit(MobHabitat.WATER))));
            d.setHabitatLimit(MobHabitat.LAVA, Math.max(0, intOf(r, "lava", d.getHabitatLimit(MobHabitat.LAVA))));
            d.setHabitatLimit(MobHabitat.AIR, Math.max(0, intOf(r, "air", d.getHabitatLimit(MobHabitat.AIR))));
            d.setMaxMobs(Math.max(-1, intOf(r, "max", d.getMaxMobs())));
            return List.of();
        }
    }

    /**
     * A chance, as a percentage typed in, so tiny chances like 0.005% are possible.
     */
    public static class Chance extends Form {
        private final String what;
        private final double initial;
        private final Consumer<Double> set;

        /**
         * @param user    the admin
         * @param what    what the chance is for, shown in the title
         * @param initial the current chance
         * @param set     what to do with the new chance
         */
        public Chance(User user, String what, double initial, Consumer<Double> set) {
            super(user);
            this.what = what;
            this.initial = initial;
            this.set = set;
        }

        @Override
        protected String title() {
            return F + "chance.title";
        }

        @Override
        public void show(Runnable onDone, Runnable onCancel) {
            super.show(onDone, onCancel);
        }

        @Override
        protected List<DialogField> fields() {
            return List.of(DialogField.text("chance", Util.parseMiniMessageOrLegacy(t("chance.field", "[what]", what)),
                    RecipeCodec.number(initial)).maxLength(12));
        }

        @Override
        public List<String> apply(DialogResponse r) {
            List<String> probs = new ArrayList<>();
            chance(r, "chance", probs, what, user).ifPresent(set);
            return probs;
        }
    }

    /**
     * A mob's chance and, for mobs that have one, its variant.
     */
    public static class Mob extends Form {
        private final RecipeDraft d;
        private final int index;

        /**
         * @param user  the admin
         * @param d     the draft
         * @param index which mob
         */
        public Mob(User user, RecipeDraft d, int index) {
            super(user);
            this.d = d;
            this.index = index;
        }

        @Override
        protected String title() {
            return F + "mob.title";
        }

        @Override
        protected List<DialogField> fields() {
            RecipeDraft.MobSpec m = d.getMobs().get(index);
            List<DialogField> f = new ArrayList<>();
            f.add(DialogField.text("chance", Util.parseMiniMessageOrLegacy(
                    t("chance.field", "[what]", Util.prettifyText(m.type().name()))), RecipeCodec.number(m.chance()))
                    .maxLength(12));
            List<String> variants = RecipeCodec.mobData().get(m.type());
            if (variants != null) {
                List<Option> options = new ArrayList<>();
                options.add(Option.of(user, NONE, F + "mob.no-variant"));
                variants.forEach(v -> options.add(new Option(v, net.kyori.adventure.text.Component.text(Util.prettifyText(v)))));
                String current = m.data() == null ? NONE : m.data().toUpperCase(java.util.Locale.ENGLISH);
                if (options.stream().noneMatch(o -> o.id().equals(current))) {
                    options.add(new Option(current, net.kyori.adventure.text.Component.text(current)));
                }
                f.add(DialogField.choice(user, "variant", F + "mob.variant", options, current).width(250));
            }
            return f;
        }

        @Override
        public List<String> apply(DialogResponse r) {
            RecipeDraft.MobSpec m = d.getMobs().get(index);
            List<String> probs = new ArrayList<>();
            Optional<Double> c = chance(r, "chance", probs, Util.prettifyText(m.type().name()), user);
            if (probs.isEmpty()) {
                String variant = r.getChoice("variant").orElse(m.data() == null ? NONE : m.data());
                d.getMobs().set(index, new RecipeDraft.MobSpec(m.type(), NONE.equals(variant) ? null : variant,
                        c.orElseThrow(), m.spawnOn()));
            }
            return probs;
        }
    }

    /**
     * How many of a required block.
     */
    public static class Count extends Form {
        private final String what;
        private final int initial;
        private final Consumer<Integer> set;

        public Count(User user, String what, int initial, Consumer<Integer> set) {
            super(user);
            this.what = what;
            this.initial = initial;
            this.set = set;
        }

        @Override
        protected String title() {
            return F + "count.title";
        }

        @Override
        protected List<DialogField> fields() {
            return List.of(DialogField.number("count",
                    Util.parseMiniMessageOrLegacy(t("count.field", "[what]", what)), 1, 256, initial).step(1)
                    .width(300));
        }

        @Override
        public List<String> apply(DialogResponse r) {
            set.accept(Math.clamp(intOf(r, "count", initial), 1, 256));
            return List.of();
        }
    }

    /** What a recipe key may look like */
    static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");

    /**
     * The key for a new recipe, which can never be changed afterwards.
     */
    public static class NewKey extends Form {
        private final java.util.function.Predicate<String> taken;
        private final Consumer<String> set;
        private @Nullable String initial;

        /**
         * @param user    the admin
         * @param initial a suggestion, or null
         * @param taken   tells whether a key is already used
         * @param set     what to do with the key
         */
        public NewKey(User user, @Nullable String initial, java.util.function.Predicate<String> taken,
                Consumer<String> set) {
            super(user);
            this.initial = initial;
            this.taken = taken;
            this.set = set;
        }

        @Override
        protected String title() {
            return F + "key.title";
        }

        @Override
        protected List<DialogField> fields() {
            return List.of(DialogField.text(user, "key", F + "key.field", initial).maxLength(32).width(300));
        }

        @Override
        public List<String> apply(DialogResponse r) {
            String key = r.getText("key").orElse("").strip();
            initial = key;
            if (!KEY_PATTERN.matcher(key).matches()) {
                return List.of(t("key.bad"));
            }
            if (taken.test(key)) {
                return List.of(t("key.taken", "[key]", key));
            }
            set.accept(key);
            return List.of();
        }
    }

    /**
     * @param type an entity type
     * @return true if mobs of this type accept variant data
     */
    static boolean hasVariants(@NonNull EntityType type) {
        return RecipeCodec.mobData().containsKey(type);
    }
}
