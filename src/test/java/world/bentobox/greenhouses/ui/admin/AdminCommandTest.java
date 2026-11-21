package world.bentobox.greenhouses.ui.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.World;
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
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.WhiteBox;

/**
 * @author tastybento
 */
class AdminCommandTest {

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
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(parent.getLabel()).thenReturn("bsbadmin");
        when(parent.getTopLabel()).thenReturn("bsbadmin");
        when(parent.getWorld()).thenReturn(world);
        cmd = new AdminCommand(addon, parent);
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
     * Test method for {@link AdminCommand#setup()}.
     */
    @Test
    void testSetup() {
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
    void testSubCommandsRegistered() {
        // Six of our own, plus the help command CompositeCommand registers automatically
        assertEquals(7, cmd.getSubCommands().size());
        List.of("list", "info", "delete", "tp", "verify", "reload")
        .forEach(c -> assertTrue(cmd.getSubCommand(c).isPresent(), c + " is not registered"));
    }

}
