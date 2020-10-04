package world.bentobox.greenhouses.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
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
    @Mock
    private Block block;
    private CounterCheck cc;
    @Mock
    private Roof roof;
    @Mock
    private Walls walls;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Location
        when(location.getBlockX()).thenReturn(5);
        when(location.getBlockY()).thenReturn(14);
        when(location.getBlockZ()).thenReturn(25);
        when(location.getWorld()).thenReturn(world);

        // Block
        when(block.getX()).thenReturn(5);
        when(block.getY()).thenReturn(14);
        when(block.getZ()).thenReturn(25);
        when(block.getType()).thenReturn(Material.GLASS);
        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);

        // Roof
        when(roof.getHeight()).thenReturn(ROOF_HEIGHT);
        when(walls.getMinX()).thenReturn(5);
        when(walls.getMaxX()).thenReturn(25);
        when(walls.getMinZ()).thenReturn(6);
        when(walls.getMaxZ()).thenReturn(26);
        when(walls.getFloor()).thenReturn(0);
        when(roof.getLocation()).thenReturn(location);

        // World
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(world.getMaxHeight()).thenReturn(30);


        gf = new GreenhouseFinder();
        cc = gf.new CounterCheck();
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkGreenhouse(world.bentobox.greenhouses.data.Greenhouse, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls)}.
     */
    @Test
    public void testCheckGreenhouse() {
        Greenhouse gh2 = new Greenhouse(world, walls, ROOF_HEIGHT);
        Set<GreenhouseResult> result = gf.checkGreenhouse(gh2, roof, walls);
        assertTrue(result.isEmpty()); // Success
        assertEquals(441, gf.getWallBlockCount());
        assertEquals(0, gf.getWallDoors());
        assertEquals(0, gf.getGhHopper());
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
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkBlock(world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls, org.bukkit.block.Block)}.
     */
    @Test
    public void testCheckBlock() {
        // Block has to be > roof height
        when(block.getY()).thenReturn(ROOF_HEIGHT + 1);
        Set<GreenhouseResult> result = gf.checkBlock(cc, roof, walls, block);
        result.forEach(gr -> assertEquals(GreenhouseResult.FAIL_BLOCKS_ABOVE, gr));
        gf.getRedGlass().forEach(l -> assertEquals(location, l));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkBlock(world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls, org.bukkit.block.Block)}.
     */
    @Test
    public void testCheckBlockRoofHeight() {
        // Block has to be > roof height
        when(block.getY()).thenReturn(ROOF_HEIGHT);
        Set<GreenhouseResult> result = gf.checkBlock(cc, roof, walls, block);
        assertTrue(result.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkBlock(world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls, org.bukkit.block.Block)}.
     */
    @Test
    public void testCheckBlockNether() {
        when(world.getEnvironment()).thenReturn(Environment.NETHER);
        // Block has to be > roof height
        when(block.getY()).thenReturn(ROOF_HEIGHT + 1);
        Set<GreenhouseResult> result = gf.checkBlock(cc, roof, walls, block);
        assertTrue(result.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkBlock(world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls, org.bukkit.block.Block)}.
     */
    @Test
    public void testCheckBlockAir() {
        when(block.isEmpty()).thenReturn(true);
        // Block has to be > roof height
        when(block.getY()).thenReturn(ROOF_HEIGHT + 1);
        Set<GreenhouseResult> result = gf.checkBlock(cc, roof, walls, block);
        assertTrue(result.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkWalls(org.bukkit.block.Block, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls, world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck)}.
     */
    @Test
    public void testCheckWallsAirHole() {
        // Make block AIR
        when(block.isEmpty()).thenReturn(true);
        when(block.getType()).thenReturn(Material.AIR);
        assertTrue(gf.checkWalls(block, roof, walls, cc));
        assertFalse(gf.getRedGlass().isEmpty());
        gf.getRedGlass().forEach(l -> assertEquals(location, l));
        assertTrue(cc.airHole);
        assertFalse(gf.isInCeiling());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkWalls(org.bukkit.block.Block, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls, world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck)}.
     */
    @Test
    public void testCheckWallsAirHoleInRoof() {
        // Make block AIR
        when(block.isEmpty()).thenReturn(true);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.getY()).thenReturn(ROOF_HEIGHT);
        assertTrue(gf.checkWalls(block, roof, walls, cc));
        assertFalse(gf.getRedGlass().isEmpty());
        gf.getRedGlass().stream().forEach(l -> assertEquals(location, l));
        assertTrue(cc.airHole);
        assertTrue(gf.isInCeiling());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkWalls(org.bukkit.block.Block, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls, world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck)}.
     */
    @Test
    public void testCheckWalls() {
        // Make block GLASS
        when(block.isEmpty()).thenReturn(false);
        when(block.getType()).thenReturn(Material.GLASS);
        assertTrue(gf.checkWalls(block, roof, walls, cc));
        assertTrue(gf.getRedGlass().isEmpty());
        assertFalse(cc.airHole);
        assertFalse(gf.isInCeiling());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkWalls(org.bukkit.block.Block, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls, world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck)}.
     */
    @Test
    public void testCheckWallsInRoof() {
        // Make block GLASS
        when(block.isEmpty()).thenReturn(false);
        when(block.getType()).thenReturn(Material.GLASS);
        when(block.getY()).thenReturn(ROOF_HEIGHT);
        assertTrue(gf.checkWalls(block, roof, walls, cc));
        assertTrue(gf.getRedGlass().isEmpty());
        assertFalse(cc.airHole);
        assertFalse(gf.isInCeiling());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkWalls(org.bukkit.block.Block, world.bentobox.greenhouses.greenhouse.Roof, world.bentobox.greenhouses.greenhouse.Walls, world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck)}.
     */
    @Test
    public void testCheckWallsNotInWall() {
        when(block.getX()).thenReturn(0);
        when(block.getY()).thenReturn(0);
        when(block.getZ()).thenReturn(0);
        // Make block GLASS
        when(block.isEmpty()).thenReturn(false);
        when(block.getType()).thenReturn(Material.GLASS);
        assertFalse(gf.checkWalls(block, roof, walls, cc));
        assertTrue(gf.getRedGlass().isEmpty());
        assertFalse(cc.airHole);
        assertFalse(gf.isInCeiling());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck, org.bukkit.block.Block)}.
     */
    @Test
    public void testCheckDoorsHoppers() {
        when(Tag.DOORS.isTagged(any(Material.class))).thenReturn(true);
        when(block.getType()).thenReturn(Material.ACACIA_DOOR);
        gf.checkDoorsHoppers(cc, block);
        assertTrue(gf.getRedGlass().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck, org.bukkit.block.Block)}.
     */
    @Test
    public void testCheckDoorsHoppersTooManyDoors() {
        gf.setWallDoors(8);
        when(Tag.DOORS.isTagged(any(Material.class))).thenReturn(true);
        when(block.getType()).thenReturn(Material.ACACIA_DOOR);
        CounterCheck cc = gf.new CounterCheck();
        gf.checkDoorsHoppers(cc, block);
        assertFalse(gf.getRedGlass().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck, org.bukkit.block.Block)}.
     */
    @Test
    public void testCheckDoorsHoppersHopper() {
        when(Tag.DOORS.isTagged(any(Material.class))).thenReturn(false);
        when(block.getType()).thenReturn(Material.HOPPER);
        when(block.getLocation()).thenReturn(location);
        CounterCheck cc = gf.new CounterCheck();
        gf.checkDoorsHoppers(cc, block);
        assertTrue(gf.getRedGlass().isEmpty());
        assertEquals(location, gf.getGh().getRoofHopperLocation());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#checkDoorsHoppers(world.bentobox.greenhouses.managers.GreenhouseFinder.CounterCheck, org.bukkit.block.Block)}.
     */
    @Test
    public void testCheckDoorsHoppersTooManyHoppers() {
        gf.setGhHopper(3);
        when(Tag.DOORS.isTagged(any(Material.class))).thenReturn(false);
        when(block.getType()).thenReturn(Material.HOPPER);
        when(block.getLocation()).thenReturn(location);
        CounterCheck cc = gf.new CounterCheck();
        gf.checkDoorsHoppers(cc, block);
        assertFalse(gf.getRedGlass().isEmpty());
        assertNull(gf.getGh().getRoofHopperLocation());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#getGh()}.
     */
    @Test
    public void testGetGh() {
        assertNotNull(gf.getGh());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.GreenhouseFinder#getRedGlass()}.
     */
    @Test
    public void testGetRedGlass() {
        assertTrue(gf.getRedGlass().isEmpty());
    }

}
