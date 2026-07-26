package world.bentobox.greenhouses.ui.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

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
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.mocks.ServerMocks;

/**
 * @author tastybento
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class })
public class AdminCommandTest {

    @Mock
    private CompositeCommand parent;
    @Mock
    private Greenhouses addon;
    @Mock
    private User user;
    @Mock
    private World world;

    @Mock
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;

    private AdminCommand cmd;

    @Before
    public void setUp() {
        ServerMocks.newServer();
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Help output and player lookups go through the plugin
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(parent.getLabel()).thenReturn("bsbadmin");
        when(parent.getTopLabel()).thenReturn("bsbadmin");
        when(parent.getWorld()).thenReturn(world);
        cmd = new AdminCommand(addon, parent);
    }

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
    }

    /**
     * Test method for {@link AdminCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertTrue(cmd.getPermission().contains("greenhouses.admin"));
        // Usable from the console
        assertFalse(cmd.isOnlyPlayer());
        assertTrue(cmd.getAliases().contains("greenhouse"));
        assertTrue(cmd.getAliases().contains("gh"));
    }

    /**
     * Test method for {@link AdminCommand#setup()}.
     */
    @Test
    public void testSubCommandsRegistered() {
        // Six of our own, plus the help command CompositeCommand registers automatically
        assertEquals(7, cmd.getSubCommands().size());
        List.of("list", "info", "delete", "tp", "verify", "reload")
        .forEach(c -> assertTrue(c + " is not registered", cmd.getSubCommand(c).isPresent()));
    }


}
