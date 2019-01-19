package world.bentobox.greenhouses;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import world.bentobox.greenhouses.greenhouse.Greenhouse;
import world.bentobox.greenhouses.ui.Locale;

/**
 * @author tastybento
 * Provides a memory cache of online player information
 * This is the one-stop-shop of player info
 */
public class PlayerCache {
    private HashMap<UUID, Players> playerCache = new HashMap<UUID, Players>();
    private final Greenhouses plugin;

    public PlayerCache(Greenhouses plugin) {
        this.plugin = plugin;
        playerCache.clear();
        // Add any players currently online (handles the /reload condition)
        final Collection<? extends Player> serverPlayers = plugin.getServer().getOnlinePlayers();
        for (Player p : serverPlayers) {
            // Add this player to the online cache
            playerCache.put(p.getUniqueId(), new Players(p));
        }
    }

    /**
     * Add a player to the cache when they join the server (called in JoinLeaveEvents)
     * @param player
     */
    public void addPlayer(Player player) {
        if (!playerCache.containsKey(player.getUniqueId())) {
            playerCache.put(player.getUniqueId(),new Players(player));
        }
        // Check permission limits on number of greenhouses
        int limit = plugin.getMaxGreenhouses(player); // 0 = none allowed. Positive numbers = limit. Negative = unlimited
        /*
	    if (plugin.getPlayerGHouse(player.getUniqueId()) == null) {
	    	return;
	    }
	    */
        List<Greenhouse> toBeRemoved = new ArrayList<Greenhouse>();
        // Look at how many greenhouses player has and remove any over their limit
        int owned = 0;
	    for (Greenhouse g: plugin.getGreenhouses()) {
            if (g.getOwner().equals(player.getUniqueId())) {
                owned++;
                if (owned <= limit) {
                    // Allowed
                    playerCache.get(player.getUniqueId()).incrementGreenhouses();
                    g.setPlayerName(player.getName());
                } else {
                    // Over the limit
                    toBeRemoved.add(g);
                }
            }
        }
        if (Settings.deleteExtras && limit >= 0) {
            // Remove greenhouses
            for (Greenhouse g : toBeRemoved) {
                plugin.removeGreenhouse(g);
                plugin.logger(2,"Removed greenhouse over the limit for " + player.getName());
            }
            if (toBeRemoved.size() > 0) {
                if (limit == 0) {
                    player.sendMessage(ChatColor.RED + Locale.limitsnoneallowed.replace("[number]",String.valueOf(toBeRemoved.size())));
                } else {
                    player.sendMessage(ChatColor.RED + Locale.limitslimitedto.replace("[limit]", String.valueOf(limit)).replace("[number]",String.valueOf(toBeRemoved.size())));
                }
            }
        }
    }


    public void removeOnlinePlayer(Player player) {
        if (playerCache.containsKey(player.getUniqueId())) {
            playerCache.remove(player);
            plugin.logger(3,"Removing player from cache: " + player);
        }
    }

    /**
     * Removes all players on the server now from cache and saves their info
     */
    public void removeAllPlayers() {
        playerCache.clear();
    }

    /*
     * Player info query methods
     */
    /*
    public void setInGreenhouse(Player player, Greenhouse inGreenhouse) {
        if (playerCache.containsKey(player.getUniqueId())) {
            playerCache.get(player.getUniqueId()).setInGreenhouse(inGreenhouse);
        }
    }
*/
    /**
     * @param playerUUID
     * @return the greenhouse the player is in or null if no greenhouse
     */
    public Greenhouse getInGreenhouse(Player player) {
        for (Greenhouse g : plugin.getGreenhouses()) {
            if (g.insideGreenhouse(player.getLocation())) {
                return g;
            }
        }
        return null;
    }

    /**
     * Increments the player's greenhouse count if permissions allow
     * @param player
     * @return true if successful, otherwise false
     */
    public boolean incGreenhouseCount(Player player) {
        // Do a permission check if there are limits
        // Check permission limits on number of greenhouses
        int limit = plugin.getMaxGreenhouses(player); // 0 = none allowed. Positive numbers = limit. Negative = unlimited
        if (limit < 0 || playerCache.get(player.getUniqueId()).getNumberOfGreenhouses() < limit) {
            playerCache.get(player.getUniqueId()).incrementGreenhouses();
            return true;
        }    

        // At the limit, sorry
        return false;	
    }

    /**
     * Decrements the number of greenhouses this player has
     * @param player
     */
    public void decGreenhouseCount(Player player) {
        playerCache.get(player.getUniqueId()).decrementGreenhouses();
    }

    /**
     * Decrements by UUID
     * @param playerUUID
     */
    public void decGreenhouseCount(UUID playerUUID) {
        if (playerCache.containsKey(playerUUID)) {
            playerCache.get(playerUUID).decrementGreenhouses();
        }
    }


    /**
     * Returns true if the player is at their permitted limit of greenhouses otherwise false
     * @param player
     * @return
     */
    public boolean isAtLimit(Player player) {
        if (getRemainingGreenhouses(player) == 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns how many greenhouses the player is allowed to make
     * @param player
     * @return
     */
    public int getRemainingGreenhouses(Player player) {
        int limit = plugin.getMaxGreenhouses(player);
        if (limit < 0) {
            return -1;
        }
        int size = 0;
        if (plugin.getPlayerhouses().containsKey(player.getUniqueId())) {
            size = plugin.getPlayerhouses().get(player.getUniqueId()).size();
        }
        int remaining = limit - size;
        if (remaining < 0) {
            return 0;
        } else {
            return remaining;
        }

    }
}



