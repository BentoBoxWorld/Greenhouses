package world.bentobox.greenhouses.managers;

import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.greenhouses.greenhouse.GreenhouseMob;
import world.bentobox.greenhouses.greenhouse.MobHabitat;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.mockito.quality.Strictness;

import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.EcoSystemManager.GrowthBlock;

/**
 * @author tastybento
 *
 */
class EcoSystemManagerTest {

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
    @Mock
    private BiomeRecipe recipe;

    // CUT
    private EcoSystemManager eco;

    private MockitoSession mockSession;
    private MockedStatic<RecipeManager> mockedRecipeManager;

    /**
     */
    @BeforeEach
    void setUp() {
        mockSession = Mockito.mockitoSession()
            .initMocks(this)
            .strictness(Strictness.LENIENT)
            .startMocking();
        MockBukkit.mock();
        @SuppressWarnings("unused")
        var unusedTagRef = org.bukkit.Tag.LEAVES;

        // Mock RecipeManager static
        mockedRecipeManager = Mockito.mockStatic(RecipeManager.class);
        when(RecipeManager.getBiomeRecipies(any())).thenReturn(Optional.of(recipe));

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
        when(air.getRelative(BlockFace.UP)).thenReturn(air);
        // Plant
        // Empty false
        // Liquid false
        when(plant.isPassable()).thenReturn(true);
        when(plant.getRelative(BlockFace.UP)).thenReturn(air);
        // Liquid
        // Empty false
        when(liquid.isLiquid()).thenReturn(true);
        when(liquid.isPassable()).thenReturn(true);
        when(liquid.getRelative(BlockFace.UP)).thenReturn(air);
        // Default for block
        // Empty false
        // Passable false
        // Liquid false
        when(block.getRelative(BlockFace.UP)).thenReturn(air);

        // Recipe
        when(recipe.noMobs()).thenReturn(true);

        eco = new EcoSystemManager(null, null);
    }

    @AfterEach
    void tearDown() {
        if (mockedRecipeManager != null) {
            mockedRecipeManager.closeOnDemand();
        }
        if (mockSession != null) {
            mockSession.finishMocking();
        }
        MockBukkit.unmock();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    void testGetAvailableBlocksAirAboveBlock() {
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(64, result.size());
        assertEquals(air, result.get(0).block());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    void testGetAvailableBlocksPlantAboveBlock() {
        when(block.getRelative(BlockFace.UP)).thenReturn(plant);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(64, result.size());
        assertEquals(plant, result.get(0).block());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    void testGetAvailableBlocksAllAir() {
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(0, result.size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    void testGetAvailableBlocksAllLiquid() {
        when(liquid.getRelative(BlockFace.UP)).thenReturn(liquid);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(liquid);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(64, result.size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    void testGetAvailableBlocksAllLiquid2() {
        when(liquid.getRelative(BlockFace.UP)).thenReturn(liquid);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(liquid);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, true);
        assertEquals(0, result.size());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    void testGetAvailableBlocksAllPlant() {
        when(plant.getRelative(BlockFace.UP)).thenReturn(plant);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(plant);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(64, result.size());
        assertEquals(plant, result.get(0).block());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    void testGetAvailableBlocksLiquidAboveBlockIgnoreLiquids() {
        when(block.getRelative(BlockFace.UP)).thenReturn(liquid);
        List<GrowthBlock> result = eco.getAvailableBlocks(gh, true);
        assertEquals(64, result.size());
        assertEquals(liquid, result.get(0).block());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    void testGetAvailableBlocksAirAboveLiquidNotIgnoreLiquids() {
        when(world.getBlockAt(anyInt(), eq(3), anyInt())).thenReturn(air);
        when(world.getBlockAt(anyInt(), eq(2), anyInt())).thenReturn(liquid);
        when(world.getBlockAt(anyInt(), eq(1), anyInt())).thenReturn(block);
        when(liquid.getRelative(BlockFace.UP)).thenReturn(air);
        when(block.getRelative(BlockFace.UP)).thenReturn(liquid);

        List<GrowthBlock> result = eco.getAvailableBlocks(gh, false);
        assertEquals(48, result.size());
        int airCount = 0;
        int liquidCount = 0;
        int blockCount = 0;
        for (GrowthBlock value : result) {

            if (value.block()== air) {
                airCount++;

            } else if (value.block()== liquid) {
                liquidCount++;

            } else if (value.block()== block) {
                blockCount++;

            } 
        }
        assertEquals(16, airCount);
        assertEquals(32, liquidCount);
        assertEquals(0, blockCount);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#getAvailableBlocks(Greenhouse, boolean)}.
     */
    @Test
    void testGetAvailableBlocksAirAboveLiquidIgnoreLiquids() {
        when(world.getBlockAt(anyInt(), eq(3), anyInt())).thenReturn(air);
        when(world.getBlockAt(anyInt(), eq(2), anyInt())).thenReturn(liquid);
        when(world.getBlockAt(anyInt(), eq(1), anyInt())).thenReturn(block);
        when(liquid.getRelative(BlockFace.UP)).thenReturn(air);
        when(block.getRelative(BlockFace.UP)).thenReturn(liquid);

        List<GrowthBlock> result = eco.getAvailableBlocks(gh, true);
        assertEquals(32, result.size());
        for (GrowthBlock value : result) {
            assertEquals(liquid, value.block());
        }
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#addMobs(Greenhouse, double)}.
     */
    @Test
    void testAddMobsChunkNotLoaded() {
        assertFalse(eco.addMobs(gh, 1D));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#addMobs(Greenhouse, double)}.
     */
    @Test
    void testAddMobsChunkLoadedNoMobs() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        assertFalse(eco.addMobs(gh, 1D));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#addMobs(Greenhouse, double)}.
     */
    @Test
    void testAddMobsChunkLoadedWithMobsInRecipeMaxMobsZero() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.noMobs()).thenReturn(false);
        assertFalse(eco.addMobs(gh, 1D));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#addMobs(Greenhouse, double)}.
     */
    @Test
    void testAddMobsChunkLoadedWithMobsInRecipeMaxMobsNotZero() {
        // Nothing spawned here
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.noMobs()).thenReturn(false);
        when(recipe.getMaxMob()).thenReturn(10);
        assertFalse(eco.addMobs(gh, 1D));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#addMobs(Greenhouse, double)}.
     */
    @Test
    void testAddMobsSpawnMob() {
        // Nothing spawned here
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.noMobs()).thenReturn(false);
        when(recipe.getMaxMob()).thenReturn(10);
        when(recipe.spawnMob(any(), any())).thenReturn(true);
        assertTrue(eco.addMobs(gh, 1D));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#addMobs(Greenhouse, double)}.
     * Issue #127 - the absolute maxmobs cap must be enforced on every spawn iteration, not only
     * before the loop. The greenhouse area is 16 and the mob limit (density) is set to 1, so density
     * alone would allow up to 16 mobs. maxmobs is 3, so exactly 3 mobs should spawn.
     */
    @Test
    void testAddMobsRespectsMaxMobLimit() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.noMobs()).thenReturn(false);
        when(recipe.getMaxMob()).thenReturn(3);
        when(recipe.getMobLimit(MobHabitat.GROUND)).thenReturn(1);
        when(recipe.spawnMob(any(), any())).thenReturn(true);
        assertTrue(eco.addMobs(gh, 1D));
        // Without the fix the loop ignores maxMob and keeps spawning until density stops
        verify(recipe, times(3)).spawnMob(any(), any());
    }

    /**
     * Issue #121 - a sample that picks no columns must not touch the world at all, so the many
     * calls that sample nothing stay cheap.
     */
    @Test
    void testAddMobsFractionZeroDoesNothing() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.noMobs()).thenReturn(false);
        when(recipe.spawnMob(any(), any())).thenReturn(true);
        assertFalse(eco.addMobs(gh, 0D));
        verify(world, never()).isChunkLoaded(anyInt(), anyInt());
        verify(world, never()).getNearbyEntities(any(BoundingBox.class), any());
        verify(recipe, never()).spawnMob(any(), any());
    }

    /**
     * Issue #121 - mobs are counted with a bounding-box search, not by scanning every entity in
     * the world.
     */
    @Test
    void testAddMobsCountsOnlyNearbyEntities() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.noMobs()).thenReturn(false);
        eco.addMobs(gh, 1D);
        verify(world).getNearbyEntities(eq(gh.getBoundingBox()), any());
        verify(world, never()).getEntities();
    }

    /**
     * A greenhouse already at its maxmobs cap gets no spawn attempts.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testAddMobsAtCapSpawnsNothing() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.noMobs()).thenReturn(false);
        when(recipe.getMaxMob()).thenReturn(2);
        when(recipe.spawnMob(any(), any())).thenReturn(true);
        when(world.getNearbyEntities(any(BoundingBox.class), any(Predicate.class)))
        .thenReturn(List.of(mock(Entity.class), mock(Entity.class)));
        assertFalse(eco.addMobs(gh, 1D));
        verify(recipe, never()).spawnMob(any(), any());
    }

    /**
     * The density limit (moblimit) still applies, counting mobs already in the greenhouse.
     * Area is 16 and moblimit is 9: with 1 mob already there 16/1 >= 9 allows one more, then
     * 16/2 < 9 stops it.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testAddMobsRespectsDensityWithExistingMobs() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.noMobs()).thenReturn(false);
        when(recipe.getMaxMob()).thenReturn(-1);
        when(recipe.getMobLimit(MobHabitat.GROUND)).thenReturn(9);
        when(recipe.spawnMob(any(), any())).thenReturn(true);
        when(world.getNearbyEntities(any(BoundingBox.class), any(Predicate.class)))
        .thenReturn(List.of(mock(Entity.class)));
        assertTrue(eco.addMobs(gh, 1D));
        verify(recipe, times(1)).spawnMob(any(), any());
    }

    /**
     * Set up an ocean-style recipe: turtles on the ground, squid in the water. The ground
     * already has its one turtle (area 16, moblimit 17), so only squid have room.
     * The mock spawn offers a squid to the habitat check and spawns it if allowed.
     */
    @SuppressWarnings("unchecked")
    private void setUpOceanRecipe() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.noMobs()).thenReturn(false);
        when(recipe.getMaxMob()).thenReturn(-1);
        when(recipe.getMobTypes()).thenReturn(Set.of(EntityType.SQUID, EntityType.TURTLE));
        when(recipe.getMobPool(EntityType.SQUID)).thenReturn(MobHabitat.WATER);
        when(recipe.getMobPool(EntityType.TURTLE)).thenReturn(MobHabitat.GROUND);
        when(recipe.getMobLimit(MobHabitat.GROUND)).thenReturn(17);
        when(recipe.getMobLimit(MobHabitat.WATER)).thenReturn(10);
        Entity turtle = mock(Entity.class);
        when(turtle.getType()).thenReturn(EntityType.TURTLE);
        when(world.getNearbyEntities(any(BoundingBox.class), any(Predicate.class))).thenReturn(List.of(turtle));
        GreenhouseMob squid = new GreenhouseMob(EntityType.SQUID, Material.WATER);
        when(recipe.spawnMob(any(), any())).thenAnswer(inv -> inv.<Predicate<GreenhouseMob>>getArgument(1).test(squid));
    }

    /**
     * Issue #8 - water mobs are limited by the water blocks, not the floor area, and are not
     * blocked by a full ground. 40 water blocks at 10 per mob allows 5 squid (the density check
     * lets the next mob in while space / count >= limit).
     */
    @Test
    void testAddMobsWaterMobsUseWaterBlocks() {
        setUpOceanRecipe();
        gh.setWaterBlocks(40);
        assertTrue(eco.addMobs(gh, 1D));
        verify(recipe, times(5)).spawnMob(any(), any());
    }

    /**
     * Issue #8 - more water means more water mobs.
     */
    @Test
    void testAddMobsMoreWaterMoreMobs() {
        setUpOceanRecipe();
        gh.setWaterBlocks(10);
        assertTrue(eco.addMobs(gh, 1D));
        // 10 / 1 >= 10 lets a second squid in, 10 / 2 < 10 stops the third
        verify(recipe, times(2)).spawnMob(any(), any());
    }

    /**
     * Issue #8 - until the water has been measured, water mobs share the ground limit, which is
     * full, so nothing spawns.
     */
    @Test
    void testAddMobsWaterNotMeasuredFallsBackToGround() {
        setUpOceanRecipe();
        assertFalse(eco.addMobs(gh, 1D));
        verify(recipe, never()).spawnMob(any(), any());
    }

    /**
     * Issue #8 - a greenhouse with no water has no room for water mobs.
     */
    @Test
    void testAddMobsNoWaterNoWaterMobs() {
        setUpOceanRecipe();
        gh.setWaterBlocks(0);
        assertFalse(eco.addMobs(gh, 1D));
    }

    /**
     * maxmobs still caps the total across all habitats: one turtle plus two squid.
     */
    @Test
    void testAddMobsMaxMobsCapsAllHabitats() {
        setUpOceanRecipe();
        when(recipe.getMaxMob()).thenReturn(3);
        gh.setWaterBlocks(1000);
        assertTrue(eco.addMobs(gh, 1D));
        verify(recipe, times(2)).spawnMob(any(), any());
    }

    /**
     * Issue #8 - flying mobs are limited by the internal volume: 4 x 4 x 3 = 48 blocks.
     */
    @Test
    void testMobCountsAirUsesVolume() {
        when(recipe.getMobTypes()).thenReturn(Set.of(EntityType.BAT));
        when(recipe.getMobPool(EntityType.BAT)).thenReturn(MobHabitat.AIR);
        when(recipe.getMobLimit(MobHabitat.AIR)).thenReturn(12);
        when(recipe.getMaxMob()).thenReturn(-1);
        EcoSystemManager.MobCounts counts = new EcoSystemManager.MobCounts(gh, recipe);
        assertEquals(MobHabitat.AIR, counts.pool(EntityType.BAT));
        // 48 / n >= 12 for n up to 4, so 5 bats fit
        for (int i = 0; i < 5; i++) {
            assertTrue(counts.hasRoom(EntityType.BAT));
            counts.add(EntityType.BAT);
        }
        assertFalse(counts.hasRoom(EntityType.BAT));
        assertFalse(counts.anyRoom());
    }

    /**
     * Mobs the recipe does not know, or a recipe with no habitats set, count against the ground.
     */
    @Test
    void testMobCountsUnknownTypeIsGround() {
        EcoSystemManager.MobCounts counts = new EcoSystemManager.MobCounts(gh, recipe);
        assertEquals(MobHabitat.GROUND, counts.pool(null));
        assertEquals(MobHabitat.GROUND, counts.pool(EntityType.COW));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#sampleCount(int, double, double)}.
     */
    @Test
    void testSampleCount() {
        // Whole numbers are exact
        assertEquals(25, EcoSystemManager.sampleCount(100, 0.25D, 0.99D));
        // Remainder is sampled with that probability: expected 0.5
        assertEquals(1, EcoSystemManager.sampleCount(10, 0.05D, 0.4D));
        assertEquals(0, EcoSystemManager.sampleCount(10, 0.05D, 0.6D));
        // Expected 2.5
        assertEquals(3, EcoSystemManager.sampleCount(50, 0.05D, 0.1D));
        assertEquals(2, EcoSystemManager.sampleCount(50, 0.05D, 0.9D));
        // Edge cases
        assertEquals(0, EcoSystemManager.sampleCount(0, 0.5D, 0D));
        assertEquals(0, EcoSystemManager.sampleCount(100, 0D, 0D));
        assertEquals(0, EcoSystemManager.sampleCount(100, -1D, 0D));
        // Fraction is capped at 1
        assertEquals(100, EcoSystemManager.sampleCount(100, 3D, 0D));
    }

    /**
     * Issue #121 - over many runs the average sample matches the fraction asked for, so the old
     * per-block rates are kept (option (a)).
     */
    @Test
    void testSampleCountAverageMatchesFraction() {
        // 64 columns, 1 second sampling of a 5 minute mobtick
        double fraction = 20D / (5 * 60 * 20);
        int runs = 300_000;
        long total = 0;
        java.util.Random r = new java.util.Random(121);
        for (int i = 0; i < runs; i++) {
            total += EcoSystemManager.sampleCount(64, fraction, r.nextDouble());
        }
        double expected = 64 * fraction * runs;
        assertEquals(expected, total, expected * 0.02);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#convertBlocks(Greenhouse, double)}.
     * Fraction 1 samples as many blocks as there are in the greenhouse: 4 x 4 x 4.
     */
    @Test
    void testConvertBlocksFullFraction() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.getBlockConvert()).thenReturn(true);
        assertEquals(64, eco.convertBlocks(gh, 1D));
        verify(recipe, times(64)).convertBlock(block);
    }

    /**
     * Empty blocks are sampled but never converted.
     */
    @Test
    void testConvertBlocksSkipsAir() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.getBlockConvert()).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        assertEquals(64, eco.convertBlocks(gh, 1D));
        verify(recipe, never()).convertBlock(any());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#convertBlocks(Greenhouse, double)}.
     */
    @Test
    void testConvertBlocksNoConversionsInRecipe() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.getBlockConvert()).thenReturn(false);
        assertEquals(0, eco.convertBlocks(gh, 1D));
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#convertBlocks(Greenhouse, double)}.
     */
    @Test
    void testConvertBlocksChunkNotLoaded() {
        when(recipe.getBlockConvert()).thenReturn(true);
        assertEquals(0, eco.convertBlocks(gh, 1D));
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#convertBlocks(Greenhouse, double)}.
     */
    @Test
    void testConvertBlocksFractionZero() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(recipe.getBlockConvert()).thenReturn(true);
        assertEquals(0, eco.convertBlocks(gh, 0D));
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#cancel()}.
     * Tasks are null when their tick is 0 (disabled); cancel must not throw.
     */
    @Test
    void testCancelWithDisabledTasks() {
        eco.cancel();
    }

    /**
     * Wire a hopper block into the greenhouse's roof hopper location.
     * @return the hopper's inventory mock
     */
    private Inventory mockHopper() {
        Block hopperBlock = mock(Block.class);
        Hopper hopper = mock(Hopper.class);
        Inventory inv = mock(Inventory.class);
        when(hopperBlock.getType()).thenReturn(Material.HOPPER);
        when(hopperBlock.getState()).thenReturn(hopper);
        when(hopper.getInventory()).thenReturn(inv);
        when(world.getBlockAt(any(Location.class))).thenReturn(hopperBlock);
        gh.setRoofHopperLocation(new Vector(1, 1, 1));
        return inv;
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#setBoneMeal(Greenhouse, int)}.
     * A zero-amount ItemStack is rejected by newer Paper APIs, so the last bone meal must just be removed.
     */
    @Test
    void testSetBoneMealZeroRemovesWithoutAdding() {
        Inventory inv = mockHopper();
        eco.setBoneMeal(gh, 0);
        verify(inv).remove(Material.BONE_MEAL);
        verify(inv, never()).addItem(any(ItemStack.class));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#setBoneMeal(Greenhouse, int)}.
     */
    @Test
    void testSetBoneMealNegativeRemovesWithoutAdding() {
        Inventory inv = mockHopper();
        eco.setBoneMeal(gh, -1);
        verify(inv).remove(Material.BONE_MEAL);
        verify(inv, never()).addItem(any(ItemStack.class));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#setBoneMeal(Greenhouse, int)}.
     */
    @Test
    void testSetBoneMealPositiveReplacesStack() {
        Inventory inv = mockHopper();
        eco.setBoneMeal(gh, 5);
        verify(inv).remove(Material.BONE_MEAL);
        ArgumentCaptor<ItemStack> captor = ArgumentCaptor.forClass(ItemStack.class);
        verify(inv).addItem(captor.capture());
        assertEquals(Material.BONE_MEAL, captor.getValue().getType());
        assertEquals(5, captor.getValue().getAmount());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.managers.EcoSystemManager#setBoneMeal(Greenhouse, int)}.
     */
    @Test
    void testSetBoneMealNoHopper() {
        // No hopper location set - nothing to do and no exception
        eco.setBoneMeal(gh, 0);
        eco.setBoneMeal(gh, 5);
    }

}
