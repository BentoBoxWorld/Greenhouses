package world.bentobox.greenhouses.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoSession;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseMap;

/**
 * @author tastybento
 */
class GreenhouseEventsTest {
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
    @Mock
    private PlayerInventory inv;
    @Mock
    private ItemStack waterBucket;

    private MockedStatic<User> mockedUser;
    private MockitoSession mockSession;

    @BeforeEach
    void setUp() {
        mockSession = Mockito.mockitoSession()
            .initMocks(this)
            .strictness(Strictness.LENIENT)
            .startMocking();
        MockBukkit.mock();
        @SuppressWarnings("unused")
        var unusedTagRef = Tag.LEAVES;

        mockedUser = Mockito.mockStatic(User.class, Mockito.CALLS_REAL_METHODS);
        mockedUser.when(() -> User.getInstance(any(Player.class))).thenReturn(user);

        // Always in greenhouse
        when(addon.getManager()).thenReturn(gm);
        when(gm.getMap()).thenReturn(map);
        when(map.inGreenhouse(any())).thenReturn(true);
        // Get greenhouse
        when(map.getGreenhouse(eq(location))).thenReturn(Optional.of(gh1));
        when(map.getGreenhouse(eq(location2))).thenReturn(Optional.of(gh2));
        BiomeRecipe br = new BiomeRecipe();
        br.setFriendlyName("recipe1");
        br.setType(Biome.PLAINS);
        BiomeRecipe br2 = new BiomeRecipe();
        br2.setFriendlyName("recipe2");
        br2.setType(Biome.NETHER_WASTES);
        // Names
        when(gh1.getBiomeRecipe()).thenReturn(br);
        when(gh2.getBiomeRecipe()).thenReturn(br2);
        when(gh1.getOriginalBiome()).thenReturn(Biome.BAMBOO_JUNGLE);

        when(player.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(Environment.NETHER);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(waterBucket);

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

    @AfterEach
    void tearDown() {
        if (mockedUser != null) {
            mockedUser.closeOnDemand();
        }
        if (mockSession != null) {
            mockSession.finishMocking();
        }
        MockBukkit.unmock();
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(PlayerBucketEmptyEvent)}.
     */
    @Test
    @Disabled("Biomes are nulls")
    void testOnPlayerInteractInNetherInGreenhouse() {
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        when(nextBlock.getLocation()).thenReturn(location);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, nextBlock, clickedBlock, BlockFace.UP, Material.WATER_BUCKET, item, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        verify(nextBlock).setType(Material.WATER);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(PlayerBucketEmptyEvent)}.
     */
    @Test
    void testOnPlayerInteractInNetherOutsideOfGreenhouse() {
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        when(nextBlock.getLocation()).thenReturn(mock(Location.class));
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, nextBlock, clickedBlock, BlockFace.UP, Material.WATER_BUCKET, item, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        verify(nextBlock, never()).setType(Material.WATER);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(PlayerBucketEmptyEvent)}.
     */
    @Test
    void testOnPlayerInteractInNetherGreenhouse() {
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location2);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, nextBlock, clickedBlock, BlockFace.UP, Material.WATER_BUCKET, item, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        verify(nextBlock, never()).setType(Material.WATER);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(PlayerBucketEmptyEvent)}.
     */
    @Test
    void testOnPlayerInteractInNetherNotInNether() {
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        when(clickedBlock.getWorld()).thenReturn(world);
        when(nextBlock.getWorld()).thenReturn(world);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, nextBlock, clickedBlock, BlockFace.UP, Material.WATER_BUCKET, item, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        verify(nextBlock, never()).setType(Material.WATER);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(PlayerBucketEmptyEvent)}.
     */
    @Test
    void testOnPlayerInteractInNetherNotWaterBucket() {
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.ACACIA_BOAT);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, nextBlock, clickedBlock, BlockFace.UP, Material.WATER, item, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        verify(nextBlock, never()).setType(Material.WATER);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerInteractInNether(PlayerBucketEmptyEvent)}.
     */
    @Test
    void testOnPlayerInteractInNetherNotInGreenhouse() {
        when(map.getGreenhouse(location)).thenReturn(Optional.empty());
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        Block nextBlock = mock(Block.class);
        when(clickedBlock.getRelative(any())).thenReturn(nextBlock);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, nextBlock, clickedBlock, BlockFace.UP, Material.WATER_BUCKET, item, EquipmentSlot.HAND);
        ghe.onPlayerInteractInNether(e);
        verify(nextBlock, never()).setType(Material.WATER);
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    @Disabled("Biomes are nulls")
    void testOnIceBreak() {
        when(Tag.ICE.isTagged(any(Material.class))).thenReturn(true);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.ICE);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onIceBreak(e);
        verify(block).setType(Material.WATER);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    void testOnIceBreakNetherBiomeGreenhouse() {
        // Material.ICE is naturally in Tag.ICE, so no need to mock
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.ICE);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location2);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onIceBreak(e);
        verify(block, never()).setType(Material.WATER);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    void testOnIceBreakNotIce() {
        // Material.STONE is not in Tag.ICE
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);
        when(block.getWorld()).thenReturn(world);
        // Nether gh
        when(block.getLocation()).thenReturn(location2);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onIceBreak(e);
        verify(block, never()).setType(Material.WATER);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    @Disabled("Biomes are nulls")
    void testOnIceBreakNotNetherNetherGreenhouse() {
        when(world.getEnvironment()).thenReturn(Environment.THE_END);
        when(Tag.ICE.isTagged(any(Material.class))).thenReturn(true);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.ICE);
        when(block.getWorld()).thenReturn(world);
        // Nether gh
        when(block.getLocation()).thenReturn(location2);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onIceBreak(e);
        verify(block).setType(Material.AIR);
        assertTrue(e.isCancelled());
        verify(world).playSound(any(Location.class), eq(Sound.BLOCK_GLASS_BREAK), eq(1F), eq(1F));

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    void testOnIceBreakNotNetherNotNetherGreenhouse() {
        when(world.getEnvironment()).thenReturn(Environment.THE_END);
        // Material.ICE is naturally in Tag.ICE
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.ICE);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onIceBreak(e);
        assertFalse(e.isCancelled());
        verify(block, never()).setType(Material.AIR);
    }


    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onIceBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    void testOnIceBreakNotInGreenhouse() {
        when(map.inGreenhouse(any())).thenReturn(false);
        // Material.ICE is naturally in Tag.ICE
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
    void testOnPlayerMove() {
        PlayerMoveEvent e = new PlayerMoveEvent(player, location, location2);
        ghe.onPlayerMove(e);
        verify(user).sendMessage("greenhouses.event.leaving", "[biome]", "recipe1");
        verify(user).sendMessage("greenhouses.event.entering", "[biome]", "recipe2");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    void testOnPlayerMoveEnteringOnly() {
        PlayerMoveEvent e = new PlayerMoveEvent(player, location, location2);
        when(map.getGreenhouse(location)).thenReturn(Optional.empty());
        when(map.getGreenhouse(location2)).thenReturn(Optional.of(gh2));
        ghe.onPlayerMove(e);
        verify(user, never()).sendMessage("greenhouses.event.leaving", "[biome]", "recipe1");
        verify(user).sendMessage("greenhouses.event.entering", "[biome]", "recipe2");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    void testOnPlayerMoveLeavingOnly() {
        PlayerMoveEvent e = new PlayerMoveEvent(player, location, location2);
        when(map.getGreenhouse(location)).thenReturn(Optional.of(gh1));
        when(map.getGreenhouse(location2)).thenReturn(Optional.empty());
        ghe.onPlayerMove(e);
        verify(user).sendMessage("greenhouses.event.leaving", "[biome]", "recipe1");
        verify(user, never()).sendMessage("greenhouses.event.entering", "[biome]", "recipe2");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    void testOnPlayerMoveSameGreenhouse() {
        PlayerMoveEvent e = new PlayerMoveEvent(player, location, location2);
        when(map.getGreenhouse(location)).thenReturn(Optional.of(gh1));
        when(map.getGreenhouse(location2)).thenReturn(Optional.of(gh1));
        ghe.onPlayerMove(e);
        verify(user, never()).sendMessage("greenhouses.event.leaving", "[biome]", "recipe1");
        verify(user, never()).sendMessage("greenhouses.event.entering", "[biome]", "recipe2");
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    void testOnPlayerTeleport() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, location2, TeleportCause.CHORUS_FRUIT);
        ghe.onPlayerTeleport(e);
        verify(user).sendMessage("greenhouses.event.leaving", "[biome]", "recipe1");
        verify(user).sendMessage("greenhouses.event.entering", "[biome]", "recipe2");

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    void testOnPlayerTeleportNulls() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, null, TeleportCause.CHORUS_FRUIT);
        ghe.onPlayerTeleport(e);
        verify(user, never()).sendMessage("greenhouses.event.leaving", "[biome]", "recipe1");
        verify(user, never()).sendMessage("greenhouses.event.entering", "[biome]", "recipe2");

    }

    /**
     * Test method for {@link world.bentobox.greenhouses.listeners.GreenhouseEvents#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    @Disabled("Biomes are nulls")
    void testOnBlockBreak() {
        when(gh1.isRoofOrWallBlock(any())).thenReturn(true);
        // Location is a wall block
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        ghe.onBlockBreak(e);
        verify(user).sendMessage("greenhouses.event.broke", "[biome]", "Bamboo Jungle");
        verify(gm).removeGreenhouse(any());
    }

}
