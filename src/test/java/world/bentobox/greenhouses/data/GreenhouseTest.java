package world.bentobox.greenhouses.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.Walls;
import world.bentobox.greenhouses.managers.RecipeManager;

/**
 * @author tastybento
 *
 */
class GreenhouseTest {

    private static final int MINX = -10;
    private static final int MINZ = 10;
    private static final int MAXX = 20;
    private static final int MAXZ = 25;
    private static final int FLOOR = 60;
    private static final int CEILING = 70;

    // Class under test
    private Greenhouse gh;
    @Mock
    private World world;

    private Walls walls;

    private BiomeRecipe br;

    private MockitoSession mockSession;
    private MockedStatic<RecipeManager> mockedRecipeManager;

    @BeforeEach
    public void setUp() {
        mockSession = Mockito.mockitoSession()
            .initMocks(this)
            .strictness(Strictness.LENIENT)
            .startMocking();
        MockBukkit.mock();
        @SuppressWarnings("unused")
        var unusedTagRef = org.bukkit.Tag.LEAVES;

        br = mock(BiomeRecipe.class);
        // RecipeManager
        mockedRecipeManager = Mockito.mockStatic(RecipeManager.class, Mockito.RETURNS_DEEP_STUBS);
        when(br.getName()).thenReturn("test");
        mockedRecipeManager.when(() -> RecipeManager.getBiomeRecipies("test")).thenReturn(Optional.of(br));
        // Walls
        walls = mock(Walls.class);
        when(walls.getMinX()).thenReturn(MINX);
        when(walls.getMinZ()).thenReturn(MINZ);
        when(walls.getMaxX()).thenReturn(MAXX);
        when(walls.getMaxZ()).thenReturn(MAXZ);
        when(walls.getFloor()).thenReturn(FLOOR);
        gh = new Greenhouse(world, walls, CEILING);
    }

    @AfterEach
    public void tearDown() {
        if (mockSession != null) {
            mockSession.finishMocking();
        }
        if (mockedRecipeManager != null) {
            mockedRecipeManager.closeOnDemand();
        }
        MockBukkit.unmock();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getBiomeRecipeName()}.
     */
    @Test
    public void testGetBiomeRecipeName() {
        assertNull(gh.getBiomeRecipeName());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getCeilingHeight()}.
     */
    @Test
    public void testGetCeilingHeight() {
        assertEquals(CEILING + 1, gh.getCeilingHeight());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getFloorHeight()}.
     */
    @Test
    public void testGetFloorHeight() {
        assertEquals(FLOOR, gh.getFloorHeight());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getFloorHeight()}.
     */
    @Test
    public void testGetFloorHeightNullLocation() {
        // A database record can have no location; falls back to the bounding box instead of NPEing
        gh.setLocation(null);
        assertEquals(FLOOR, gh.getFloorHeight());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getLocation()}.
     */
    @Test
    public void testGetLocation() {
        Location l = gh.getLocation();
        assertEquals(MINX, l.getBlockX());
        assertEquals(FLOOR, l.getBlockY());
        assertEquals(MINZ, l.getBlockZ());

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getOriginalBiome()}.
     */
    @Test
    public void testGetOriginalBiome() {
        assertNull(gh.getOriginalBiome());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getRoofHopperLocation()}.
     */
    @Test
    public void testGetRoofHopperLocation() {
        assertNull(gh.getRoofHopperLocation());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getUniqueId()}.
     */
    @Test
    public void testGetUniqueId() {
        assertFalse(gh.getUniqueId().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#isBroken()}.
     */
    @Test
    public void testIsBroken() {
        assertFalse(gh.isBroken());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#setBiomeRecipeName(java.lang.String)}.
     */
    @Test
    public void testSetBiomeRecipeName() {
        gh.setBiomeRecipeName("test");
        assertEquals("test", gh.getBiomeRecipeName());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#setBroken(boolean)}.
     */
    @Test
    public void testSetBroken() {
        gh.setBroken(true);
        assertTrue(gh.isBroken());
        gh.setBroken(false);
        assertFalse(gh.isBroken());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#setLocation(org.bukkit.Location)}.
     */
    @Test
    public void testSetLocation() {
        Location l = new Location(world, 1,2,3);
        gh.setLocation(l);
        assertEquals(l, gh.getLocation());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#setOriginalBiome(org.bukkit.block.Biome)}.
     */
    @Test
    public void testSetOriginalBiome() {
        gh.setOriginalBiome(Biome.BADLANDS);
        assertEquals(Biome.BADLANDS, gh.getOriginalBiome());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#setRoofHopperLocation(Vector)}.
     */
    @Test
    public void testSetRoofHopperLocation() {
        gh.setRoofHopperLocation(new Vector(1,2,3));
        assertEquals(world, gh.getRoofHopperLocation().getWorld());
        assertEquals(1, gh.getRoofHopperLocation().getBlockX());
        assertEquals(2, gh.getRoofHopperLocation().getBlockY());
        assertEquals(3, gh.getRoofHopperLocation().getBlockZ());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getBoundingBox()}.
     */
    @Test
    public void testGetBoundingBox() {
        BoundingBox bb = gh.getBoundingBox();
        assertEquals(MINX, (int)bb.getMinX());
        assertEquals(MINZ, (int)bb.getMinZ());
        assertEquals(FLOOR, (int)bb.getMinY());
        assertEquals(MAXX + 1, (int)bb.getMaxX());
        assertEquals(MAXZ + 1, (int)bb.getMaxZ());
        assertEquals(CEILING + 1, (int)bb.getMaxY());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#setBoundingBox(org.bukkit.util.BoundingBox)}.
     */
    @Test
    public void testSetBoundingBox() {
        BoundingBox bb = new BoundingBox();
        gh.setBoundingBox(bb);
        assertEquals(bb, gh.getBoundingBox());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#setUniqueId(java.lang.String)}.
     */
    @Test
    public void testSetUniqueId() {
        gh.setUniqueId("test");
        assertEquals("test", gh.getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getArea()}.
     */
    @Test
    public void testGetArea() {
        assertEquals(406, gh.getArea());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getInternalVolume()}.
     */
    @Test
    void testGetInternalVolume() {
        // Inside the walls, above the floor and below the roof
        assertEquals(406 * (CEILING - FLOOR - 1), gh.getInternalVolume());
    }

    /**
     * Liquid counts are not known until the recipe is checked.
     */
    @Test
    void testLiquidBlocks() {
        assertNull(gh.getWaterBlocks());
        assertNull(gh.getLavaBlocks());
        gh.setWaterBlocks(12);
        gh.setLavaBlocks(3);
        assertEquals(Integer.valueOf(12), gh.getWaterBlocks());
        assertEquals(Integer.valueOf(3), gh.getLavaBlocks());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getWorld()}.
     */
    @Test
    public void testGetWorld() {
        assertEquals(world, gh.getWorld());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#contains(org.bukkit.Location)}.
     */
    @Test
    public void testContains() {
        for (int x = MINX; x < MAXX + 1; x++) {
            for (int y = FLOOR; y < CEILING; y++) {
                for (int z = MINZ; z < MAXZ; z++) {
                    assertTrue(gh.contains(new Location(world, x,y,z)), "(" + x + "," + y + "," + z + ")");
                }
            }
        }
        // Wrong world check
        assertFalse(gh.contains(new Location(mock(World.class), MINX, FLOOR, MINZ)));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#setBiomeRecipe(world.bentobox.greenhouses.greenhouse.BiomeRecipe)}.
     */
    @Test
    public void testSetBiomeRecipe() {
        gh.setBiomeRecipe(br);
        assertEquals(br, gh.getBiomeRecipe());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getBiomeRecipe()}.
     */
    @Test
    public void testGetBiomeRecipe() {
        assertNotNull(gh.getBiomeRecipe());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#setMissingBlocks(java.util.Map)}.
     */
    @Test
    public void testSetMissingBlocks() {
        gh.setMissingBlocks(Collections.singletonMap(Material.ACACIA_BOAT, 20));
        assertEquals(20, (int) gh.getMissingBlocks().get(Material.ACACIA_BOAT));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.Greenhouse#getMissingBlocks()}.
     */
    @Test
    public void testGetMissingBlocks() {
        assertNotNull(gh.getMissingBlocks());
    }

}
