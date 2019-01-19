package world.bentobox.greenhouses.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.Settings;
import world.bentobox.greenhouses.greenhouse.Greenhouse;
import world.bentobox.greenhouses.ui.Locale;
import world.bentobox.greenhouses.util.Util;

/**
 * @author tastybento
 * This class listens for changes to greenhouses and reacts to them
 */
public class GreenhouseEvents implements Listener {
    private final Greenhouses plugin;
    private List<Location> blockedPistons = new ArrayList<Location>();

    public GreenhouseEvents(final Greenhouses plugin) {
        this.plugin = plugin;

    }

    /**
     * Permits water to be placed in the Nether if in a greenhouse and in an acceptable biome
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        World world = player.getWorld();
        // Check we are in the right world
        if (!world.getEnvironment().equals(Environment.NETHER) || !Settings.worldName.contains(world.getName())) {
            return;
        }
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Biome biome = event.getClickedBlock().getBiome();
            if (event.getItem() != null && biome != null) {
                // Allow pouring of water if biome is okay
                if (event.getItem().getType().equals(Material.WATER_BUCKET) && !biome.equals(Biome.NETHER)
                        && !biome.equals(Biome.DESERT) && !biome.equals(Biome.DESERT_HILLS)) {
                    event.setCancelled(true);
                    event.getClickedBlock().getRelative(event.getBlockFace()).setType(Material.WATER);
                }
            }
        }
    }

    /**
     * Makes water in the Nether if ice is broken and in a greenhouse
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
    public void onIceBreak(BlockBreakEvent event){
        Player player = event.getPlayer();
        World world = player.getWorld();
        // Check we are in the right world
        if (!Settings.worldName.contains(world.getName())) {
            return;
        }
        Biome biome = event.getBlock().getBiome();
        // Set to water if the biome is okay.
        if(event.getBlock().getWorld().getEnvironment() == Environment.NETHER && event.getBlock().getType() == Material.ICE
                && !biome.equals(Biome.NETHER) && !biome.equals(Biome.DESERT) && !biome.equals(Biome.DESERT_HILLS)) {
            event.setCancelled(true);
            event.getBlock().setType(Material.WATER);
        }
    }

    /**
     * Tracks player movement
     * @param event
     */
    /*
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        //plugin.logger(3,event.getEventName());
        Player player = event.getPlayer();
    	UUID uuid = player.getUniqueId();
    	if (plugin.getPlayerGHouse(uuid) == null || plugin.getPlayerGHouse(uuid).isEmpty()) {
    		return;
    	}

        World world = player.getWorld();
        // Check we are in the right world
        if (!Settings.worldName.contains(world.getName())) {
            plugin.logger(4,"Not in a Greenhouse world");
            return;
        }
        // Did we move a block?
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            boolean result = checkMove(player, event.getFrom(), event.getTo(), uuid);
            if (result) {
                Location newLoc = event.getFrom();
                newLoc.setX(newLoc.getBlockX() + 0.5);
                newLoc.setY(newLoc.getBlockY());
                newLoc.setZ(newLoc.getBlockZ() + 0.5);
                event.setTo(newLoc);
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Strangely, sometimes these worlds can be null
        if (event.getFrom() == null || event.getTo() == null) {
            return;
        }
        // Check if they changed worlds
    	UUID uuid = event.getPlayer().getUniqueId();
    	if (plugin.getPlayerGHouse(uuid) == null || plugin.getPlayerGHouse(uuid).isEmpty()) {
    		return;
    	}

        World fromWorld = event.getFrom().getWorld();
        World toWorld = event.getTo().getWorld();
        // Check we are in the right world
        if (!Settings.worldName.contains(fromWorld.getName()) && !Settings.worldName.contains(toWorld.getName())) {
            return;
        }
        // Did we move a block?
        checkMove(event.getPlayer(), event.getFrom(), event.getTo(), uuid);
    }
     */
    /**
     * @param player
     * @param from
     * @param to
     * @return false if the player can move into that area, true if not allowed
     */
    /*
    private boolean checkMove(Player player, Location from, Location to, UUID uuid) {
        Greenhouse fromGreenhouse = null;
        Greenhouse toGreenhouse= null;
        if (plugin.getGreenhouses().isEmpty()) {
            // No greenhouses yet
            return false;
        }
        plugin.logger(4,"Checking greenhouses");
        plugin.logger(4,"From : " + from.toString());
        plugin.logger(4,"From: " + from.getBlockX() + "," + from.getBlockZ());
        plugin.logger(4,"To: " + to.getBlockX() + "," + to.getBlockZ());
    	for (Greenhouse d: plugin.getPlayerGHouse(uuid)) {
            plugin.logger(4,"Greenhouse (" + d.getPos1().getBlockX() + "," + d.getPos1().getBlockZ() + " : " + d.getPos2().getBlockX() + "," + d.getPos2().getBlockZ() + ")");
            if (d.insideGreenhouse(to)) {
                plugin.logger(4,"To intersects d!");
                toGreenhouse = d;
            }
            if (d.insideGreenhouse(from)) {
                plugin.logger(4,"From intersects d!");
                fromGreenhouse = d;
            }


        }
        // No greenhouse interaction
        if (fromGreenhouse == null && toGreenhouse == null) {
            // Clear the greenhouse flag (the greenhouse may have been deleted while they were offline)
            plugin.players.setInGreenhouse(player, null);
            return false;
        } else if (fromGreenhouse == toGreenhouse) {
            // Set the greenhouse - needs to be done if the player teleports too (should be done on a teleport event)
            plugin.players.setInGreenhouse(player, toGreenhouse);
            return false;
        }
        if (fromGreenhouse != null && toGreenhouse == null) {
            // leaving a greenhouse
            if (!fromGreenhouse.getFarewellMessage().isEmpty()) {
                player.sendMessage(fromGreenhouse.getFarewellMessage());
            }
            plugin.players.setInGreenhouse(player, null);
            //if (plugin.players.getNumberInGreenhouse(fromGreenhouse) == 0) {
            //	fromGreenhouse.ƒ();
            //}
        } else if (fromGreenhouse == null && toGreenhouse != null){
            // Going into a greenhouse
            if (!toGreenhouse.getEnterMessage().isEmpty()) {
                player.sendMessage(toGreenhouse.getEnterMessage());

                //plugin.visualize(toGreenhouse, player);
            }
            toGreenhouse.startBiome(false);
            plugin.players.setInGreenhouse(player, toGreenhouse);

        } else if (fromGreenhouse != null && toGreenhouse != null){
            // Leaving one greenhouse and entering another greenhouse immediately
            if (!fromGreenhouse.getFarewellMessage().isEmpty()) {
                player.sendMessage(fromGreenhouse.getFarewellMessage());
            }
            plugin.players.setInGreenhouse(player, toGreenhouse);
            //if (plugin.players.getNumberInGreenhouse(fromGreenhouse) == 0) {
            //fromGreenhouse.endBiome();
            //}
            toGreenhouse.startBiome(false);
            if (!toGreenhouse.getEnterMessage().isEmpty()) {
                player.sendMessage(toGreenhouse.getEnterMessage());
            }
        }
        return false;
    }
     */

    /**
     * Checks is broken blocks cause the greenhouse to fail
     * @param e
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockBreak(final BlockBreakEvent e) {
        if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
            return;
        }
        plugin.logger(3,"block break");
        // Get the greenhouse that this block is in (if any)
        Greenhouse g = plugin.getInGreenhouse(e.getBlock().getLocation());

        if (g == null) {
            // Not in a greenhouse
            plugin.logger(3,"not in greenhouse");
            return;
        }
        // Check to see if this causes the greenhouse to break
        if ((e.getBlock().getLocation().getBlockY() == g.getHeightY()) || (g.isAWall(e.getBlock().getLocation()))) {
            e.getPlayer().sendMessage(ChatColor.RED + Locale.eventbroke.replace("[biome]",Util.prettifyText(g.getOriginalBiome().toString())));
            e.getPlayer().sendMessage(ChatColor.RED + Locale.eventfix);
            plugin.removeGreenhouse(g);
            return;
        }
    }

    /**
     * Prevents placing of blocks above the greenhouses
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
        if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
            return;
        }
        if (e.getPlayer().getWorld().getEnvironment().equals(Environment.NETHER)) {
            return;
        }
        // If the offending block is not above a greenhouse, forget it!
        Greenhouse g = plugin.aboveAGreenhouse(e.getBlock().getLocation());
        if (g == null) {
            return;
        }
        e.getPlayer().sendMessage(ChatColor.RED + Locale.eventcannotplace);
        e.getPlayer().sendMessage("Greenhouse is at " + g.getPos1() + " to " + g.getPos2());
        e.setCancelled(true);
    }

    /**
     * Check to see if anyone is sneaking a block over a greenhouse by using a piston
     * @param e
     */
    @EventHandler
    public void onPistonPush(final BlockPistonExtendEvent e) {
        if (!Settings.worldName.contains(e.getBlock().getWorld().getName())) {
            return;
        }
        if (e.getBlock().getWorld().getEnvironment().equals(Environment.NETHER)) {
            return;
        }
        // Check if piston is already extended to avoid the double event effect

        Location l = e.getBlock().getLocation();
        if (blockedPistons.contains(l)) {
            // Cancel the double call
            blockedPistons.remove(l);
            e.setCancelled(true);
            return;
        }
        plugin.logger(3,"Direction: " + e.getDirection());
        plugin.logger(3,"Location of piston block:" + l);
        // Pistons can push up to 12 blocks - find the end block + 1
        for (int i = 0; i < 13; i++) {
            l = l.getBlock().getRelative(e.getDirection()).getLocation();
            if (!l.getBlock().getType().isSolid()) {
                break;
            }
        }
        plugin.logger(3,"Location of end block + 1:" + l);
        // The end block location is now l
        if (plugin.aboveAGreenhouse(l)  == null) {
            return;
        }
        // Find out who is around the piston
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (Settings.worldName.contains(p.getLocation().getWorld().getName())) {
                if (p.getLocation().distanceSquared(e.getBlock().getLocation()) <= 25) {
                    p.sendMessage(ChatColor.RED + Locale.eventpistonerror);
                    e.setCancelled(true);
                    blockedPistons.add(e.getBlock().getLocation());
                }
            }
        }
    }
}

