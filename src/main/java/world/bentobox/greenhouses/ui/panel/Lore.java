package world.bentobox.greenhouses.ui.panel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word-wraps text for item lore, which the client never wraps itself.
 * <p>
 * The text may contain MiniMessage tags. Tags take no width, and a tag that is still open at
 * the end of a line is closed there and opened again at the start of the next, so each line
 * stands alone - lore lines are separate components, and formatting would otherwise stop at
 * the line break. Line breaks already in the text ({@code \n} or {@code <newline>}) are kept.
 *
 * @author tastybento
 */
public final class Lore {

    /** A sensible lore width in characters: about what fits beside an inventory on a normal screen */
    public static final int WIDTH = 32;

    /**
     * A MiniMessage tag: opening, closing or self-closing, with any arguments. The quantifiers
     * are possessive: the name takes every name character and the rest takes what follows, with
     * no backtracking between them.
     */
    private static final Pattern TAG = Pattern.compile("<(/?+)([!?#a-zA-Z0-9_:.\\-]++)([^<>]*+)>");
    private static final Pattern NEWLINE = Pattern.compile("\\r?\\n|<newline>|<br>");

    private Lore() {
        // Utility class
    }

    /**
     * Wraps text at {@link #WIDTH} characters.
     *
     * @param text text, may contain MiniMessage tags; null or blank gives no lines
     * @return the lines
     */
    public static List<String> wrap(String text) {
        return wrap(text, WIDTH);
    }

    /**
     * Wraps text at the given width. Words longer than the width get a line to themselves
     * rather than being split.
     *
     * @param text  text, may contain MiniMessage tags; null or blank gives no lines
     * @param width the most visible characters on a line
     * @return the lines
     */
    public static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        // Tags open at the end of the last line, oldest first: names and their full opening text
        Deque<String[]> open = new ArrayDeque<>();
        for (String paragraph : NEWLINE.split(text.strip(), -1)) {
            StringBuilder line = new StringBuilder(reopen(open));
            int visible = 0;
            for (String word : paragraph.trim().split(" +")) {
                int w = visibleLength(word);
                if (visible > 0 && visible + 1 + w > width) {
                    lines.add(line + close(open));
                    line = new StringBuilder(reopen(open));
                    visible = 0;
                }
                if (visible > 0) {
                    line.append(' ');
                    visible++;
                }
                line.append(word);
                visible += w;
                track(open, word);
            }
            lines.add(line + close(open));
        }
        return lines;
    }

    /** A legacy code: a hex color ({@code §x§r§r§g§g§b§b}) or a single color or format code */
    private static final Pattern LEGACY = Pattern.compile("\u00A7x(?:\u00A7[0-9a-fA-F]){6}|\u00A7[0-9a-fk-orA-FK-OR]");

    /**
     * Wraps already-translated text, which has legacy {@code §} codes rather than MiniMessage
     * tags, at {@link #WIDTH} characters. Codes take no width, and the color and formats in force
     * at the end of a line start the next one, so each line stands alone. Line breaks already in
     * the text are kept.
     *
     * @param text text with legacy codes; null or blank gives no lines
     * @return the lines
     */
    public static List<String> wrapLegacy(String text) {
        return wrapLegacy(text, WIDTH);
    }

    /**
     * Wraps already-translated text with legacy {@code §} codes at the given width. Words longer
     * than the width get a line to themselves rather than being split.
     *
     * @param text  text with legacy codes; null or blank gives no lines
     * @param width the most visible characters on a line
     * @return the lines
     */
    public static List<String> wrapLegacy(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        // The color and formats in force so far
        StringBuilder active = new StringBuilder();
        for (String paragraph : text.strip().split("\\r?\\n", -1)) {
            StringBuilder line = new StringBuilder(active);
            int visible = 0;
            for (String word : paragraph.trim().split(" +")) {
                int w = LEGACY.matcher(word).replaceAll("").length();
                if (visible > 0 && visible + 1 + w > width) {
                    lines.add(line.toString());
                    line = new StringBuilder(active);
                    visible = 0;
                }
                if (visible > 0) {
                    line.append(' ');
                    visible++;
                }
                line.append(word);
                visible += w;
                Matcher m = LEGACY.matcher(word);
                while (m.find()) {
                    char c = Character.toLowerCase(m.group().charAt(1));
                    if (c >= 'k' && c <= 'o') {
                        // A format adds to the color
                        active.append(m.group());
                    } else {
                        // A color or reset clears formats
                        active.setLength(0);
                        if (c != 'r') {
                            active.append(m.group());
                        }
                    }
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * @param s text
     * @return how many characters show, not counting MiniMessage tags
     */
    static int visibleLength(String s) {
        return TAG.matcher(s).replaceAll("").length();
    }

    /** Updates the open tags with the tags in a word */
    private static void track(Deque<String[]> open, String word) {
        Matcher m = TAG.matcher(word);
        while (m.find()) {
            // Tags with arguments, e.g. <color:#ff0000>, close by their base name: </color>
            String name = m.group(2).toLowerCase(java.util.Locale.ENGLISH).split(":")[0];
            boolean closing = !m.group(1).isEmpty();
            boolean selfClosing = m.group(3).endsWith("/");
            if (name.equals("reset")) {
                open.clear();
            } else if (closing) {
                // Close the most recent tag of this name and anything opened inside it
                removeLatest(open, name);
            } else if (!selfClosing) {
                open.addLast(new String[] { name, m.group() });
            }
        }
    }

    private static void removeLatest(Deque<String[]> open, String name) {
        Iterator<String[]> it = open.descendingIterator();
        while (it.hasNext()) {
            if (it.next()[0].equals(name)) {
                it.remove();
                return;
            }
        }
    }

    private static String reopen(Deque<String[]> open) {
        StringBuilder sb = new StringBuilder();
        open.forEach(t -> sb.append(t[1]));
        return sb.toString();
    }

    private static String close(Deque<String[]> open) {
        StringBuilder sb = new StringBuilder();
        open.descendingIterator().forEachRemaining(t -> sb.append("</").append(t[0]).append('>'));
        return sb.toString();
    }
}
