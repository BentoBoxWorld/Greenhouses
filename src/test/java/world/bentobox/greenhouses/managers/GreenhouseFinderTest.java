package world.bentobox.greenhouses.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.Roof;
import world.bentobox.greenhouses.greenhouse.Walls;
import world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.mocks.ServerMocks;
import world.bentobox.greenhouses.world.AsyncWorldCache;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, User.class})
public class GreenhouseFinderTest {

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

    @Before
    public void setUp() {
        ServerMocks.newServer();

        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Tag.TRAPDOORS.isTagged(Material.BIRCH_TRAPDOOR)).thenReturn(true);
        // Declare mock after mocking Bukkit
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

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkGreenhouse(AsyncWorldCache, Roof, Walls)}.
     */
    @Test
    public void testCheckGreenhouse() {
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
    public void testCheckErrors() {
        Collection<GreenhouseResult> result = gf.checkErrors(roof, 0);
        assertTrue(result.isEmpty());

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkErrors(world.bentobox.greenhouses.greenhouse.Roof, int)}.
     */
    @Test
    public void testCheckErrorsFailBelow() {
        Collection<GreenhouseResult> result = gf.checkErrors(roof, ROOF_HEIGHT);
        assertFalse(result.isEmpty());
        result.forEach(gr -> assertEquals(GreenhouseResult.FAIL_BELOW, gr));

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkErrors(world.bentobox.greenhouses.greenhouse.Roof, int)}.
     */
    @Test
    public void testCheckErrorsFailAllErrors() {
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
    public void testCheckErrorsFailHoleInWall() {
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
    public void testCheckBlockRoofHeight() {
        // Glass block should be ok at roof height
        assertTrue(gf.checkBlock(cc, Material.GLASS, roof, walls, new Vector(0, ROOF_HEIGHT, 0)));
    }


    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkBlock(CounterCheck, Material, Roof, Walls, Vector)}
     */
    @Test
    public void testCheckBlockAir() {
        // Glass air should be not allowed at roof height
        assertFalse(gf.checkBlock(cc, Material.AIR, roof, walls, new Vector(0, ROOF_HEIGHT, 0)));
    }


    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(CounterCheck, Material, Vector)}
     */
    @Test
    public void testCheckDoorsHoppers() {
        // Doors are 2 blocks
        when(Tag.DOORS.isTagged(any(Material.class))).thenReturn(true);
        for (int i = 0; i < 8; i++) {
            assertTrue("Door number " + i, gf.checkDoorsHoppers(cc, Material.ACACIA_DOOR, new Vector(0,0,0)));
        }
        // 5th door will fail
        assertFalse(gf.checkDoorsHoppers(cc, Material.ACACIA_DOOR, new Vector(0,0,0)));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(CounterCheck, Material, Vector)}
     */
    @Test
    public void testCheckDoorsHoppersTrapdoors() {
        // Trapdoors are one block
        for (int i = 0; i < 4; i++) {
            assertTrue("Door number " + i, gf.checkDoorsHoppers(cc, Material.BIRCH_TRAPDOOR, new Vector(0,0,0)));
        }
        // 5th door will fail
        assertFalse(gf.checkDoorsHoppers(cc, Material.BIRCH_TRAPDOOR, new Vector(0,0,0)));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(CounterCheck, Material, Vector)}.
     */
    @Test
    public void testCheckDoorsHoppersHopper() {
        Greenhouse gh = new Greenhouse(world, walls, 10);
        // Set the greenhouse so the world is known
        gf.setGh(gh);
        when(Tag.DOORS.isTagged(any(Material.class))).thenReturn(false);
        CounterCheck cc = new CounterCheck();
        assertTrue(gf.checkDoorsHoppers(cc, Material.HOPPER, new Vector(5,14,25)));
        assertTrue(gf.getRedGlass().isEmpty());
        assertEquals(5, gf.getGh().getRoofHopperLocation().getBlockX());
        assertEquals(14, gf.getGh().getRoofHopperLocation().getBlockY());
        assertEquals(25, gf.getGh().getRoofHopperLocation().getBlockZ());
        assertFalse(gf.checkDoorsHoppers(cc, Material.HOPPER, new Vector(5,14,25)));
    }


    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#getGh()}.
     */
    @Test
    public void testGetGh() {
        assertNull(gf.getGh());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#getRedGlass()}.
     */
    @Test
    public void testGetRedGlass() {
        assertTrue(gf.getRedGlass().isEmpty());
    }

}
