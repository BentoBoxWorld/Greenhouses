package world.bentobox.greenhouses.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseMap;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, User.class})
public class GreenhouseEventsTest {

    @Mock
    private User user;
    @Mock
    private Greenhouses addon;
    @Mock
    private Player player;
    // Class under test
    private GreenhouseEvents ghe;
    @Mock
    private World world;
    @Mock
    private GreenhouseManager gm;
    @Mock
    private GreenhouseMap map;
    @Mock
    private Location location;
    @Mock
    private Location location2;
    @Mock
    private Greenhouse gh1;
    @Mock
    private Greenhouse gh2;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(any(Player.class))).thenReturn(user);
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Always in greenhouse
        when(addon.getManager()).thenReturn(gm);
        when(gm.getMap()).thenReturn(map);
        when(map.inGreenhouse(any())).thenReturn(true);
        // Get greenhouse
        when(map.getGreenhouse(eq(location))).thenReturn(Optional.of(gh1));
        when(map.getGreenhouse(eq(location2))).thenReturn(Optional.of(gh2));
        BiomeRecipe br = new BiomeRecipe();
        br.setFriendlyName("recipe1");
        BiomeRecipe br2 = new BiomeRecipe();
        br2.setFriendlyName("recipe2");
        // Names
        when(gh1.getBiomeRecipe()).thenReturn(br);
        when(gh2.getBiomeRecipe()).thenReturn(br2);
        when(gh1.getOriginalBiome()).thenReturn(Biome.BAMBOO_JUNGLE);

        when(player.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(Environment.NETHER);
        // Location
        when(location.getBlockX()).thenReturn(5);
        when(location.getBlockY()).thenReturn(15);
        when(location.getBlockZ()).thenReturn(25);
        when(location.getX()).thenReturn(5D);
        when(location.getY()).thenReturn(15D);
        when(location.getZ()).thenReturn(25D);

        when(location2.getBlockX()).thenReturn(15);
        when(location2.getBlockY()).thenReturn(25);
        when(location2.getBlockZ()).thenReturn(35);
        when(location2.getX()).thenReturn(15D);
        when(location2.getY()).thenReturn(25D);
        when(location2.getZ()).thenReturn(35D);
        ghe = new GreenhouseEvents(addon);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractInNether() {
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        assertEquals(Result.DENY, e.useItemInHand());
        verify(nextBlock).setType(Material.WATER);
        verify(item).setType(Material.BUCKET);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractInNetherNotInNether() {
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        assertEquals(Result.DEFAULT, e.useItemInHand());
        verify(nextBlock, never()).setType(Material.WATER);
        verify(item, never()).setType(Material.BUCKET);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractInNetherNotWaterBucket() {
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.ACACIA_BOAT);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        assertEquals(Result.DEFAULT, e.useItemInHand());
        verify(nextBlock, never()).setType(Material.WATER);
        verify(item, never()).setType(Material.BUCKET);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractInNetherNotInGreenhouse() {
        when(map.inGreenhouse(any())).thenReturn(false);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        assertEquals(Result.DEFAULT, e.useItemInHand());
        verify(nextBlock, never()).setType(Material.WATER);
        verify(item, never()).setType(Material.BUCKET);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractInNetherNullItem() {
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, clickedBlock, BlockFace.EAST, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        assertEquals(Result.DEFAULT, e.useItemInHand());
        verify(nextBlock, never()).setType(Material.WATER);
        verify(item, never()).setType(Material.BUCKET);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractInNetherNotBlockClick() {
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, clickedBlock, BlockFace.EAST, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        assertEquals(Result.DEFAULT, e.useItemInHand());
        verify(nextBlock, never()).setType(Material.WATER);
        verify(item, never()).setType(Material.BUCKET);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnIceBreak() {
        when(Tag.ICE.isTagged(any(Material.class))).thenReturn(true);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.ICE);
        when(block.getWorld()).thenReturn(world);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onIceBreak(e);
        verify(block).setType(Material.WATER);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnIceBreakNotIce() {
        when(Tag.ICE.isTagged(any(Material.class))).thenReturn(false);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.ACACIA_BOAT);
        when(block.getWorld()).thenReturn(world);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onIceBreak(e);
        verify(block, never()).setType(Material.WATER);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnIceBreakNotNether() {
        when(world.getEnvironment()).thenReturn(Environment.THE_END);
        when(Tag.ICE.isTagged(any(Material.class))).thenReturn(true);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.ICE);
        when(block.getWorld()).thenReturn(world);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onIceBreak(e);
        verify(block, never()).setType(Material.WATER);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnIceBreakNotInGreenhouse() {
        when(map.inGreenhouse(any())).thenReturn(false);
        when(Tag.ICE.isTagged(any(Material.class))).thenReturn(true);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.ICE);
        when(block.getWorld()).thenReturn(world);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onIceBreak(e);
        verify(block, never()).setType(Material.WATER);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnPlayerMove() {
        PlayerMoveEvent e = new PlayerMoveEvent(player, location, location2);
        ghe.onPlayerMove(e);
        verify(user).sendMessage(eq("greenhouses.event.leaving"), eq("[biome]"), eq("recipe1"));
        verify(user).sendMessage(eq("greenhouses.event.entering"), eq("[biome]"), eq("recipe2"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnPlayerMoveEnteringOnly() {
        PlayerMoveEvent e = new PlayerMoveEvent(player, location, location2);
        when(map.getGreenhouse(eq(location))).thenReturn(Optional.empty());
        when(map.getGreenhouse(eq(location2))).thenReturn(Optional.of(gh2));
        ghe.onPlayerMove(e);
        verify(user, never()).sendMessage(eq("greenhouses.event.leaving"), eq("[biome]"), eq("recipe1"));
        verify(user).sendMessage(eq("greenhouses.event.entering"), eq("[biome]"), eq("recipe2"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnPlayerMoveLeavingOnly() {
        PlayerMoveEvent e = new PlayerMoveEvent(player, location, location2);
        when(map.getGreenhouse(eq(location))).thenReturn(Optional.of(gh1));
        when(map.getGreenhouse(eq(location2))).thenReturn(Optional.empty());
        ghe.onPlayerMove(e);
        verify(user).sendMessage(eq("greenhouses.event.leaving"), eq("[biome]"), eq("recipe1"));
        verify(user, never()).sendMessage(eq("greenhouses.event.entering"), eq("[biome]"), eq("recipe2"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnPlayerMoveSameGreenhouse() {
        PlayerMoveEvent e = new PlayerMoveEvent(player, location, location2);
        when(map.getGreenhouse(eq(location))).thenReturn(Optional.of(gh1));
        when(map.getGreenhouse(eq(location2))).thenReturn(Optional.of(gh1));
        ghe.onPlayerMove(e);
        verify(user, never()).sendMessage(eq("greenhouses.event.leaving"), eq("[biome]"), eq("recipe1"));
        verify(user, never()).sendMessage(eq("greenhouses.event.entering"), eq("[biome]"), eq("recipe2"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnPlayerTeleport() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, location2, TeleportCause.CHORUS_FRUIT);
        ghe.onPlayerTeleport(e );
        verify(user).sendMessage(eq("greenhouses.event.leaving"), eq("[biome]"), eq("recipe1"));
        verify(user).sendMessage(eq("greenhouses.event.entering"), eq("[biome]"), eq("recipe2"));

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnPlayerTeleportNulls() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, null, TeleportCause.CHORUS_FRUIT);
        ghe.onPlayerTeleport(e );
        verify(user, never()).sendMessage(eq("greenhouses.event.leaving"), eq("[biome]"), eq("recipe1"));
        verify(user, never()).sendMessage(eq("greenhouses.event.entering"), eq("[biome]"), eq("recipe2"));

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreak() {
        BoundingBox bb = BoundingBox.of(location, location2);
        when(gh1.getBoundingBox()).thenReturn(bb);
        // Location is a wall block
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onBlockBreak(e);
        verify(user).sendMessage(eq("greenhouses.event.broke"), eq("[biome]"), eq("Bamboo Jungle"));
        verify(gm).removeGreenhouse(any());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnPlayerBlockPlace() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getY()).thenReturn(255);
        when(block.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        BlockState bs = mock(BlockState.class);
        Block pa = mock(Block.class);
        ItemStack item = mock(ItemStack.class);
        BlockPlaceEvent e = new BlockPlaceEvent(block, bs, pa, item, player, true, EquipmentSlot.HAND);
        ghe.onPlayerBlockPlace(e);
        assertTrue(e.isCancelled());
        verify(user).sendMessage(eq("greenhouses.error.cannot-place"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnPlayerBlockPlaceNether() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getY()).thenReturn(255);
        when(block.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(Environment.NETHER);
        BlockState bs = mock(BlockState.class);
        Block pa = mock(Block.class);
        ItemStack item = mock(ItemStack.class);
        BlockPlaceEvent e = new BlockPlaceEvent(block, bs, pa, item, player, true, EquipmentSlot.HAND);
        ghe.onPlayerBlockPlace(e);
        assertFalse(e.isCancelled());
        verify(user, never()).sendMessage(eq("greenhouses.error.cannot-place"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnPlayerBlockPlaceBelowGH() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getY()).thenReturn(0);
        when(block.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        BlockState bs = mock(BlockState.class);
        Block pa = mock(Block.class);
        ItemStack item = mock(ItemStack.class);
        BlockPlaceEvent e = new BlockPlaceEvent(block, bs, pa, item, player, true, EquipmentSlot.HAND);
        ghe.onPlayerBlockPlace(e);
        assertFalse(e.isCancelled());
        verify(user, never()).sendMessage(eq("greenhouses.error.cannot-place"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPistonPush(org.bukkit.event.block.BlockPistonExtendEvent)}.
     */
    @Test
    public void testOnPistonPush() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getY()).thenReturn(255);
        when(block.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        BlockPistonExtendEvent e = new BlockPistonExtendEvent(block, Collections.singletonList(block), BlockFace.EAST);
        ghe.onPistonPush(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPistonPush(org.bukkit.event.block.BlockPistonExtendEvent)}.
     */
    @Test
    public void testOnPistonPushUnderGH() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getY()).thenReturn(0);
        when(block.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        BlockPistonExtendEvent e = new BlockPistonExtendEvent(block, Collections.singletonList(block), BlockFace.EAST);
        ghe.onPistonPush(e);
        assertFalse(e.isCancelled());
    }

}
