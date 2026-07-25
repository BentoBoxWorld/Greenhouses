package world.bentobox.greenhouses.ui.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.Material;
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
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.managers.GreenhouseMap;
import world.bentobox.greenhouses.mocks.ServerMocks;

/**
 * @author tastybento
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class })
public class AdminVerifyCommandTest {

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
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;

    private AdminVerifyCommand cmd;
    private Greenhouse gh;
    private BiomeRecipe br;

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
        when(gm.getMap()).thenReturn(map);
        br = mock(BiomeRecipe.class);
        gh = mock(Greenhouse.class);
        when(gh.getUniqueId()).thenReturn(ID);
        when(gh.getLocation()).thenReturn(new Location(world, 1, 60, 2));
        when(gh.getBiomeRecipe()).thenReturn(br);
        when(gh.getMissingBlocks()).thenReturn(Collections.emptyMap());
        cmd = new AdminVerifyCommand(parent);
    }

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
    }

    /**
     * Test method for {@link AdminVerifyCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteNoGreenhouses() {
        when(map.getGreenhouses()).thenReturn(Collections.emptyList());
        assertFalse(cmd.canExecute(user, "verify", Collections.emptyList()));
        verify(user).sendMessage("greenhouses.commands.admin.list.none");
    }

    /**
     * Test method for {@link AdminVerifyCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteUnknownId() {
        when(gm.getGreenhouseById(anyString())).thenReturn(Optional.empty());
        assertFalse(cmd.canExecute(user, "verify", List.of("nope")));
        verify(user).sendMessage(eq("greenhouses.commands.admin.errors.unknown-id"), eq("[id]"), eq("nope"));
    }

    /**
     * Test method for {@link AdminVerifyCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecutePassingRecipe() {
        when(gm.getGreenhouseById(ID)).thenReturn(Optional.of(gh));
        when(br.checkRecipe(gh)).thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
        assertTrue(cmd.canExecute(user, "verify", List.of(ID)));
        assertTrue(cmd.execute(user, "verify", List.of(ID)));
        verify(user).sendMessage(eq("greenhouses.commands.admin.verify.ok"), eq("[id]"), anyString(), eq("[xyz]"),
                eq("1,60,2"));
    }

    /**
     * Test method for {@link AdminVerifyCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteFailingRecipeListsReasonsAndMissingBlocks() {
        when(gm.getGreenhouseById(ID)).thenReturn(Optional.of(gh));
        when(br.checkRecipe(gh)).thenReturn(CompletableFuture.completedFuture(
                Set.of(GreenhouseResult.FAIL_INSUFFICIENT_WATER, GreenhouseResult.FAIL_INSUFFICIENT_BLOCKS)));
        when(gh.getMissingBlocks()).thenReturn(Map.of(Material.DIRT, 4));
        assertTrue(cmd.canExecute(user, "verify", List.of(ID)));
        assertTrue(cmd.execute(user, "verify", List.of(ID)));
        // Reasons are sorted so the output is stable
        verify(user).sendMessage(eq("greenhouses.commands.admin.verify.broken"), eq("[id]"), anyString(), eq("[xyz]"),
                eq("1,60,2"), eq("[reasons]"), eq("FAIL_INSUFFICIENT_BLOCKS, FAIL_INSUFFICIENT_WATER"));
        verify(user).sendMessage(eq("greenhouses.commands.user.make.missing-blocks"), eq("[material]"), eq("DIRT"),
                eq("[number]"), eq("4"));
    }

    /**
     * Test method for {@link AdminVerifyCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteAllGreenhouses() {
        when(map.getGreenhouses()).thenReturn(List.of(gh));
        when(br.checkRecipe(any())).thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
        assertTrue(cmd.canExecute(user, "verify", Collections.emptyList()));
        assertTrue(cmd.execute(user, "verify", Collections.emptyList()));
        verify(user).sendMessage(eq("greenhouses.commands.admin.verify.checking"), eq("[number]"), eq("1"));
    }

}
