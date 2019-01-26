package world.bentobox.greenhouses.listeners;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * @author tastybento
 * This class listens for changes to greenhouses and reacts to them
 */
public class GreenhouseEvents implements Listener {
    private final Greenhouses plugin;

    public GreenhouseEvents(final Greenhouses plugin) {
        this.plugin = plugin;

    }

    /**
     * Permits water to be placed in the Nether if in a greenhouse and in an acceptable biome
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!e.getPlayer().getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            return;
        }
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getItem() != null && e.getItem().getType().equals(Material.WATER_BUCKET)) {
            e.setCancelled(true);
            e.getClickedBlock().getRelative(e.getBlockFace()).setType(Material.WATER);
            e.getItem().setType(Material.BUCKET);
        }
    }

    /**
     * Makes water in the Nether if ice is broken and in a greenhouse
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
    public void onIceBreak(BlockBreakEvent e) {
        if (!e.getBlock().getWorld().getEnvironment().equals(World.Environment.NETHER)
                || (!e.getBlock().getType().equals(Material.ICE) && !e.getBlock().getType().equals(Material.BLUE_ICE))) {
            return;
        }
        if (plugin.getManager().getMap().getGreenhouse(e.getBlock().getLocation()).isPresent()) {
            e.setCancelled(true);
            e.getBlock().setType(Material.WATER);
        }
    }

    /**
     * Tracks player movement
     * @param e - event
     */

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        handleTransition(User.getInstance(e.getPlayer()), e.getTo(), e.getFrom());
    }

    private void handleTransition(User user, Location toLoc, Location fromLoc) {
        Optional<Greenhouse> to = plugin.getManager().getMap().getGreenhouse(toLoc);
        Optional<Greenhouse> from = plugin.getManager().getMap().getGreenhouse(fromLoc);
        if (!to.isPresent() && !from.isPresent()) {
            return;
        }
        if (to.isPresent() && from.isPresent() && to.equals(from)) {
            // Same greenhouse
            return;
        }
        // to is a greenhouse
        if (to.isPresent() && from.isPresent() && !to.equals(from)) {
            // Leaving greenhouse, entering another
            user.sendRawMessage("Leaving " + to.get().getBiomeRecipe().getFriendlyName() + " greenhouse");
            user.sendRawMessage("Entering " + from.get().getBiomeRecipe().getFriendlyName() + " greenhouse");
            return;
        }
        // from is a greenhouse
        if (from.isPresent() && !to.isPresent()) {
            // Exiting
            user.sendRawMessage("Leaving " + from.get().getBiomeRecipe().getFriendlyName() + " greenhouse");
            return;
        }
        if (!from.isPresent()) {
            // Entering
            user.sendRawMessage("Entering " + to.get().getBiomeRecipe().getFriendlyName() + " greenhouse");
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        // Strangely, sometimes these worlds can be null
        if (e.getFrom() == null || e.getTo() == null) {
            return;
        }
        handleTransition(User.getInstance(e.getPlayer()), e.getTo(), e.getFrom());
    }


    /**
     * Checks is broken blocks cause the greenhouse to fail
     * @param e - event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockBreak(final BlockBreakEvent e) {
        User user = User.getInstance(e.getPlayer());
        plugin.getManager().getMap().getGreenhouse(e.getBlock().getLocation()).ifPresent(g -> {
            // Check to see if wall or roof block broken
            if ((e.getBlock().getLocation().getBlockY() == g.getCeilingHeight())
                    || e.getBlock().getLocation().getBlockX() == g.getFootprint().getMinX()
                    || e.getBlock().getLocation().getBlockX() == g.getFootprint().getMaxX()
                    || e.getBlock().getLocation().getBlockZ() == g.getFootprint().getMinY()
                    || e.getBlock().getLocation().getBlockZ() == g.getFootprint().getMaxY()
                    ) {
                user.sendMessage("greenhouses.broken");
                plugin.getManager().getMap().removeGreenhouse(g);
            }
        });
    }

    /**
     * Prevents placing of blocks above the greenhouses
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
        if (checkBlockHeight(e.getBlock())) {
            e.setCancelled(true);
            User user = User.getInstance(e.getPlayer());
            user.sendMessage("greenhouses.error.cannot-place");
        }
    }

    private boolean checkBlockHeight(Block block) {
        return plugin.getManager().getMap().getGreenhouse(block.getLocation())
                .filter(g -> g.getCeilingHeight() < block.getY())
                .filter(g -> !block.getWorld().getEnvironment().equals(World.Environment.NETHER))
                .isPresent();

    }

    /**
     * Check to see if anyone is sneaking a block over a greenhouse by using a piston
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onPistonPush(final BlockPistonExtendEvent e) {
        e.setCancelled(e.getBlocks().stream().anyMatch(this::checkBlockHeight));
    }
}

