package world.bentobox.greenhouses.ui.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.greenhouses.CommonTestSetup;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.managers.GreenhouseManager.UnloadedGreenhouse;
import world.bentobox.greenhouses.managers.GreenhouseMap;

/**
 * Tests {@link GreenhouseBrowser}.
 *
 * @author tastybento
 */
class GreenhouseBrowserTest extends CommonTestSetup {

    @Mock
    private Greenhouses addon;
    @Mock
    private CompositeCommand admin;
    @Mock
    private GreenhouseManager gm;
    @Mock
    private GreenhouseMap map;
    @Mock
    private Inventory inventory;
    @Mock
    private Greenhouse loaded;
    @Mock
    private Greenhouse lost;
    @Mock
    private BiomeRecipe recipe;

    private final Map<String, CompositeCommand> subs = new HashMap<>();
    private User user;
    private GreenhouseBrowser browser;
    private boolean backed;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockedBukkit.when(() -> Bukkit.createInventory(any(), anyInt(), any(net.kyori.adventure.text.Component.class)))
                .thenReturn(inventory);
        when(admin.getAddon()).thenReturn(addon);
        when(addon.getManager()).thenReturn(gm);
        when(addon.getIslands()).thenReturn(im);
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        when(gm.getMap()).thenReturn(map);
        for (String label : List.of("tp", "verify", "info", "delete")) {
            CompositeCommand c = mock(CompositeCommand.class);
            when(c.getLabel()).thenReturn(label);
            when(c.getPermission()).thenReturn("bskyblock.greenhouses.admin." + label);
            subs.put(label, c);
        }
        when(admin.getSubCommand(anyString())).thenAnswer(i -> Optional.ofNullable(subs.get(i.getArgument(0))));
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);

        when(recipe.getIcon()).thenReturn(Material.GRASS_BLOCK);
        when(recipe.getFriendlyName()).thenReturn("Plains");
        when(loaded.getUniqueId()).thenReturn("aaaaaaaa-1111");
        when(loaded.getBiomeRecipe()).thenReturn(recipe);
        when(loaded.getBiomeRecipeName()).thenReturn("PLAINS");
        Location l = mock(Location.class);
        when(l.getWorld()).thenReturn(world);
        when(loaded.getLocation()).thenReturn(l);
        when(lost.getUniqueId()).thenReturn("bbbbbbbb-2222");
        when(lost.getBiomeRecipeName()).thenReturn("GONE");
        when(map.getGreenhouses()).thenReturn(List.of(loaded));
        when(gm.getUnloaded()).thenReturn(List.of(new UnloadedGreenhouse(lost, GreenhouseResult.FAIL_UNKNOWN_RECIPE)));

        user = User.getInstance(mockPlayer);
        browser = new GreenhouseBrowser(admin, user, () -> backed = true);
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

    private void click(PanelItem item, ClickType type) {
        item.getClickHandler().orElseThrow().onClick(openPanel(), user, type, 0);
    }

    @Test
    void testListsLoadedThenUnloaded() {
        browser.open(0);
        Map<Integer, PanelItem> items = openPanel().getItems();
        assertEquals(Material.GRASS_BLOCK, items.get(0).getItem().getType());
        assertEquals(Material.BARRIER, items.get(1).getItem().getType());
        assertTrue(items.get(1).getDescription().contains("greenhouses.browser.unloaded"));
    }

    @Test
    void testClicksRunCommands() {
        browser.open(0);
        PanelItem entry = openPanel().getItems().get(0);
        click(entry, ClickType.LEFT);
        verify(subs.get("tp")).call(user, "tp", List.of("aaaaaaaa-1111"));
        click(entry, ClickType.RIGHT);
        verify(subs.get("verify")).call(user, "verify", List.of("aaaaaaaa-1111"));
        click(entry, ClickType.SHIFT_LEFT);
        verify(subs.get("info")).call(user, "info", List.of("aaaaaaaa-1111"));
    }

    @Test
    void testUnloadedCannotBeVerifiedOrVisited() {
        browser.open(0);
        PanelItem entry = openPanel().getItems().get(1);
        click(entry, ClickType.RIGHT);
        click(entry, ClickType.LEFT);
        verify(subs.get("verify"), never()).call(any(), anyString(), any());
        verify(subs.get("tp"), never()).call(any(), anyString(), any());
        assertFalse(entry.getDescription().contains("greenhouses.browser.click.tp"));
    }

    @Test
    void testNoPermissionNoAction() {
        when(mockPlayer.hasPermission("bskyblock.greenhouses.admin.tp")).thenReturn(false);
        browser.open(0);
        PanelItem entry = openPanel().getItems().get(0);
        assertFalse(entry.getDescription().contains("greenhouses.browser.click.tp"));
        click(entry, ClickType.LEFT);
        verify(subs.get("tp"), never()).call(any(), anyString(), any());
    }

    @Test
    void testOwnerFilterHidesOthersAndUnloaded() {
        browser.owner("someone").open(0);
        Map<Integer, PanelItem> items = openPanel().getItems();
        // Nobody owns the test greenhouse, so only the "none" entry shows
        assertEquals(Material.BARRIER, items.get(0).getItem().getType());
        assertTrue(items.get(0).getName().contains("greenhouses.browser.none"));
    }

    @Test
    void testDelete() {
        when(gm.deleteById("bbbbbbbb-2222")).thenReturn(true);
        browser.delete(lost);
        verify(gm).deleteById("bbbbbbbb-2222");
    }

    @Test
    void testBack() {
        browser.open(0);
        click(openPanel().getItems().get(45 + GreenhouseBrowser.BACK), ClickType.LEFT);
        assertTrue(backed);
    }
}
