package world.bentobox.greenhouses.greenhouse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import world.bentobox.greenhouses.Greenhouses;

/**
 * Contains the parameters of a greenhouse roof
 * @author tastybento
 *
 */
public class Roof extends MinMaxXZ {
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
    private final Location location;
    private int height;
    private boolean roofFound;

    /**
     * Finds a roof from a starting location under the roof and characterizes it
     * @param loc - starting location
     */
    public Roof(Location loc) {
        this.location = loc;
        roofFound = findRoof(loc);
    }


    private boolean findRoof(Location loc) {
        World world = loc.getWorld();
        // This section tries to find a roof block
        // Try just going up - this covers every case except if the player is standing under a hole
        roofFound = false;

        // This does a ever-growing check around the player to find a wall block. It is possible for the player
        // to be outside the greenhouse in this situation, so a check is done later to make sure the player is inside
        int roofY = loc.getBlockY();
        for (int y = roofY; y < world.getMaxHeight(); y++) {
            if (roofBlocks(world.getBlockAt(loc.getBlockX(),y,loc.getBlockZ()).getType())) {
                roofFound = true;
                loc = new Location(world,loc.getBlockX(),y,loc.getBlockZ());
                break;
            }
        }
        // If the roof was not found start going around in circles until something is found
        // Expand in ever increasing squares around location until a wall block is found
        spiralSearch(loc, roofY);
        if (!roofFound) return false;
        // Record the height
        this.height = loc.getBlockY();
        // Now we have a roof block, find how far we can go NSWE
        minX = loc.getBlockX();
        maxX = loc.getBlockX();
        minZ = loc.getBlockZ();
        maxZ = loc.getBlockZ();
        expandCoords(world, loc.toVector());
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
                    expandCoords(world, new Vector(x, loc.getBlockY(), z));
                }
            }
            // Repeat until nothing changes
        } while (minx != minX || maxx != maxX || minz != minZ || maxz != maxZ);
        // That's as much as we can do!
        return true;
    }

    private void spiralSearch(Location loc, int roofY) {
        for (int radius = 0; radius < 3 && !roofFound; radius++) {
            for (int x = loc.getBlockX() - radius; x <= loc.getBlockX() + radius && !roofFound; x++) {
                for (int z = loc.getBlockZ() - radius; z <= loc.getBlockZ() + radius && !roofFound; z++) {
                    if (!((x > loc.getBlockX() - radius && x < loc.getBlockX() + radius) && (z > loc.getBlockZ() - radius && z < loc.getBlockZ() + radius))) {
                        checkVertically(loc, x, roofY, z);
                    }
                }
            }
        }

    }

    private void checkVertically(Location loc, int x, int roofY, int z) {
        World world = loc.getWorld();
        Block b = world.getBlockAt(x, roofY, z);
        if (!Walls.wallBlocks(b.getType())) {
            // Look up
            for (int y = roofY; y < world.getMaxHeight() && !roofFound; y++) {
                if (roofBlocks(world.getBlockAt(x,y,z).getType())) {
                    roofFound = true;
                    loc = new Location(world,x,y,z);
                }
            }
        }

    }

    /**
     * This takes any location and tries to go as far as possible in NWSE directions finding contiguous roof blocks
     * up to 100 in any direction
     * @param height - location to start search
     */
    private void expandCoords(World world, Vector height) {
        Location maxx = height.toLocation(world);
        Location minx = height.toLocation(world);
        Location maxz = height.toLocation(world);
        Location minz = height.toLocation(world);
        int limit = 0;
        while (ROOF_BLOCKS
                .contains(world.getBlockAt(maxx).getType()) && limit < 100) {
            limit++;
            maxx.add(new Vector(1,0,0));
        }
        if (maxx.getBlockX()-1 > maxX) {
            maxX = maxx.getBlockX()-1;
        }

        while (roofBlocks(world.getBlockAt(minx).getType()) && limit < 200) {
            limit++;
            minx.subtract(new Vector(1,0,0));
        }
        if (minx.getBlockX() + 1 < minX) {
            minX = minx.getBlockX() + 1;
        }

        while (roofBlocks(world.getBlockAt(maxz).getType()) && limit < 300) {
            limit++;
            maxz.add(new Vector(0,0,1));
        }
        if (maxz.getBlockZ() - 1 > maxZ) {
            maxZ = maxz.getBlockZ() - 1;
        }

        while (roofBlocks(world.getBlockAt(minz).getType()) && limit < 400) {
            limit++;
            minz.subtract(new Vector(0,0,1));
        }
        if (minz.getBlockZ() + 1 < minZ) {
            minZ = minz.getBlockZ() + 1;
        }
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

    /**
     * @return the roofFound
     */
    public boolean isRoofFound() {
        return roofFound;
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


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Roof [location=" + location + ", minX=" + minX + ", maxX=" + maxX + ", minZ=" + minZ + ", maxZ=" + maxZ
                + ", height=" + height + ", roofFound=" + roofFound + "]";
    }
}
