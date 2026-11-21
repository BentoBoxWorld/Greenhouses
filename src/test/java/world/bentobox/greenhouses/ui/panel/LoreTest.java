package world.bentobox.greenhouses.ui.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link Lore}.
 *
 * @author tastybento
 */
class LoreTest {

    @Test
    void testEmpty() {
        assertTrue(Lore.wrap(null).isEmpty());
        assertTrue(Lore.wrap("   ").isEmpty());
    }

    @Test
    void testShortTextIsOneLine() {
        assertEquals(List.of("A quiet beach."), Lore.wrap("A quiet beach."));
    }

    @Test
    void testWrapsAtWordBoundaries() {
        List<String> lines = Lore.wrap("Sun-bleached sand and warm shallows. Turtles come ashore to nest.", 20);
        assertEquals(List.of("Sun-bleached sand", "and warm shallows.", "Turtles come ashore", "to nest."), lines);
        lines.forEach(l -> assertTrue(l.length() <= 20, l));
    }

    @Test
    void testDefaultWidth() {
        String text = "Hot, green and noisy. Melons, roses, ferns and cocoa beans grow fast, and parrots squawk above the pandas.";
        List<String> lines = Lore.wrap(text);
        assertTrue(lines.size() > 1);
        lines.forEach(l -> assertTrue(l.length() <= Lore.WIDTH, l));
        assertEquals(text, String.join(" ", lines));
    }

    @Test
    void testLongWordGetsItsOwnLine() {
        assertEquals(List.of("a", "supercalifragilistic", "b"), Lore.wrap("a supercalifragilistic b", 10));
    }

    @Test
    void testExplicitLineBreaksKept() {
        assertEquals(List.of("One.", "Two."), Lore.wrap("One.\nTwo.", 30));
        assertEquals(List.of("One.", "Two."), Lore.wrap("One.<newline>Two.", 30));
    }

    @Test
    void testTagsTakeNoWidth() {
        assertEquals(5, Lore.visibleLength("<red>Hello</red>"));
        assertEquals(List.of("<red>Hello</red> world"), Lore.wrap("<red>Hello</red> world", 11));
    }

    @Test
    void testOpenTagCarriesToNextLine() {
        List<String> lines = Lore.wrap("<gold>Watch out for the drowned</gold> tonight", 12);
        assertEquals(List.of("<gold>Watch out</gold>", "<gold>for the</gold>", "<gold>drowned</gold>", "tonight"), lines);
    }

    @Test
    void testNestedTagsCloseInReverseOrder() {
        List<String> lines = Lore.wrap("<red><bold>Very hot lava here</bold></red>", 9);
        assertEquals("<red><bold>Very hot</bold></red>", lines.getFirst());
        assertEquals("<red><bold>lava here</bold></red>", lines.get(1));
    }

    @Test
    void testTagWithArgumentClosesByName() {
        List<String> lines = Lore.wrap("<color:#ff8800>orange sky above</color>", 10);
        assertEquals(List.of("<color:#ff8800>orange sky</color>", "<color:#ff8800>above</color>"), lines);
    }

    @Test
    void testResetClosesEverything() {
        List<String> lines = Lore.wrap("<red>red words <reset>plain words here", 15);
        // After <reset> nothing is open, so nothing is closed at the break or carried over
        assertEquals("<red>red words <reset>plain", lines.getFirst());
        assertEquals("words here", lines.get(1));
    }
}
