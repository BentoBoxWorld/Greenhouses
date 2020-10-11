package world.bentobox.greenhouses.greenhouse;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.greenhouses.greenhouse.Walls.WallFinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public class WallsTest {

    @Mock
    private Roof roof;
    @Mock
    private Block block;
    @Mock
    private Location location;
    @Mock
    private World world;
    /**
     * Class under test
     */
    private Walls walls;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        walls = new Walls();
        when(world.getMaxHeight()).thenReturn(255);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(world.getBlockAt(any(Location.class))).thenReturn(block);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(10);
        when(location.getBlockY()).thenReturn(10);
        when(location.getBlockZ()).thenReturn(10);
        when(location.getBlock()).thenReturn(block);
        when(location.clone()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        when(block.getType()).thenReturn(Material.GLASS);
        when(roof.getHeight()).thenReturn(1);
        when(roof.getLocation()).thenReturn(location);

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#findWalls(world.bentobox.greenhouses.greenhouse.Roof)}.
     */
    @Test
    public void testFindWalls() {
        walls.findWalls(roof);
        assertEquals("Walls [minX=-2, maxX=11, minZ=-2, maxZ=11, floor=0]", walls.toString());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#lookAround(org.bukkit.Location, world.bentobox.greenhouses.greenhouse.Walls.WallFinder, world.bentobox.greenhouses.greenhouse.Roof)}.
     */
    @Test
    public void testLookAround() {
        WallFinder wf = walls.new WallFinder();
        walls.lookAround(location, wf, roof);
        assertTrue(wf.stopMaxX);
        assertTrue(wf.stopMaxZ);
        assertFalse(wf.stopMinX);
        assertFalse(wf.stopMinZ);
        assertEquals(1, wf.radiusMinX);
        assertEquals(0, wf.radiusMaxX);
        assertEquals(1, wf.radiusMinZ);
        assertEquals(0, wf.radiusMaxZ);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#analyzeFindings(world.bentobox.greenhouses.greenhouse.Walls.WallFinder, world.bentobox.greenhouses.greenhouse.Roof)}.
     */
    @Test
    public void testAnalyzeFindings() {
        WallFinder wf = walls.new WallFinder();
        walls.analyzeFindings(wf, roof);
        assertFalse(wf.stopMaxX);
        assertFalse(wf.stopMaxZ);
        assertFalse(wf.stopMinX);
        assertFalse(wf.stopMinZ);
        assertEquals(1, wf.radiusMinX);
        assertEquals(1, wf.radiusMaxX);
        assertEquals(1, wf.radiusMinZ);
        assertEquals(1, wf.radiusMaxZ);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#analyzeFindings(world.bentobox.greenhouses.greenhouse.Walls.WallFinder, world.bentobox.greenhouses.greenhouse.Roof)}.
     */
    @Test
    public void testAnalyzeFindingsStop() {
        walls.minX = -1;
        walls.maxX = 1;
        walls.minZ = -1;
        walls.maxZ = 1;
        WallFinder wf = walls.new WallFinder();
        walls.analyzeFindings(wf, roof);
        assertTrue(wf.stopMaxX);
        assertTrue(wf.stopMaxZ);
        assertTrue(wf.stopMinX);
        assertTrue(wf.stopMinZ);
        assertEquals(0, wf.radiusMinX);
        assertEquals(0, wf.radiusMaxX);
        assertEquals(0, wf.radiusMinZ);
        assertEquals(0, wf.radiusMaxZ);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#lookAtBlockFaces(world.bentobox.greenhouses.greenhouse.Walls.WallFinder, org.bukkit.World, int, int, int)}.
     */
    @Test
    public void testLookAtBlockFaces() {
        WallFinder wf = walls.new WallFinder();
        walls.lookAtBlockFaces(wf, world, 0, 5, -1);
        assertTrue(wf.stopMaxX);
        assertTrue(wf.stopMaxZ);
        assertTrue(wf.stopMinX);
        assertTrue(wf.stopMinZ);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#lookAtBlockFaces(world.bentobox.greenhouses.greenhouse.Walls.WallFinder, org.bukkit.World, int, int, int)}.
     */
    @Test
    public void testLookAtBlockFacesNoGlass() {
        when(block.getType()).thenReturn(Material.AIR);
        WallFinder wf = walls.new WallFinder();
        walls.lookAtBlockFaces(wf, world, 0, 5, -1);
        assertFalse(wf.stopMaxX);
        assertFalse(wf.stopMaxZ);
        assertFalse(wf.stopMinX);
        assertFalse(wf.stopMinZ);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#getFloorY(org.bukkit.World, int, int, int, int, int)}.
     */
    @Test
    public void testGetFloorYZeroY() {
        assertEquals(0, walls.getFloorY(world, 10, 0, 1, 0, 1));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#getFloorY(org.bukkit.World, int, int, int, int, int)}.
     */
    @Test
    public void testGetFloorY() {
        when(block.getType()).thenReturn(Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS,
                Material.AIR);
        assertEquals(8, walls.getFloorY(world, 10, 0, 1, 0, 1));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#wallBlocks(org.bukkit.Material)}.
     */
    @Test
    public void testWallBlocks() {
        assertFalse(Walls.wallBlocks(Material.ACACIA_BOAT));
        assertTrue(Walls.wallBlocks(Material.GLASS));
        assertTrue(Walls.wallBlocks(Material.ACACIA_DOOR));
        assertTrue(Walls.wallBlocks(Material.HOPPER));
        assertTrue(Walls.wallBlocks(Material.PURPLE_STAINED_GLASS_PANE));
        assertFalse(Walls.wallBlocks(Material.BIRCH_TRAPDOOR));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#getFloor()}.
     */
    @Test
    public void testGetFloor() {
        assertEquals(0, walls.getFloor());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#getWidth()}.
     */
    @Test
    public void testGetWidth() {
        assertEquals(0, walls.getWidth());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#getLength()}.
     */
    @Test
    public void testGetLength() {
        assertEquals(0, walls.getLength());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Walls#toString()}.
     */
    @Test
    public void testToString() {
        assertEquals("Walls [minX=0, maxX=0, minZ=0, maxZ=0, floor=0]", walls.toString());
    }

}
