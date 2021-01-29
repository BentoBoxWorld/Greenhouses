package world.bentobox.greenhouses.listeners;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * @author tastybento
 * This class listens for changes to greenhouses and reacts to them
 */
public class GreenhouseEvents implements Listener {
    private static final String BIOME = "[biome]";
    private final Greenhouses addon;

    public GreenhouseEvents(final Greenhouses addon) {
        this.addon = addon;
    }

    /**
     * Permits water to be placed in the Nether if in a greenhouse and in an acceptable biome
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPlayerInteractInNether(PlayerBucketEmptyEvent e) {
        if (e.getPlayer().getWorld().getEnvironment().equals(World.Environment.NETHER)
                && e.getBucket().equals(Material.WATER_BUCKET)
                && addon.getManager().getMap().inGreenhouse(e.getBlockClicked().getLocation())) {
            e.getBlockClicked().getRelative(e.getBlockFace()).setType(Material.WATER);
        }
    }

    /**
     * Makes water in the Nether if ice is broken and in a greenhouse
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
    public void onIceBreak(BlockBreakEvent e) {
        if (!e.getBlock().getWorld().getEnvironment().equals(World.Environment.NETHER)
                || !Tag.ICE.isTagged(e.getBlock().getType())) {
            return;
        }
        if (addon.getManager().getMap().inGreenhouse(e.getBlock().getLocation())) {
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
        Optional<Greenhouse> to = addon.getManager().getMap().getGreenhouse(toLoc);
        Optional<Greenhouse> from = addon.getManager().getMap().getGreenhouse(fromLoc);
        if (!to.isPresent() && !from.isPresent()) {
            return;
        }
        if (to.isPresent() && from.isPresent()) {
            if (!to.get().equals(from.get())) {
                // Leaving greenhouse, entering another
                user.sendMessage("greenhouses.event.leaving", BIOME, from.get().getBiomeRecipe().getFriendlyName());
                user.sendMessage("greenhouses.event.entering", BIOME,  to.get().getBiomeRecipe().getFriendlyName());
            }
            // Same greenhouse
            return;
        }
        // from is a greenhouse
        if (from.isPresent() && !to.isPresent()) {
            // Exiting
            user.sendMessage("greenhouses.event.leaving", BIOME, from.get().getBiomeRecipe().getFriendlyName());
            return;
        }
        if (!from.isPresent()) {
            // Entering
            user.sendMessage("greenhouses.event.entering", BIOME, to.get().getBiomeRecipe().getFriendlyName());
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
     * Checks if broken blocks cause the greenhouse to fail
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onBlockBreak(final BlockBreakEvent e) {
        User user = User.getInstance(e.getPlayer());
        addon.getManager().getMap().getGreenhouse(e.getBlock().getLocation()).ifPresent(g -> {
            // Check to see if wall or roof block broken
            if ((e.getBlock().getLocation().getBlockY() == g.getCeilingHeight() - 1)
                    || e.getBlock().getLocation().getBlockX() == (int)g.getBoundingBox().getMinX()
                    || e.getBlock().getLocation().getBlockX() == (int)g.getBoundingBox().getMaxX() - 1
                    || e.getBlock().getLocation().getBlockZ() == (int)g.getBoundingBox().getMinZ()
                    || e.getBlock().getLocation().getBlockZ() == (int)g.getBoundingBox().getMaxZ() - 1
                    ) {
                user.sendMessage("greenhouses.event.broke", BIOME, Util.prettifyText(g.getOriginalBiome().name()));
                addon.getManager().removeGreenhouse(g);
            }
        });
    }

    private boolean checkBlockHeight(Block block) {
        return addon.getManager().getMap().getGreenhouse(block.getLocation())
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

