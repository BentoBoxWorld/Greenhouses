package world.bentobox.greenhouses.ui.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoSession;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.WhiteBox;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.managers.GreenhouseManager.UnloadedGreenhouse;
import world.bentobox.greenhouses.managers.GreenhouseMap;
import world.bentobox.greenhouses.managers.RecipeManager;

/**
 * @author tastybento
 */
class AdminReloadCommandTest {

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
     * Test method for {@link AdminReloadCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteReloadsRecipesBeforeGreenhouses() {
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
    void testExecuteWarnsAboutUnloadedRecords() {
        when(gm.getUnloaded())
        .thenReturn(List.of(new UnloadedGreenhouse(null, GreenhouseResult.FAIL_OVERLAPPING)));
        assertTrue(cmd.execute(user, "reload", Collections.emptyList()));
        verify(user).sendMessage("greenhouses.commands.admin.reload.unloaded", "[number]", "1");
    }

}
