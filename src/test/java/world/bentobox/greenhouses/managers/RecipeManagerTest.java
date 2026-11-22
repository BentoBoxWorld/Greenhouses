package world.bentobox.greenhouses.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.Settings;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.RecipeDraft;
import world.bentobox.greenhouses.managers.RecipeManager.SaveResult.Status;

/**
 * Tests saving and deleting recipes in {@link RecipeManager}, on a copy of the shipped biomes.yml.
 *
 * @author tastybento
 */
class RecipeManagerTest {

    @TempDir
    Path dataFolder;
    private RecipeManager rm;
    private File biomesFile;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();
        biomesFile = dataFolder.resolve("biomes.yml").toFile();
        Files.copy(Path.of("src/main/resources/biomes.yml"), biomesFile.toPath());
        Greenhouses addon = mock(Greenhouses.class);
        when(addon.getDataFolder()).thenReturn(dataFolder.toFile());
        when(addon.getSettings()).thenReturn(new Settings());
        rm = new RecipeManager(addon);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        Mockito.framework().clearInlineMocks();
    }

    private YamlConfiguration file() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.load(biomesFile);
        return y;
    }

    private int backups() {
        File[] f = rm.getBackupFolder().listFiles();
        return f == null ? 0 : f.length;
    }

    @Test
    void testDraftIsACopy() {
        RecipeDraft a = rm.getDraft("PLAINS").orElseThrow();
        a.setMobLimit(99);
        assertEquals(1, rm.getDraft("PLAINS").orElseThrow().getMobLimit());
        assertTrue(rm.getDraft("NOPE").isEmpty());
    }

    @Test
    void testSaveUpdatesFileAndLiveRecipe() throws Exception {
        BiomeRecipe before = RecipeManager.getBiomeRecipies("PLAINS").orElseThrow();
        int index = rm.getBiomeRecipes().indexOf(before);
        RecipeDraft d = rm.getDraft("PLAINS").orElseThrow();
        d.setWaterCoverage(20);
        d.getRequired().put(Material.POPPY, 2);
        assertEquals(Status.SAVED, rm.save(d, rm.getRevision("PLAINS")).status());
        // The file
        assertEquals(20, file().getInt("biomes.PLAINS.watercoverage"));
        assertEquals(2, file().getInt("biomes.PLAINS.contents.POPPY"));
        // The live recipe, replaced in the same place
        BiomeRecipe after = RecipeManager.getBiomeRecipies("PLAINS").orElseThrow();
        assertNotSame(before, after);
        assertEquals(20, after.getWaterCoverage());
        assertEquals(index, rm.getBiomeRecipes().indexOf(after));
        assertEquals(19, rm.getBiomeRecipes().size());
        // A backup was taken
        assertEquals(1, backups());
        // And it survives a reload
        rm.reload();
        assertEquals(20, rm.getDraft("PLAINS").orElseThrow().getWaterCoverage());
    }

    @Test
    void testSaveKeepsComments() throws Exception {
        RecipeDraft d = rm.getDraft("beaches").orElseThrow();
        d.setPriority(3);
        rm.save(d, rm.getRevision("beaches"));
        String text = Files.readString(biomesFile.toPath());
        assertTrue(text.contains("# Biome recipe name - can be anything you like"));
        assertTrue(text.contains("priority: 3"));
    }

    @Test
    void testStaleDraftIsRefused() {
        RecipeDraft mine = rm.getDraft("OCEAN").orElseThrow();
        int myRevision = rm.getRevision("OCEAN");
        RecipeDraft theirs = rm.getDraft("OCEAN").orElseThrow();
        theirs.setMaxMobs(5);
        assertEquals(Status.SAVED, rm.save(theirs, rm.getRevision("OCEAN")).status());
        mine.setMaxMobs(50);
        assertEquals(Status.CONFLICT, rm.save(mine, myRevision).status());
        assertEquals(5, rm.getDraft("OCEAN").orElseThrow().getMaxMobs());
    }

    @Test
    void testReloadMakesDraftsStale() {
        int revision = rm.getRevision("OCEAN");
        rm.reload();
        assertNotEquals(revision, rm.getRevision("OCEAN"));
        assertEquals(Status.CONFLICT, rm.save(rm.getDraft("OCEAN").orElseThrow(), revision).status());
    }

    @Test
    void testInvalidDraftIsRefused() {
        RecipeDraft d = rm.getDraft("OCEAN").orElseThrow();
        d.setWaterCoverage(150);
        var result = rm.save(d, rm.getRevision("OCEAN"));
        assertEquals(Status.INVALID, result.status());
        assertFalse(result.problems().isEmpty());
        assertEquals(0, backups());
    }

    @Test
    void testNewRecipe() throws Exception {
        RecipeDraft d = new RecipeDraft("CHERRY");
        d.setBiome(Biome.CHERRY_GROVE);
        d.setIcon(Material.CHERRY_SAPLING);
        d.setFriendlyName("<light_purple>Cherry Grove</light_purple>");
        d.getRequired().put(Material.CHERRY_LOG, 3);
        assertEquals(0, rm.getRevision("CHERRY"));
        assertEquals(Status.SAVED, rm.save(d, RecipeManager.NEW_RECIPE).status());
        assertEquals(20, rm.getBiomeRecipes().size());
        assertTrue(RecipeManager.getBiomeRecipies("CHERRY").isPresent());
        assertEquals("CHERRY_GROVE", file().getString("biomes.CHERRY.biome"));
        // Creating it again would overwrite it, so it is a conflict
        assertEquals(Status.CONFLICT, rm.save(d, RecipeManager.NEW_RECIPE).status());
    }

    @Test
    void testDelete() throws Exception {
        assertTrue(rm.delete("DESERT"));
        assertFalse(file().contains("biomes.DESERT"));
        assertTrue(RecipeManager.getBiomeRecipies("DESERT").isEmpty());
        assertEquals(18, rm.getBiomeRecipes().size());
        assertEquals(1, backups());
        assertFalse(rm.delete("DESERT"));
    }

    @Test
    void testBackupsAreLimited() throws Exception {
        for (int i = 0; i < RecipeManager.BACKUPS_KEPT + 5; i++) {
            rm.backup();
        }
        assertEquals(RecipeManager.BACKUPS_KEPT, backups());
    }

    @Test
    void testValidate() {
        RecipeDraft d = rm.getDraft("PLAINS").orElseThrow();
        assertTrue(d.validate().isEmpty(), d.validate().toString());
        d.setMobLimit(0);
        d.setLavaCoverage(-5);
        d.getPlants().add(new RecipeDraft.PlantSpec(Material.POPPY, 120, Material.GRASS_BLOCK));
        assertEquals(3, d.validate().size(), d.validate().toString());
        // Liquids that cannot all fit on the floor
        RecipeDraft wet = rm.getDraft("OCEAN").orElseThrow();
        wet.setLavaCoverage(10);
        assertEquals(1, wet.validate().size(), wet.validate().toString());
    }
}
