package world.bentobox.greenhouses.greenhouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import world.bentobox.greenhouses.Settings;
import world.bentobox.greenhouses.world.AsyncWorldCache;

class RoofTest {

    private Roof roof;
    @Mock
    private Location location;
    @Mock
    private World world;
    @Mock
    private Greenhouses addon;
    private Settings s;
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

        s = new Settings();
        when(addon.getSettings()).thenReturn(s);
        when(world.getMaxHeight()).thenReturn(255);

        when(cache.getBlockType(any())).thenReturn(Material.AIR, Material.AIR, Material.AIR, Material.AIR,
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
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(10);
        when(location.getBlockY()).thenReturn(10);
        when(location.getBlockZ()).thenReturn(10);
        when(location.clone()).thenReturn(location);

        roof = new Roof(cache, location, addon);
        assertTrue(roof.findRoof(new Vector(10, 10, 10)));
    }

    @AfterEach
    void tearDown() {
        if (mockSession != null) {
            mockSession.finishMocking();
        }
        MockBukkit.unmock();
        Mockito.framework().clearInlineMocks();
    }

    @Test
    void testNoGlass() {
        when(cache.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR);
        roof = new Roof(cache, location, addon);
        assertFalse(roof.findRoof(new Vector(10, 10, 10)));
    }

    @Test
    void testGetMinX() {
        assertEquals(-9, roof.getMinX());
    }

    @Test
    void testGetMaxX() {
        assertEquals(28, roof.getMaxX());
    }

    @Test
    void testGetMinZ() {
        assertEquals(-9, roof.getMinZ());
    }

    @Test
    void testGetMaxZ() {
        assertEquals(29, roof.getMaxZ());
    }

    @Test
    void testGetArea() {
        assertEquals(1406, roof.getArea());
    }

    @Test
    void testGetHeight() {
        assertEquals(14, roof.getHeight());
    }

    @Test
    void testGetLocation() {
        assertEquals(location, roof.getLocation());
    }

    @Test
    void testToString() {
        assertEquals("Roof [height=14, roofFound=true, minX=-9, maxX=28, minZ=-9, maxZ=29]", roof.toString());
    }

    @Test
    void testWallBlocks() {
        assertFalse(roof.roofBlocks(Material.ACACIA_BOAT));
        assertTrue(roof.roofBlocks(Material.GLASS));
        assertTrue(roof.roofBlocks(Material.GLOWSTONE));
        assertFalse(roof.roofBlocks(Material.ACACIA_DOOR));
        assertTrue(roof.roofBlocks(Material.HOPPER));
        assertTrue(roof.roofBlocks(Material.PURPLE_STAINED_GLASS_PANE));
        assertTrue(roof.roofBlocks(Material.BIRCH_TRAPDOOR));
    }

    @Test
    void testWallBlocksNoGlowStoneNoPanes() {
        s.setAllowGlowstone(false);
        s.setAllowPanes(false);
        assertFalse(roof.roofBlocks(Material.ACACIA_BOAT));
        assertTrue(roof.roofBlocks(Material.GLASS));
        assertFalse(roof.roofBlocks(Material.GLOWSTONE));
        assertFalse(roof.roofBlocks(Material.ACACIA_DOOR));
        assertTrue(roof.roofBlocks(Material.HOPPER));
        assertFalse(roof.roofBlocks(Material.PURPLE_STAINED_GLASS_PANE));
        assertTrue(roof.roofBlocks(Material.BIRCH_TRAPDOOR));
    }
}
