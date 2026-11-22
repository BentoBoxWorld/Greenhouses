package world.bentobox.greenhouses.greenhouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import world.bentobox.greenhouses.greenhouse.Walls.WallFinder;
import world.bentobox.greenhouses.world.AsyncWorldCache;

class WallsTest {

    private Roof roof;
    @Mock
    private Location location;
    @Mock
    private World world;
    private Walls walls;
    @Mock
    private Greenhouses addon;
    private Settings s;
    @Mock
    private AsyncWorldCache cache;
    private CompletableFuture<Walls> r;
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

        roof = mock(Roof.class);
        s = new Settings();
        when(addon.getSettings()).thenReturn(s);
        when(addon.getPlugin()).thenReturn(null);
        when(addon.wallBlocks(any())).thenCallRealMethod();
        walls = new Walls(cache);
        when(world.getMaxHeight()).thenReturn(255);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(10);
        when(location.getBlockY()).thenReturn(10);
        when(location.getBlockZ()).thenReturn(10);
        when(location.clone()).thenReturn(location);
        when(cache.getBlockType(any())).thenReturn(Material.GLASS);
        when(cache.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.GLASS);
        when(roof.getHeight()).thenReturn(1);
        when(roof.getLocation()).thenReturn(location);
        r = new CompletableFuture<>();
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
    void testFindWalls() {
        walls.findWalls(r, roof);
        assertEquals("Walls [minX=-2, maxX=11, minZ=-2, maxZ=11, floor=0]", walls.toString());
    }

    @Test
    void testLookAround() {
        WallFinder wf = new WallFinder();
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

    @Test
    void testAnalyzeFindings() {
        WallFinder wf = new WallFinder();
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

    @Test
    void testAnalyzeFindingsStop() {
        walls.minX = -1;
        walls.maxX = 1;
        walls.minZ = -1;
        walls.maxZ = 1;
        WallFinder wf = new WallFinder();
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

    @Test
    void testLookAtBlockFaces() {
        WallFinder wf = new WallFinder();
        walls.lookAtBlockFaces(wf, 0, 5, -1);
        assertTrue(wf.stopMaxX);
        assertTrue(wf.stopMaxZ);
        assertTrue(wf.stopMinX);
        assertTrue(wf.stopMinZ);
    }

    @Test
    void testLookAtBlockFacesNoGlass() {
        when(cache.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR);
        WallFinder wf = new WallFinder();
        walls.lookAtBlockFaces(wf, 0, 5, -1);
        assertFalse(wf.stopMaxX);
        assertFalse(wf.stopMaxZ);
        assertFalse(wf.stopMinX);
        assertFalse(wf.stopMinZ);
    }

    @Test
    void testGetFloorYZeroY() {
        assertEquals(-64, walls.getFloorY(10, 0, 1, 0, 1, -64));
    }

    @Test
    void testGetFloorY() {
        when(cache.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS,
                Material.GLASS, Material.GLASS,
                Material.AIR);
        assertEquals(8, walls.getFloorY(10, 0, 1, 0, 1, -64));
    }

    @Test
    void testWallBlocks() {
        assertFalse(addon.wallBlocks(Material.ACACIA_BOAT));
        assertTrue(addon.wallBlocks(Material.GLASS));
        assertTrue(addon.wallBlocks(Material.GLOWSTONE));
        assertTrue(addon.wallBlocks(Material.ACACIA_DOOR));
        assertTrue(addon.wallBlocks(Material.HOPPER));
        assertTrue(addon.wallBlocks(Material.PURPLE_STAINED_GLASS_PANE));
        assertFalse(addon.wallBlocks(Material.BIRCH_TRAPDOOR));
    }

    @Test
    void testWallBlocksNoGlowStoneNoPanes() {
        s.setAllowGlowstone(false);
        s.setAllowPanes(false);
        assertFalse(addon.wallBlocks(Material.ACACIA_BOAT));
        assertTrue(addon.wallBlocks(Material.GLASS));
        assertFalse(addon.wallBlocks(Material.GLOWSTONE));
        assertTrue(addon.wallBlocks(Material.ACACIA_DOOR));
        assertTrue(addon.wallBlocks(Material.HOPPER));
        assertFalse(addon.wallBlocks(Material.PURPLE_STAINED_GLASS_PANE));
        assertFalse(addon.wallBlocks(Material.BIRCH_TRAPDOOR));
    }

    @Test
    void testGetFloor() {
        assertEquals(0, walls.getFloor());
    }

    @Test
    void testGetWidth() {
        assertEquals(0, walls.getWidth());
    }

    @Test
    void testGetLength() {
        assertEquals(0, walls.getLength());
    }

    @Test
    void testToString() {
        assertEquals("Walls [minX=0, maxX=0, minZ=0, maxZ=0, floor=0]", walls.toString());
    }
}
