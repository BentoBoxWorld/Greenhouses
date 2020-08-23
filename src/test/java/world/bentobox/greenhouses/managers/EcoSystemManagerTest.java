/**
 *
 */
package world.bentobox.greenhouses.managers;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class EcoSystemManagerTest {

    @Mock
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
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // 4x4x4 greenhouse
        BoundingBox bb = BoundingBox.of(new Vector(0,0,0), new Vector(5,5,5));
        when(gh.getBoundingBox()).thenReturn(bb);
        // World
        when(gh.getWorld()).thenReturn(world);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        // Block
        // Air
        when(air.isEmpty()).thenReturn(true);
        when(air.isPassable()).thenReturn(true);
        when(air.getRelative(eq(BlockFace.UP))).thenReturn(air);
        // Plant
        when(plant.isPassable()).thenReturn(true);
        when(plant.getRelative(eq(BlockFace.UP))).thenReturn(air);
        // Liquid
        when(liquid.isLiquid()).thenReturn(true);
        when(liquid.getRelative(eq(BlockFace.UP))).thenReturn(air);
        // Default for block
        when(block.getRelative(eq(BlockFace.UP))).thenReturn(air);

        eco = new EcoSystemManager(null, null);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testGetAvailableBlocksAirAboveBlock() {
        List<Block> result = eco.getAvailableBlocks(gh, false);
        assertEquals(16, result.size());
        assertEquals(air, result.get(0));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testGetAvailableBlocksPlantAboveBlock() {
        when(block.getRelative(eq(BlockFace.UP))).thenReturn(plant);
        List<Block> result = eco.getAvailableBlocks(gh, false);
        assertEquals(16, result.size());
        assertEquals(plant, result.get(0));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testGetAvailableBlocksAllAir() {
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        List<Block> result = eco.getAvailableBlocks(gh, false);
        assertEquals(0, result.size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testGetAvailableBlocksAllLiquid() {
        when(liquid.getRelative(eq(BlockFace.UP))).thenReturn(liquid);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(liquid);
        List<Block> result = eco.getAvailableBlocks(gh, false);
        assertEquals(0, result.size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testGetAvailableBlocksAllPlant() {
        when(plant.getRelative(eq(BlockFace.UP))).thenReturn(plant);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(plant);
        List<Block> result = eco.getAvailableBlocks(gh, false);
        assertEquals(0, result.size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testGetAvailableBlocksLiquidAboveBlockIgnoreLiquids() {
        when(block.getRelative(eq(BlockFace.UP))).thenReturn(liquid);
        List<Block> result = eco.getAvailableBlocks(gh, true);
        assertEquals(16, result.size());
        assertEquals(liquid, result.get(0));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testGetAvailableBlocksLiquidAboveBlock() {
        when(block.getRelative(eq(BlockFace.UP))).thenReturn(liquid);
        List<Block> result = eco.getAvailableBlocks(gh, false);
        assertEquals(0, result.size());
    }
}
