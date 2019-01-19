package world.bentobox.greenhouses.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.Settings;
import world.bentobox.greenhouses.greenhouse.Greenhouse;

public class GreenhouseGuard implements Listener {
    private final Greenhouses plugin;
    public GreenhouseGuard(final Greenhouses plugin) {
	this.plugin = plugin;

    }

    // Stop lava flow or water into or out of a greenhouse
    @EventHandler(priority = EventPriority.NORMAL)
    public void onFlow(final BlockFromToEvent e) {
	// Flow may be allowed anyway
	if (Settings.allowFlowIn && Settings.allowFlowOut) {
	    return;
	}
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getBlock().getWorld().getName())) {
	    return;
	}
	// Get To and From Districts
	Greenhouse to = plugin.getInGreenhouse(e.getToBlock().getLocation());
	Greenhouse from = plugin.getInGreenhouse(e.getBlock().getLocation());
	// Scenarios
	// 1. inside district or outside - always ok
	// 2. inside to outside - allowFlowOut determines
	// 3. outside to inside - allowFlowIn determines
	if (to == null && from == null) {
	    return;
	}
	if (to !=null && from != null && to.equals(from)) {
	    return;
	}
	// to or from or both are districts, NOT the same and flow is across a boundary
	// if to is a district, flow in is allowed 
	if (to != null && Settings.allowFlowIn) {
	    return;
	}
	// if from is a district, flow may allowed
	if (from != null && Settings.allowFlowOut) {
	    return;
	}
	// Otherwise cancel - the flow is not allowed
	e.setCancelled(true);
    }


}

