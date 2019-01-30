/**
 * 
 */
package world.bentobox.greenhouses.data.adapters;

import static org.junit.Assert.*;

import java.awt.Rectangle;

import org.junit.Before;
import org.junit.Test;

/**
 * @author tastybento
 *
 */
public class RectangleAdapterTest {

    private RectangleAdapter ra;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        ra = new RectangleAdapter();
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.adapters.RectangleAdapter#Serialize(java.lang.Object)}.
     */
    @Test
    public void testSerialize() {
        Rectangle rectangle = new Rectangle(10,20,30,40);;
        assertEquals("10,20,30,40", ra.serialize(rectangle));
    }
    
    /**
     * Test method for {@link world.bentobox.greenhouses.data.adapters.RectangleAdapter#serialize(java.lang.Object)}.
     */
    @Test
    public void testSerializeNull() {
        assertEquals("null", ra.serialize(null));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.adapters.RectangleAdapter#serialize(java.lang.Object)}.
     */
    @Test
    public void testDeserialize() {
        Rectangle rectangle = new Rectangle(10,20,30,40);;
        assertEquals(rectangle, ra.deserialize("10,20,30,40"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.adapters.RectangleAdapter#deserialize(java.lang.Object)}.
     */
    @Test
    public void testDeserializeNull() {
        assertNull(ra.deserialize("null"));
    }
}
