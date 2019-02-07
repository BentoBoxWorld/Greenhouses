/**
 * 
 */
package world.bentobox.greenhouses.greenhouse;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


/**
 * @author tastybento
 *
 */
public class RoofTest {

    private Roof roof;
    private Block block;
    private Location location;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        location = mock(Location.class);
        World world = mock(World.class);
        when(world.getMaxHeight()).thenReturn(255);
        block = mock(Block.class);
        when(block.getType()).thenReturn(Material.GLASS);
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(block);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(10);
        when(location.getBlockY()).thenReturn(10);
        when(location.getBlockZ()).thenReturn(10);
        
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#Roof(org.bukkit.Location)}.
     */
    @Test
    public void testRoof() {
        //roof = new Roof(location);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getMinX()}.
     */
    @Test
    public void testGetMinX() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#setMinX(int)}.
     */
    @Test
    public void testSetMinX() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getMaxX()}.
     */
    @Test
    public void testGetMaxX() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#setMaxX(int)}.
     */
    @Test
    public void testSetMaxX() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getMinZ()}.
     */
    @Test
    public void testGetMinZ() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#setMinZ(int)}.
     */
    @Test
    public void testSetMinZ() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getMaxZ()}.
     */
    @Test
    public void testGetMaxZ() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#setMaxZ(int)}.
     */
    @Test
    public void testSetMaxZ() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getArea()}.
     */
    @Test
    public void testGetArea() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#isRoofFound()}.
     */
    @Test
    public void testIsRoofFound() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getHeight()}.
     */
    @Test
    public void testGetHeight() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#getLocation()}.
     */
    @Test
    public void testGetLocation() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.Roof#toString()}.
     */
    @Test
    public void testToString() {
        fail("Not yet implemented"); // TODO
    }

}
