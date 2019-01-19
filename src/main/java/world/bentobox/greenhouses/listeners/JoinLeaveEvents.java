package world.bentobox.greenhouses.listeners;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.PlayerCache;
import world.bentobox.greenhouses.ui.Locale;

public class JoinLeaveEvents implements Listener {
    private Greenhouses addon;
    private PlayerCache players;

    public JoinLeaveEvents(Greenhouses greenhouses, PlayerCache onlinePlayers) {
        this.addon = greenhouses;
        this.players = onlinePlayers;
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        Player p = event.getPlayer();
        final UUID playerUUID = p.getUniqueId();
        // Add player to the cache, and clear any greenhouses over their permitted limit
        addon.players.addPlayer(p);
        addon.logger(3,"Cached " + p.getName());
        // Load any messages for the player
        final List<String> messages = addon.getMessages(playerUUID);
        if (!messages.isEmpty()) {
            addon.getServer().getScheduler().runTaskLater(addon.getPlugin(), () -> {
                event.getPlayer().sendMessage(ChatColor.AQUA + Locale.newsheadline);
                int i = 1;
                for (String message : messages) {
                    event.getPlayer().sendMessage(i++ + ": " + message);
                }
            }, 40L);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        players.removeOnlinePlayer(event.getPlayer());
    }
}