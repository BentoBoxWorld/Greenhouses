package world.bentobox.greenhouses.listeners;

import java.util.Optional;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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
    private static final Set<Biome> NETHER_BIOMES;
    static {
        NETHER_BIOMES = Set.of(Biome.NETHER_WASTES, Biome.WARPED_FOREST, Biome.CRIMSON_FOREST,
                Biome.SOUL_SAND_VALLEY, Biome.BASALT_DELTAS);
    }
    private final Greenhouses addon;

    public GreenhouseEvents(final Greenhouses addon) {
        this.addon = addon;
    }

    /**
     * Permits water to be placed in the Nether if in a greenhouse and in an acceptable biome
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerInteractInNether(PlayerBucketEmptyEvent e) {
        if (!e.getBucket().equals(Material.WATER_BUCKET)) {
            return;
        }
        Block b = e.getBlockClicked().getRelative(e.getBlockFace());
        if (e.getPlayer().getWorld().getEnvironment().equals(World.Environment.NETHER)
                && !addon.getManager().getMap().getGreenhouse(b.getLocation())
                .map(gh -> gh.getBiomeRecipe().getBiome()).map(NETHER_BIOMES::contains).orElse(true)) {
            // In Nether not a nether greenhouse
            b.setType(Material.WATER);
        } else if (!e.getPlayer().getWorld().getEnvironment().equals(World.Environment.NETHER)
                && addon.getManager().getMap().getGreenhouse(b.getLocation())
                .map(gh -> gh.getBiomeRecipe().getBiome()).map(NETHER_BIOMES::contains).orElse(false)) {
            // Not in Nether, in a nether greenhouse
            e.setCancelled(true);
            if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.WATER_BUCKET)) {
                e.getPlayer().getInventory().getItemInMainHand().setType(Material.BUCKET);
            } else if (e.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.WATER_BUCKET)) {
                e.getPlayer().getInventory().getItemInOffHand().setType(Material.BUCKET);
            }

            b.getWorld().spawnParticle(Particle.SMOKE, b.getLocation(), 10);
            b.getWorld().playSound(b.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1F, 5F);
        }
    }

    /**
     * Makes water in the Nether if ice is broken and in a greenhouse
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onIceBreak(BlockBreakEvent e) {
        if (!Tag.ICE.isTagged(e.getBlock().getType())) {
            return;
        }
        Block b = e.getBlock();
        if (b.getWorld().getEnvironment().equals(World.Environment.NETHER)
                && !addon.getManager().getMap().getGreenhouse(b.getLocation())
                .map(gh -> gh.getBiomeRecipe().getBiome()).map(NETHER_BIOMES::contains).orElse(true)) {
            //
            e.setCancelled(true);
            b.setType(Material.WATER);
        } else if (!e.getPlayer().getWorld().getEnvironment().equals(World.Environment.NETHER)
                && addon.getManager().getMap().getGreenhouse(b.getLocation())
                .map(gh -> gh.getBiomeRecipe().getBiome()).map(NETHER_BIOMES::contains).orElse(false)) {
            // Not in Nether, in a nether greenhouse
            e.setCancelled(true);
            b.setType(Material.AIR);
            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
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

    /**
     * @param user user
     * @param toLoc to location
     * @param fromLoc from location
     */
    private void handleTransition(User user, Location toLoc, Location fromLoc) {
        if (user == null) {
            return;
        }
        Optional<Greenhouse> to = addon.getManager().getMap().getGreenhouse(toLoc);
        Optional<Greenhouse> from = addon.getManager().getMap().getGreenhouse(fromLoc);
        if (to.isEmpty() && from.isEmpty()) {
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
        if (from.isPresent()) {
            // Exiting
            user.sendMessage("greenhouses.event.leaving", BIOME, from.get().getBiomeRecipe().getFriendlyName());
            return;
        }
        // Entering
        user.sendMessage("greenhouses.event.entering", BIOME, to.get().getBiomeRecipe().getFriendlyName());

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
        addon.getManager().getMap().getGreenhouse(e.getBlock().getLocation())
        .filter(g -> g.isRoofOrWallBlock(e.getBlock().getLocation()))
        .ifPresent(g -> {
            if (g.getOriginalBiome() != null) {
                user.sendMessage("greenhouses.event.broke", BIOME, Util.prettifyText(g.getOriginalBiome().name()));
            }
            addon.getManager().removeGreenhouse(g);
        });
    }

}

