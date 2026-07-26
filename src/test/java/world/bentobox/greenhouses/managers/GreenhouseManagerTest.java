package world.bentobox.greenhouses.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.BoundingBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.managers.GreenhouseManager.UnloadedGreenhouse;
import world.bentobox.greenhouses.mocks.ServerMocks;

/**
 * Tests the admin-facing parts of {@link GreenhouseManager} - retention of records that could
 * not be loaded, lookup by ID, and deletion by ID.
 *
 * @author tastybento
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ GreenhouseManager.class })
// Database's static initializer calls DatabaseSetup.getDatabase(), which needs a live server
@SuppressStaticInitializationFor("world.bentobox.bentobox.database.Database")
public class GreenhouseManagerTest {

    private static final String ID_A = "aaaaaaaa-1111-1111-1111-111111111111";
    private static final String ID_B = "bbbbbbbb-2222-2222-2222-222222222222";
    private static final String ID_B2 = "bbbbbbbb-3333-3333-3333-333333333333";

    @Mock
    private Greenhouses addon;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private Island island;
    @Mock
    private World world;
    private Database<Greenhouse> handler;
    private GreenhouseManager manager;
    private BiomeRecipe br;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        ServerMocks.newServer();
        handler = mock(Database.class);
        when(addon.getIslands()).thenReturn(im);
        when(addon.getPlayers()).thenReturn(pm);
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        when(pm.getName(any())).thenReturn("tastybento");
        when(world.getName()).thenReturn("bskyblock_world");
        br = mock(BiomeRecipe.class);
        when(br.getBiome()).thenReturn(mock(Biome.class));
        when(br.getName()).thenReturn("PLAINS");
        manager = new GreenhouseManager(addon, handler);
    }

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
    }

    private Greenhouse gh(String id, int minX, int minZ, int maxX, int maxZ) {
        Greenhouse gh = mock(Greenhouse.class);
        when(gh.getUniqueId()).thenReturn(id);
        when(gh.getLocation()).thenReturn(new Location(world, minX, 60, minZ));
        when(gh.getWorld()).thenReturn(world);
        when(gh.getBoundingBox()).thenReturn(new BoundingBox(minX, 60, minZ, maxX, 62, maxZ));
        when(gh.getBiomeRecipe()).thenReturn(br);
        when(gh.getBiomeRecipeName()).thenReturn("PLAINS");
        return gh;
    }

    /**
     * Test method for {@link GreenhouseManager#reload()} and {@link GreenhouseManager#getUnloaded()}.
     */
    @Test
    public void testOverlappingRecordIsRetainedAsUnloaded() {
        Greenhouse first = gh(ID_A, 0, 0, 10, 10);
        Greenhouse overlapper = gh(ID_B, 5, 5, 15, 15);
        when(handler.loadObjects()).thenReturn(List.of(first, overlapper));

        manager.reload();

        // The first loads, the overlapping one is kept aside rather than silently dropped
        assertEquals(1, manager.getMap().getSize());
        List<UnloadedGreenhouse> unloaded = manager.getUnloaded();
        assertEquals(1, unloaded.size());
        assertEquals(ID_B, unloaded.get(0).greenhouse().getUniqueId());
        assertEquals(GreenhouseResult.FAIL_OVERLAPPING, unloaded.get(0).reason());
        // Skipped records must not be deleted from the database
        verify(handler, never()).deleteObject(overlapper);
    }

    /**
     * Test method for {@link GreenhouseManager#reload()}.
     */
    @Test
    public void testReloadClearsPreviousUnloaded() {
        List<Greenhouse> both = List.of(gh(ID_A, 0, 0, 10, 10), gh(ID_B, 5, 5, 15, 15));
        when(handler.loadObjects()).thenReturn(both);
        manager.reload();
        assertEquals(1, manager.getUnloaded().size());
        // Second reload with no conflict should leave nothing unloaded
        List<Greenhouse> one = List.of(gh(ID_A, 0, 0, 10, 10));
        when(handler.loadObjects()).thenReturn(one);
        manager.reload();
        assertTrue(manager.getUnloaded().isEmpty());
    }

    /**
     * Test method for {@link GreenhouseManager#getGreenhouseById(String)}.
     */
    @Test
    public void testGetGreenhouseByIdFullId() {
        List<Greenhouse> loaded = List.of(gh(ID_A, 0, 0, 10, 10));
        when(handler.loadObjects()).thenReturn(loaded);
        manager.reload();
        assertTrue(manager.getGreenhouseById(ID_A).isPresent());
        assertEquals(ID_A, manager.getGreenhouseById(ID_A).get().getUniqueId());
    }

    /**
     * Test method for {@link GreenhouseManager#getGreenhouseById(String)}.
     */
    @Test
    public void testGetGreenhouseByIdPrefixAndCase() {
        List<Greenhouse> loaded = List.of(gh(ID_A, 0, 0, 10, 10));
        when(handler.loadObjects()).thenReturn(loaded);
        manager.reload();
        assertTrue(manager.getGreenhouseById("aaaaaaaa").isPresent());
        assertTrue(manager.getGreenhouseById("AAAAAAAA").isPresent());
    }

    /**
     * Test method for {@link GreenhouseManager#getGreenhouseById(String)}.
     */
    @Test
    public void testGetGreenhouseByIdFindsUnloadedRecords() {
        List<Greenhouse> loaded = List.of(gh(ID_A, 0, 0, 10, 10), gh(ID_B, 5, 5, 15, 15));
        when(handler.loadObjects()).thenReturn(loaded);
        manager.reload();
        // The overlapping record is not in the map but must still be findable
        assertTrue(manager.getMap().getGreenhouses().stream().noneMatch(g -> g.getUniqueId().equals(ID_B)));
        assertTrue(manager.getGreenhouseById(ID_B).isPresent());
    }

    /**
     * Test method for {@link GreenhouseManager#getGreenhouseById(String)}.
     */
    @Test
    public void testGetGreenhouseByIdAmbiguousPrefixIsNotAMatch() {
        // Two IDs sharing the "bbbbbbbb" prefix - one loads, one overlaps
        List<Greenhouse> loaded = List.of(gh(ID_B, 0, 0, 10, 10), gh(ID_B2, 5, 5, 15, 15));
        when(handler.loadObjects()).thenReturn(loaded);
        manager.reload();
        assertFalse(manager.getGreenhouseById("bbbbbbbb").isPresent());
        // The full IDs still resolve
        assertTrue(manager.getGreenhouseById(ID_B).isPresent());
        assertTrue(manager.getGreenhouseById(ID_B2).isPresent());
    }

    /**
     * Test method for {@link GreenhouseManager#getGreenhouseById(String)}.
     */
    @Test
    public void testGetGreenhouseByIdUnknownOrEmpty() {
        List<Greenhouse> loaded = List.of(gh(ID_A, 0, 0, 10, 10));
        when(handler.loadObjects()).thenReturn(loaded);
        manager.reload();
        assertFalse(manager.getGreenhouseById("zzzz").isPresent());
        assertFalse(manager.getGreenhouseById("").isPresent());
        assertFalse(manager.getGreenhouseById(null).isPresent());
    }

    /**
     * Test method for {@link GreenhouseManager#deleteById(String)}.
     */
    @Test
    public void testDeleteByIdLoadedGreenhouse() {
        Greenhouse gh = gh(ID_A, 0, 0, 10, 10);
        when(handler.loadObjects()).thenReturn(List.of(gh));
        manager.reload();
        assertEquals(1, manager.getMap().getSize());

        assertTrue(manager.deleteById(ID_A));

        verify(handler).deleteObject(gh);
        assertEquals(0, manager.getMap().getSize());
    }

    /**
     * Test method for {@link GreenhouseManager#deleteById(String)}.
     */
    @Test
    public void testDeleteByIdUnloadedGreenhouse() {
        Greenhouse first = gh(ID_A, 0, 0, 10, 10);
        Greenhouse overlapper = gh(ID_B, 5, 5, 15, 15);
        when(handler.loadObjects()).thenReturn(List.of(first, overlapper));
        manager.reload();

        assertTrue(manager.deleteById(ID_B));

        verify(handler).deleteObject(overlapper);
        // Removed from the unloaded list, and the loaded one is untouched
        assertTrue(manager.getUnloaded().isEmpty());
        assertEquals(1, manager.getMap().getSize());
        verify(handler, never()).deleteObject(first);
    }

    /**
     * Test method for {@link GreenhouseManager#deleteById(String)}.
     */
    @Test
    public void testDeleteByIdUnknown() {
        List<Greenhouse> loaded = List.of(gh(ID_A, 0, 0, 10, 10));
        when(handler.loadObjects()).thenReturn(loaded);
        manager.reload();
        assertFalse(manager.deleteById("does-not-exist"));
        verify(handler, never()).deleteObject(any());
    }

    /**
     * Test method for {@link GreenhouseManager#describe(Greenhouse)}.
     */
    @Test
    public void testDescribe() {
        Greenhouse gh = gh(ID_A, 1, 2, 11, 12);
        String desc = manager.describe(gh);
        assertTrue(desc.contains("id=" + ID_A));
        assertTrue(desc.contains("recipe=PLAINS"));
        assertTrue(desc.contains("owner=tastybento"));
        assertTrue(desc.contains("world=bskyblock_world"));
        assertTrue(desc.contains("location=1,60,2"));
        assertTrue(desc.contains("bbox=[1,60,2 -> 11,62,12]"));
    }
}
