package world.bentobox.greenhouses.ui.admin;

import java.util.List;

import org.bukkit.Location;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
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

    /**
     * Root of the admin locale keys and of the admin permissions
     */
    private static final String KEY = "greenhouses.commands.admin.";

    private AdminUtil() {
        // Utility class
    }

    /**
     * Applies the permission, description and parameter help that every admin sub-command
     * declares, derived from its label. Commands default to being usable from the console;
     * those that are not should call {@code setOnlyPlayer(true)} themselves.
     * @param cmd - the sub-command being set up
     * @param hasParameters - whether the command takes arguments and so has a parameters key
     */
    static void setup(CompositeCommand cmd, boolean hasParameters) {
        cmd.setPermission("greenhouses.admin." + cmd.getLabel());
        cmd.setOnlyPlayer(false);
        cmd.setDescription(KEY + cmd.getLabel() + ".description");
        if (hasParameters) {
            cmd.setParametersHelp(KEY + cmd.getLabel() + ".parameters");
        }
    }

    /**
     * Looks up a greenhouse by ID, telling the user if there is no match.
     * @param cmd - the sub-command, used to reach the addon
     * @param user - user to report failure to
     * @param id - unique ID, or a unique prefix of one
     * @return the greenhouse, or null if there is no unique match
     */
    static Greenhouse byId(CompositeCommand cmd, User user, String id) {
        Greenhouses addon = cmd.getAddon();
        Greenhouse gh = addon.getManager().getGreenhouseById(id).orElse(null);
        if (gh == null) {
            user.sendMessage(KEY + "errors.unknown-id", "[id]", id);
        }
        return gh;
    }

    /**
     * Looks up the greenhouse named by a command that takes exactly one ID argument. Shows the
     * command help if the argument count is wrong.
     * @param cmd - the sub-command
     * @param user - user to report failure to
     * @param args - command arguments
     * @return the greenhouse, or null if the arguments are wrong or nothing matches
     */
    static Greenhouse requireOne(CompositeCommand cmd, User user, List<String> args) {
        if (args.size() != 1) {
            cmd.showHelp(cmd, user);
            return null;
        }
        return byId(cmd, user, args.get(0));
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
