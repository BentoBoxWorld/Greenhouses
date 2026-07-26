package world.bentobox.greenhouses.ui.admin;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.bukkit.World;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.managers.GreenhouseManager.UnloadedGreenhouse;
import world.bentobox.greenhouses.managers.GreenhouseMap;
import world.bentobox.greenhouses.managers.RecipeManager;
import world.bentobox.greenhouses.mocks.ServerMocks;

/**
 * @author tastybento
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class })
public class AdminReloadCommandTest {

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
    private User user;
    @Mock
    private World world;

    @Mock
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;

    private AdminReloadCommand cmd;

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
        when(addon.getRecipes()).thenReturn(rm);
        when(gm.getMap()).thenReturn(map);
        when(map.getSize()).thenReturn(7);
        when(gm.getUnloaded()).thenReturn(Collections.emptyList());
        cmd = new AdminReloadCommand(parent);
    }

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
    }

    /**
     * Test method for {@link AdminReloadCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteReloadsRecipesBeforeGreenhouses() {
        assertTrue(cmd.execute(user, "reload", Collections.emptyList()));
        // Greenhouses resolve their recipe by name, so recipes must be reloaded first
        InOrder order = inOrder(rm, gm);
        order.verify(rm).reload();
        order.verify(gm).reload();
        verify(user).sendMessage("greenhouses.commands.admin.reload.success", "[number]", "7");
        verify(user, never()).sendMessage("greenhouses.commands.admin.reload.unloaded", "[number]", "0");
    }

    /**
     * Test method for {@link AdminReloadCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteWarnsAboutUnloadedRecords() {
        when(gm.getUnloaded())
        .thenReturn(List.of(new UnloadedGreenhouse(null, GreenhouseResult.FAIL_OVERLAPPING)));
        assertTrue(cmd.execute(user, "reload", Collections.emptyList()));
        verify(user).sendMessage("greenhouses.commands.admin.reload.unloaded", "[number]", "1");
    }

}
