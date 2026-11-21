package world.bentobox.greenhouses.managers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.MobHabitat;
import world.bentobox.greenhouses.greenhouse.RecipeDraft;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.ConversionSpec;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.MobSpec;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.PlantSpec;

/**
 * The one place that knows the biomes.yml recipe format. It reads a recipe section into a
 * {@link RecipeDraft}, writes a draft back into a section, and builds the live
 * {@link BiomeRecipe} from a draft.
 * <p>
 * Reading is forgiving: an entry that cannot be understood is reported and skipped, and the
 * rest of the recipe still loads. Writing changes only what differs from what is in the
 * section already, so comments, layout and unknown keys in a server's biomes.yml survive an
 * edit.
 *
 * @author tastybento
 */
public class RecipeCodec {

    static final String FRIENDLY_NAME = "friendlyname";
    static final String DESCRIPTION = "description";
    static final String BIOME = "biome";
    static final String ICON = "icon";
    static final String PRIORITY = "priority";
    static final String PERMISSION = "permission";
    static final String WATER = "watercoverage";
    static final String LAVA = "lavacoverage";
    static final String ICE = "icecoverage";
    static final String MAX_MOBS = "maxmobs";
    static final String CONTENTS = "contents";
    static final String PLANTS = "plants";
    static final String MOBS = "mobs";
    static final String CONVERSIONS = "conversions";
    static final String CONVERSION_LIST = "conversion-list";

    private static final String COULD_NOT_PARSE = "Could not parse ";
    private static final String UNKNOWN_MOB_DATA = "Unknown mob data: %s.";

    private final @Nullable Greenhouses addon;
    private final Consumer<String> warn;

    /**
     * @param addon the addon, used to build live recipes and to report problems; may be null
     *              when only reading and writing drafts
     * @param warn  where to report entries that cannot be read
     */
    public RecipeCodec(@Nullable Greenhouses addon, @NonNull Consumer<String> warn) {
        this.addon = addon;
        this.warn = warn;
    }

    // ------------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------------

    /**
     * Reads a recipe.
     *
     * @param key the recipe's key
     * @param s   its section of biomes.yml
     * @return the draft, or null if the recipe cannot be used at all (no or unknown biome)
     */
    @Nullable
    public RecipeDraft read(@NonNull String key, @NonNull ConfigurationSection s) {
        Biome biome = readBiome(key, s);
        if (biome == null) {
            return null;
        }
        RecipeDraft d = new RecipeDraft(key);
        d.setBiome(biome);
        d.setFriendlyName(toMiniMessage(s.getString(FRIENDLY_NAME, Util.prettifyText(key))));
        d.setDescription(toMiniMessage(s.getString(DESCRIPTION, "")));
        String icon = s.getString(ICON, "");
        Material m = Material.matchMaterial(icon);
        if (m == null || !m.isItem()) {
            warn.accept("Unknown icon '" + icon + "' in biome recipe " + key + " - using an oak sapling");
            m = Material.OAK_SAPLING;
        }
        d.setIcon(m);
        d.setPriority(s.getInt(PRIORITY, 0));
        d.setPermission(s.getString(PERMISSION, ""));
        // A zero coverage means NONE is allowed, e.g. a desert; missing means not checked
        d.setWaterCoverage(s.getInt(WATER, RecipeDraft.UNSET));
        d.setLavaCoverage(s.getInt(LAVA, RecipeDraft.UNSET));
        d.setIceCoverage(s.getInt(ICE, RecipeDraft.UNSET));
        d.setMobLimit(s.getInt(MobHabitat.GROUND.getConfigKey(), RecipeDraft.DEFAULT_MOB_LIMIT));
        for (MobHabitat habitat : MobHabitat.values()) {
            if (habitat != MobHabitat.GROUND) {
                d.setHabitatLimit(habitat, s.getInt(habitat.getConfigKey(), 0));
            }
        }
        d.setMaxMobs(s.getInt(MAX_MOBS, RecipeDraft.UNSET));
        readContents(key, s, d);
        readPlants(key, s, d);
        readMobs(key, s, d);
        readConversions(key, s, d);
        return d;
    }

    @SuppressWarnings("deprecation")
    private @Nullable Biome readBiome(String key, ConfigurationSection s) {
        if (!s.contains(BIOME)) {
            warn.accept("No biome defined in the biome recipe " + key + ". Skipping...");
            return null;
        }
        String name = Objects.requireNonNull(s.getString(BIOME)).toUpperCase(Locale.ENGLISH);
        if (name.equals("NETHER")) {
            return Biome.NETHER_WASTES;
        }
        try {
            return Biome.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            StringBuilder valid = new StringBuilder();
            org.bukkit.Registry.BIOME.forEach(b -> valid.append(' ').append(b.getKey().getKey()));
            warn.accept("Biome " + name + " in recipe " + key + " is invalid! Valid biomes are" + valid);
            return null;
        }
    }

    private void readContents(String key, ConfigurationSection s, RecipeDraft d) {
        ConfigurationSection c = s.getConfigurationSection(CONTENTS);
        if (c == null) {
            return;
        }
        for (String block : c.getKeys(false)) {
            Material m = Material.matchMaterial(block);
            if (m == null) {
                warn.accept("Could not parse required block " + block + " in recipe " + key);
            } else {
                d.getRequired().merge(m, c.getInt(block), Integer::sum);
            }
        }
    }

    private void readPlants(String key, ConfigurationSection s, RecipeDraft d) {
        ConfigurationSection c = s.getConfigurationSection(PLANTS);
        if (c == null) {
            return;
        }
        // Plant: % chance:block it grows on
        for (String plant : c.getKeys(false)) {
            try {
                String[] split = Objects.requireNonNull(c.getString(plant)).split(":");
                d.getPlants().add(new PlantSpec(material(plant), Double.parseDouble(split[0]), material(split[1])));
            } catch (Exception e) {
                warn.accept(COULD_NOT_PARSE + "plant " + plant + " in recipe " + key);
            }
        }
    }

    private void readMobs(String key, ConfigurationSection s, RecipeDraft d) {
        ConfigurationSection c = s.getConfigurationSection(MOBS);
        if (c == null) {
            return;
        }
        // Mob[#data]: % chance:block it spawns on
        for (String mob : c.getKeys(false)) {
            try {
                String[] name = mob.toUpperCase(Locale.ENGLISH).split("#", 2);
                EntityType type = EntityType.valueOf(name[0]);
                String[] split = Objects.requireNonNull(c.getString(mob)).split(":");
                d.getMobs().add(new MobSpec(type, name.length > 1 ? name[1] : null, Double.parseDouble(split[0]),
                        material(split[1])));
            } catch (Exception e) {
                warn.accept(COULD_NOT_PARSE + "mob " + mob + " in recipe " + key);
            }
        }
    }

    private void readConversions(String key, ConfigurationSection s, RecipeDraft d) {
        // Map form - from: % chance:to[:adjacent]
        ConfigurationSection c = s.getConfigurationSection(CONVERSIONS);
        if (c != null) {
            for (String from : c.getKeys(false)) {
                String value = c.getString(from, "");
                if (value.isEmpty()) {
                    continue;
                }
                parseConversion(key, from + ":" + value, d);
            }
        }
        // List form - from:% chance:to[:adjacent], which allows one block to convert in several ways
        for (String line : s.getStringList(CONVERSION_LIST)) {
            parseConversion(key, line, d);
        }
    }

    private void parseConversion(String key, String line, RecipeDraft d) {
        try {
            String[] split = line.split(":");
            d.getConversions().add(new ConversionSpec(material(split[0]), Double.parseDouble(split[1]),
                    material(split[2]), split.length > 3 ? material(split[3]) : null));
        } catch (Exception e) {
            warn.accept(COULD_NOT_PARSE + "conversion " + line + " in recipe " + key);
        }
    }

    private static Material material(String name) {
        Material m = Material.matchMaterial(name.trim());
        if (m == null) {
            throw new IllegalArgumentException("Unknown material " + name);
        }
        return m;
    }

    /**
     * Text in biomes.yml may use MiniMessage tags or old {@code &} color codes; old codes are
     * converted so everything downstream deals with MiniMessage only.
     *
     * @param text text from biomes.yml
     * @return MiniMessage text
     */
    static String toMiniMessage(String text) {
        return Util.isLegacyFormat(text) ? Util.legacyToMiniMessage(text) : text;
    }

    // ------------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------------

    /**
     * Writes a draft into its section of biomes.yml, changing only what differs, so comments,
     * layout and keys this codec does not know about are kept.
     *
     * @param d the draft
     * @param s its section, empty for a new recipe
     */
    public void write(@NonNull RecipeDraft d, @NonNull ConfigurationSection s) {
        // What the section says now, to compare against; reading quietly
        RecipeDraft now = new RecipeCodec(null, m -> {}).read(d.getKey(), s);
        if (now == null || !Objects.equals(now.getBiome(), d.getBiome())) {
            s.set(BIOME, d.getBiome().getKey().getKey().toUpperCase(Locale.ENGLISH));
        }
        if (now == null || !now.getFriendlyName().equals(d.getFriendlyName())) {
            s.set(FRIENDLY_NAME, blankToNull(d.getFriendlyName()));
        }
        if (now == null || !now.getDescription().equals(d.getDescription())) {
            s.set(DESCRIPTION, blankToNull(d.getDescription()));
        }
        if (now == null || now.getIcon() != d.getIcon()) {
            s.set(ICON, d.getIcon().name());
        }
        if (now == null || now.getPriority() != d.getPriority()) {
            s.set(PRIORITY, d.getPriority());
        }
        if (now == null || !now.getPermission().equals(d.getPermission())) {
            s.set(PERMISSION, blankToNull(d.getPermission()));
        }
        setUnlessUnset(s, WATER, now == null ? null : now.getWaterCoverage(), d.getWaterCoverage(), RecipeDraft.UNSET);
        setUnlessUnset(s, LAVA, now == null ? null : now.getLavaCoverage(), d.getLavaCoverage(), RecipeDraft.UNSET);
        setUnlessUnset(s, ICE, now == null ? null : now.getIceCoverage(), d.getIceCoverage(), RecipeDraft.UNSET);
        if (now == null || now.getMobLimit() != d.getMobLimit()) {
            s.set(MobHabitat.GROUND.getConfigKey(), d.getMobLimit());
        }
        for (MobHabitat h : MobHabitat.values()) {
            if (h != MobHabitat.GROUND) {
                setUnlessUnset(s, h.getConfigKey(), now == null ? null : now.getHabitatLimit(h),
                        d.getHabitatLimit(h), 0);
            }
        }
        setUnlessUnset(s, MAX_MOBS, now == null ? null : now.getMaxMobs(), d.getMaxMobs(), RecipeDraft.UNSET);
        if (now == null || !now.getRequired().equals(d.getRequired())) {
            replaceSection(s, CONTENTS, d.getRequired().isEmpty() ? null : section -> d.getRequired()
                    .forEach((m, n) -> section.set(m.name(), n)));
        }
        if (now == null || !now.getPlants().equals(d.getPlants())) {
            replaceSection(s, PLANTS, d.getPlants().isEmpty() ? null : section -> d.getPlants()
                    .forEach(p -> section.set(p.plant().name(), number(p.chance()) + ":" + p.growOn().name())));
        }
        if (now == null || !now.getMobs().equals(d.getMobs())) {
            replaceSection(s, MOBS, d.getMobs().isEmpty() ? null : section -> d.getMobs()
                    .forEach(m -> section.set(m.key(), number(m.chance()) + ":" + m.spawnOn().name())));
        }
        if (now == null || !now.getConversions().equals(d.getConversions())) {
            writeConversions(d, s);
        }
    }

    private void writeConversions(RecipeDraft d, ConfigurationSection s) {
        List<ConversionSpec> list = d.getConversions();
        Set<Material> from = new HashSet<>();
        boolean unique = list.stream().allMatch(c -> from.add(c.from()));
        // Keep the list form if the file used it, or if a block converts more than one way
        boolean asList = !unique || !s.getStringList(CONVERSION_LIST).isEmpty();
        if (asList) {
            s.set(CONVERSIONS, null);
            List<String> lines = new ArrayList<>();
            list.forEach(c -> lines.add(c.from().name() + ":" + number(c.chance()) + ":" + c.to().name()
                    + (c.adjacent() == null ? "" : ":" + c.adjacent().name())));
            s.set(CONVERSION_LIST, lines.isEmpty() ? null : lines);
        } else {
            s.set(CONVERSION_LIST, null);
            replaceSection(s, CONVERSIONS, list.isEmpty() ? null : section -> list.forEach(c -> section.set(
                    c.from().name(), number(c.chance()) + ":" + c.to().name()
                            + (c.adjacent() == null ? "" : ":" + c.adjacent().name()))));
        }
    }

    private static void setUnlessUnset(ConfigurationSection s, String path, @Nullable Integer now, int value,
            int unset) {
        if (now != null && now == value) {
            return;
        }
        s.set(path, value == unset ? null : value);
    }

    /**
     * Replaces a subsection's entries, keeping the subsection itself (and so its comments).
     */
    private static void replaceSection(ConfigurationSection s, String path,
            @Nullable Consumer<ConfigurationSection> fill) {
        if (fill == null) {
            s.set(path, null);
            return;
        }
        ConfigurationSection section = s.isConfigurationSection(path) ? s.getConfigurationSection(path)
                : s.createSection(path);
        for (String k : section.getKeys(false)) {
            section.set(k, null);
        }
        fill.accept(section);
    }

    private static @Nullable String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /**
     * @param d a number
     * @return it without a pointless ".0", e.g. 5 rather than 5.0
     */
    static String number(double d) {
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }

    // ------------------------------------------------------------------------
    // Building the live recipe
    // ------------------------------------------------------------------------

    /**
     * Builds the live recipe for a draft. Chances that do not fit are reported and dropped by
     * {@link BiomeRecipe} itself, as they always have been.
     *
     * @param d the draft
     * @return the recipe
     */
    @NonNull
    public BiomeRecipe toRecipe(@NonNull RecipeDraft d) {
        BiomeRecipe b = new BiomeRecipe(addon, d.getBiome(), d.getPriority());
        b.setName(d.getKey());
        b.setFriendlyName(d.getFriendlyName());
        b.setDescription(d.getDescription());
        b.setIcon(d.getIcon());
        b.setPermission(d.getPermission());
        b.setWatercoverage(d.getWaterCoverage());
        b.setLavacoverage(d.getLavaCoverage());
        b.setIcecoverage(d.getIceCoverage());
        b.setMobLimit(d.getMobLimit());
        for (MobHabitat habitat : MobHabitat.values()) {
            if (habitat != MobHabitat.GROUND) {
                b.setMobLimit(habitat, d.getHabitatLimit(habitat));
            }
        }
        b.setMaxMob(d.getMaxMobs());
        d.getRequired().forEach(b::addReqBlocks);
        d.getPlants().forEach(p -> b.addPlants(p.plant(), p.chance(), p.growOn()));
        d.getMobs().forEach(m -> b.addMobs(m.type(), m.chance(), m.spawnOn(), customizer(m)));
        d.getConversions().forEach(c -> b.addConvBlocks(c.from(), c.to(), c.chance(), c.adjacent()));
        return b;
    }

    /**
     * @param m a mob
     * @return what to do to it once spawned, from its data, or null for nothing
     */
    @Nullable
    Consumer<Entity> customizer(MobSpec m) {
        if (m.data() == null || m.data().isEmpty()) {
            return null;
        }
        String data = m.data().toUpperCase(Locale.ENGLISH);
        return switch (m.type()) {
        case CREEPER -> requireData("CHARGED", data, entity -> ((Creeper) entity).setPowered(true));
        case CHICKEN -> requireData("JOCKEY", data, entity -> {
            Zombie zombie = (Zombie) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.ZOMBIE);
            zombie.setBaby();
            entity.addPassenger(zombie);
        });
        case SHEEP -> requireData("RAINBOW", data, entity -> entity.setCustomName("jeb_"));
        case WOLF -> wolfVariant(data);
        default -> {
            warn.accept("Custom mob data for " + m.type() + " is not supported.");
            yield null;
        }
        };
    }

    private @Nullable Consumer<Entity> requireData(String expected, String data, Consumer<Entity> action) {
        if (expected.equals(data)) {
            return action;
        }
        warn.accept(String.format(UNKNOWN_MOB_DATA, data));
        return null;
    }

    private @Nullable Consumer<Entity> wolfVariant(String data) {
        Wolf.Variant variant = switch (data) {
        case "ASHEN" -> Wolf.Variant.ASHEN;
        case "BLACK" -> Wolf.Variant.BLACK;
        case "CHESTNUT" -> Wolf.Variant.CHESTNUT;
        case "PALE" -> Wolf.Variant.PALE;
        case "RUSTY" -> Wolf.Variant.RUSTY;
        case "SNOWY" -> Wolf.Variant.SNOWY;
        case "SPOTTED" -> Wolf.Variant.SPOTTED;
        case "STRIPED" -> Wolf.Variant.STRIPED;
        case "WOODS" -> Wolf.Variant.WOODS;
        default -> null;
        };
        if (variant == null) {
            warn.accept(String.format(UNKNOWN_MOB_DATA, data));
            return null;
        }
        return entity -> ((Wolf) entity).setVariant(variant);
    }

    /**
     * @return the wolf variants a mob entry may name, for editors
     */
    public static List<String> wolfVariants() {
        return List.of("ASHEN", "BLACK", "CHESTNUT", "PALE", "RUSTY", "SNOWY", "SPOTTED", "STRIPED", "WOODS");
    }

    /**
     * @return the custom data each mob type accepts, for editors
     */
    public static Map<EntityType, List<String>> mobData() {
        return Map.of(EntityType.CREEPER, List.of("CHARGED"), EntityType.CHICKEN, List.of("JOCKEY"),
                EntityType.SHEEP, List.of("RAINBOW"), EntityType.WOLF, wolfVariants());
    }
}
