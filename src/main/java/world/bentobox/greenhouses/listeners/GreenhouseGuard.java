package world.bentobox.greenhouses.listeners;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

public class GreenhouseGuard implements Listener {
    private final Greenhouses addon;

    public GreenhouseGuard(final Greenhouses addon) {
        this.addon = addon;
    }

    // Stop lava flow or water into or out of a greenhouse
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFlow(final BlockFromToEvent e) {
        // Flow may be allowed anyway
        if (addon.getSettings().isAllowFlowIn() && addon.getSettings().isAllowFlowOut()) {
            return;
        }
        if (!addon.getActiveWorlds().contains(e.getBlock().getWorld())) {
            return;
        }
        // Get To and From
        Optional<Greenhouse> to = addon.getManager().getMap().getGreenhouse(e.getToBlock().getLocation());
        Optional<Greenhouse> from = addon.getManager().getMap().getGreenhouse(e.getBlock().getLocation());
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
        if (to.isPresent() && addon.getSettings().isAllowFlowIn()) {
            return;
        }
        // from is a greenhouse
        if (from.isPresent() && addon.getSettings().isAllowFlowOut()) {
            return;
        }
        // Otherwise cancel - the flow is not allowed
        e.setCancelled(true);
    }

    /**
     * Prevents pistons from pushing greenhouse wall or roof blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent e) {
        e.setCancelled(e.getBlocks().stream()
                .map(Block::getLocation)
                .filter(this::inGreenhouse)
                .findFirst()
                .isPresent());
    }

    /**
     * Prevents sticky pistons from pulling greenhouse wall or roof blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPistonPull(BlockPistonRetractEvent e) {
        e.setCancelled(e.getBlocks().stream()
                .map(Block::getLocation)
                .filter(this::inGreenhouse)
                .findFirst()
                .isPresent());
    }

    private boolean inGreenhouse(Location l) {
        return addon.getManager().getMap().getGreenhouse(l).map(g -> g.isRoofOrWallBlock(l)).orElse(false);
    }
}

