package world.bentobox.greenhouses.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.Settings;
import world.bentobox.greenhouses.build.GreenhousePlan.Mode;
import world.bentobox.greenhouses.build.GreenhousePlan.Placement;
import world.bentobox.greenhouses.build.GreenhousePlan.Size;
import world.bentobox.greenhouses.build.GreenhousePlan.TooSmallException;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.RecipeManager;

/**
 * Tests {@link GreenhousePlan} against every recipe shipped in biomes.yml: each planned test
 * greenhouse is laid out in a pretend world and checked block by block the way the addon
 * checks a real one.
 *
 * @author tastybento
 */
class GreenhousePlanTest {

    private static final int AX = 10;
    private static final int AY = 70;
    private static final int AZ = -20;

    @TempDir
    Path dataFolder;
    private List<BiomeRecipe> recipes;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();
        Files.copy(Path.of("src/main/resources/biomes.yml"), dataFolder.resolve("biomes.yml"));
        Greenhouses addon = mock(Greenhouses.class);
        when(addon.getDataFolder()).thenReturn(dataFolder.toFile());
        when(addon.getSettings()).thenReturn(new Settings());
        recipes = new RecipeManager(addon).getBiomeRecipes();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        Mockito.framework().clearInlineMocks();
    }

    private BiomeRecipe recipe(String name) {
        return recipes.stream().filter(r -> r.getName().equals(name)).findFirst().orElseThrow();
    }

    /** Lays the plan out in an empty pretend world: last placement at a spot wins */
    private static Map<List<Integer>, Material> layOut(GreenhousePlan plan) {
        return layOut(plan, new HashMap<>());
    }

    /** Lays the plan out over what is already in a pretend world */
    private static Map<List<Integer>, Material> layOut(GreenhousePlan plan, Map<List<Integer>, Material> world) {
        for (Placement p : plan.getPlacements()) {
            List<Integer> key = List.of(p.x(), p.y(), p.z());
            if (p.onlyIfAir() && world.containsKey(key) && !world.get(key).isAir()) {
                continue;
            }
            world.put(key, p.material());
        }
        return world;
    }

    /** Counts the inside, floor to roof, the way BiomeRecipe#checkRecipe does */
    private static Map<Material, Integer> countInside(GreenhousePlan plan, Map<List<Integer>, Material> world) {
        int[] c = plan.getCorners();
        Map<Material, Integer> count = new EnumMap<>(Material.class);
        for (int y = plan.getFloorY(); y < plan.getRoofY(); y++) {
            for (int x = c[0] + 1; x < c[2]; x++) {
                for (int z = c[1] + 1; z < c[3]; z++) {
                    Material m = world.getOrDefault(List.of(x, y, z), Material.AIR);
                    if (!m.isAir()) {
                        count.merge(m, 1, Integer::sum);
                    }
                }
            }
        }
        return count;
    }

    @Test
    void testShippedRecipesLoaded() {
        assertTrue(recipes.size() >= 19, "Recipes loaded: " + recipes.size());
    }

    @Test
    void testEveryShippedRecipeIsMetAtItsDefaultSize() throws TooSmallException {
        for (Mode mode : Mode.values()) {
            checkEveryRecipe(mode);
        }
    }

    private void checkEveryRecipe(Mode mode) throws TooSmallException {
        for (BiomeRecipe r : recipes) {
            Size size = GreenhousePlan.defaultSize(r);
            GreenhousePlan plan = GreenhousePlan.create(r, AX, AY, AZ, size, BlockFace.NORTH, mode);
            Map<List<Integer>, Material> world = layOut(plan);
            Map<Material, Integer> inside = countInside(plan, world);
            int area = size.area();
            String n = r.getName() + " at " + size + " (" + mode + "): ";
            // Required blocks
            r.getRequiredBlocks().forEach((m, q) -> assertTrue(inside.getOrDefault(m, 0) >= q,
                    n + "needs " + q + " " + m + ", has " + inside.getOrDefault(m, 0)));
            // Coverage, the way checkRatios works it out
            checkCoverage(n + "water", r.getWaterCoverage(), inside.getOrDefault(Material.WATER, 0), area);
            checkCoverage(n + "lava", r.getLavaCoverage(), inside.getOrDefault(Material.LAVA, 0), area);
            int ice = inside.getOrDefault(Material.ICE, 0) + inside.getOrDefault(Material.PACKED_ICE, 0)
                    + inside.getOrDefault(Material.BLUE_ICE, 0);
            checkCoverage(n + "ice", r.getIceCoverage(), ice, area);
            // Somewhere for every plant to grow and every mob to spawn
            for (Material m : r.getPlantGrowOn()) {
                assertTrue(inside.containsKey(m), n + "nothing for plants to grow on: " + m);
            }
            for (Material m : r.getMobSpawnOn()) {
                // A recipe can forbid the liquid its own mobs spawn on (SOUL_SAND_VALLEY: striders
                // on lava, lavacoverage 0); the forbidding wins
                boolean forbidden = (m == Material.LAVA && r.getLavaCoverage() == 0)
                        || (m == Material.WATER && r.getWaterCoverage() == 0);
                assertTrue(forbidden || inside.containsKey(m), n + "nothing for mobs to spawn on: " + m);
            }
            checkShell(n, plan, world, r);
        }
    }

    private static void checkCoverage(String what, int coverage, int blocks, int area) {
        double ratio = blocks * 100D / area;
        if (coverage == 0) {
            assertEquals(0, blocks, what + " is not allowed");
        } else if (coverage > 0) {
            assertTrue(ratio >= coverage, what + " is " + ratio + "%, needs " + coverage + "%");
        }
    }

    private static void checkShell(String n, GreenhousePlan plan, Map<List<Integer>, Material> world, BiomeRecipe r) {
        int[] c = plan.getCorners();
        Material door = BiomeStyle.of(r.getBiome()).getDoor();
        int doors = 0;
        int hoppers = 0;
        for (int x = c[0]; x <= c[2]; x++) {
            for (int z = c[1]; z <= c[3]; z++) {
                boolean ring = x == c[0] || x == c[2] || z == c[1] || z == c[3];
                // Roof
                Material roof = world.get(List.of(x, plan.getRoofY(), z));
                if (roof == Material.HOPPER) {
                    hoppers++;
                } else {
                    assertEquals(Material.GLASS, roof, n + "roof at " + x + "," + z);
                }
                // Floor under the walls is never glass, so the walls start at the floor
                if (ring) {
                    assertNotEquals(Material.GLASS, world.get(List.of(x, plan.getFloorY(), z)), n + "floor ring");
                    for (int y = plan.getFloorY() + 1; y < plan.getRoofY(); y++) {
                        Material m = world.get(List.of(x, y, z));
                        if (m == door) {
                            doors++;
                        } else {
                            assertEquals(Material.GLASS, m, n + "wall at " + x + "," + y + "," + z);
                        }
                    }
                }
            }
        }
        assertEquals(1, hoppers, n + "hoppers");
        assertEquals(2, doors, n + "door halves");
    }

    @Test
    void testTreeRecipeGetsATreeWithItsLeaves() throws TooSmallException {
        BiomeRecipe forest = recipe("FOREST");
        GreenhousePlan plan = GreenhousePlan.create(forest, AX, AY, AZ, GreenhousePlan.defaultSize(forest),
                BlockFace.NORTH, Mode.CLEAR);
        Map<List<Integer>, Material> world = layOut(plan);
        // The trunk stands on the floor in one column
        List<List<Integer>> logs = world.entrySet().stream().filter(e -> e.getValue() == Material.OAK_LOG)
                .map(Map.Entry::getKey).toList();
        assertEquals(3, logs.size());
        assertEquals(1, logs.stream().map(k -> List.of(k.get(0), k.get(2))).distinct().count());
        assertEquals(AY, logs.stream().mapToInt(k -> k.get(1)).min().orElseThrow());
        // And never where the admin stands
        assertFalse(logs.stream().anyMatch(k -> k.get(0) == AX && k.get(2) == AZ));
        // The door is in the north wall, the admin's way out is clear
        assertEquals(Material.AIR, world.get(List.of(AX, AY, AZ)));
        assertEquals(Material.AIR, world.get(List.of(AX, AY + 1, AZ)));
    }

    @Test
    void testDoorFacesTheChosenWall() throws TooSmallException {
        BiomeRecipe plains = recipe("PLAINS");
        Size size = new Size(9, 9, 5);
        for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
            GreenhousePlan plan = GreenhousePlan.create(plains, AX, AY, AZ, size, face, Mode.CLEAR);
            Placement door = plan.getPlacements().stream().filter(p -> p.material() == Material.OAK_DOOR).findFirst()
                    .orElseThrow();
            assertEquals(face, door.facing());
            assertEquals(AX + face.getModX() * 5, door.x(), face.name());
            assertEquals(AZ + face.getModZ() * 5, door.z(), face.name());
        }
        assertThrows(IllegalArgumentException.class,
                () -> GreenhousePlan.create(plains, AX, AY, AZ, size, BlockFace.UP, Mode.CLEAR));
    }

    /**
     * Keep mode works around a tree already standing inside: it is not touched, nor is the floor
     * under it, but everything else the recipe needs is still laid.
     */
    @Test
    void testKeepModeWorksAroundATree() throws TooSmallException {
        BiomeRecipe plains = recipe("PLAINS");
        Map<List<Integer>, Material> world = new HashMap<>();
        int tx = AX + 2;
        int tz = AZ + 2;
        world.put(List.of(tx, AY - 1, tz), Material.DIRT);
        for (int y = AY; y < AY + 4; y++) {
            world.put(List.of(tx, y, tz), Material.BIRCH_LOG);
        }
        GreenhousePlan plan = GreenhousePlan.create(plains, AX, AY, AZ, new Size(9, 9, 5), BlockFace.NORTH,
                Mode.KEEP, (x, y, z) -> world.getOrDefault(List.of(x, y, z), Material.AIR).isAir());
        Map<List<Integer>, Material> after = layOut(plan, new HashMap<>(world));
        // The tree and the dirt under it are untouched
        for (int y = AY - 1; y < AY + 4; y++) {
            assertEquals(world.get(List.of(tx, y, tz)), after.get(List.of(tx, y, tz)), "y " + y);
        }
        // Nothing inside above the floor is replaced, only filled
        int[] c = plan.getCorners();
        plan.getPlacements().stream()
                .filter(p -> p.x() > c[0] && p.x() < c[2] && p.z() > c[1] && p.z() < c[3] && p.y() >= AY
                        && p.y() < plan.getRoofY())
                .forEach(p -> assertTrue(p.onlyIfAir(), "keep mode replaced " + p));
        // And the recipe is still met: the floor is grass
        assertTrue(countInside(plan, after).getOrDefault(Material.GRASS_BLOCK, 0) >= 3);
    }

    /**
     * Built where something is already standing, keep mode leaves it and puts what it can elsewhere,
     * never refusing for lack of room.
     */
    @Test
    void testKeepModeDoesWhatItCan() throws TooSmallException {
        BiomeRecipe forest = recipe("FOREST");
        // Every spot is taken except the floor
        GreenhousePlan plan = GreenhousePlan.create(forest, AX, AY, AZ, new Size(5, 5, 5), BlockFace.NORTH,
                Mode.KEEP, (x, y, z) -> y < AY);
        assertTrue(plan.getPlacements().stream().noneMatch(p -> p.material() == Material.OAK_LOG));
    }

    /**
     * Issue raised in testing: an ocean greenhouse must be full of water, not a wet floor.
     */
    @Test
    void testOceanIsFlooded() throws TooSmallException {
        for (Mode mode : Mode.values()) {
            BiomeRecipe ocean = recipe("OCEAN");
            GreenhousePlan plan = GreenhousePlan.create(ocean, AX, AY, AZ, GreenhousePlan.defaultSize(ocean),
                    BlockFace.NORTH, mode);
            Map<List<Integer>, Material> world = layOut(plan);
            int[] c = plan.getCorners();
            for (int x = c[0] + 1; x < c[2]; x++) {
                for (int z = c[1] + 1; z < c[3]; z++) {
                    for (int y = AY; y < plan.getRoofY() - 1; y++) {
                        assertEquals(Material.WATER, world.get(List.of(x, y, z)), mode + " " + x + "," + y + "," + z);
                    }
                    // A layer of air under the roof
                    assertTrue(world.getOrDefault(List.of(x, plan.getRoofY() - 1, z), Material.AIR).isAir());
                }
            }
        }
        // Half-water recipes keep their pools on the floor
        BiomeRecipe beach = recipe("beaches");
        GreenhousePlan plan = GreenhousePlan.create(beach, AX, AY, AZ, GreenhousePlan.defaultSize(beach),
                BlockFace.NORTH, Mode.CLEAR);
        assertTrue(plan.getPlacements().stream().noneMatch(p -> p.material() == Material.WATER && p.y() >= AY));
    }

    /**
     * Issue raised in testing: keep mode (the default) must still put down lava for the Nether.
     */
    @Test
    void testNetherGetsLavaInKeepMode() throws TooSmallException {
        BiomeRecipe nether = recipe("NETHER");
        GreenhousePlan plan = GreenhousePlan.create(nether, AX, AY, AZ, GreenhousePlan.defaultSize(nether),
                BlockFace.NORTH, Mode.KEEP);
        Map<Material, Integer> inside = countInside(plan, layOut(plan));
        assertTrue(inside.getOrDefault(Material.LAVA, 0) * 100D / plan.getSize().area() >= nether.getLavaCoverage());
    }

    @Test
    void testTooSmall() {
        BiomeRecipe dripstone = recipe("dripstone_caves");
        // 16 required blocks + 25% water + standing room cannot fit in 3x3
        assertThrows(TooSmallException.class,
                () -> GreenhousePlan.create(dripstone, AX, AY, AZ, new Size(3, 3, 3), BlockFace.NORTH, Mode.CLEAR));
        // Keep mode places nothing inside, so any size will do
        try {
            GreenhousePlan.create(dripstone, AX, AY, AZ, new Size(3, 3, 3), BlockFace.NORTH, Mode.KEEP);
        } catch (TooSmallException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void testDefaultSizes() {
        assertEquals(new Size(9, 9, 5), GreenhousePlan.defaultSize(recipe("PLAINS")));
        // Ocean is 95% water: still fits the default floor
        assertEquals(9, GreenhousePlan.defaultSize(recipe("OCEAN")).width());
    }

    @Test
    void testCoverageBlocks() {
        assertEquals(0, GreenhousePlan.coverageBlocks(-1, 81));
        assertEquals(0, GreenhousePlan.coverageBlocks(0, 81));
        assertEquals(41, GreenhousePlan.coverageBlocks(50, 81));
        assertEquals(81, GreenhousePlan.coverageBlocks(100, 81));
    }

    @Test
    void testSizeParse() {
        assertEquals(Optional.of(new Size(9, 9, GreenhousePlan.DEFAULT_HEIGHT)), Size.parse("9"));
        assertEquals(Optional.of(new Size(7, 11, GreenhousePlan.DEFAULT_HEIGHT)), Size.parse("7x11"));
        assertEquals(Optional.of(new Size(7, 11, 6)), Size.parse("7X11x6"));
        assertTrue(Size.parse("2").isEmpty());
        assertTrue(Size.parse("40x9").isEmpty());
        assertTrue(Size.parse("9x9x1").isEmpty());
        assertTrue(Size.parse("big").isEmpty());
        assertEquals("7x11x6", new Size(7, 11, 6).toString());
    }

    @Test
    void testModeParse() {
        assertEquals(Optional.of(Mode.CLEAR), Mode.parse("Clear"));
        assertEquals(Optional.of(Mode.KEEP), Mode.parse("keep"));
        assertTrue(Mode.parse("fill").isEmpty());
    }

    @Test
    void testLogsAndLeaves() {
        assertTrue(GreenhousePlan.isLog(Material.SPRUCE_LOG));
        assertTrue(GreenhousePlan.isLog(Material.CRIMSON_STEM));
        assertFalse(GreenhousePlan.isLog(Material.OAK_PLANKS));
        assertTrue(GreenhousePlan.isLeaves(Material.MANGROVE_LEAVES));
        assertTrue(GreenhousePlan.isLeaves(Material.NETHER_WART_BLOCK));
        assertEquals(Material.MANGROVE_LOG, GreenhousePlan.logFor(Material.MANGROVE_LEAVES));
        assertEquals(Material.OAK_LOG, GreenhousePlan.logFor(Material.AZALEA_LEAVES));
        assertEquals(Material.WARPED_STEM, GreenhousePlan.logFor(Material.WARPED_WART_BLOCK));
    }

    @Test
    void testFloorBlocks() {
        assertTrue(GreenhousePlan.isFloorBlock(Material.MUD));
        assertTrue(GreenhousePlan.isFloorBlock(Material.MANGROVE_ROOTS));
        assertFalse(GreenhousePlan.isFloorBlock(Material.SNOW));
        assertFalse(GreenhousePlan.isFloorBlock(Material.MOSS_CARPET));
        assertFalse(GreenhousePlan.isFloorBlock(Material.WATER));
    }

    @Test
    void testBiomeStyles() {
        assertEquals(Material.SPRUCE_DOOR, BiomeStyle.of(Biome.TAIGA).getDoor());
        assertEquals(Material.MANGROVE_DOOR, BiomeStyle.of(Biome.MANGROVE_SWAMP).getDoor());
        assertEquals(Material.CRIMSON_DOOR, BiomeStyle.of(Biome.NETHER_WASTES).getDoor());
        assertEquals(Material.NETHERRACK, BiomeStyle.of(Biome.NETHER_WASTES).getFiller());
        assertEquals(Material.ACACIA_DOOR, BiomeStyle.of(Biome.DESERT).getDoor());
        assertEquals(Material.OAK_DOOR, BiomeStyle.of(Biome.PLAINS).getDoor());
        assertEquals(Material.OAK_DOOR, BiomeStyle.of(null).getDoor());
        // Only End biomes have "end" in their name, and every door opens by hand
        Registry.BIOME.forEach(b -> {
            String key = b.getKey().getKey();
            Material door = BiomeStyle.of(b).getDoor();
            assertTrue(door.name().endsWith("_DOOR") && door != Material.IRON_DOOR, key + " " + door);
            if (key.contains("end")) {
                assertEquals(Material.END_STONE, BiomeStyle.of(b).getFiller(), key);
            }
        });
    }

    @Test
    void testBounds() throws TooSmallException {
        GreenhousePlan plan = GreenhousePlan.create(recipe("PLAINS"), AX, AY, AZ, new Size(9, 9, 5), BlockFace.NORTH,
                Mode.CLEAR);
        assertEquals(AY + 5, plan.getRoofY());
        assertEquals(AY - 1, plan.getFloorY());
        int[] c = plan.getCorners();
        assertEquals(AX - 5, c[0]);
        assertEquals(AZ - 5, c[1]);
        assertEquals(AX + 5, c[2]);
        assertEquals(AZ + 5, c[3]);
        assertTrue(plan.getBounds().contains(AX, AY, AZ));
        assertFalse(new File("unused").exists());
    }
}
