package world.bentobox.greenhouses.listeners;

import java.util.Optional;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

public class GreenhouseGuard implements Listener {
    private final Greenhouses plugin;
    public GreenhouseGuard(final Greenhouses plugin) {
        this.plugin = plugin;

    }

    // Stop lava flow or water into or out of a greenhouse
    @EventHandler(priority = EventPriority.NORMAL)
    public void onFlow(final BlockFromToEvent e) {
        // Flow may be allowed anyway
        if (plugin.getSettings().isAllowFlowIn() && plugin.getSettings().isAllowFlowOut()) {
            return;
        }
        if (!plugin.getActiveWorlds().contains(e.getBlock().getWorld())) {
            return;
        }
        // Get To and From
        Optional<Greenhouse> to = plugin.getManager().getMap().getGreenhouse(e.getToBlock().getLocation());
        Optional<Greenhouse> from = plugin.getManager().getMap().getGreenhouse(e.getBlock().getLocation());
        // Scenarios
        // 1. inside district or outside - always ok
        // 2. inside to outside - allowFlowOut determines
        // 3. outside to inside - allowFlowIn determines
        if (!to.isPresent() && !from.isPresent()) {
            return;
        }
        if (to.isPresent() && from.isPresent() && to.equals(from)) {
            return;
        }
        // to is a greenhouse
        if (to.isPresent() && plugin.getSettings().isAllowFlowIn()) {
            return;
        }
        // from is a greenhouse
        if (from.isPresent() && plugin.getSettings().isAllowFlowOut()) {
            return;
        }
        // Otherwise cancel - the flow is not allowed
        e.setCancelled(true);
    }


}

