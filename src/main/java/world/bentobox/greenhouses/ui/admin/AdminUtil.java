package world.bentobox.greenhouses.ui.admin;

import org.bukkit.Location;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * Shared formatting helpers for the admin commands.
 *
 * @author tastybento
 */
final class AdminUtil {

    /**
     * Number of characters of a unique ID shown in list output. Enough to stay unique in
     * practice, and enough to paste back into commands that accept an ID prefix.
     */
    static final int SHORT_ID_LENGTH = 8;

    private AdminUtil() {
        // Utility class
    }

    /**
     * @param gh - greenhouse
     * @return the leading characters of the greenhouse's unique ID
     */
    static String shortId(Greenhouse gh) {
        String id = gh.getUniqueId();
        return id.length() > SHORT_ID_LENGTH ? id.substring(0, SHORT_ID_LENGTH) : id;
    }

    /**
     * @param addon - addon
     * @param gh - greenhouse
     * @return the name of the owner of the island the greenhouse is on, or a placeholder
     */
    static String ownerName(Greenhouses addon, Greenhouse gh) {
        Location loc = gh.getLocation();
        if (loc == null) {
            return "unknown";
        }
        return addon.getIslands().getIslandAt(loc).map(Island::getOwner)
                .map(uuid -> addon.getPlayers().getName(uuid)).filter(n -> !n.isEmpty()).orElse("unowned");
    }

    /**
     * @param gh - greenhouse
     * @return the greenhouse's location as "x,y,z", or a placeholder if it has none
     */
    static String xyz(Greenhouse gh) {
        Location loc = gh.getLocation();
        return loc == null ? "unknown" : loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    /**
     * @param gh - greenhouse
     * @return the name of the world the greenhouse is in, or a placeholder
     */
    static String worldName(Greenhouse gh) {
        Location loc = gh.getLocation();
        return loc == null || loc.getWorld() == null ? "unknown" : loc.getWorld().getName();
    }
}
