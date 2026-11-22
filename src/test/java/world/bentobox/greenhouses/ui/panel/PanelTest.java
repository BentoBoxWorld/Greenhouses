package world.bentobox.greenhouses.ui.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.Settings;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.RecipeManager;

/**
 * Tests the recipe tooltips in {@link Panel}, using the recipes shipped in biomes.yml.
 *
 * @author tastybento
 */
class PanelTest {

    @TempDir
    Path dataFolder;
    private List<BiomeRecipe> recipes;
    private Panel panel;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();
        Files.copy(Path.of("src/main/resources/biomes.yml"), dataFolder.resolve("biomes.yml"));
        Greenhouses addon = mock(Greenhouses.class);
        when(addon.getDataFolder()).thenReturn(dataFolder.toFile());
        when(addon.getSettings()).thenReturn(new Settings());
        recipes = new RecipeManager(addon).getBiomeRecipes();
        panel = new Panel(addon);
        // Translations come back as "key|variables" so the test can see what was asked for;
        // a default answer catches every getTranslation call, however many variables it has
        user = mock(User.class, Mockito.withSettings().defaultAnswer(i -> {
            if (!i.getMethod().getName().equals("getTranslation")) {
                return Mockito.RETURNS_DEFAULTS.answer(i);
            }
            StringBuilder sb = new StringBuilder();
            for (Object a : i.getArguments()) {
                sb.append(sb.isEmpty() ? "" : "|").append(a);
            }
            return sb.toString();
        }));
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        Mockito.framework().clearInlineMocks();
    }

    private BiomeRecipe recipe(String name) {
        return recipes.stream().filter(r -> r.getName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void testShippedRecipesHaveDescriptionsAndMiniMessageNames() {
        assertEquals(19, recipes.size());
        for (BiomeRecipe r : recipes) {
            assertFalse(r.getDescription().isBlank(), r.getName() + " has no description");
            assertFalse(r.getFriendlyName().contains("&"), r.getName() + " " + r.getFriendlyName());
            assertFalse(r.getFriendlyName().contains("§"), r.getName() + " " + r.getFriendlyName());
        }
        assertEquals("<red>Nether</red>", recipe("NETHER").getFriendlyName());
        // A recipe with no friendlyname gets its key, prettified
        assertEquals("Jungle", recipe("JUNGLE").getFriendlyName());
    }

    @Test
    void testTooltipStartsWithWrappedDescriptionNotTheName() {
        BiomeRecipe snowy = recipe("Snowy_beach");
        List<String> lore = panel.getDescription(user, snowy);
        // The description, a wrapped line at a time
        List<String> wrapped = Lore.wrap(snowy.getDescription());
        assertTrue(wrapped.size() > 1, "description should wrap");
        for (int i = 0; i < wrapped.size(); i++) {
            assertEquals("greenhouses.recipe.description|[text]|" + wrapped.get(i), lore.get(i));
        }
        // Then the requirements
        assertEquals("greenhouses.recipe.minimumblockstitle", lore.get(wrapped.size()));
        assertEquals("greenhouses.recipe.block|[block]|Sand x 1", lore.get(wrapped.size() + 1));
        assertTrue(lore.contains("greenhouses.recipe.watermustbe|[coverage]|50"));
        assertTrue(lore.contains("greenhouses.recipe.icemustbe|[coverage]|10"));
        // The recipe name is not repeated
        assertTrue(lore.stream().noneMatch(l -> l.contains("Snowy")), lore.toString());
    }

    @Test
    void testLocaleCanReplaceTheDescription() {
        when(user.getTranslationOrNothing("greenhouses.recipes.OCEAN.description")).thenReturn("Mucha agua.");
        List<String> lore = panel.getDescription(user, recipe("OCEAN"));
        assertEquals("greenhouses.recipe.description|[text]|Mucha agua.", lore.getFirst());
        // No blocks needed for an ocean, just water
        assertTrue(lore.contains("greenhouses.recipe.nootherblocks"));
    }

    @Test
    void testNoDescriptionNoLines() {
        BiomeRecipe r = recipe("PLAINS");
        r.setDescription(null);
        List<String> lore = panel.getDescription(user, r);
        assertEquals("greenhouses.recipe.minimumblockstitle", lore.getFirst());
    }
}
