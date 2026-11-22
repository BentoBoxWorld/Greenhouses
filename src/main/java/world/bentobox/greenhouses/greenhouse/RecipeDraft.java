package world.bentobox.greenhouses.greenhouse;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An editable copy of a biome recipe, as plain data: what biomes.yml says, with none of a live
 * {@link BiomeRecipe}'s machinery. Editors change a draft; saving it writes biomes.yml and
 * builds a fresh {@link BiomeRecipe} from it, so a live recipe is never changed in place.
 * <p>
 * Values that biomes.yml can leave out use the same "not set" values the loader has always
 * used: -1 for a coverage or {@code maxmobs} that is not checked, and 0 for a habitat mob limit
 * that shares {@code moblimit}.
 *
 * @author tastybento
 */
public class RecipeDraft {

    /** Coverage, max mobs: not checked */
    public static final int UNSET = -1;
    /** Default blocks per mob */
    public static final int DEFAULT_MOB_LIMIT = 9;

    /**
     * A plant that can grow.
     *
     * @param plant  the plant
     * @param chance % chance each time a plant is grown
     * @param growOn the block it grows on
     */
    public record PlantSpec(@NonNull Material plant, double chance, @NonNull Material growOn) {
    }

    /**
     * A mob that can spawn.
     *
     * @param type    the mob
     * @param data    extra data after a {@code #} in biomes.yml, e.g. {@code CHARGED} for a creeper,
     *                or null for none
     * @param chance  % chance each time a mob is spawned
     * @param spawnOn the block it spawns on
     */
    public record MobSpec(@NonNull EntityType type, @Nullable String data, double chance, @NonNull Material spawnOn) {

        /**
         * @return the key used in biomes.yml, e.g. {@code CREEPER#CHARGED}
         */
        public String key() {
            return data == null || data.isEmpty() ? type.name() : type.name() + "#" + data;
        }
    }

    /**
     * A block that slowly changes into another.
     *
     * @param from     the block that changes
     * @param chance   % chance each time it is checked
     * @param to       what it changes into
     * @param adjacent a block that must be next to it for it to change, or null for none
     */
    public record ConversionSpec(@NonNull Material from, double chance, @NonNull Material to,
            @Nullable Material adjacent) {
    }

    private final String key;
    private String friendlyName = "";
    private String description = "";
    private Biome biome;
    private Material icon = Material.OAK_SAPLING;
    private int priority;
    private String permission = "";
    private int waterCoverage = UNSET;
    private int lavaCoverage = UNSET;
    private int iceCoverage = UNSET;
    private int mobLimit = DEFAULT_MOB_LIMIT;
    private final Map<MobHabitat, Integer> habitatLimits = new EnumMap<>(MobHabitat.class);
    private int maxMobs = UNSET;
    private final Map<Material, Integer> required = new LinkedHashMap<>();
    private final List<PlantSpec> plants = new ArrayList<>();
    private final List<MobSpec> mobs = new ArrayList<>();
    private final List<ConversionSpec> conversions = new ArrayList<>();

    /**
     * @param key the recipe's key in biomes.yml, which never changes once created
     */
    public RecipeDraft(@NonNull String key) {
        this.key = Objects.requireNonNull(key);
    }

    /**
     * @param other a draft to copy
     * @return a deep copy, so changes to one do not touch the other
     */
    public static RecipeDraft copyOf(RecipeDraft other) {
        return copyAs(other, other.key);
    }

    /**
     * @param other a draft to copy
     * @param key   the key for the copy, e.g. to duplicate a recipe under a new name
     * @return a deep copy with that key
     */
    public static RecipeDraft copyAs(RecipeDraft other, String key) {
        RecipeDraft d = new RecipeDraft(key);
        d.friendlyName = other.friendlyName;
        d.description = other.description;
        d.biome = other.biome;
        d.icon = other.icon;
        d.priority = other.priority;
        d.permission = other.permission;
        d.waterCoverage = other.waterCoverage;
        d.lavaCoverage = other.lavaCoverage;
        d.iceCoverage = other.iceCoverage;
        d.mobLimit = other.mobLimit;
        d.habitatLimits.putAll(other.habitatLimits);
        d.maxMobs = other.maxMobs;
        d.required.putAll(other.required);
        d.plants.addAll(other.plants);
        d.mobs.addAll(other.mobs);
        d.conversions.addAll(other.conversions);
        return d;
    }

    /**
     * Checks the draft can be saved.
     *
     * @return what is wrong, in plain English for the admin; empty if nothing
     */
    public List<String> validate() {
        List<String> problems = new ArrayList<>();
        if (biome == null) {
            problems.add("No biome");
        }
        if (icon == null || !icon.isItem()) {
            problems.add("The icon must be an item");
        }
        checkCoverage(problems, "Water", waterCoverage);
        checkCoverage(problems, "Lava", lavaCoverage);
        checkCoverage(problems, "Ice", iceCoverage);
        if (Math.max(0, waterCoverage) + Math.max(0, lavaCoverage) + Math.max(0, iceCoverage) > 100) {
            problems.add("Water, lava and ice together need more than the whole floor");
        }
        if (mobLimit < 1) {
            problems.add("Blocks per mob must be at least 1");
        }
        habitatLimits.forEach((h, v) -> {
            if (v < 0) {
                problems.add(h.getConfigKey() + " cannot be negative");
            }
        });
        if (maxMobs < UNSET) {
            problems.add("Max mobs must be -1 (no maximum) or more");
        }
        required.forEach((m, n) -> {
            if (n < 1) {
                problems.add("Required " + m + " must be at least 1");
            }
        });
        checkChances(problems, "Plant", plants.stream().mapToDouble(PlantSpec::chance).toArray());
        checkChances(problems, "Mob", mobs.stream().mapToDouble(MobSpec::chance).toArray());
        for (ConversionSpec c : conversions) {
            if (c.chance() <= 0 || c.chance() > 100) {
                problems.add("Conversion of " + c.from() + " must have a chance above 0 and up to 100%");
            }
        }
        return problems;
    }

    private static void checkCoverage(List<String> problems, String what, int coverage) {
        if (coverage < UNSET || coverage > 100) {
            problems.add(what + " coverage must be -1 (not checked) or 0 to 100");
        }
    }

    private static void checkChances(List<String> problems, String what, double[] chances) {
        double total = 0;
        for (double c : chances) {
            if (c <= 0) {
                problems.add(what + " chances must be above 0");
            }
            total += c;
        }
        // Allow for rounding, as the loader does
        if (total > 100.000001) {
            problems.add(what + " chances add up to " + total + "%, over 100%");
        }
    }

    // ------------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------------

    public String getKey() {
        return key;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName == null ? "" : friendlyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public Biome getBiome() {
        return biome;
    }

    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission == null ? "" : permission;
    }

    public int getWaterCoverage() {
        return waterCoverage;
    }

    public void setWaterCoverage(int waterCoverage) {
        this.waterCoverage = waterCoverage;
    }

    public int getLavaCoverage() {
        return lavaCoverage;
    }

    public void setLavaCoverage(int lavaCoverage) {
        this.lavaCoverage = lavaCoverage;
    }

    public int getIceCoverage() {
        return iceCoverage;
    }

    public void setIceCoverage(int iceCoverage) {
        this.iceCoverage = iceCoverage;
    }

    public int getMobLimit() {
        return mobLimit;
    }

    public void setMobLimit(int mobLimit) {
        this.mobLimit = mobLimit;
    }

    /**
     * @param habitat water, lava or air
     * @return blocks per mob for that habitat, or 0 to share {@link #getMobLimit()}
     */
    public int getHabitatLimit(MobHabitat habitat) {
        return habitat == MobHabitat.GROUND ? mobLimit : habitatLimits.getOrDefault(habitat, 0);
    }

    /**
     * @param habitat water, lava or air; ground sets {@link #setMobLimit(int)}
     * @param limit   blocks per mob, or 0 to share {@code moblimit}
     */
    public void setHabitatLimit(MobHabitat habitat, int limit) {
        if (habitat == MobHabitat.GROUND) {
            mobLimit = limit;
        } else if (limit == 0) {
            habitatLimits.remove(habitat);
        } else {
            habitatLimits.put(habitat, limit);
        }
    }

    public int getMaxMobs() {
        return maxMobs;
    }

    public void setMaxMobs(int maxMobs) {
        this.maxMobs = maxMobs;
    }

    /**
     * @return the required blocks and their minimum counts, in order; change it directly
     */
    public Map<Material, Integer> getRequired() {
        return required;
    }

    /**
     * @return the plants, in order; change it directly
     */
    public List<PlantSpec> getPlants() {
        return plants;
    }

    /**
     * @return the mobs, in order; change it directly
     */
    public List<MobSpec> getMobs() {
        return mobs;
    }

    /**
     * @return the block conversions, in order; change it directly
     */
    public List<ConversionSpec> getConversions() {
        return conversions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecipeDraft d)) {
            return false;
        }
        return key.equals(d.key) && friendlyName.equals(d.friendlyName) && description.equals(d.description)
                && Objects.equals(biome, d.biome) && icon == d.icon && priority == d.priority
                && permission.equals(d.permission) && waterCoverage == d.waterCoverage
                && lavaCoverage == d.lavaCoverage && iceCoverage == d.iceCoverage && mobLimit == d.mobLimit
                && habitatLimits.equals(d.habitatLimits) && maxMobs == d.maxMobs && required.equals(d.required)
                && plants.equals(d.plants) && mobs.equals(d.mobs) && conversions.equals(d.conversions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, friendlyName, biome, icon, priority);
    }

    @Override
    public String toString() {
        return "RecipeDraft[" + key + "]";
    }
}
