package world.bentobox.greenhouses.managers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.EcoSystemManager.GrowthBlock;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Tag.class})
public class EcoSystemManagerTest {

    private Greenhouse gh;
    @Mock
    private World world;
    @Mock
    private Block block;
    @Mock
    private Block air;
    @Mock
    private Block liquid;
    @Mock
    private Block plant;

    // CUT
    private EcoSystemManager eco;
    /**
     */
    @Before
    public void setUp() {
        PowerMockito.mockStatic(Tag.class, Mockito.RETURNS_MOCKS);
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        Tag<Keyed> tag = mock(Tag.class);
        when(Bukkit.getTag(anyString(), any(), any())).thenReturn(tag);

        gh = new Greenhouse();
        // 4x4x4 greenhouse
        BoundingBox bb = BoundingBox.of(new Vector(0,0,0), new Vector(6,5,6));
        gh.setBoundingBox(bb);
        // World
        Location l = new Location(world, 0,0,0);
        gh.setLocation(l);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        // Blocks
        // Air
        // Liquid false
        when(air.isEmpty()).thenReturn(true);
        when(air.isPassable()).thenReturn(true);
        when(air.getRelative(eq(BlockFace.UP))).thenReturn(air);
        // Plant
        // Empty false
        // Liquid false
        when(plant.isPassable()).thenReturn(true);
        when(plant.getRelative(eq(BlockFace.UP))).thenReturn(air);
        // Liquid
        // Empty false
        when(liquid.isLiquid()).thenReturn(true);
        when(liquid.isPassable()).thenReturn(true);
        when(liquid.getRelative(eq(BlockFace.UP))).thenReturn(air);
        // Default for block
        // Empty false
        // Passable false
        // Liquid false
        when(block.getRelative(eq(BlockFace.UP))).thenReturn(air);

        eco = new EcoSystemManager(null, null);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    public void testGetAvailableBlocksAirAboveBlock() {
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(16, result.size());
        assertEquals(air, result.get(0).block());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    public void testGetAvailableBlocksPlantAboveBlock() {
        when(block.getRelative(eq(BlockFace.UP))).thenReturn(plant);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(16, result.size());
        assertEquals(plant, result.get(0).block());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    public void testGetAvailableBlocksAllAir() {
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(0, result.size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    public void testGetAvailableBlocksAllLiquid() {
        when(liquid.getRelative(eq(BlockFace.UP))).thenReturn(liquid);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(liquid);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(16, result.size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    public void testGetAvailableBlocksAllLiquid2() {
        when(liquid.getRelative(eq(BlockFace.UP))).thenReturn(liquid);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(liquid);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, true);
        assertEquals(0, result.size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    public void testGetAvailableBlocksAllPlant() {
        when(plant.getRelative(eq(BlockFace.UP))).thenReturn(plant);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(plant);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(16, result.size());
        assertEquals(plant, result.get(0).block());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    public void testGetAvailableBlocksLiquidAboveBlockIgnoreLiquids() {
        when(block.getRelative(eq(BlockFace.UP))).thenReturn(liquid);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, true);
        assertEquals(16, result.size());
        assertEquals(liquid, result.get(0).block());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    public void testGetAvailableBlocksAirAboveLiquidNotIgnoreLiquids() {
        when(world.getBlockAt(anyInt(), eq(3), anyInt())).thenReturn(air);
        when(world.getBlockAt(anyInt(), eq(2), anyInt())).thenReturn(liquid);
        when(world.getBlockAt(anyInt(), eq(1), anyInt())).thenReturn(block);
        when(liquid.getRelative(eq(BlockFace.UP))).thenReturn(air);
        when(block.getRelative(eq(BlockFace.UP))).thenReturn(liquid);

        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(16, result.size());
        for (GrowthBlock value : result) {
            assertEquals(air, value.block());
        }
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    public void testGetAvailableBlocksAirAboveLiquidIgnoreLiquids() {
        when(world.getBlockAt(anyInt(), eq(3), anyInt())).thenReturn(air);
        when(world.getBlockAt(anyInt(), eq(2), anyInt())).thenReturn(liquid);
        when(world.getBlockAt(anyInt(), eq(1), anyInt())).thenReturn(block);
        when(liquid.getRelative(eq(BlockFace.UP))).thenReturn(air);
        when(block.getRelative(eq(BlockFace.UP))).thenReturn(liquid);

        List<GrowthBlock> result = eco.getAvailableBlocks(gh, true);
        assertEquals(16, result.size());
        for (GrowthBlock value : result) {
            assertEquals(liquid, value.block());
        }
    }
}
