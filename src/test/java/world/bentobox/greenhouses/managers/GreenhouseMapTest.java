package world.bentobox.greenhouses.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

/**
 * Tests for {@link GreenhouseMap}, in particular the overlap detection used when loading
 * persisted greenhouses.
 *
 * @author tastybento
 */
class GreenhouseMapTest {

    @Mock
    private Greenhouses addon;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private World world;

    private GreenhouseMap map;
    private BiomeRecipe br;
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

        when(addon.getIslands()).thenReturn(im);
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));
        br = mock(BiomeRecipe.class);
        when(br.getBiome()).thenReturn(mock(Biome.class));

        map = new GreenhouseMap(addon);
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
     * Make a mock greenhouse whose bounding box runs from (minX, 60, minZ) to (maxX, 70, maxZ)
     */
    private Greenhouse gh(int minX, int minZ, int maxX, int maxZ) {
        Greenhouse gh = mock(Greenhouse.class);
        Location loc = new Location(world, minX, 60, minZ);
        when(gh.getLocation()).thenReturn(loc);
        when(gh.getWorld()).thenReturn(world);
        when(gh.getBoundingBox()).thenReturn(new BoundingBox(minX, 60, minZ, maxX, 70, maxZ));
        when(gh.getBiomeRecipe()).thenReturn(br);
        return gh;
    }

    /**
     * Test method for {@link GreenhouseMap#getOverlappingGreenhouse(Greenhouse)}.
     */
    @Test
    void testGetOverlappingGreenhouseNoOverlap() {
        Greenhouse first = gh(0, 0, 10, 10);
        assertEquals(GreenhouseResult.SUCCESS, map.addGreenhouse(first));
        // Sits alongside the first one, sharing no volume
        Greenhouse second = gh(10, 0, 20, 10);
        assertTrue(map.getOverlappingGreenhouse(second).isEmpty());
        assertEquals(GreenhouseResult.SUCCESS, map.addGreenhouse(second));
        assertEquals(2, map.getSize());
    }

    /**
     * Test method for {@link GreenhouseMap#getOverlappingGreenhouse(Greenhouse)}.
     */
    @Test
    void testGetOverlappingGreenhouseReportsConflict() {
        Greenhouse first = gh(0, 0, 10, 10);
        assertEquals(GreenhouseResult.SUCCESS, map.addGreenhouse(first));
        Greenhouse overlapper = gh(5, 5, 15, 15);
        Optional<Greenhouse> conflict = map.getOverlappingGreenhouse(overlapper);
        assertTrue(conflict.isPresent());
        // The conflicting greenhouse must be identified so it can be logged
        assertEquals(first, conflict.get());
        assertEquals(GreenhouseResult.FAIL_OVERLAPPING, map.addGreenhouse(overlapper));
        // The overlapping greenhouse is not added
        assertEquals(1, map.getSize());
    }

    /**
     * Test method for {@link GreenhouseMap#getOverlappingGreenhouse(Greenhouse)}.
     */
    @Test
    void testGetOverlappingGreenhouseDifferentWorld() {
        Greenhouse first = gh(0, 0, 10, 10);
        assertEquals(GreenhouseResult.SUCCESS, map.addGreenhouse(first));
        // Same coords, different world
        World otherWorld = mock(World.class);
        Greenhouse other = mock(Greenhouse.class);
        when(other.getLocation()).thenReturn(new Location(otherWorld, 0, 60, 0));
        when(other.getWorld()).thenReturn(otherWorld);
        when(other.getBoundingBox()).thenReturn(new BoundingBox(0, 60, 0, 10, 70, 10));
        when(other.getBiomeRecipe()).thenReturn(br);
        assertTrue(map.getOverlappingGreenhouse(other).isEmpty());
        assertEquals(GreenhouseResult.SUCCESS, map.addGreenhouse(other));
    }

    /**
     * Test method for {@link GreenhouseMap#getOverlappingGreenhouse(Greenhouse)}.
     */
    @Test
    void testGetOverlappingGreenhouseNullLocation() {
        Greenhouse gh = mock(Greenhouse.class);
        when(gh.getLocation()).thenReturn(null);
        assertTrue(map.getOverlappingGreenhouse(gh).isEmpty());
    }

    /**
     * Test method for {@link GreenhouseMap#getOverlappingGreenhouse(Greenhouse)}.
     */
    @Test
    void testGetOverlappingGreenhouseEmptyMap() {
        Greenhouse gh = gh(0, 0, 10, 10);
        // Nothing loaded yet, so nothing to overlap with
        assertFalse(map.getOverlappingGreenhouse(gh).isPresent());
        assertEquals(GreenhouseResult.SUCCESS, map.addGreenhouse(gh));
    }
}
