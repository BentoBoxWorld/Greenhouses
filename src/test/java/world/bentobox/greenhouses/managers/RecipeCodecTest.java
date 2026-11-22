package world.bentobox.greenhouses.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.Settings;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.MobHabitat;
import world.bentobox.greenhouses.greenhouse.RecipeDraft;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.ConversionSpec;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.MobSpec;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.PlantSpec;

/**
 * Tests {@link RecipeCodec}: reading, writing and building recipes, against the biomes.yml
 * the addon ships.
 *
 * @author tastybento
 */
class RecipeCodecTest {

    private final List<String> warnings = new ArrayList<>();
    private RecipeCodec codec;
    private YamlConfiguration shipped;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();
        Greenhouses addon = mock(Greenhouses.class);
        when(addon.getSettings()).thenReturn(new Settings());
        codec = new RecipeCodec(addon, warnings::add);
        shipped = new YamlConfiguration();
        shipped.options().parseComments(true);
        shipped.load(new File("src/main/resources/biomes.yml"));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        Mockito.framework().clearInlineMocks();
    }

    private ConfigurationSection recipeSection(String key) {
        return shipped.getConfigurationSection("biomes." + key);
    }

    @Test
    void testShippedRecipesReadCleanly() {
        for (String key : shipped.getConfigurationSection("biomes").getKeys(false)) {
            assertNotNull(codec.read(key, recipeSection(key)), key);
        }
        assertEquals(List.of(), warnings);
    }

    /**
     * Every shipped recipe, written into an empty section and read back, comes out the same.
     */
    @Test
    void testRoundTripIntoEmptySection() {
        for (String key : shipped.getConfigurationSection("biomes").getKeys(false)) {
            RecipeDraft d = codec.read(key, recipeSection(key));
            YamlConfiguration out = new YamlConfiguration();
            ConfigurationSection s = out.createSection(key);
            codec.write(d, s);
            assertEquals(d, codec.read(key, s), key + "\n" + out.saveToString());
        }
    }

    /**
     * Writing a recipe that has not changed changes nothing in the file, comments included.
     */
    @Test
    void testUnchangedWriteLeavesFileAlone() {
        String before = shipped.saveToString();
        for (String key : shipped.getConfigurationSection("biomes").getKeys(false)) {
            codec.write(codec.read(key, recipeSection(key)), recipeSection(key));
        }
        assertEquals(before, shipped.saveToString());
    }

    /**
     * An edit changes only what was edited; the documentation comments in the first recipe survive.
     */
    @Test
    void testEditChangesOnlyThatValue() {
        String before = shipped.saveToString();
        RecipeDraft beach = codec.read("beaches", recipeSection("beaches"));
        beach.setMobLimit(4);
        codec.write(beach, recipeSection("beaches"));
        String after = shipped.saveToString();
        assertTrue(after.contains("# Biome recipe name - can be anything you like"), "comments lost");
        assertTrue(after.contains("# Maxmobs - this is the maximum number of greenhouse-spawed mobs"));
        List<String> a = before.lines().toList();
        List<String> b = after.lines().toList();
        assertEquals(a.size(), b.size());
        long changed = 0;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                changed++;
                assertEquals("    moblimit: 4", b.get(i));
            }
        }
        assertEquals(1, changed);
    }

    @Test
    void testUnsetValuesAreRemoved() {
        RecipeDraft ocean = codec.read("OCEAN", recipeSection("OCEAN"));
        ocean.setWaterCoverage(RecipeDraft.UNSET);
        ocean.setMaxMobs(RecipeDraft.UNSET);
        ocean.setHabitatLimit(MobHabitat.WATER, 0);
        codec.write(ocean, recipeSection("OCEAN"));
        ConfigurationSection s = recipeSection("OCEAN");
        assertFalse(s.contains("watercoverage"));
        assertFalse(s.contains("maxmobs"));
        assertFalse(s.contains("moblimit-water"));
    }

    @Test
    void testListsRewritten() {
        RecipeDraft desert = codec.read("DESERT", recipeSection("DESERT"));
        desert.getPlants().removeFirst();
        desert.getMobs().add(new MobSpec(EntityType.CAMEL, null, 5.5, Material.SAND));
        desert.getRequired().put(Material.SANDSTONE, 2);
        codec.write(desert, recipeSection("DESERT"));
        RecipeDraft back = codec.read("DESERT", recipeSection("DESERT"));
        assertEquals(desert, back);
        assertEquals("5.5:SAND", recipeSection("DESERT").getString("mobs.CAMEL"));
    }

    @Test
    void testConversionFormats() {
        // DESERT uses the list form, SWAMP... the map form is used by MANGROVE_SWAMP
        RecipeDraft desert = codec.read("DESERT", recipeSection("DESERT"));
        assertEquals(3, desert.getConversions().size());
        desert.getConversions().add(new ConversionSpec(Material.GRAVEL, 1, Material.SAND, null));
        codec.write(desert, recipeSection("DESERT"));
        assertEquals(4, recipeSection("DESERT").getStringList("conversion-list").size());
        assertFalse(recipeSection("DESERT").contains("conversions"));

        RecipeDraft mangrove = codec.read("MANGROVE_SWAMP", recipeSection("MANGROVE_SWAMP"));
        assertEquals(new ConversionSpec(Material.GRASS_BLOCK, 50, Material.MUD, Material.GRASS_BLOCK),
                mangrove.getConversions().getFirst());
        mangrove.getConversions().add(new ConversionSpec(Material.DIRT, 10, Material.MUD, null));
        codec.write(mangrove, recipeSection("MANGROVE_SWAMP"));
        // Still the map form while each block converts one way
        assertEquals("10:MUD", recipeSection("MANGROVE_SWAMP").getString("conversions.DIRT"));
        // A second way for the same block needs the list form
        mangrove.getConversions().add(new ConversionSpec(Material.DIRT, 5, Material.CLAY, Material.WATER));
        codec.write(mangrove, recipeSection("MANGROVE_SWAMP"));
        assertFalse(recipeSection("MANGROVE_SWAMP").contains("conversions"));
        assertTrue(recipeSection("MANGROVE_SWAMP").getStringList("conversion-list").contains("DIRT:5:CLAY:WATER"));
        assertEquals(mangrove, codec.read("MANGROVE_SWAMP", recipeSection("MANGROVE_SWAMP")));
    }

    @Test
    void testNumbers() {
        assertEquals("5", RecipeCodec.number(5D));
        assertEquals("0.005", RecipeCodec.number(0.005));
        assertEquals("12.5", RecipeCodec.number(12.5));
    }

    @Test
    void testForgivingRead() {
        YamlConfiguration y = new YamlConfiguration();
        ConfigurationSection s = y.createSection("test");
        s.set("biome", "plains");
        s.set("icon", "NOT_A_BLOCK");
        s.set("friendlyname", "&cRed");
        s.set("plants.POPPY", "10:GRASS_BLOCK");
        s.set("plants.NOT_A_PLANT", "10:GRASS_BLOCK");
        s.set("mobs.COW", "oops");
        s.set("mobs.CREEPER#CHARGED", "5:STONE");
        s.set("contents.STONE", 3);
        s.set("contents.FAKE", 3);
        RecipeDraft d = codec.read("test", s);
        assertNotNull(d);
        assertEquals(Biome.PLAINS, d.getBiome());
        assertEquals(Material.OAK_SAPLING, d.getIcon());
        assertEquals("<red>Red</red>", d.getFriendlyName());
        assertEquals(List.of(new PlantSpec(Material.POPPY, 10, Material.GRASS_BLOCK)), d.getPlants());
        assertEquals(List.of(new MobSpec(EntityType.CREEPER, "CHARGED", 5, Material.STONE)), d.getMobs());
        assertEquals(Map.of(Material.STONE, 3), d.getRequired());
        assertEquals(4, warnings.size(), warnings.toString());
    }

    @Test
    void testNoOrBadBiome() {
        YamlConfiguration y = new YamlConfiguration();
        assertNull(codec.read("none", y.createSection("none")));
        ConfigurationSection s = y.createSection("bad");
        s.set("biome", "NOT_A_BIOME");
        assertNull(codec.read("bad", s));
        // The old alias still works
        s.set("biome", "nether");
        assertEquals(Biome.NETHER_WASTES, codec.read("bad", s).getBiome());
    }

    @Test
    void testToRecipe() {
        RecipeDraft ocean = codec.read("OCEAN", recipeSection("OCEAN"));
        BiomeRecipe r = codec.toRecipe(ocean);
        assertEquals("OCEAN", r.getName());
        assertEquals(95, r.getWaterCoverage());
        assertEquals(ocean.getMobLimit(), r.getMobLimit());
        assertEquals(ocean.getHabitatLimit(MobHabitat.WATER), r.getMobLimit(MobHabitat.WATER));
        assertTrue(r.getMobTypes().contains(EntityType.DOLPHIN));
        RecipeDraft jungle = codec.read("JUNGLE", recipeSection("JUNGLE"));
        assertEquals(jungle.getRequired(), codec.toRecipe(jungle).getRequiredBlocks());
    }

    @Test
    void testMobData() {
        assertNotNull(codec.customizer(new MobSpec(EntityType.CREEPER, "CHARGED", 1, Material.STONE)));
        assertNotNull(codec.customizer(new MobSpec(EntityType.WOLF, "snowy", 1, Material.STONE)));
        assertNull(codec.customizer(new MobSpec(EntityType.WOLF, "PURPLE", 1, Material.STONE)));
        assertNull(codec.customizer(new MobSpec(EntityType.COW, "BIG", 1, Material.STONE)));
        assertNull(codec.customizer(new MobSpec(EntityType.COW, null, 1, Material.STONE)));
        assertEquals(2, warnings.size());
        assertEquals("CREEPER#CHARGED", new MobSpec(EntityType.CREEPER, "CHARGED", 1, Material.STONE).key());
        assertTrue(RecipeCodec.mobData().get(EntityType.WOLF).contains("SNOWY"));
    }
}
