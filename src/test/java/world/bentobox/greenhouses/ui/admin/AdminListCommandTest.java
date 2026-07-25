package world.bentobox.greenhouses.ui.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.managers.GreenhouseManager.UnloadedGreenhouse;
import world.bentobox.greenhouses.managers.GreenhouseMap;
import world.bentobox.greenhouses.mocks.ServerMocks;

/**
 * @author tastybento
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class })
public class AdminListCommandTest {

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

    private AdminListCommand cmd;

    @Before
    public void setUp() {
        ServerMocks.newServer();
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Help output and player lookups go through the plugin
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getPlayers()).thenReturn(pm);
        when(plugin.getIslands()).thenReturn(im);
        when(parent.getAddon()).thenReturn(addon);
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(parent.getLabel()).thenReturn("greenhouses");
        when(parent.getTopLabel()).thenReturn("bsbadmin");
        when(parent.getWorld()).thenReturn(world);
        when(addon.getManager()).thenReturn(gm);
        when(addon.getIslands()).thenReturn(im);
        when(addon.getPlayers()).thenReturn(pm);
        when(gm.getMap()).thenReturn(map);
        when(gm.getUnloaded()).thenReturn(Collections.emptyList());
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        when(pm.getName(any())).thenReturn("tastybento");
        when(world.getName()).thenReturn("bskyblock_world");
        cmd = new AdminListCommand(parent);
    }

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
    }

    private Greenhouse gh(int x) {
        Greenhouse gh = mock(Greenhouse.class);
        when(gh.getUniqueId()).thenReturn(UUID.randomUUID().toString());
        when(gh.getLocation()).thenReturn(new Location(world, x, 60, 0));
        when(gh.getBiomeRecipeName()).thenReturn("PLAINS");
        return gh;
    }

    private List<Greenhouse> ghs(int count) {
        List<Greenhouse> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(gh(i));
        }
        return list;
    }

    /**
     * Test method for {@link AdminListCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteNothingAtAll() {
        when(map.getGreenhouses()).thenReturn(Collections.emptyList());
        assertTrue(cmd.canExecute(user, "list", Collections.emptyList()));
        assertTrue(cmd.execute(user, "list", Collections.emptyList()));
        verify(user).sendMessage("greenhouses.commands.admin.list.none");
    }

    /**
     * Test method for {@link AdminListCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteFirstPageIsCapped() {
        List<Greenhouse> list = ghs(12);
        when(map.getGreenhouses()).thenReturn(list);
        assertTrue(cmd.canExecute(user, "list", Collections.emptyList()));
        assertTrue(cmd.execute(user, "list", Collections.emptyList()));
        verify(user).sendMessage(eq("greenhouses.commands.admin.list.title"), eq("[number]"), eq("12"), eq("[page]"),
                eq("1"), eq("[pages]"), eq("2"));
        // Only 10 of the 12 on page one
        verify(user, times(10)).sendMessage(eq("greenhouses.commands.admin.list.entry"), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString());
        verify(user).sendMessage(eq("greenhouses.commands.admin.list.more"), anyString(), anyString(), anyString(),
                anyString());
    }

    /**
     * Test method for {@link AdminListCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteSecondPage() {
        List<Greenhouse> list = ghs(12);
        when(map.getGreenhouses()).thenReturn(list);
        assertTrue(cmd.canExecute(user, "list", List.of("2")));
        assertTrue(cmd.execute(user, "list", List.of("2")));
        verify(user).sendMessage(eq("greenhouses.commands.admin.list.title"), eq("[number]"), eq("12"), eq("[page]"),
                eq("2"), eq("[pages]"), eq("2"));
        // The 2 remaining entries
        verify(user, times(2)).sendMessage(eq("greenhouses.commands.admin.list.entry"), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString());
    }

    /**
     * Test method for {@link AdminListCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        when(pm.getUUID(anyString())).thenReturn(null);
        assertFalse(cmd.canExecute(user, "list", List.of("nobody")));
        verify(user).sendMessage(eq("general.errors.unknown-player"), eq("[name]"), eq("nobody"));
    }

    /**
     * Test method for {@link AdminListCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteKnownPlayerUsesTheirIslands() {
        UUID uuid = UUID.randomUUID();
        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(im.getIslands(world, uuid)).thenReturn(List.of(island));
        List<Greenhouse> list = ghs(1);
        when(map.getGreenhouses(island)).thenReturn(list);
        assertTrue(cmd.canExecute(user, "list", List.of("tastybento")));
        assertTrue(cmd.execute(user, "list", List.of("tastybento")));
        // Scoped to the player's island, not everything
        verify(map, never()).getGreenhouses();
        verify(user).sendMessage(eq("greenhouses.commands.admin.list.title"), eq("[number]"), eq("1"), eq("[page]"),
                eq("1"), eq("[pages]"), eq("1"));
    }

    /**
     * Test method for {@link AdminListCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteAlwaysShowsUnloadedRecords() {
        when(map.getGreenhouses()).thenReturn(Collections.emptyList());
        Greenhouse bad = gh(0);
        when(gm.getUnloaded()).thenReturn(List.of(new UnloadedGreenhouse(bad, GreenhouseResult.FAIL_OVERLAPPING)));
        assertTrue(cmd.canExecute(user, "list", Collections.emptyList()));
        assertTrue(cmd.execute(user, "list", Collections.emptyList()));
        // Not reported as "none" - there is something to act on
        verify(user, never()).sendMessage("greenhouses.commands.admin.list.none");
        verify(user).sendMessage(eq("greenhouses.commands.admin.list.unloaded-title"), eq("[number]"), eq("1"));
        verify(user).sendMessage(eq("greenhouses.commands.admin.list.unloaded-entry"), anyString(), anyString(),
                eq("[reason]"), eq("FAIL_OVERLAPPING"), anyString(), anyString(), anyString(), anyString());
    }

}
