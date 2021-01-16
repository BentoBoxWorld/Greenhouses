package world.bentobox.greenhouses.greenhouse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.Settings;
import world.bentobox.greenhouses.world.AsyncWorldCache;


/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Greenhouses.class)
public class RoofTest {

    private Roof roof;
    @Mock
    private Block block;
    @Mock
    private Location location;
    @Mock
    private World world;
    @Mock
    private Greenhouses addon;
    private Settings s;
    private AsyncWorldCache cache;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Greenhouses.class, Mockito.RETURNS_MOCKS);
        when(Greenhouses.getInstance()).thenReturn(addon);
        s = new Settings();
        when(addon.getSettings()).thenReturn(s);

        when(world.getMaxHeight()).thenReturn(255);
        // Block
        when(block.getType()).thenReturn(Material.AIR, Material.AIR, Material.AIR, Material.AIR,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.AIR,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.AIR,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.AIR,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS, Material.GLASS, Material.GLASS,
                Material.AIR);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(world.getBlockAt(any(Location.class))).thenReturn(block);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(10);
        when(location.getBlockY()).thenReturn(10);
        when(location.getBlockZ()).thenReturn(10);
        when(location.getBlock()).thenReturn(block);
        when(location.clone()).thenReturn(location);

        cache = new AsyncWorldCache(world);
        // Test
        roof = new Roof(cache, location);
        roof.findRoof();
    }

    @Test
    public void testNoGlass() {
        when(block.getType()).thenReturn(Material.AIR);
        roof = new Roof(cache, location);
        roof.findRoof();
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getMinX()}.
     */
    @Test
    public void testGetMinX() {
        assertEquals(-9, roof.getMinX());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getMaxX()}.
     */
    @Test
    public void testGetMaxX() {
        assertEquals(28, roof.getMaxX());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getMinZ()}.
     */
    @Test
    public void testGetMinZ() {
        assertEquals(-9, roof.getMinZ());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getMaxZ()}.
     */
    @Test
    public void testGetMaxZ() {
        assertEquals(29, roof.getMaxZ());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getArea()}.
     */
    @Test
    public void testGetArea() {
        assertEquals(1406, roof.getArea());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getHeight()}.
     */
    @Test
    public void testGetHeight() {
        assertEquals(14, roof.getHeight());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getLocation()}.
     */
    @Test
    public void testGetLocation() {
        assertEquals(location, roof.getLocation());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#toString()}.
     */
    @Test
    public void testToString() {
        assertTrue(roof.toString().endsWith("minX=-9, maxX=28, minZ=-9, maxZ=29, height=14, roofFound=true]"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#roofBlocks(org.bukkit.Material)}.
     */
    @Test
    public void testWallBlocks() {
        assertFalse(Roof.roofBlocks(Material.ACACIA_BOAT));
        assertTrue(Roof.roofBlocks(Material.GLASS));
        assertTrue(Roof.roofBlocks(Material.GLOWSTONE));
        assertFalse(Roof.roofBlocks(Material.ACACIA_DOOR));
        assertTrue(Roof.roofBlocks(Material.HOPPER));
        assertTrue(Roof.roofBlocks(Material.PURPLE_STAINED_GLASS_PANE));
        assertTrue(Roof.roofBlocks(Material.BIRCH_TRAPDOOR));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#roofBlocks(org.bukkit.Material)}.
     */
    @Test
    public void testWallBlocksNoGlowStoneNoPanes() {
        s.setAllowGlowstone(false);
        s.setAllowPanes(false);
        assertFalse(Roof.roofBlocks(Material.ACACIA_BOAT));
        assertTrue(Roof.roofBlocks(Material.GLASS));
        assertFalse(Roof.roofBlocks(Material.GLOWSTONE));
        assertFalse(Roof.roofBlocks(Material.ACACIA_DOOR));
        assertTrue(Roof.roofBlocks(Material.HOPPER));
        assertFalse(Roof.roofBlocks(Material.PURPLE_STAINED_GLASS_PANE));
        assertTrue(Roof.roofBlocks(Material.BIRCH_TRAPDOOR));
    }
}
