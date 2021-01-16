package world.bentobox.greenhouses.greenhouse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.world.AsyncWorldCache;

/**
 * Contains the parameters of a greenhouse roof
 * @author tastybento
 *
 */
public class Roof extends MinMaxXZ {
    private static final BentoBox PLUGIN = Greenhouses.getInstance().getPlugin();
    private static final List<Material> ROOF_BLOCKS;
    static {
        List<Material> r = Arrays.stream(Material.values())
                .filter(Material::isBlock) // Blocks only, no items
                .filter(m -> m.name().contains("TRAPDOOR") // All trapdoors
                        || (m.name().contains("GLASS") && !m.name().contains("GLASS_PANE")) // All glass blocks
                        || m.equals(Material.HOPPER)) // Hoppers
                .collect(Collectors.toList());
        ROOF_BLOCKS = Collections.unmodifiableList(r);
    }
    /**
     * Check if material is a roof material
     * @param m - material
     * @return true if roof material
     */
    public static boolean roofBlocks(Material m) {
        return ROOF_BLOCKS.contains(m)
                || (m.equals(Material.GLOWSTONE) && Greenhouses.getInstance().getSettings().isAllowGlowstone())
                || (m.name().endsWith("GLASS_PANE") && Greenhouses.getInstance().getSettings().isAllowPanes());
    }
    private final AsyncWorldCache cache;
    private int height;
    private final Location location;
    private boolean roofFound;

    private final World world;


    /**
     * Finds a roof from a starting location under the roof and characterizes it
     * @param cache
     * @param loc - starting location
     */
    public Roof(AsyncWorldCache cache, Location loc) {
        this.cache = cache;
        this.location = loc;
        this.world = loc.getWorld();
    }



    /**
     * This takes any location and tries to go as far as possible in NWSE directions finding contiguous roof blocks
     * up to 100 in any direction
     * @param vector - vector to start search
     */
    private void expandCoords(Vector vector) {
        Vector maxx = vector.clone();
        Vector minx = vector.clone();
        Vector maxz = vector.clone();
        Vector minz = vector.clone();
        int limit = 0;
        while (roofBlocks(cache.getBlockType(maxx)) && limit < 100) {
            limit++;
            maxx.add(new Vector(1,0,0));
        }
        // Set Max x
        if (maxx.getBlockX() - 1 > maxX) {
            maxX = maxx.getBlockX() - 1;
        }
        limit = 0;
        while (roofBlocks(cache.getBlockType(minx)) && limit < 100) {
            limit++;
            minx.subtract(new Vector(1,0,0));
        }
        if (minx.getBlockX() + 1 < minX) {
            minX = minx.getBlockX() + 1;
        }
        limit = 0;
        while (roofBlocks(cache.getBlockType(maxz)) && limit < 100) {
            limit++;
            maxz.add(new Vector(0,0,1));
        }
        if (maxz.getBlockZ() - 1 > maxZ) {
            maxZ = maxz.getBlockZ() - 1;
        }
        limit = 0;
        while (roofBlocks(cache.getBlockType(minz)) && limit < 100) {
            limit++;
            minz.subtract(new Vector(0,0,1));
        }
        if (minz.getBlockZ() + 1 < minZ) {
            minZ = minz.getBlockZ() + 1;
        }
    }

    public CompletableFuture<Boolean> findRoof() {
        CompletableFuture<Boolean> r = new CompletableFuture<>();
        Vector loc = location.toVector();
        // This section tries to find a roof block
        // Try just going up - this covers every case except if the player is standing under a hole
        Bukkit.getScheduler().runTaskAsynchronously(PLUGIN, () -> {
            boolean found = findRoof(loc);
            Bukkit.getScheduler().runTask(PLUGIN, () -> r.complete(found));
        });
        return r;
    }

    private boolean findRoof(Vector loc) {
        // This does a ever-growing check around the player to find a wall block. It is possible for the player
        // to be outside the greenhouse in this situation, so a check is done later to make sure the player is inside
        int roofY = loc.getBlockY();
        for (int y = roofY; y < world.getMaxHeight(); y++) {
            if (roofBlocks(cache.getBlockType(loc.getBlockX(),y,loc.getBlockZ()))) {
                roofFound = true;
                loc = new Vector(loc.getBlockX(),y,loc.getBlockZ());
                break;
            }
        }
        // If the roof was not found start going around in circles until something is found
        // Expand in ever increasing squares around location until a wall block is found
        spiralSearch(loc, roofY);
        if (!roofFound) {
            return false;
        }
        // Record the height
        this.height = loc.getBlockY();
        // Now we have a roof block, find how far we can go NSWE
        minX = loc.getBlockX();
        maxX = loc.getBlockX();
        minZ = loc.getBlockZ();
        maxZ = loc.getBlockZ();
        expandCoords(loc);
        int minx;
        int maxx;
        int minz;
        int maxz;
        // Now we have some idea of the mins and maxes, check each block and see if it goes further
        do {
            minx = minX;
            maxx = maxX;
            minz = minZ;
            maxz = maxZ;
            for (int x = minx; x <= maxx; x++) {
                for (int z = minz; z <= maxz; z++) {
                    // This will push out the coords if possible
                    expandCoords(new Vector(x, loc.getBlockY(), z));
                }
            }
            // Repeat until nothing changes
        } while (minx != minX || maxx != maxX || minz != minZ || maxz != maxZ);
        // That's as much as we can do!
        return true;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }


    /**
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

    private void spiralSearch(Vector v, int roofY) {
        for (int radius = 0; radius < 3 && !roofFound; radius++) {
            for (int x = v.getBlockX() - radius; x <= v.getBlockX() + radius && !roofFound; x++) {
                for (int z = v.getBlockZ() - radius; z <= v.getBlockZ() + radius && !roofFound; z++) {
                    if (!((x > v.getBlockX() - radius && x < v.getBlockX() + radius) && (z > v.getBlockZ() - radius && z < v.getBlockZ() + radius))) {
                        checkVertically(v, x, roofY, z);
                    }
                }
            }
        }

    }

    /**
     * Get highest roof block
     * @param v - vector of search block
     * @param x - x coord of current search
     * @param roofY - roof y coord
     * @param z - z coord of current search
     */
    private void checkVertically(Vector v, final int x, final int roofY, final int z) {
        if (!Walls.wallBlocks(cache.getBlockType(x, roofY, z))) {
            // Look up
            for (int y = roofY; y < world.getMaxHeight() && !roofFound; y++) {
                if (roofBlocks(cache.getBlockType(x,y,z))) {
                    roofFound = true;
                    // Move roof up because there is a higher block
                    v = new Vector(x,y,z);
                }
            }
        }

    }


    @Override
    public String toString() {
        return "Roof [" + (cache != null ? "cache=" + cache + ", " : "") + "height=" + height + ", "
                + (location != null ? "location=" + location + ", " : "") + "roofFound=" + roofFound + ", "
                + (world != null ? "world=" + world + ", " : "") + "minX=" + minX + ", maxX=" + maxX + ", minZ=" + minZ
                + ", maxZ=" + maxZ + "]";
    }

}
