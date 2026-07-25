package world.bentobox.greenhouses.ui.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.mocks.ServerMocks;

/**
 * @author tastybento
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class })
public class AdminDeleteCommandTest {

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
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private Island island;

    @Mock
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;

    private AdminDeleteCommand cmd;
    private Greenhouse gh;

    @Before
    public void setUp() {
        ServerMocks.newServer();
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Help output and player lookups go through the plugin
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(parent.getAddon()).thenReturn(addon);
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(parent.getLabel()).thenReturn("greenhouses");
        when(parent.getTopLabel()).thenReturn("bsbadmin");
        when(parent.getWorld()).thenReturn(world);
        when(addon.getManager()).thenReturn(gm);
        when(addon.getIslands()).thenReturn(im);
        when(addon.getPlayers()).thenReturn(pm);
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        when(pm.getName(any())).thenReturn("tastybento");
        gh = mock(Greenhouse.class);
        when(gh.getUniqueId()).thenReturn(ID);
        when(gh.getLocation()).thenReturn(new Location(world, 0, 60, 0));
        cmd = spy(new AdminDeleteCommand(parent));
    }

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
    }

    /**
     * Test method for {@link AdminDeleteCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertTrue(cmd.getPermission().contains("greenhouses.admin.delete"));
    }

    /**
     * Test method for {@link AdminDeleteCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteNoArgsShowsHelp() {
        assertFalse(cmd.canExecute(user, "delete", Collections.emptyList()));
        verify(gm, never()).deleteById(anyString());
    }

    /**
     * Test method for {@link AdminDeleteCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteTooManyArgs() {
        assertFalse(cmd.canExecute(user, "delete", List.of(ID, "extra")));
    }

    /**
     * Test method for {@link AdminDeleteCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteUnknownId() {
        when(gm.getGreenhouseById(anyString())).thenReturn(Optional.empty());
        assertFalse(cmd.canExecute(user, "delete", List.of("nope")));
        verify(user).sendMessage(eq("greenhouses.commands.admin.errors.unknown-id"), eq("[id]"), eq("nope"));
    }

    /**
     * Test method for {@link AdminDeleteCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteAsksForConfirmationBeforeDeleting() {
        when(gm.getGreenhouseById(ID)).thenReturn(Optional.of(gh));
        doNothing().when(cmd).askConfirmation(any(), any(), any(Runnable.class));
        assertTrue(cmd.canExecute(user, "delete", List.of(ID)));
        assertTrue(cmd.execute(user, "delete", List.of(ID)));
        // Nothing is deleted until the admin confirms
        verify(gm, never()).deleteById(anyString());

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(cmd).askConfirmation(eq(user), any(), captor.capture());
        when(gm.deleteById(ID)).thenReturn(true);
        captor.getValue().run();
        verify(gm).deleteById(ID);
        verify(user).sendMessage(eq("greenhouses.commands.admin.delete.success"), eq("[id]"), anyString());
    }

    /**
     * Test method for {@link AdminDeleteCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteReportsFailedDelete() {
        when(gm.getGreenhouseById(ID)).thenReturn(Optional.of(gh));
        doNothing().when(cmd).askConfirmation(any(), any(), any(Runnable.class));
        assertTrue(cmd.canExecute(user, "delete", List.of(ID)));
        assertTrue(cmd.execute(user, "delete", List.of(ID)));
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(cmd).askConfirmation(eq(user), any(), captor.capture());
        // Record vanished between the prompt and the confirmation
        when(gm.deleteById(ID)).thenReturn(false);
        captor.getValue().run();
        verify(user).sendMessage(eq("greenhouses.commands.admin.errors.unknown-id"), eq("[id]"), eq(ID));
    }

}
