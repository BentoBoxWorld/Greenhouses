package world.bentobox.greenhouses.ui.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoSession;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.WhiteBox;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseMap;

/**
 * @author tastybento
 */
class AdminInfoCommandTest {

    private static final String ID = "aaaaaaaa-1111-1111-1111-111111111111";

    @Mock
    private CompositeCommand parent;
    @Mock
    private Greenhouses addon;
    @Mock
    private GreenhouseManager gm;
    @Mock
    private GreenhouseMap map;
    @Mock
    private User user;
    @Mock
    private World world;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private Island island;

    @Mock
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;

    private AdminInfoCommand cmd;
    private Greenhouse gh;
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

        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);

        // Help output and player lookups go through the plugin
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // Parent command
        when(parent.getAddon()).thenReturn(addon);
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(parent.getLabel()).thenReturn("greenhouses");
        when(parent.getTopLabel()).thenReturn("bsbadmin");
        when(parent.getWorld()).thenReturn(world);
        // Addon
        when(addon.getManager()).thenReturn(gm);
        when(addon.getIslands()).thenReturn(im);
        when(addon.getPlayers()).thenReturn(pm);
        when(gm.getMap()).thenReturn(map);
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        when(pm.getName(any())).thenReturn("tastybento");
        when(world.getName()).thenReturn("bskyblock_world");
        // User
        when(user.isPlayer()).thenReturn(true);
        when(user.getLocation()).thenReturn(new Location(world, 5, 61, 5));
        // Greenhouse
        gh = mock(Greenhouse.class);
        when(gh.getUniqueId()).thenReturn(ID);
        when(gh.getLocation()).thenReturn(new Location(world, 0, 60, 0));
        when(gh.getBoundingBox()).thenReturn(new BoundingBox(0, 60, 0, 10, 70, 10));
        when(gh.getBiomeRecipeName()).thenReturn("PLAINS");
        when(gh.getMissingBlocks()).thenReturn(Collections.emptyMap());
        when(gm.getUnloaded()).thenReturn(Collections.emptyList());

        cmd = new AdminInfoCommand(parent);
    }

    @AfterEach
    void tearDown() {
        if (mockSession != null) {
            mockSession.finishMocking();
        }
        MockBukkit.unmock();
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link AdminInfoCommand#setup()}.
     */
    @Test
    void testSetup() {
        assertTrue(cmd.getPermission().contains("greenhouses.admin.info"));
        assertFalse(cmd.isOnlyPlayer());
    }

    /**
     * Test method for {@link AdminInfoCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteNoArgsInGreenhouse() {
        when(map.getGreenhouse(any(Location.class))).thenReturn(Optional.of(gh));
        assertTrue(cmd.canExecute(user, "info", Collections.emptyList()));
    }

    /**
     * Test method for {@link AdminInfoCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteNoArgsNotInGreenhouse() {
        when(map.getGreenhouse(any(Location.class))).thenReturn(Optional.empty());
        assertFalse(cmd.canExecute(user, "info", Collections.emptyList()));
        verify(user).sendMessage("greenhouses.errors.not-inside");
    }

    /**
     * Test method for {@link AdminInfoCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteNoArgsFromConsole() {
        when(user.isPlayer()).thenReturn(false);
        assertFalse(cmd.canExecute(user, "info", Collections.emptyList()));
        verify(user).sendMessage("greenhouses.commands.admin.errors.id-required");
        // The console has no location, so the map must not be consulted
        verify(map, never()).getGreenhouse(any());
    }

    /**
     * Test method for {@link AdminInfoCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteKnownId() {
        when(gm.getGreenhouseById(ID)).thenReturn(Optional.of(gh));
        assertTrue(cmd.canExecute(user, "info", List.of(ID)));
    }

    /**
     * Test method for {@link AdminInfoCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteUnknownId() {
        when(gm.getGreenhouseById(anyString())).thenReturn(Optional.empty());
        assertFalse(cmd.canExecute(user, "info", List.of("nope")));
        verify(user).sendMessage("greenhouses.commands.admin.errors.unknown-id", "[id]", "nope");
    }

    /**
     * Test method for {@link AdminInfoCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteShowsDetail() {
        when(gm.getGreenhouseById(ID)).thenReturn(Optional.of(gh));
        assertTrue(cmd.canExecute(user, "info", List.of(ID)));
        assertTrue(cmd.execute(user, "info", List.of(ID)));
        verify(user).sendMessage("greenhouses.commands.admin.info.title");
        verify(user).sendMessage("greenhouses.commands.admin.info.id", "[id]", ID);
        verify(user).sendMessage("greenhouses.commands.admin.info.recipe", "[recipe]", "PLAINS");
        verify(user).sendMessage("greenhouses.commands.admin.info.bounding-box", "[min]", "0,60,0",
                "[max]", "10,70,10");
    }

    /**
     * Test method for {@link AdminInfoCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteReportsWhyRecordIsNotLoaded() {
        when(gm.getGreenhouseById(ID)).thenReturn(Optional.of(gh));
        when(gm.getUnloaded()).thenReturn(List.of(new GreenhouseManager.UnloadedGreenhouse(gh,
                GreenhouseManager.GreenhouseResult.FAIL_OVERLAPPING)));
        assertTrue(cmd.canExecute(user, "info", List.of(ID)));
        assertTrue(cmd.execute(user, "info", List.of(ID)));
        verify(user).sendMessage("greenhouses.commands.admin.info.not-loaded", "[reason]",
                "FAIL_OVERLAPPING");
    }

}
