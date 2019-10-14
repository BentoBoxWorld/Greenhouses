package world.bentobox.greenhouses.greenhouse;

import static org.junit.Assert.assertEquals;
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
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public class RoofTest {

    private Roof roof;
    @Mock
    private Block block;
    @Mock
    private Location location;
    @Mock
    private World world;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        when(world.getMaxHeight()).thenReturn(255);
        // Block
        when(block.getType()).thenReturn(Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR, 
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

        // Test
        roof = new Roof(location);
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
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#isRoofFound()}.
     */
    @Test
    public void testIsRoofFound() {
        assertTrue(roof.isRoofFound());
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

}
