package world.bentobox.greenhouses.ui.admin.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.greenhouses.CommonTestSetup;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.RecipeDraft;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.ConversionSpec;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.MobSpec;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.PlantSpec;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseMap;
import world.bentobox.greenhouses.managers.RecipeManager;
import world.bentobox.greenhouses.managers.RecipeManager.SaveResult;
import world.bentobox.greenhouses.managers.RecipeManager.SaveResult.Status;

/**
 * Tests {@link RecipeEditor}: the editor panels and what clicks on list entries do.
 * Dialog forms can't be built without a real server, so form logic is tested in
 * {@link FormsTest} and the click paths that only open a form are left out here.
 *
 * @author tastybento
 */
class RecipeEditorTest extends CommonTestSetup {

    @Mock
    private Greenhouses addon;
    @Mock
    private RecipeManager rm;
    @Mock
    private GreenhouseManager gm;
    @Mock
    private GreenhouseMap map;
    @Mock
    private Inventory inventory;

    private User user;
    private RecipeEditor editor;
    private RecipeDraft draft;
    private EditSession session;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockedBukkit.when(() -> Bukkit.createInventory(any(), anyInt(), any(net.kyori.adventure.text.Component.class)))
                .thenReturn(inventory);
        when(addon.getRecipes()).thenReturn(rm);
        when(addon.getManager()).thenReturn(gm);
        when(gm.getMap()).thenReturn(map);
        Greenhouse gh = mock(Greenhouse.class);
        when(gh.getBiomeRecipeName()).thenReturn("PLAINS");
        when(map.getGreenhouses()).thenReturn(List.of(gh, gh));
        user = User.getInstance(mockPlayer);
        editor = new RecipeEditor(addon, user);

        draft = new RecipeDraft("PLAINS");
        draft.setBiome(Biome.PLAINS);
        draft.setIcon(Material.GRASS_BLOCK);
        draft.setFriendlyName("Horse Plains");
        draft.getRequired().put(Material.GRASS_BLOCK, 3);
        draft.getPlants().add(new PlantSpec(Material.SHORT_GRASS, 10, Material.GRASS_BLOCK));
        draft.getMobs().add(new MobSpec(EntityType.HORSE, null, 18, Material.GRASS_BLOCK));
        draft.getConversions().add(new ConversionSpec(Material.DIRT, 5, Material.GRASS_BLOCK, Material.WATER));
        session = EditSession.start(uuid, new EditSession(draft, 7, false));
        when(rm.getDraft("PLAINS")).thenAnswer(i -> Optional.of(RecipeDraft.copyOf(draft)));
        when(rm.getRevision("PLAINS")).thenReturn(7);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        EditSession.endAll();
        PanelListenerManager.getOpenPanels().clear();
        super.tearDown();
    }

    private Panel openPanel() {
        return PanelListenerManager.getOpenPanels().get(uuid);
    }

    @Test
    void testGreenhousesUsing() {
        assertEquals(2, editor.greenhousesUsing("PLAINS"));
        assertEquals(0, editor.greenhousesUsing("OCEAN"));
    }

    @Test
    void testEditorLayout() {
        editor.open(session);
        Panel p = openPanel();
        assertNotNull(p);
        Map<Integer, PanelItem> items = p.getItems();
        for (int slot : List.of(RecipeEditor.ICON, RecipeEditor.GENERAL, RecipeEditor.BIOME, RecipeEditor.COVERAGE,
                RecipeEditor.MOB_LIMITS, RecipeEditor.BLOCKS, RecipeEditor.PLANTS, RecipeEditor.MOBS,
                RecipeEditor.CONVERSIONS, RecipeEditor.BACK, RecipeEditor.DISCARD, RecipeEditor.SAVE,
                RecipeEditor.DUPLICATE, RecipeEditor.DELETE)) {
            assertNotNull(items.get(slot), "slot " + slot);
        }
        assertEquals(Material.GRASS_BLOCK, items.get(RecipeEditor.ICON).getItem().getType());
        // Nothing changed yet, so Save says so
        assertTrue(items.get(RecipeEditor.SAVE).getDescription().contains("greenhouses.editor.save.nothing"));
    }

    @Test
    void testNewRecipeHasNoDelete() {
        editor.open(EditSession.start(uuid, new EditSession(draft, RecipeManager.NEW_RECIPE, true)));
        assertNull(openPanel().getItems().get(RecipeEditor.DELETE));
    }

    @Test
    void testSaveShowsProblems() {
        draft.setMobLimit(0);
        session.changed();
        editor.open(session);
        PanelItem save = openPanel().getItems().get(RecipeEditor.SAVE);
        assertEquals(Material.REDSTONE_BLOCK, save.getItem().getType());
        assertTrue(save.getDescription().contains("greenhouses.editor.save.problems"));
    }

    @Test
    void testRequiredBlockClicks() {
        editor.blockClick(session, Material.GRASS_BLOCK, ClickType.LEFT, 0);
        assertEquals(4, draft.getRequired().get(Material.GRASS_BLOCK));
        assertTrue(session.isDirty());
        editor.blockClick(session, Material.GRASS_BLOCK, ClickType.RIGHT, 0);
        editor.blockClick(session, Material.GRASS_BLOCK, ClickType.RIGHT, 0);
        editor.blockClick(session, Material.GRASS_BLOCK, ClickType.RIGHT, 0);
        editor.blockClick(session, Material.GRASS_BLOCK, ClickType.RIGHT, 0);
        // Never below 1
        assertEquals(1, draft.getRequired().get(Material.GRASS_BLOCK));
        editor.blockClick(session, Material.GRASS_BLOCK, ClickType.DROP, 0);
        assertFalse(draft.getRequired().containsKey(Material.GRASS_BLOCK));
        // The list page was shown again
        assertNotNull(openPanel());
    }

    @Test
    void testRemovingListEntries() {
        editor.plantClick(session, 0, ClickType.DROP, 0);
        assertTrue(draft.getPlants().isEmpty());
        editor.mobClick(session, 0, ClickType.CONTROL_DROP, 0);
        assertTrue(draft.getMobs().isEmpty());
        editor.conversionClick(session, 0, ClickType.DROP, 0);
        assertTrue(draft.getConversions().isEmpty());
        assertTrue(session.isDirty());
    }

    @Test
    void testConversionAnywhere() {
        editor.conversionClick(session, 0, ClickType.SHIFT_LEFT, 0);
        assertNull(draft.getConversions().getFirst().adjacent());
        assertEquals(Material.GRASS_BLOCK, draft.getConversions().getFirst().to());
    }

    @Test
    void testOtherClicksDoNothing() {
        editor.blockClick(session, Material.GRASS_BLOCK, ClickType.MIDDLE, 0);
        editor.plantClick(session, 0, ClickType.MIDDLE, 0);
        assertEquals(3, draft.getRequired().get(Material.GRASS_BLOCK));
        assertEquals(1, draft.getPlants().size());
        assertFalse(session.isDirty());
    }

    @Test
    void testListPages() {
        editor.openBlocks(session, 0);
        assertEquals(Material.GRASS_BLOCK, openPanel().getItems().get(0).getItem().getType());
        editor.openPlants(session, 0);
        assertEquals(Material.SHORT_GRASS, openPanel().getItems().get(0).getItem().getType());
        editor.openMobs(session, 0);
        assertEquals(Material.HORSE_SPAWN_EGG, openPanel().getItems().get(0).getItem().getType());
        editor.openConversions(session, 0);
        assertEquals(Material.DIRT, openPanel().getItems().get(0).getItem().getType());
    }

    @Test
    void testSaveSavedStartsFresh() {
        session.changed();
        when(rm.save(any(), anyInt())).thenReturn(new SaveResult(Status.SAVED, List.of()));
        editor.save(session);
        verify(rm).save(draft, 7);
        // A fresh, clean session for the saved recipe
        EditSession now = EditSession.get(uuid).orElseThrow();
        assertFalse(now.isDirty());
        assertEquals("PLAINS", now.getKey());
    }

    @Test
    void testSaveInvalidKeepsEditing() {
        session.changed();
        when(rm.save(any(), anyInt())).thenReturn(new SaveResult(Status.INVALID, List.of("Bad")));
        editor.save(session);
        assertTrue(EditSession.get(uuid).orElseThrow().isDirty());
    }

    @Test
    void testSaveNothingChanged() {
        editor.save(session);
        verify(rm, never()).save(any(), anyInt());
    }

    @Test
    void testEditResumesUnsavedChanges() {
        draft.setPriority(42);
        session.changed();
        editor.edit("PLAINS");
        assertEquals(42, EditSession.get(uuid).orElseThrow().getDraft().getPriority());
    }

    @Test
    void testEditStartsFreshWhenClean() {
        editor.edit("PLAINS");
        assertFalse(EditSession.get(uuid).orElseThrow().isDirty());
        assertEquals(7, EditSession.get(uuid).orElseThrow().getRevision());
    }

    @Test
    void testEditUnknownRecipe() {
        EditSession.end(uuid);
        when(rm.getDraft("NOPE")).thenReturn(Optional.empty());
        editor.edit("NOPE");
        assertTrue(EditSession.get(uuid).isEmpty());
    }

    @Test
    void testRecipeList() {
        BiomeRecipe r = mock(BiomeRecipe.class);
        when(r.getName()).thenReturn("PLAINS");
        when(r.getFriendlyName()).thenReturn("Horse Plains");
        when(r.getIcon()).thenReturn(Material.GRASS_BLOCK);
        when(r.getBiome()).thenReturn(Biome.PLAINS);
        when(rm.getBiomeRecipes()).thenReturn(List.of(r));
        editor.openList();
        Panel p = openPanel();
        assertEquals(Material.GRASS_BLOCK, p.getItems().get(0).getItem().getType());
        // The New recipe button
        assertEquals(Material.WRITABLE_BOOK, p.getItems().get(45 + RecipeEditor.LIST_ADD).getItem().getType());
    }

    @Test
    void testEggs() {
        assertEquals(Material.COW_SPAWN_EGG, RecipeEditor.egg(EntityType.COW));
        assertEquals(Material.NAME_TAG, RecipeEditor.egg(EntityType.PLAYER));
    }

    @Test
    void testPickHelpers() {
        assertEquals(Material.WATER_BUCKET, Pick.icon(Material.WATER));
        assertEquals(Material.LAVA_BUCKET, Pick.icon(Material.LAVA));
        assertEquals(Material.STONE, Pick.icon(Material.STONE));
        assertEquals(Optional.of(Material.WATER), Pick.fromItem(new org.bukkit.inventory.ItemStack(Material.WATER_BUCKET)));
        assertEquals(Optional.of(Material.STONE), Pick.fromItem(new org.bukkit.inventory.ItemStack(Material.STONE)));
        assertTrue(Pick.fromItem(new org.bukkit.inventory.ItemStack(Material.STICK)).isEmpty());
        verify(mockPlayer, never()).sendMessage(anyString());
    }
}
