package world.bentobox.greenhouses.greenhouse;

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

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Piglin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.Settings;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.EcoSystemManager.GrowthBlock;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseMap;

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

    @Mock
    private Greenhouse gh;

    private BoundingBox bb;
    @Mock
    private World world;
    @Mock
    private Block block;
    @Mock
    private Location location;
    @Mock
    private BlockData bd;
    @Mock
    private BentoBox plugin;
    @Mock
    private GreenhouseManager mgr;
    @Mock
    private GreenhouseMap map;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private Settings settings;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.createBlockData(any(Material.class))).thenReturn(bd);
        Biome type = Biome.BADLANDS;
        // Greenhouse
        when(gh.getArea()).thenReturn(100);
        when(gh.getFloorHeight()).thenReturn(100);
        when(gh.getCeilingHeight()).thenReturn(120);
        bb = new BoundingBox(10, 100, 10, 20, 120, 20);
        when(gh.getBoundingBox()).thenReturn(bb);
        BoundingBox ibb = bb.clone().expand(-1);
        when(gh.getInternalBoundingBox()).thenReturn(ibb);
        when(gh.getWorld()).thenReturn(world);
        when(gh.contains(any())).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        // Block
        when(block.getType()).thenReturn(Material.AIR,
                Material.GRASS_BLOCK, Material.GRASS_BLOCK,
                Material.WATER,
                Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE,
                Material.LAVA,
                Material.AIR);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location);
        when(location.clone()).thenReturn(location);
        when(location.add(any(Vector.class))).thenReturn(location);

        // Plugin
        when(addon.getPlugin()).thenReturn(plugin);
        // Manager
        when(addon.getManager()).thenReturn(mgr);
        // GH Map
        when(mgr.getMap()).thenReturn(map);
        Optional<Greenhouse> optionalGh = Optional.of(gh);
        when(map.getGreenhouse(any(Location.class))).thenReturn(optionalGh);
        // Bukkit Scheduler
        when(Bukkit.getScheduler()).thenReturn(scheduler);
        // Settings
        when(addon.getSettings()).thenReturn(settings);
        when(settings.isStartupLog()).thenReturn(true);
        // World
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);

        // Set up default recipe
        br = new BiomeRecipe(addon, type, 0);
        br.setIcecoverage(2); // 1%
        br.setLavacoverage(1); // 1%
        br.setWatercoverage(1); // 1%
        br.addReqBlocks(Material.GRASS_BLOCK, 2);
        br.setFriendlyName("name");
        br.setName("name2");
        br.setIcon(Material.ACACIA_BOAT);
        br.setPermission("perm");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addConvBlocks(org.bukkit.Material, org.bukkit.Material, double, org.bukkit.Material)}.
     */
    @Test
    public void testAddConvBlocks() {
        Material oldMaterial = Material.SAND;
        Material newMaterial = Material.CLAY;
        double convChance = 100D;
        Material localMaterial = Material.WATER;
        br.addConvBlocks(oldMaterial, newMaterial, convChance, localMaterial);
        verify(addon).log("   100.0% chance for Sand to convert to Clay");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addMobs(org.bukkit.entity.EntityType, double, org.bukkit.Material)}.
     */
    @Test
    public void testAddMobs() {
        EntityType mobType = EntityType.CAT;
        int mobProbability = 50;
        Material mobSpawnOn = Material.GRASS_BLOCK;
        br.addMobs(mobType, mobProbability, mobSpawnOn);
        verify(addon).log("   50.0% chance for Cat to spawn on Grass Block.");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addMobs(org.bukkit.entity.EntityType, double, org.bukkit.Material)}.
     */
    @Test
    public void testAddMobsOver100Percent() {
        EntityType mobType = EntityType.CAT;
        int mobProbability = 50;
        Material mobSpawnOn = Material.GRASS_BLOCK;
        br.addMobs(mobType, mobProbability, mobSpawnOn);
        br.addMobs(mobType, mobProbability, mobSpawnOn);
        br.addMobs(mobType, mobProbability, mobSpawnOn);
        verify(addon).logError("Mob chances add up to > 100% in BADLANDS biome recipe! Skipping CAT");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addMobs(org.bukkit.entity.EntityType, double, org.bukkit.Material)}.
     */
    @Test
    public void testAddMobsOver100PercentDouble() {
        EntityType mobType = EntityType.CAT;
        double mobProbability = 50.5;
        Material mobSpawnOn = Material.GRASS_BLOCK;
        br.addMobs(mobType, mobProbability, mobSpawnOn);
        br.addMobs(mobType, mobProbability, mobSpawnOn);
        verify(addon).logError("Mob chances add up to > 100% in BADLANDS biome recipe! Skipping CAT");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addPlants(org.bukkit.Material, double, org.bukkit.Material)}.
     */
    @Test
    public void testAddPlants() {
        Material plantMaterial = Material.JUNGLE_SAPLING;
        int plantProbability = 20;
        Material plantGrowOn = Material.DIRT;
        br.addPlants(plantMaterial, plantProbability, plantGrowOn);
        verify(addon).log("   20.0% chance for Jungle Sapling to grow on Dirt");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addPlants(org.bukkit.Material, double, org.bukkit.Material)}.
     */
    @Test
    public void testAddPlantsOver100Percent() {
        Material plantMaterial = Material.JUNGLE_SAPLING;
        int plantProbability = 60;
        Material plantGrowOn = Material.DIRT;
        br.addPlants(plantMaterial, plantProbability, plantGrowOn);
        br.addPlants(plantMaterial, plantProbability, plantGrowOn);
        verify(addon).logError("Plant chances add up to > 100% in BADLANDS biome recipe! Skipping JUNGLE_SAPLING");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#addReqBlocks(org.bukkit.Material, int)}.
     */
    @Test
    public void testAddReqBlocks() {
        Material blockMaterial = Material.BLACK_CONCRETE;
        int blockQty = 30;
        br.addReqBlocks(blockMaterial, blockQty);
        verify(addon).log("   BLACK_CONCRETE x 30");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#checkRecipe(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testCheckRecipe() {
        br.checkRecipe(gh).thenAccept(result ->
        assertTrue(result.isEmpty()));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#checkRecipe(world.bentobox.greenhouses.data.Greenhouse)}.
     */
    @Test
    public void testCheckRecipeNotEnough() {
        br.addReqBlocks(Material.ACACIA_LEAVES, 3);
        br.checkRecipe(gh).thenAccept(result ->
        assertFalse(result.isEmpty()));

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#convertBlock(org.bukkit.block.Block)}.
     */
    @Test
    public void testConvertBlock() {
        // Setup
        this.testAddConvBlocks();
        // Mock
        Block b = mock(Block.class);
        when(b.getType()).thenReturn(Material.SAND);
        Block ab = mock(Block.class);
        when(ab.getType()).thenReturn(Material.WATER);
        when(b.getRelative(any())).thenReturn(ab);
        br.convertBlock(b);
        verify(b).setType(Material.CLAY);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#convertBlock(org.bukkit.block.Block)}.
     */
    @Test
    public void testConvertBlockNoWater() {
        // Setup
        this.testAddConvBlocks();
        // Mock
        Block b = mock(Block.class);
        when(b.getType()).thenReturn(Material.SAND);
        Block ab = mock(Block.class);
        when(ab.getType()).thenReturn(Material.SAND);
        when(b.getRelative(any())).thenReturn(ab);
        br.convertBlock(b);
        verify(b, never()).setType(Material.CLAY);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#convertBlock(org.bukkit.block.Block)}.
     */
    @Test
    public void testConvertBlockNoConverts() {
        // Mock
        Block b = mock(Block.class);
        when(b.getType()).thenReturn(Material.SAND);
        br.convertBlock(b);
        verify(b, never()).setType(Material.CLAY);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#convertBlock(org.bukkit.block.Block)}.
     */
    @Test
    public void testConvertBlockNoLocalBlock() {
        // Setup
        Material oldMaterial = Material.SAND;
        Material newMaterial = Material.CLAY;
        double convChance = 100D;
        br.addConvBlocks(oldMaterial, newMaterial, convChance, null);

        // Mock
        Block b = mock(Block.class);
        when(b.getType()).thenReturn(Material.SAND);
        br.convertBlock(b);

        verify(b, never()).getRelative(any());
        verify(b).setType(Material.CLAY);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#convertBlock(org.bukkit.block.Block)}.
     */
    @Test
    public void testConvertBlockNoProbability() {
        // Setup
        Material oldMaterial = Material.SAND;
        Material newMaterial = Material.CLAY;
        double convChance = 0D;
        Material localMaterial = Material.WATER;
        br.addConvBlocks(oldMaterial, newMaterial, convChance, localMaterial);

        // Mock
        Block b = mock(Block.class);
        when(b.getType()).thenReturn(Material.SAND);
        Block ab = mock(Block.class);
        when(ab.getType()).thenReturn(Material.WATER);
        when(b.getRelative(any())).thenReturn(ab);
        br.convertBlock(b);
        verify(b, never()).setType(Material.CLAY);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getBiome()}.
     */
    @Test
    public void testGetBiome() {
        assertEquals(Biome.BADLANDS, br.getBiome());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getBlockConvert()}.
     */
    @Test
    public void testGetBlockConvert() {
        assertFalse(br.getBlockConvert());
        this.testAddConvBlocks();
        assertTrue(br.getBlockConvert());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getFriendlyName()}.
     */
    @Test
    public void testGetFriendlyName() {
        assertEquals("name", br.getFriendlyName());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getIceCoverage()}.
     */
    @Test
    public void testGetIceCoverage() {
        assertEquals(2, br.getIceCoverage());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getIcon()}.
     */
    @Test
    public void testGetIcon() {
        assertEquals(Material.ACACIA_BOAT, br.getIcon());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getLavaCoverage()}.
     */
    @Test
    public void testGetLavaCoverage() {
        assertEquals(1, br.getLavaCoverage());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getMobLimit()}.
     */
    @Test
    public void testGetMobLimit() {
        assertEquals(9, br.getMobLimit());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getName()}.
     */
    @Test
    public void testGetName() {
        assertEquals("name2", br.getName());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getPermission()}.
     */
    @Test
    public void testGetPermission() {
        assertEquals("perm", br.getPermission());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#spawnMob(org.bukkit.block.Block)}.
     */
    @Test
    public void testSpawnMobyZero() {
        when(block.getY()).thenReturn(0);
        assertFalse(br.spawnMob(block));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#spawnMob(org.bukkit.block.Block)}.
     */
    @Test
    public void testSpawnNoMobs() {
        when(block.getY()).thenReturn(10);
        assertFalse(br.spawnMob(block));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#spawnMob(org.bukkit.block.Block)}.
     */
    @Test
    public void testSpawnMobOutsideWall() {
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.GRASS_BLOCK);
        when(block.getRelative(any())).thenReturn(block);

        EntityType mobType = EntityType.CAT;
        int mobProbability = 100;
        Material mobSpawnOn = Material.GRASS_BLOCK;

        Entity cat = mock(Cat.class);
        // Same box as greenhouse
        when(cat.getBoundingBox()).thenReturn(bb);
        when(world.spawnEntity(any(), any())).thenReturn(cat);
        when(cat.getWorld()).thenReturn(world);


        br.addMobs(mobType, mobProbability, mobSpawnOn);
        assertFalse(br.spawnMob(block));
        verify(world).spawnEntity(location, EntityType.CAT);
        verify(location).add(any(Vector.class));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#spawnMob(org.bukkit.block.Block)}.
     */
    @Test
    public void testSpawnMob() {
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.GRASS_BLOCK);
        when(block.getRelative(any())).thenReturn(block);

        EntityType mobType = EntityType.CAT;
        int mobProbability = 100;
        Material mobSpawnOn = Material.GRASS_BLOCK;

        Entity cat = mock(Cat.class);
        // Exactly 1 block smaller than the greenhouse blocks
        BoundingBox small = new BoundingBox(11, 101, 11, 19, 119, 19);
        when(cat.getBoundingBox()).thenReturn(small);
        when(world.spawnEntity(any(), any())).thenReturn(cat);
        when(cat.getWorld()).thenReturn(world);

        br.addMobs(mobType, mobProbability, mobSpawnOn);
        assertTrue(br.spawnMob(block));
        verify(world).spawnEntity(location, EntityType.CAT);
        verify(location).add(any(Vector.class));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#spawnMob(org.bukkit.block.Block)}.
     */
    @Test
    public void testSpawnMobHoglin() {
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.GRASS_BLOCK);
        when(block.getRelative(any())).thenReturn(block);

        EntityType mobType = EntityType.HOGLIN;
        int mobProbability = 100;
        Material mobSpawnOn = Material.GRASS_BLOCK;

        Hoglin hoglin = mock(Hoglin.class);
        // Exactly 1 block smaller than the greenhouse blocks
        BoundingBox small = new BoundingBox(11, 101, 11, 19, 119, 19);
        when(hoglin.getBoundingBox()).thenReturn(small);
        when(hoglin.getWorld()).thenReturn(world);
        when(world.spawnEntity(any(), any())).thenReturn(hoglin);


        br.addMobs(mobType, mobProbability, mobSpawnOn);
        assertTrue(br.spawnMob(block));
        verify(world).spawnEntity(location, EntityType.HOGLIN);
        verify(location).add(any(Vector.class));
        verify(hoglin).setImmuneToZombification(true);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#spawnMob(org.bukkit.block.Block)}.
     */
    @Test
    public void testSpawnMobPiglin() {
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.GRASS_BLOCK);
        when(block.getRelative(any())).thenReturn(block);

        EntityType mobType = EntityType.PIGLIN;
        int mobProbability = 100;
        Material mobSpawnOn = Material.GRASS_BLOCK;

        Piglin piglin = mock(Piglin.class);
        // Exactly 1 block smaller than the greenhouse blocks
        BoundingBox small = new BoundingBox(11, 101, 11, 19, 119, 19);
        when(piglin.getBoundingBox()).thenReturn(small);
        when(piglin.getWorld()).thenReturn(world);
        when(world.spawnEntity(any(), any())).thenReturn(piglin);


        br.addMobs(mobType, mobProbability, mobSpawnOn);
        assertTrue(br.spawnMob(block));
        verify(world).spawnEntity(location, EntityType.PIGLIN);
        verify(location).add(any(Vector.class));
        verify(piglin).setImmuneToZombification(true);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#spawnMob(org.bukkit.block.Block)}.
     */
    @Test
    public void testSpawnMobPiglinNether() {
        when(world.getEnvironment()).thenReturn(Environment.NETHER);

        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.GRASS_BLOCK);
        when(block.getRelative(any())).thenReturn(block);

        EntityType mobType = EntityType.PIGLIN;
        int mobProbability = 100;
        Material mobSpawnOn = Material.GRASS_BLOCK;

        Piglin piglin = mock(Piglin.class);
        // Exactly 1 block smaller than the greenhouse blocks
        BoundingBox small = new BoundingBox(11, 101, 11, 19, 119, 19);
        when(piglin.getBoundingBox()).thenReturn(small);
        when(piglin.getWorld()).thenReturn(world);
        when(world.spawnEntity(any(), any())).thenReturn(piglin);


        br.addMobs(mobType, mobProbability, mobSpawnOn);
        assertTrue(br.spawnMob(block));
        verify(world).spawnEntity(location, EntityType.PIGLIN);
        verify(location).add(any(Vector.class));
        verify(piglin, never()).setImmuneToZombification(true);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#spawnMob(org.bukkit.block.Block)}.
     */
    @Test
    public void testSpawnMobWrongSurface() {
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.STONE);
        when(block.getRelative(any())).thenReturn(block);

        EntityType mobType = EntityType.CAT;
        int mobProbability = 100;
        Material mobSpawnOn = Material.GRASS_BLOCK;

        Entity cat = mock(Cat.class);
        when(world.spawnEntity(any(), any())).thenReturn(cat);


        br.addMobs(mobType, mobProbability, mobSpawnOn);
        assertFalse(br.spawnMob(block));
        verify(world, never()).spawnEntity(location, EntityType.CAT);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getRecipeBlocks()}.
     */
    @Test
    public void testGetRecipeBlocks() {
        assertEquals(1, br.getRecipeBlocks().size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getWaterCoverage()}.
     */
    @Test
    public void testGetWaterCoverage() {
        assertEquals(1, br.getWaterCoverage());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#growPlant(org.bukkit.block.Block)}.
     */
    @Test
    public void testGrowPlantNotAir() {
        when(block.getType()).thenReturn(Material.SOUL_SAND);
        assertFalse(br.growPlant(new GrowthBlock(block, true), false));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#growPlant(org.bukkit.block.Block)}.
     */
    @Test
    public void testGrowPlantNoPlants() {
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        assertFalse(br.growPlant(new GrowthBlock(block, true), false));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#growPlant(org.bukkit.block.Block)}.
     */
    @Test
    public void testGrowPlantPlantsYZero() {
        when(block.getY()).thenReturn(0);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        assertTrue(br.addPlants(Material.BAMBOO_SAPLING, 100, Material.GRASS_BLOCK));
        assertFalse(br.growPlant(new GrowthBlock(block, true), false));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#growPlant(org.bukkit.block.Block)}.
     */
    @Test
    public void testGrowPlantPlants() {
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        Block ob = mock(Block.class);
        when(ob.getType()).thenReturn(Material.GRASS_BLOCK);

        when(block.getRelative(any())).thenReturn(ob);
        assertTrue(br.addPlants(Material.BAMBOO_SAPLING, 100, Material.GRASS_BLOCK));
        assertTrue(br.growPlant(new GrowthBlock(block, true), false));
        verify(world).spawnParticle(eq(Particle.SNOWBALL), any(Location.class), anyInt(), anyDouble(), anyDouble(), anyDouble());
        verify(block).setBlockData(eq(bd), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#growPlant(org.bukkit.block.Block)}.
     */
    @Test
    public void testGrowPlantCeilingPlants() {
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        Block ob = mock(Block.class);
        when(ob.getType()).thenReturn(Material.GLASS);

        when(block.getRelative(any())).thenReturn(ob);
        assertTrue(br.addPlants(Material.SPORE_BLOSSOM, 100, Material.GLASS));
        assertTrue(br.growPlant(new GrowthBlock(block, false), false));
        verify(world).spawnParticle(eq(Particle.SNOWBALL), any(Location.class), anyInt(), anyDouble(), anyDouble(), anyDouble());
        verify(block).setBlockData(eq(bd), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#growPlant(org.bukkit.block.Block)}.
     */
    @Test
    public void testGrowPlantCeilingPlantsFail() {
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        Block ob = mock(Block.class);
        when(ob.getType()).thenReturn(Material.GLASS);

        when(block.getRelative(any())).thenReturn(ob);
        assertTrue(br.addPlants(Material.SPORE_BLOSSOM, 100, Material.GLASS));
        // Not a ceiling block
        assertFalse(br.growPlant(new GrowthBlock(block, true), false));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#growPlant(org.bukkit.block.Block)}.
     */
    @Test
    public void testGrowPlantPlantsDoublePlant() {
        Bisected bisected = mock(Bisected.class);
        when(Bukkit.createBlockData(any(Material.class))).thenReturn(bisected);
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        Block ob = mock(Block.class);
        when(ob.getType()).thenReturn(Material.GRASS_BLOCK);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(ob);
        when(block.getRelative(BlockFace.UP)).thenReturn(block);
        assertTrue(br.addPlants(Material.SUNFLOWER, 100, Material.GRASS_BLOCK));
        assertTrue(br.growPlant(new GrowthBlock(block, true), false));
        verify(world).spawnParticle(eq(Particle.SNOWBALL), any(Location.class), anyInt(), anyDouble(), anyDouble(), anyDouble());
        verify(bisected).setHalf(Half.BOTTOM);
        verify(bisected).setHalf(Half.TOP);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#growPlant(org.bukkit.block.Block)}.
     */
    @Test
    public void testGrowPlantPlantsDoublePlantNoRoom() {
        Bisected bisected = mock(Bisected.class);
        when(Bukkit.createBlockData(any(Material.class))).thenReturn(bisected);
        when(block.getY()).thenReturn(10);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        Block ob = mock(Block.class);
        when(ob.getType()).thenReturn(Material.GRASS_BLOCK);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(ob);
        when(block.getRelative(BlockFace.UP)).thenReturn(ob);
        assertTrue(br.addPlants(Material.SUNFLOWER, 100, Material.GRASS_BLOCK));
        assertFalse(br.growPlant(new GrowthBlock(block, true), false));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setIcecoverage(int)}.
     */
    @Test
    public void testSetIcecoverage() {
        verify(addon).log("   Ice > 2%");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setLavacoverage(int)}.
     */
    @Test
    public void testSetLavacoverage() {
        verify(addon).log("   Lava > 1%");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setMobLimit(int)}.
     */
    @Test
    public void testSetMobLimit() {
        br.setMobLimit(0);
        assertEquals(0, br.getMobLimit());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setPriority(int)}.
     */
    @Test
    public void testSetPriority() {
        br.setPriority(20);
        assertEquals(20, br.getPriority());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setType(org.bukkit.block.Biome)}.
     */
    @Test
    public void testSetType() {
        br.setType(Biome.BADLANDS);
        assertEquals(Biome.BADLANDS, br.getBiome());
    }

    /**
     *
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#setWatercoverage(int)}.
     */
    @Test
    public void testSetWatercoverage() {
        verify(addon).log("   Water > 1%");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#compareTo(world.bentobox.greenhouses.greenhouse.BiomeRecipe)}.
     * Only priorty is compared
     */
    @Test
    public void testCompareTo() {
        assertEquals(0, br.compareTo(br));
        BiomeRecipe a = new BiomeRecipe();
        a.setPriority(20);
        assertEquals(1, br.compareTo(a));
        assertEquals(-1, a.compareTo(br));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#noMobs()}.
     */
    @Test
    public void testNoMobs() {
        assertTrue(br.noMobs());
        this.testAddMobs();
        assertFalse(br.noMobs());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.greenhouse.BiomeRecipe#getMobTypes()}.
     */
    @Test
    public void testGetMobTypes() {
        assertTrue(br.getMobTypes().isEmpty());
        this.testAddMobs();
        assertFalse(br.getMobTypes().isEmpty());
    }

}
