package world.bentobox.greenhouses.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.Roof;
import world.bentobox.greenhouses.greenhouse.Walls;
import world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.world.AsyncWorldCache;

/**
 * @author tastybento
 *
 */
class GreenhouseFinderTest {

    private static final int ROOF_HEIGHT = 15;

    @Mock
    private Greenhouses addon;
    @Mock
    private World world;
    @Mock
    private Location location;
    // Class under test
    private GreenhouseFinder gf;
    private CounterCheck cc;

    private Roof roof;

    private Walls walls;

    @Mock
    private AsyncWorldCache cache;

    private MockitoSession mockSession;

    @BeforeEach
    void setUp() {
        mockSession = Mockito.mockitoSession()
            .initMocks(this)
            .strictness(Strictness.LENIENT)
            .startMocking();
        MockBukkit.mock();
        @SuppressWarnings("unused")
        var unusedTagRef = org.bukkit.Tag.LEAVES;

        // Declare mock after the server is setup
        roof = mock(Roof.class);
        when(roof.roofBlocks(any())).thenCallRealMethod();
        // Location
        when(location.getBlockX()).thenReturn(5);
        when(location.getBlockY()).thenReturn(14);
        when(location.getBlockZ()).thenReturn(25);
        when(location.getWorld()).thenReturn(world);
        // Addon
        when(addon.wallBlocks(any())).thenCallRealMethod();
        // Block
        when(cache.getBlockType(any())).thenReturn(Material.GLASS);
        when(cache.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.GLASS);
        // Roof
        when(roof.getHeight()).thenReturn(ROOF_HEIGHT);
        walls = mock(Walls.class); // Mock after the server is setup
        when(walls.getMinX()).thenReturn(5);
        when(walls.getMaxX()).thenReturn(25);
        when(walls.getMinZ()).thenReturn(6);
        when(walls.getMaxZ()).thenReturn(26);
        when(walls.getFloor()).thenReturn(0);
        when(roof.getLocation()).thenReturn(location);

        // World
        when(cache.getEnvironment()).thenReturn(Environment.NORMAL);
        when(cache.getMaxHeight()).thenReturn(30);


        gf = new GreenhouseFinder(addon);
        cc = new CounterCheck();
    }

    @AfterEach
    void tearDown() {
        if (mockSession != null) {
            mockSession.finishMocking();
        }
        MockBukkit.unmock();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkGreenhouse(AsyncWorldCache, Roof, Walls)}.
     */
    @Test
    void testCheckGreenhouse() {
        gf.checkGreenhouse(cache, roof, walls).thenAccept(result -> {
            assertTrue(result.isEmpty()); // Success
            assertEquals(441, gf.getWallBlockCount());
            assertEquals(0, gf.getWallDoors());
            assertEquals(0, gf.getGhHopper());
        });
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkErrors(world.bentobox.greenhouses.greenhouse.Roof, int)}.
     */
    @Test
    void testCheckErrors() {
        Collection<GreenhouseResult> result = gf.checkErrors(roof, 0);
        assertTrue(result.isEmpty());

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkErrors(world.bentobox.greenhouses.greenhouse.Roof, int)}.
     */
    @Test
    void testCheckErrorsFailBelow() {
        Collection<GreenhouseResult> result = gf.checkErrors(roof, ROOF_HEIGHT);
        assertFalse(result.isEmpty());
        result.forEach(gr -> assertEquals(GreenhouseResult.FAIL_BELOW, gr));

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkErrors(world.bentobox.greenhouses.greenhouse.Roof, int)}.
     */
    @Test
    void testCheckErrorsFailAllErrors() {
        gf.setGhHopper(2);
        gf.setWallDoors(10);
        gf.setAirHoles(true);
        gf.setInCeiling(true);
        gf.setOtherBlocks(true);
        gf.setOtherBlockLayer(ROOF_HEIGHT + 1);
        Collection<GreenhouseResult> result = gf.checkErrors(roof, ROOF_HEIGHT);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(GreenhouseResult.FAIL_BELOW));
        assertTrue(result.contains(GreenhouseResult.FAIL_TOO_MANY_HOPPERS));
        assertTrue(result.contains(GreenhouseResult.FAIL_TOO_MANY_DOORS));
        assertTrue(result.contains(GreenhouseResult.FAIL_UNEVEN_WALLS));
        assertTrue(result.contains(GreenhouseResult.FAIL_HOLE_IN_ROOF));
        assertEquals(5, result.size());

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkErrors(world.bentobox.greenhouses.greenhouse.Roof, int)}.
     */
    @Test
    void testCheckErrorsFailHoleInWall() {
        gf.setGhHopper(2);
        gf.setWallDoors(10);
        gf.setAirHoles(true);
        gf.setInCeiling(false);
        gf.setOtherBlocks(true);
        gf.setOtherBlockLayer(5);
        Collection<GreenhouseResult> result = gf.checkErrors(roof, 0);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(GreenhouseResult.FAIL_TOO_MANY_HOPPERS));
        assertTrue(result.contains(GreenhouseResult.FAIL_TOO_MANY_DOORS));
        assertTrue(result.contains(GreenhouseResult.FAIL_BAD_WALL_BLOCKS));
        assertTrue(result.contains(GreenhouseResult.FAIL_HOLE_IN_WALL));
        assertEquals(4, result.size());

    }


    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkBlock(CounterCheck, Material, Roof, Walls, Vector)}
     */
    @Test
    void testCheckBlockRoofHeight() {
        // Glass block should be ok at roof height
        assertTrue(gf.checkBlock(cc, Material.GLASS, roof, walls, new Vector(0, ROOF_HEIGHT, 0)));
    }


    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkBlock(CounterCheck, Material, Roof, Walls, Vector)}
     */
    @Test
    void testCheckBlockAir() {
        // Glass air should be not allowed at roof height
        assertFalse(gf.checkBlock(cc, Material.AIR, roof, walls, new Vector(0, ROOF_HEIGHT, 0)));
    }


    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(CounterCheck, Material, Vector)}
     */
    @Test
    void testCheckDoorsHoppers() {
        // Doors are 2 blocks
        for (int i = 0; i < 8; i++) {
            assertTrue(gf.checkDoorsHoppers(cc, Material.ACACIA_DOOR, new Vector(0,0,0)), "Door number " + i);
        }
        // 5th door will fail
        assertFalse(gf.checkDoorsHoppers(cc, Material.ACACIA_DOOR, new Vector(0,0,0)));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(CounterCheck, Material, Vector)}
     */
    @Test
    void testCheckDoorsHoppersTrapdoors() {
        // Trapdoors are one block
        for (int i = 0; i < 4; i++) {
            assertTrue(gf.checkDoorsHoppers(cc, Material.BIRCH_TRAPDOOR, new Vector(0,0,0)), "Door number " + i);
        }
        // 5th door will fail
        assertFalse(gf.checkDoorsHoppers(cc, Material.BIRCH_TRAPDOOR, new Vector(0,0,0)));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(CounterCheck, Material, Vector)}.
     */
    @Test
    void testCheckDoorsHoppersHopper() {
        Greenhouse gh = new Greenhouse(world, walls, 10);
        // Set the greenhouse so the world is known
        gf.setGh(gh);
        CounterCheck counter = new CounterCheck();
        assertTrue(gf.checkDoorsHoppers(counter, Material.HOPPER, new Vector(5,14,25)));
        assertTrue(gf.getRedGlass().isEmpty());
        assertEquals(5, gf.getGh().getRoofHopperLocation().getBlockX());
        assertEquals(14, gf.getGh().getRoofHopperLocation().getBlockY());
        assertEquals(25, gf.getGh().getRoofHopperLocation().getBlockZ());
        assertFalse(gf.checkDoorsHoppers(counter, Material.HOPPER, new Vector(5,14,25)));
    }


    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#getGh()}.
     */
    @Test
    void testGetGh() {
        assertNull(gf.getGh());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#getRedGlass()}.
     */
    @Test
    void testGetRedGlass() {
        assertTrue(gf.getRedGlass().isEmpty());
    }

}
