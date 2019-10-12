package world.bentobox.greenhouses.greenhouse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class BiomeRecipeTest {
    
    private BiomeRecipe br;
    @Mock
    private Greenhouses addon;

    private Biome type;
    @Mock
    private Greenhouse gh;

    private BoundingBox bb;
    @Mock
    private World world;
    @Mock
    private Block block;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        type = Biome.BADLANDS;
        // Greenhouse
        when(gh.getArea()).thenReturn(100);
        when(gh.getFloorHeight()).thenReturn(100);
        when(gh.getCeilingHeight()).thenReturn(120);
        bb = new BoundingBox(10, 100, 10, 20, 120, 20);
        when(gh.getBoundingBox()).thenReturn(bb);        
        when(gh.getWorld()).thenReturn(world);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(block.getType()).thenReturn(Material.AIR,
                Material.GRASS_BLOCK, Material.GRASS_BLOCK,
                Material.WATER,
                Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE,
                Material.LAVA,
                Material.AIR);
        // Set up default recipe
        br = new BiomeRecipe(addon, type, 0);
        br.setIcecoverage(2); // 1%
        br.setLavacoverage(1); // 1%
        br.setWatercoverage(1); // 1%
        br.addReqBlocks(Material.GRASS_BLOCK, 2);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addConvBlocks(org.bukkit.Material, org.bukkit.Material, double, org.bukkit.Material)}.
     */
    @Test
    public void testAddConvBlocks() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addMobs(org.bukkit.entity.EntityType, int, org.bukkit.Material)}.
     */
    @Test
    public void testAddMobs() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addPlants(org.bukkit.Material, int, org.bukkit.Material)}.
     */
    @Test
    public void testAddPlants() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addReqBlocks(org.bukkit.Material, int)}.
     */
    @Test
    public void testAddReqBlocks() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#checkRecipe(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testCheckRecipe() {
        Set<GreenhouseResult> result = br.checkRecipe(gh);
        assertTrue(result.isEmpty());
    }
    
    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#checkRecipe(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testCheckRecipeNotEnough() {
        br.addReqBlocks(Material.ACACIA_LEAVES, 3);
        Set<GreenhouseResult> result = br.checkRecipe(gh);
        assertFalse(result.isEmpty());
        
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#convertBlock(org.bukkit.block.Block)}.
     */
    @Test
    public void testConvertBlock() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getBiome()}.
     */
    @Test
    public void testGetBiome() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getBlockConvert()}.
     */
    @Test
    public void testGetBlockConvert() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getFriendlyName()}.
     */
    @Test
    public void testGetFriendlyName() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getIceCoverage()}.
     */
    @Test
    public void testGetIceCoverage() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getIcon()}.
     */
    @Test
    public void testGetIcon() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getLavaCoverage()}.
     */
    @Test
    public void testGetLavaCoverage() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getMobLimit()}.
     */
    @Test
    public void testGetMobLimit() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getName()}.
     */
    @Test
    public void testGetName() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getPermission()}.
     */
    @Test
    public void testGetPermission() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#spawnMob(org.bukkit.block.Block)}.
     */
    @Test
    public void testSpawnMob() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getRecipeBlocks()}.
     */
    @Test
    public void testGetRecipeBlocks() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getWaterCoverage()}.
     */
    @Test
    public void testGetWaterCoverage() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#growPlant(org.bukkit.block.Block)}.
     */
    @Test
    public void testGrowPlant() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setFriendlyName(java.lang.String)}.
     */
    @Test
    public void testSetFriendlyName() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setIcecoverage(int)}.
     */
    @Test
    public void testSetIcecoverage() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setIcon(org.bukkit.Material)}.
     */
    @Test
    public void testSetIcon() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setLavacoverage(int)}.
     */
    @Test
    public void testSetLavacoverage() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setMobLimit(int)}.
     */
    @Test
    public void testSetMobLimit() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setName(java.lang.String)}.
     */
    @Test
    public void testSetName() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setPermission(java.lang.String)}.
     */
    @Test
    public void testSetPermission() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setPriority(int)}.
     */
    @Test
    public void testSetPriority() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setType(org.bukkit.block.Biome)}.
     */
    @Test
    public void testSetType() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setWatercoverage(int)}.
     */
    @Test
    public void testSetWatercoverage() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getMissingBlocks()}.
     */
    @Test
    public void testGetMissingBlocks() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#compareTo(world.bentobox.greenhouses.greenhouse.BiomeRecipe)}.
     */
    @Test
    public void testCompareTo() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#noMobs()}.
     */
    @Test
    public void testNoMobs() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getMobTypes()}.
     */
    @Test
    public void testGetMobTypes() {
        fail("Not yet implemented");
    }

}
