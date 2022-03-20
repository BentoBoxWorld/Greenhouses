package world.bentobox.greenhouses.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Snow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import com.google.common.base.Enums;

import world.bentobox.bentobox.util.Util;
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

    /**
     * @param gh - greenhouse
     * @return true if snow was create, false if not.
     */
    private boolean getAirBlocks(Greenhouse gh) {
        if (gh.getLocation() == null) {
            // Greenhouse does not have a location for some reason.
            return false;
        }
        boolean createdSnow = false;
        List<Block> waterBlocks = new ArrayList<>();
        final BoundingBox bb = gh.getBoundingBox();
        for (int x = (int)bb.getMinX() + 1; x < (int)bb.getMaxX() -1; x++) {
            for (int z = (int)bb.getMinZ() + 1; z < (int)bb.getMaxZ() - 1; z++) {
                for (int y = (int)bb.getMaxY() - 2; y >= (int)bb.getMinY(); y--) {
                    Block b = Objects.requireNonNull(gh.getLocation().getWorld()).getBlockAt(x, y, z);
                    Material type = b.getType();
                    if (type.equals(Material.AIR) || type.equals(Material.SNOW)) {
                        b.getWorld().spawnParticle(Particle.SNOWBALL, b.getLocation(), 5);
                    } else {
                        // Add snow
                        if (type.equals(Material.WATER)) {
                            waterBlocks.add(b);
                        } else {
                            // Not water
                            if (Math.random() < addon.getSettings().getSnowDensity()
                                    && !b.isLiquid()
                                    && (b.getRelative(BlockFace.UP).getType().equals(Material.AIR)
                                            || b.getRelative(BlockFace.UP).getType().equals(Material.SNOW))) {


                                createdSnow = placeSnow(b);
                            }
                        }

                        break;
                    }
                }
            }
        }
        // Check if any water blocks can be turned to ice
        /*
         * TODO - find a way to calculate water blocks
        int maxSize = waterBlocks.size() - (gh.getArea() / gh.getBiomeRecipe().getWaterCoverage());
        if (maxSize > 0) {
            waterBlocks.stream().limit(maxSize).filter(b -> Math.random() < addon.getSettings().getSnowDensity()).forEach(b -> b.setType(Material.ICE));
        }
         */
        return createdSnow;
    }

    private boolean placeSnow(Block b) {
        Optional<Material> snowCauldron = Enums.getIfPresent(Material.class, "POWDER_SNOW_CAULDRON").toJavaUtil();
        if (snowCauldron.isPresent()) {
            if (b.getType().equals(Material.CAULDRON)) {
                b.setType(snowCauldron.get());
                return true;
            } else if (b.getType().equals(snowCauldron.get())) {
                // Fill up the snow cauldron some more
                return incrementLevel(b);
            }
        }
        // Pile up snow
        if (b.getRelative(BlockFace.UP).getType().equals(Material.SNOW)) {
            return incrementLevel(b.getRelative(BlockFace.UP));
        } else {
            b.getRelative(BlockFace.UP).setType(Material.SNOW);
        }
        return true;
    }

    private boolean incrementLevel(Block b) {
        if (b.getBlockData() instanceof Levelled data) {
            int max = data.getMaximumLevel();
            if (data.getLevel() < max) {
                data.setLevel(data.getLevel() + 1);
                b.setBlockData(data);
                return true;
            }
        }
        if (b.getBlockData() instanceof Snow data) {
            int max = data.getMaximumLayers();
            if (data.getLayers() < max) {
                data.setLayers(data.getLayers() + 1);
                b.setBlockData(data);
                return true;
            }
        }
        return false;
    }

    /**
     * TODO finish
     * @param e block form event
     */
    @EventHandler
    public void onBlockFormEvent(final BlockFormEvent e) {
        if (e.getNewState().getType().equals(Material.SNOW) && addon.getManager().getMap().isAboveGreenhouse(e.getBlock().getLocation())) {
            e.setCancelled(true);
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
        // Scatter snow
        if (getAirBlocks(g) && g.getRoofHopperLocation() != null) {
            Hopper h = ((Hopper)g.getRoofHopperLocation().getBlock().getState());
            h.getInventory().removeItem(new ItemStack(Material.WATER_BUCKET));
            h.getInventory().addItem(new ItemStack(Material.BUCKET));
        }
    }

    private void shakeGlobes(World world) {
        addon.getManager().getMap().getGreenhouses().stream().filter(g -> g.getBiomeRecipe().getIceCoverage() > 0)
        .filter(g -> (Objects.requireNonNull(Objects.requireNonNull(g.getLocation()).getWorld()).isChunkLoaded(((int) g.getBoundingBox().getMaxX()) >> 4, ((int) g.getBoundingBox().getMaxZ()) >> 4) && g.getLocation().getWorld().isChunkLoaded(((int) g.getBoundingBox().getMinX()) >> 4, ((int) g.getBoundingBox().getMinZ()) >> 4)))
        .filter(g -> g.getLocation().getWorld().equals(world))
        .filter(g -> !g.isBroken())
        .filter(g -> g.getRoofHopperLocation() != null)
        .forEach(g -> Util.getChunkAtAsync(g.getRoofHopperLocation()).thenRun(() -> {
            if (g.getRoofHopperLocation().getBlock().getType().equals(Material.HOPPER)
                    && ((Hopper)g.getRoofHopperLocation().getBlock().getState()).getInventory().contains(Material.WATER_BUCKET)) {
                removeWaterBucketAndShake(g);
            }
        }));
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
