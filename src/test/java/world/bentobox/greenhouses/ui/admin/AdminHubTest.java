package world.bentobox.greenhouses.ui.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.dialogs.DialogResponse;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.greenhouses.CommonTestSetup;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseMap;
import world.bentobox.greenhouses.managers.RecipeManager;

/**
 * Tests {@link AdminHub}.
 *
 * @author tastybento
 */
class AdminHubTest extends CommonTestSetup {

    private static final List<String> LABELS = List.of("list", "info", "delete", "tp", "verify", "reload", "build",
            "edit");

    @Mock
    private Greenhouses addon;
    @Mock
    private CompositeCommand admin;
    @Mock
    private RecipeManager rm;
    @Mock
    private GreenhouseManager gm;
    @Mock
    private GreenhouseMap map;
    @Mock
    private Inventory inventory;
    @Mock
    private AdminBuildCommand build;

    private final Map<String, CompositeCommand> subs = new java.util.HashMap<>();
    private User user;
    private AdminHub hub;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockedBukkit.when(() -> Bukkit.createInventory(any(), anyInt(), any(net.kyori.adventure.text.Component.class)))
                .thenReturn(inventory);
        when(admin.getAddon()).thenReturn(addon);
        when(addon.getRecipes()).thenReturn(rm);
        when(addon.getManager()).thenReturn(gm);
        when(gm.getMap()).thenReturn(map);
        when(gm.getUnloaded()).thenReturn(List.of());
        for (String label : LABELS) {
            CompositeCommand c = label.equals("build") ? build : mock(CompositeCommand.class);
            when(c.getLabel()).thenReturn(label);
            when(c.getPermission()).thenReturn("bskyblock.greenhouses.admin." + label);
            subs.put(label, c);
        }
        when(admin.getSubCommand(anyString())).thenAnswer(i -> Optional.ofNullable(subs.get(i.getArgument(0))));
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);
        user = User.getInstance(mockPlayer);
        hub = new AdminHub(admin, user);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        PanelListenerManager.getOpenPanels().clear();
        super.tearDown();
    }

    private Panel openPanel() {
        return PanelListenerManager.getOpenPanels().get(uuid);
    }

    @Test
    void testAllButtons() {
        hub.open();
        Map<Integer, PanelItem> items = openPanel().getItems();
        for (int slot : List.of(AdminHub.RECIPES, AdminHub.GREENHOUSES, AdminHub.BUILD, AdminHub.VERIFY,
                AdminHub.RELOAD, AdminHub.CLOSE)) {
            assertNotNull(items.get(slot), "slot " + slot);
        }
    }

    @Test
    void testButtonsNeedPermission() {
        when(mockPlayer.hasPermission("bskyblock.greenhouses.admin.list")).thenReturn(false);
        when(mockPlayer.hasPermission("bskyblock.greenhouses.admin.edit")).thenReturn(false);
        hub.open();
        Map<Integer, PanelItem> items = openPanel().getItems();
        assertNull(items.get(AdminHub.GREENHOUSES));
        assertNull(items.get(AdminHub.RECIPES));
        assertNotNull(items.get(AdminHub.VERIFY));
    }

    @Test
    void testVerifyAllRunsTheCommand() {
        hub.open();
        openPanel().getItems().get(AdminHub.VERIFY).getClickHandler().orElseThrow().onClick(openPanel(), user,
                ClickType.LEFT, AdminHub.VERIFY);
        verify(subs.get("verify")).call(user, "verify", List.of());
    }

    @Test
    void testCallNeedsPermission() {
        when(mockPlayer.hasPermission("bskyblock.greenhouses.admin.reload")).thenReturn(false);
        hub.call("reload");
        verify(subs.get("reload"), never()).call(any(), anyString(), any());
    }

    @Test
    void testBuildFromDialog() {
        BiomeRecipe r = mock(BiomeRecipe.class);
        when(r.getName()).thenReturn("OCEAN");
        hub.build(r, DialogResponse.of(Map.of("width", 9f, "depth", 7f, "height", 99f, "mode", "clear")));
        // Height is held to the most allowed
        verify(build).buildNow(eq(user), eq(List.of("OCEAN", "9x7x15", "clear")));
    }

    @Test
    void testBuildRecipePicker() {
        BiomeRecipe r = mock(BiomeRecipe.class);
        when(r.getName()).thenReturn("OCEAN");
        when(r.getFriendlyName()).thenReturn("Ocean");
        when(r.getIcon()).thenReturn(Material.WATER_BUCKET);
        when(r.getRecipeBlocks()).thenReturn(List.of());
        when(rm.getBiomeRecipes()).thenReturn(List.of(r));
        hub.pickBuildRecipe();
        assertEquals(Material.WATER_BUCKET, openPanel().getItems().get(0).getItem().getType());
    }
}
