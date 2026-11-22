package world.bentobox.greenhouses.ui.panel;

import java.util.List;

import org.bukkit.Material;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.panels.PagedPanel;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;

/**
 * Small helpers shared by the admin panels.
 *
 * @author tastybento
 */
public final class Items {

    private Items() {
        // Utility class
    }

    /**
     * @param lore translated lines of lore, each possibly long or holding line breaks
     * @return the lines wrapped so tooltips stay narrow
     */
    public static List<String> wrap(List<String> lore) {
        return lore.stream().flatMap(l -> Lore.wrapLegacy(l).stream()).toList();
    }

    /**
     * @param icon  the icon
     * @param name  the translated name
     * @param lore  translated lore, wrapped here
     * @param click what a click does, or null for nothing
     * @return the item
     */
    public static PanelItem item(Material icon, String name, List<String> lore, PanelItem.@Nullable ClickHandler click) {
        PanelItemBuilder b = new PanelItemBuilder().icon(icon).name(name).description(wrap(lore));
        if (click != null) {
            b.clickHandler(click);
        }
        return b.build();
    }

    /**
     * @param r what to do
     * @return a click handler that does it, whatever the click
     */
    public static PanelItem.ClickHandler run(Runnable r) {
        return (panel, u, click, slot) -> {
            r.run();
            return true;
        };
    }

    /**
     * @param panel a panel
     * @return the page it shows, or 0 if it is not paged
     */
    public static int page(Panel panel) {
        return panel instanceof PagedPanel p ? p.getPage() : 0;
    }
}
