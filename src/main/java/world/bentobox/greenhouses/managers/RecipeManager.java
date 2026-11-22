package world.bentobox.greenhouses.managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.RecipeDraft;

/**
 * Loads the biome recipes from biomes.yml, and saves changes to them.
 * <p>
 * Each recipe is kept both as a live {@link BiomeRecipe} and as a {@link RecipeDraft} of what
 * biomes.yml says. Editors work on a {@link #getDraft(String) copy of the draft} and
 * {@link #save(RecipeDraft, int) save} it: biomes.yml is backed up, the recipe's section is
 * updated in place, and a new live recipe replaces the old one. Greenhouses look their recipe
 * up by name every time, so every greenhouse of that type uses the new rules at once.
 * <p>
 * Each recipe has a revision number that goes up whenever it is saved, deleted or reloaded.
 * A save names the revision it started from and is refused if the recipe has changed since,
 * so two admins cannot silently overwrite each other.
 *
 * @author tastybento
 */
public class RecipeManager {

    /** Most recipes that can be loaded: what fits in the recipe panel */
    public static final int MAXIMUM_INVENTORY_SIZE = 49;
    /** The revision to give when saving a recipe that does not exist yet */
    public static final int NEW_RECIPE = 0;
    /** How many backups of biomes.yml to keep */
    static final int BACKUPS_KEPT = 10;
    private static final String BIOMES = "biomes";
    private static final String BIOMES_YML = "biomes.yml";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Greenhouses addon;
    private static final List<BiomeRecipe> biomeRecipes = new ArrayList<>();
    private final Map<String, RecipeDraft> drafts = new LinkedHashMap<>();
    private final Map<String, Integer> revisions = new HashMap<>();
    private final RecipeCodec codec;

    /**
     * How a save went.
     *
     * @param status   what happened
     * @param problems why it was refused, for {@link Status#INVALID}
     */
    public record SaveResult(Status status, List<String> problems) {
        /** What happened to a save */
        public enum Status {
            /** Saved, and the live recipe replaced */
            SAVED,
            /** Not saved: the draft has problems */
            INVALID,
            /** Not saved: the recipe changed since the draft was taken */
            CONFLICT,
            /** Not saved: biomes.yml could not be written */
            ERROR
        }

        static SaveResult of(Status status) {
            return new SaveResult(status, List.of());
        }
    }

    public RecipeManager(Greenhouses addon) {
        this.addon = Objects.requireNonNull(addon, "addon");
        this.codec = new RecipeCodec(this.addon, this.addon::logError);
        reload();
    }

    /**
     * Re-read biomes.yml, replacing the currently loaded recipes.
     */
    public void reload() {
        try {
            loadBiomeRecipes();
        } catch (Exception e) {
            addon.logError(e.getMessage());
        }
    }

    /**
     * Get BiomeRecipe by name
     * @param name - name
     * @return Optional BiomeRecipe found
     */
    public static Optional<BiomeRecipe> getBiomeRecipies(String name) {
        return biomeRecipes.stream().filter(r -> r.getName().equals(name)).findFirst();
    }

    private File biomeFile() {
        return new File(addon.getDataFolder(), BIOMES_YML);
    }

    /**
     * Loads all the biome recipes from the file biomes.yml.
     * @throws InvalidConfigurationException - bad YAML
     * @throws IOException - io exception
     */
    private void loadBiomeRecipes() throws IOException, InvalidConfigurationException {
        biomeRecipes.clear();
        drafts.clear();
        // Anything loaded again counts as changed, so drafts taken before a reload cannot overwrite it
        revisions.replaceAll((k, v) -> v + 1);
        File file = biomeFile();
        if (!file.exists()) {
            addon.logError("No biomes.yml file!");
            addon.saveResource(BIOMES_YML, true);
        }
        YamlConfiguration biomeConfig = new YamlConfiguration();
        biomeConfig.load(file);
        if (!biomeConfig.isConfigurationSection(BIOMES)) {
            addon.logError("biomes.yml file is missing, empty or corrupted. Delete and reload plugin again!");
            return;
        }
        ConfigurationSection biomeSection = biomeConfig.getConfigurationSection(BIOMES);
        for (String key : biomeSection.getKeys(false)) {
            if (biomeRecipes.size() == MAXIMUM_INVENTORY_SIZE) {
                addon.logWarning("Cannot load any more biome recipies - limit is " + MAXIMUM_INVENTORY_SIZE);
                break;
            }
            ConfigurationSection section = biomeSection.getConfigurationSection(key);
            RecipeDraft draft = section == null ? null : codec.read(key, section);
            if (draft == null) {
                addon.logError("Problem loading biome recipe " + key + " - skipping!");
                continue;
            }
            if (addon.getSettings().isStartupLog()) {
                addon.log("Adding biome recipe for " + key);
            }
            drafts.put(key, draft);
            revisions.merge(key, 1, Integer::sum);
            biomeRecipes.add(codec.toRecipe(draft));
        }
        addon.log("Loaded " + biomeRecipes.size() + " biome recipes.");
    }

    /**
     * @return the biomeRecipes
     */
    public List<BiomeRecipe> getBiomeRecipes() {
        return biomeRecipes;
    }

    /**
     * @param key a recipe key
     * @return an editable copy of the recipe as biomes.yml has it, or empty if there is no such recipe
     */
    public Optional<RecipeDraft> getDraft(String key) {
        return Optional.ofNullable(drafts.get(key)).map(RecipeDraft::copyOf);
    }

    /**
     * @param key a recipe key
     * @return the recipe's revision, to give to {@link #save(RecipeDraft, int)}; {@link #NEW_RECIPE}
     *         if there is no such recipe
     */
    public int getRevision(String key) {
        return drafts.containsKey(key) ? revisions.getOrDefault(key, NEW_RECIPE) : NEW_RECIPE;
    }

    /**
     * Saves a recipe: writes biomes.yml (after backing it up) and replaces the live recipe.
     *
     * @param draft    the recipe
     * @param revision the revision the draft was taken at, from {@link #getRevision(String)}, or
     *                 {@link #NEW_RECIPE} to create a recipe
     * @return how it went
     */
    @NonNull
    public SaveResult save(@NonNull RecipeDraft draft, int revision) {
        List<String> problems = draft.validate();
        boolean exists = drafts.containsKey(draft.getKey());
        if (!exists && biomeRecipes.size() >= MAXIMUM_INVENTORY_SIZE) {
            problems = new ArrayList<>(problems);
            problems.add("There can be at most " + MAXIMUM_INVENTORY_SIZE + " recipes");
        }
        if (!problems.isEmpty()) {
            return new SaveResult(SaveResult.Status.INVALID, problems);
        }
        if (revision != getRevision(draft.getKey())) {
            return SaveResult.of(SaveResult.Status.CONFLICT);
        }
        try {
            YamlConfiguration config = loadFile();
            ConfigurationSection biomes = config.isConfigurationSection(BIOMES) ? config.getConfigurationSection(BIOMES)
                    : config.createSection(BIOMES);
            ConfigurationSection section = biomes.isConfigurationSection(draft.getKey())
                    ? biomes.getConfigurationSection(draft.getKey())
                    : biomes.createSection(draft.getKey());
            codec.write(draft, section);
            backup();
            config.save(biomeFile());
        } catch (IOException | InvalidConfigurationException e) {
            addon.logError("Could not save biomes.yml: " + e.getMessage());
            return SaveResult.of(SaveResult.Status.ERROR);
        }
        // Swap in the new live recipe, in the same place in the list
        RecipeDraft saved = RecipeDraft.copyOf(draft);
        BiomeRecipe recipe = codec.toRecipe(saved);
        int i = indexOf(draft.getKey());
        if (i < 0) {
            biomeRecipes.add(recipe);
        } else {
            biomeRecipes.set(i, recipe);
        }
        drafts.put(draft.getKey(), saved);
        revisions.merge(draft.getKey(), 1, Integer::sum);
        return SaveResult.of(SaveResult.Status.SAVED);
    }

    /**
     * Deletes a recipe from biomes.yml (after backing it up) and from the live recipes.
     * Greenhouses of that type keep their records; the caller should reload greenhouses so
     * they are listed as unloaded.
     *
     * @param key the recipe key
     * @return true if it was deleted
     */
    public boolean delete(@NonNull String key) {
        if (!drafts.containsKey(key)) {
            return false;
        }
        try {
            YamlConfiguration config = loadFile();
            config.set(BIOMES + "." + key, null);
            backup();
            config.save(biomeFile());
        } catch (IOException | InvalidConfigurationException e) {
            addon.logError("Could not save biomes.yml: " + e.getMessage());
            return false;
        }
        int i = indexOf(key);
        if (i >= 0) {
            biomeRecipes.remove(i);
        }
        drafts.remove(key);
        revisions.merge(key, 1, Integer::sum);
        return true;
    }

    private int indexOf(String key) {
        for (int i = 0; i < biomeRecipes.size(); i++) {
            if (biomeRecipes.get(i).getName().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private YamlConfiguration loadFile() throws IOException, InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        // Keep the file's comments when it is written back
        config.options().parseComments(true);
        if (biomeFile().exists()) {
            config.load(biomeFile());
        }
        return config;
    }

    /**
     * Copies biomes.yml into the backups folder, keeping the newest {@link #BACKUPS_KEPT}.
     *
     * @throws IOException if it cannot be copied
     */
    void backup() throws IOException {
        Path source = biomeFile().toPath();
        if (!Files.exists(source)) {
            return;
        }
        Path dir = getBackupFolder().toPath();
        Files.createDirectories(dir);
        // Timestamped, with a counter if two land in the same millisecond, so none is overwritten
        String stamp = "biomes-" + LocalDateTime.now(ZoneId.systemDefault()).format(STAMP);
        Path target = dir.resolve(stamp + ".yml");
        for (int n = 1; Files.exists(target); n++) {
            target = dir.resolve(stamp + "-" + n + ".yml");
        }
        Files.copy(source, target);
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> old = files.filter(p -> p.getFileName().toString().startsWith("biomes-"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed()).skip(BACKUPS_KEPT).toList();
            for (Path p : old) {
                Files.deleteIfExists(p);
            }
        }
    }

    /**
     * @return where backups of biomes.yml go
     */
    public File getBackupFolder() {
        return new File(addon.getDataFolder(), "backups");
    }
}
