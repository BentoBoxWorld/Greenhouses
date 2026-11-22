package world.bentobox.greenhouses.ui.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoSession;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.WhiteBox;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager;

/**
 * @author tastybento
 */
class AdminTeleportCommandTest {

    private static final String ID = "aaaaaaaa-1111-1111-1111-111111111111";

    @Mock
    private CompositeCommand parent;
    @Mock
    private Greenhouses addon;
    @Mock
    private GreenhouseManager gm;
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

    private AdminTeleportCommand cmd;
    private Greenhouse gh;
    private MockitoSession mockSession;
    private MockedStatic<Util> mockedUtil;

    @BeforeEach
    void setUp() {
        mockSession = Mockito.mockitoSession()
            .initMocks(this)
            .strictness(Strictness.LENIENT)
            .startMocking();
        MockBukkit.mock();
        @SuppressWarnings("unused")
        var unusedTagRef = org.bukkit.Tag.LEAVES;

        mockedUtil = Mockito.mockStatic(Util.class);
        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);

        // Help output and player lookups go through the plugin
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        mockedUtil.when(() -> Util.teleportAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(true));
        when(parent.getAddon()).thenReturn(addon);
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(parent.getLabel()).thenReturn("greenhouses");
        when(parent.getTopLabel()).thenReturn("bsbadmin");
        when(parent.getWorld()).thenReturn(world);
        when(addon.getManager()).thenReturn(gm);
        when(user.getPlayer()).thenReturn(player);
        gh = mock(Greenhouse.class);
        when(gh.getUniqueId()).thenReturn(ID);
        when(gh.getLocation()).thenReturn(new Location(world, 0, 60, 0));
        when(gh.getBoundingBox()).thenReturn(new BoundingBox(0, 60, 0, 10, 70, 10));
        when(gh.getFloorHeight()).thenReturn(60);
        cmd = new AdminTeleportCommand(parent);
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

    /**
     * Test method for {@link AdminTeleportCommand#setup()}.
     */
    @Test
    void testSetupIsPlayerOnly() {
        assertTrue(cmd.isOnlyPlayer());
    }

    /**
     * Test method for {@link AdminTeleportCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteNoArgs() {
        assertFalse(cmd.canExecute(user, "tp", Collections.emptyList()));
    }

    /**
     * Test method for {@link AdminTeleportCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteUnknownId() {
        when(gm.getGreenhouseById(anyString())).thenReturn(Optional.empty());
        assertFalse(cmd.canExecute(user, "tp", List.of("nope")));
        verify(user).sendMessage("greenhouses.commands.admin.errors.unknown-id", "[id]", "nope");
    }

    /**
     * Test method for {@link AdminTeleportCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteRecordWithNoWorld() {
        // Records with an unloaded world are exactly the ones that fail to load
        when(gh.getLocation()).thenReturn(null);
        when(gm.getGreenhouseById(ID)).thenReturn(Optional.of(gh));
        assertFalse(cmd.canExecute(user, "tp", List.of(ID)));
        verify(user).sendMessage(eq("greenhouses.commands.admin.errors.no-location"), eq("[id]"), anyString());
    }

    /**
     * Test method for {@link AdminTeleportCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteTeleportsToFloorCentre() {
        when(gm.getGreenhouseById(ID)).thenReturn(Optional.of(gh));
        assertTrue(cmd.canExecute(user, "tp", List.of(ID)));
        assertTrue(cmd.execute(user, "tp", List.of(ID)));

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        mockedUtil.verify(() -> Util.teleportAsync(eq(player), captor.capture()));
        Location target = captor.getValue();
        // Centre of the box in x/z, standing on the floor rather than in it
        assertEquals(5D, target.getX(), 0D);
        assertEquals(5D, target.getZ(), 0D);
        assertEquals(61D, target.getY(), 0D);
        verify(user).sendMessage(eq("greenhouses.commands.admin.tp.success"), eq("[id]"), anyString());
    }

}
