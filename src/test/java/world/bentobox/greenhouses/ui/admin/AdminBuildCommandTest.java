package world.bentobox.greenhouses.ui.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.WhiteBox;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseMap;
import world.bentobox.greenhouses.managers.RecipeManager;

/**
 * Tests {@link AdminBuildCommand}.
 *
 * @author tastybento
 */
class AdminBuildCommandTest {

    private static final String KEY = "greenhouses.commands.admin.build.";

    @Mock
    private CompositeCommand parent;
    @Mock
    private Greenhouses addon;
    @Mock
    private GreenhouseManager gm;
    @Mock
    private GreenhouseMap map;
    @Mock
    private RecipeManager rm;
    @Mock
    private BiomeRecipe recipe;
    @Mock
    private User user;
    @Mock
    private World world;
    @Mock
    private Player player;
    @Mock
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private Block block;

    private AdminBuildCommand cmd;
    private MockitoSession mockSession;
    private MockedStatic<Util> mockedUtil;

    @BeforeEach
    void setUp() {
        mockSession = Mockito.mockitoSession().initMocks(this).strictness(Strictness.LENIENT).startMocking();
        MockBukkit.mock();
        @SuppressWarnings("unused")
        var unusedTagRef = org.bukkit.Tag.LEAVES;
        mockedUtil = Mockito.mockStatic(Util.class);
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);
        mockedUtil.when(() -> Util.tabLimit(any(), any())).thenCallRealMethod();
        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);

        when(plugin.getIWM()).thenReturn(iwm);
        when(plugin.getIslands()).thenReturn(im);
        world.bentobox.bentobox.Settings bbSettings = new world.bentobox.bentobox.Settings();
        when(plugin.getSettings()).thenReturn(bbSettings);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(parent.getAddon()).thenReturn(addon);
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(parent.getLabel()).thenReturn("greenhouses");
        when(parent.getTopLabel()).thenReturn("bsbadmin");
        when(parent.getWorld()).thenReturn(world);
        when(addon.getManager()).thenReturn(gm);
        when(addon.getRecipes()).thenReturn(rm);
        when(gm.getMap()).thenReturn(map);
        when(map.getOverlappingGreenhouse(any())).thenReturn(Optional.empty());
        when(gm.tryToMakeGreenhouse(any(), any())).thenReturn(new CompletableFuture<>());
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // A plains-like recipe
        when(recipe.getName()).thenReturn("PLAINS");
        when(recipe.getBiome()).thenReturn(Biome.PLAINS);
        when(recipe.getRequiredBlocks()).thenReturn(Map.of(Material.GRASS_BLOCK, 3));
        when(recipe.getPlantGrowOn()).thenReturn(Set.of(Material.GRASS_BLOCK));
        when(recipe.getMobSpawnOn()).thenReturn(Set.of(Material.GRASS_BLOCK));
        when(recipe.getWaterCoverage()).thenReturn(-1);
        when(recipe.getLavaCoverage()).thenReturn(-1);
        when(recipe.getIceCoverage()).thenReturn(-1);
        when(recipe.hasPlants()).thenReturn(true);
        when(rm.getBiomeRecipes()).thenReturn(List.of(recipe));

        // The admin stands at 100, 64, 100 facing north
        when(user.getPlayer()).thenReturn(player);
        when(user.isPlayer()).thenReturn(false); // chat confirmations, not dialogs
        when(user.getLocation()).thenReturn(new Location(world, 100.5, 64, 100.5, 180F, 0F));
        when(user.getWorld()).thenReturn(world);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0));
        when(user.getTranslation(anyString(), anyString(), anyString())).thenAnswer(i -> i.getArgument(0));
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(block.isEmpty()).thenReturn(true);

        cmd = new AdminBuildCommand(parent);
    }

    @AfterEach
    void tearDown() {
        if (mockedUtil != null) {
            mockedUtil.closeOnDemand();
        }
        if (mockSession != null) {
            mockSession.finishMocking();
        }
        MockBukkit.unmock();
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    @Test
    void testSetup() {
        assertTrue(cmd.isOnlyPlayer());
        assertEquals("bskyblock.greenhouses.admin.build", cmd.getPermission());
    }

    @Test
    void testNoArgs() {
        assertFalse(cmd.canExecute(user, "build", Collections.emptyList()));
    }

    @Test
    void testUnknownRecipe() {
        assertFalse(cmd.canExecute(user, "build", List.of("jungle")));
        verify(user).sendMessage("greenhouses.commands.user.make.unknown-recipe");
        verify(user).sendMessage("greenhouses.commands.user.make.recipe-format", "[name]", "PLAINS");
    }

    @Test
    void testRecipeIgnoresCase() {
        assertTrue(cmd.canExecute(user, "build", List.of("plains")));
    }

    @Test
    void testCoordinatesAndOptions() {
        assertTrue(cmd.canExecute(user, "build", List.of("PLAINS", "~", "~-1", "~5", "7x7x4", "clear")));
        assertTrue(cmd.canExecute(user, "build", List.of("PLAINS", "10", "70", "-20", "keep", "5")));
    }

    @Test
    void testIncompleteCoordinates() {
        assertFalse(cmd.canExecute(user, "build", List.of("PLAINS", "~", "~")));
        verify(user).sendMessage(KEY + "bad-coordinates");
    }

    @Test
    void testBadCoordinate() {
        assertFalse(cmd.canExecute(user, "build", List.of("PLAINS", "~", "~x", "~")));
        verify(user).sendMessage(KEY + "bad-coordinates");
    }

    @Test
    void testBadArgument() {
        assertFalse(cmd.canExecute(user, "build", List.of("PLAINS", "fill")));
        verify(user).sendMessage(KEY + "bad-argument", "[description]", "fill");
    }

    @Test
    void testTooSmall() {
        when(recipe.getRequiredBlocks()).thenReturn(Map.of(Material.STONE, 20));
        assertFalse(cmd.canExecute(user, "build", List.of("PLAINS", "3x3x3", "clear")));
        verify(user).sendMessage(eq(KEY + "too-small"), eq("[size]"), eq("3x3x3"), eq("[description]"), anyString());
    }

    @Test
    void testWrongWorld() {
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(mock(World.class));
        assertFalse(cmd.canExecute(user, "build", List.of("PLAINS")));
        verify(user).sendMessage("general.errors.wrong-world");
    }

    @Test
    void testOutOfWorld() {
        assertFalse(cmd.canExecute(user, "build", List.of("PLAINS", "~", "318", "~")));
        verify(user).sendMessage(KEY + "out-of-world");
    }

    @Test
    void testNotOnIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertFalse(cmd.canExecute(user, "build", List.of("PLAINS")));
        verify(user).sendMessage(KEY + "not-on-island");
    }

    @Test
    void testCornerOnAnotherIsland() {
        Island other = mock(Island.class);
        when(im.getIslandAt(any())).thenAnswer(i -> {
            Location l = i.getArgument(0);
            return Optional.of(l.getBlockX() > 102 ? other : island);
        });
        assertFalse(cmd.canExecute(user, "build", List.of("PLAINS")));
        verify(user).sendMessage(KEY + "not-on-island");
    }

    @Test
    void testOverlapping() {
        Greenhouse gh = mock(Greenhouse.class);
        when(gh.getUniqueId()).thenReturn("abcdef12-0000");
        when(map.getOverlappingGreenhouse(any())).thenReturn(Optional.of(gh));
        assertFalse(cmd.canExecute(user, "build", List.of("PLAINS")));
        verify(user).sendMessage(KEY + "overlapping", "[id]", "abcdef12");
    }

    @Test
    void testExecuteKeepBuildsAndMakes() {
        assertTrue(cmd.canExecute(user, "build", List.of("PLAINS", "5x5x3")));
        assertTrue(cmd.execute(user, "build", List.of("PLAINS", "5x5x3")));
        verify(block, Mockito.atLeastOnce()).setBlockData(any(), eq(false));
        verify(user).sendMessage(eq(KEY + "built"), eq("[size]"), eq("5x5x3"), eq("[number]"), anyString(),
                eq("[recipe]"), eq("PLAINS"));
        verify(gm).tryToMakeGreenhouse(eq(new Location(world, 100.5, 64, 100.5)), eq(recipe));
    }

    @Test
    void testExecuteClearAsksFirst() {
        assertTrue(cmd.canExecute(user, "build", List.of("PLAINS", "clear")));
        assertTrue(cmd.execute(user, "build", List.of("PLAINS", "clear")));
        // Nothing is built until confirmed
        verify(block, never()).setBlockData(any(), Mockito.anyBoolean());
        verify(gm, never()).tryToMakeGreenhouse(any(), any());
    }

    @Test
    void testTabComplete() {
        assertEquals(List.of("PLAINS"), cmd.tabComplete(user, "build", List.of("build", "P")).orElseThrow());
        assertEquals(List.of("~", "100"), cmd.tabComplete(user, "build", List.of("build", "PLAINS", "")).orElseThrow()
                .subList(0, 2));
        assertTrue(cmd.tabComplete(user, "build", List.of("build", "PLAINS", "~", "")).orElseThrow().contains("64"));
        List<String> after = cmd.tabComplete(user, "build", List.of("build", "PLAINS", "~", "~", "~", ""))
                .orElseThrow();
        assertTrue(after.containsAll(List.of("keep", "clear", "9x9x5")));
        assertFalse(after.contains("~"));
    }

    @Test
    void testCoordinate() {
        assertEquals(OptionalInt.of(5), AdminBuildCommand.coordinate("~", 5));
        assertEquals(OptionalInt.of(3), AdminBuildCommand.coordinate("~-2", 5));
        assertEquals(OptionalInt.of(12), AdminBuildCommand.coordinate("~7", 5));
        assertEquals(OptionalInt.of(-40), AdminBuildCommand.coordinate("-40", 5));
        assertTrue(AdminBuildCommand.coordinate("x", 5).isEmpty());
    }

    @Test
    void testHasCoordinates() {
        assertFalse(AdminBuildCommand.hasCoordinates(List.of("PLAINS")));
        assertTrue(AdminBuildCommand.hasCoordinates(List.of("PLAINS", "~")));
        assertTrue(AdminBuildCommand.hasCoordinates(List.of("PLAINS", "1", "2", "3")));
        // A lone number is a size
        assertFalse(AdminBuildCommand.hasCoordinates(List.of("PLAINS", "9")));
        assertFalse(AdminBuildCommand.hasCoordinates(List.of("PLAINS", "9", "clear")));
        assertFalse(AdminBuildCommand.hasCoordinates(List.of("PLAINS", "9x9", "1", "2")));
    }

    @Test
    void testDoorWall() {
        assertEquals(BlockFace.SOUTH, AdminBuildCommand.doorWall(new Location(world, 0, 0, 0, 0F, 0F)));
        assertEquals(BlockFace.WEST, AdminBuildCommand.doorWall(new Location(world, 0, 0, 0, 90F, 0F)));
        assertEquals(BlockFace.NORTH, AdminBuildCommand.doorWall(new Location(world, 0, 0, 0, -180F, 0F)));
        assertEquals(BlockFace.EAST, AdminBuildCommand.doorWall(new Location(world, 0, 0, 0, -90F, 0F)));
        assertEquals(BlockFace.NORTH, AdminBuildCommand.doorWall(null));
    }
}
