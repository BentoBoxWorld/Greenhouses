package world.bentobox.greenhouses.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * Monitors the greenhouses and grows things, adds weather etc.
 * @author tastybento
 *
 */
public class SnowTracker implements Listener {
    private final Greenhouses addon;
    private final Map<World, BukkitTask> snowTasks;

    public SnowTracker(Greenhouses addon) {
        this.addon = addon;
        snowTasks = new HashMap<>();
        // Start snow if ongoing
        addon.getActiveWorlds().stream().filter(World::isThundering)
        .forEach(w -> snowTasks.putIfAbsent(w, Bukkit.getScheduler().runTaskTimer(addon.getPlugin(), () -> shakeGlobes(w), 0L, 100L)));

    }

    private void getAirBlocks(Greenhouse gh) {
        List<Block> waterBlocks = new ArrayList<>();
        for (int x = (int)gh.getBoundingBox().getMinX() + 1; x < (int)gh.getBoundingBox().getMaxX(); x++) {
            for (int z = (int)gh.getBoundingBox().getMinY() + 1; z < (int)gh.getBoundingBox().getMaxY(); z++) {
                for (int y = gh.getCeilingHeight() - 1; y >= gh.getFloorHeight(); y--) {
                    Block b = gh.getLocation().getWorld().getBlockAt(x, y, z);
                    if (b.getType().equals(Material.AIR) || b.getType().equals(Material.SNOW)) {
                        b.getWorld().spawnParticle(Particle.SNOWBALL, b.getLocation(), 5);
                    } else {
                        // Add snow
                        if (b.getType().equals(Material.WATER)) {
                            waterBlocks.add(b);
                        } else {
                            // Not water
                            if (Math.random() < addon.getSettings().getSnowDensity() && !b.isLiquid()) {
                                b.getRelative(BlockFace.UP).setType(Material.SNOW);
                            }
                        }

                        break;
                    }
                }
            }
        }
        // Check if any water blocks can be turned to ice
        int maxSize = waterBlocks.size() - (gh.getArea() / gh.getBiomeRecipe().getWaterCoverage());
        if (maxSize > 0) {
            waterBlocks.stream().limit(maxSize).filter(b -> Math.random() < addon.getSettings().getSnowDensity()).forEach(b -> b.setType(Material.ICE));
        }
    }

    @EventHandler
    public void onWeatherChangeEvent(final WeatherChangeEvent e) {
        if (!addon.getActiveWorlds().contains(e.getWorld())) {
            return;
        }
        if (e.toWeatherState()) {
            // It's raining
            startSnow(e.getWorld());
        } else {
            // It's stopped raining!
            stopSnow(e.getWorld());
        }
    }

    private void removeWaterBucketAndShake(Greenhouse g) {
        Hopper h = ((Hopper)g.getRoofHopperLocation().getBlock().getState());
        h.getInventory().removeItem(new ItemStack(Material.WATER_BUCKET));
        h.getInventory().addItem(new ItemStack(Material.BUCKET));
        // Scatter snow
        getAirBlocks(g);
    }

    private void shakeGlobes(World world) {
        addon.getManager().getMap().getGreenhouses().stream().filter(g -> g.getBiomeRecipe().getIceCoverage() > 0)
        .filter(g -> g.getLocation().getWorld().equals(world))
        .filter(g -> !g.isBroken())
        .filter(g -> g.getRoofHopperLocation() != null)
        .filter(g -> g.getRoofHopperLocation().getBlock().getType().equals(Material.HOPPER))
        .filter(g -> ((Hopper)g.getRoofHopperLocation().getBlock().getState()).getInventory().contains(Material.WATER_BUCKET))
        .forEach(this::removeWaterBucketAndShake);
    }

    private void startSnow(World world) {
        // Start timer
        snowTasks.putIfAbsent(world, Bukkit.getScheduler().runTaskTimer(addon.getPlugin(), () -> shakeGlobes(world), 0L, 100L)); // every 5 seconds
    }

    private void stopSnow(World world) {
        if (snowTasks.containsKey(world)) {
            snowTasks.get(world).cancel();
            snowTasks.remove(world);
        }
    }
}