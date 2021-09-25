package world.bentobox.greenhouses.greenhouse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.world.AsyncWorldCache;

@SuppressWarnings("deprecation")
public class Walls extends MinMaxXZ {
    private static final List<Material> WALL_BLOCKS;
    static {
        // Hoppers
        WALL_BLOCKS = Arrays.stream(Material.values())
                .filter(Material::isBlock) // Blocks only, no items
                .filter(m -> !m.isLegacy())
                .filter(m -> !m.name().contains("TRAPDOOR")) // No trap doors
                .filter(m -> m.name().contains("DOOR") // All doors
                        || (m.name().contains("GLASS") && !m.name().contains("GLASS_PANE")) // All glass blocks
                        || m.equals(Material.HOPPER)).toList();
    }

    private int floor;

    private final AsyncWorldCache cache;

    static class WallFinder {
        int radiusMinX;
        int radiusMaxX;
        int radiusMinZ;
        int radiusMaxZ;
        boolean stopMinX;
        boolean stopMaxX;
        boolean stopMinZ;
        boolean stopMaxZ;
        boolean isSearching() {
            return !stopMinX || !stopMaxX || !stopMinZ || !stopMaxZ;
        }
        @Override
        public String toString() {
            return "WallFinder [radiusMinX=" + radiusMinX + ", radiusMaxX=" + radiusMaxX + ", radiusMinZ=" + radiusMinZ
                    + ", radiusMaxZ=" + radiusMaxZ + ", stopMinX=" + stopMinX + ", stopMaxX=" + stopMaxX + ", stopMinZ="
                    + stopMinZ + ", stopMaxZ=" + stopMaxZ + "]";
        }
    }

    public Walls(AsyncWorldCache cache) {
        this.cache = cache;
    }

    /**
     * Find walls given a roof
     * @param roof - the roof
     * @return Future walls
     */
    public CompletableFuture<Walls> findWalls(final Roof roof) {
        CompletableFuture<Walls> r = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(BentoBox.getInstance(), () -> findWalls(r, roof));
        return r;
    }

    Walls findWalls(CompletableFuture<Walls> r, Roof roof) {
        // The player is under the roof
        // Assume the player is inside the greenhouse they are trying to create
        final Location loc = roof.getLocation();
        floor = getFloorY(roof.getHeight(), roof.getMinX(), roof.getMaxX(), roof.getMinZ(), roof.getMaxZ());
        // Now start with the player's x and z location
        WallFinder wf = new WallFinder();
        minX = loc.getBlockX();
        maxX = loc.getBlockX();
        minZ = loc.getBlockZ();
        maxZ = loc.getBlockZ();
        do {
            lookAround(loc, wf, roof);
        } while (wf.isSearching());
        // We should have the largest cube we can make now
        minX--;
        maxX++;
        minZ--;
        maxZ++;
        // Find the floor again, only looking within the walls
        floor = getFloorY(roof.getHeight(), minX, maxX, minZ,maxZ);
        // Complete on main thread
        Bukkit.getScheduler().runTask(BentoBox.getInstance(), () -> r.complete(this));
        return this;

    }

    void lookAround(final Location loc, WallFinder wf, final Roof roof) {
        // Look around player in an ever expanding cube
        minX = loc.getBlockX() - wf.radiusMinX;
        maxX = loc.getBlockX() + wf.radiusMaxX;
        minZ = loc.getBlockZ() - wf.radiusMinZ;
        maxZ = loc.getBlockZ() + wf.radiusMaxZ;
        for (int y = roof.getHeight() - 1; y > floor; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Only look around outside edge
                    if (!((x > minX && x < maxX) && (z > minZ && z < maxZ))) {
                        // Look at block faces
                        lookAtBlockFaces(wf, x, y, z);
                    }
                }
            }
        }
        analyzeFindings(wf, roof);
    }

    void analyzeFindings(WallFinder wf, Roof roof) {
        if (minX < roof.getMinX()) {
            wf.stopMinX = true;
        }
        if (maxX > roof.getMaxX()) {
            wf.stopMaxX = true;
        }
        if (minZ < roof.getMinZ()) {
            wf.stopMinZ = true;
        }
        if (maxZ > roof.getMaxZ()) {
            wf.stopMaxZ = true;
        }
        // Expand the edges
        if (!wf.stopMinX) {
            wf.radiusMinX++;
        }
        if (!wf.stopMaxX) {
            wf.radiusMaxX++;
        }
        if (!wf.stopMinZ) {
            wf.radiusMinZ++;
        }
        if (!wf.stopMaxZ) {
            wf.radiusMaxZ++;
        }
    }

    void lookAtBlockFaces(WallFinder wf, int x, int y, int z) {
        // positive x
        if (WALL_BLOCKS.contains(cache.getBlockType(x + 1, y, z))) {
            wf.stopMaxX = true;
        }
        // negative x
        if (WALL_BLOCKS.contains(cache.getBlockType(x - 1, y, z))) {
            wf.stopMinX = true;
        }
        // negative Z
        if (WALL_BLOCKS.contains(cache.getBlockType(x, y, z - 1))) {
            wf.stopMinZ = true;
        }
        // positive Z
        if (WALL_BLOCKS.contains(cache.getBlockType(x, y, z + 1))) {
            wf.stopMaxZ = true;
        }
    }

    int getFloorY(int y, int minX, int maxX, int minZ, int maxZ) {
        // Find the floor - defined as the last y under the roof where there are no wall blocks
        int wallBlockCount;
        do {
            wallBlockCount = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (WALL_BLOCKS.contains(cache.getBlockType(x, y, z))) {
                        wallBlockCount++;
                    }
                }
            }

        } while( y-- > 0 && wallBlockCount > 0);
        return y + 1;

    }

    /**
     * Check if material is a wall material
     * @param m - material
     * @return true if wall material
     */
    public static boolean wallBlocks(Material m) {
        return WALL_BLOCKS.contains(m)
                || (m.equals(Material.GLOWSTONE) && Greenhouses.getInstance().getSettings().isAllowGlowstone())
                || (m.name().endsWith("GLASS_PANE") && Greenhouses.getInstance().getSettings().isAllowPanes());
    }

    /**
     * @return the floor
     */
    public int getFloor() {
        return floor;
    }

    /**
     * @return width of the space
     */
    public int getWidth() {
        return Math.abs(maxX - minX);
    }

    /**
     * @return length of the space
     */
    public int getLength() {
        return Math.abs(maxZ - minZ);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Walls [minX=" + minX + ", maxX=" + maxX + ", minZ=" + minZ + ", maxZ=" + maxZ + ", floor=" + floor
                + "]";
    }


}
