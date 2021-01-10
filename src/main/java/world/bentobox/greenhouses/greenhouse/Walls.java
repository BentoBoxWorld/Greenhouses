package world.bentobox.greenhouses.greenhouse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import world.bentobox.greenhouses.Greenhouses;

public class Walls extends MinMaxXZ {
    private static final List<Material> WALL_BLOCKS;
    static {
        List<Material> w = Arrays.stream(Material.values())
                .filter(Material::isBlock) // Blocks only, no items
                .filter(m -> !m.name().contains("TRAPDOOR")) // No trap doors
                .filter(m -> m.name().contains("DOOR") // All doors
                        || (m.name().contains("GLASS") && !m.name().contains("GLASS_PANE")) // All glass blocks
                        || m.equals(Material.HOPPER)) // Hoppers
                .collect(Collectors.toList());
        WALL_BLOCKS = Collections.unmodifiableList(w);
    }

    private int floor;

    private static final List<BlockFace> ORDINALS = Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    class WallFinder {
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
    }

    public Walls findWalls(Roof roof) {
        // The player is under the roof
        // Assume the player is inside the greenhouse they are trying to create
        Location loc = roof.getLocation();
        World world = loc.getWorld();
        floor = getFloorY(world, roof.getHeight(), roof.getMinX(), roof.getMaxX(), roof.getMinZ(), roof.getMaxZ());
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
        floor = getFloorY(world, roof.getHeight(), minX, maxX, minZ,maxZ);
        return this;
    }

    void lookAround(Location loc, WallFinder wf, Roof roof) {
        World world = loc.getWorld();
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
                        lookAtBlockFaces(wf, world, x, y, z);
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

    void lookAtBlockFaces(WallFinder wf, World world, int x, int y, int z) {
        for (BlockFace bf: ORDINALS) {
            switch (bf) {
            case EAST:
                // positive x
                if (WALL_BLOCKS.contains(world.getBlockAt(x, y, z).getRelative(bf).getType())) {
                    wf.stopMaxX = true;
                }
                break;
            case WEST:
                // negative x
                if (WALL_BLOCKS.contains(world.getBlockAt(x, y, z).getRelative(bf).getType())) {
                    wf.stopMinX = true;
                }
                break;
            case NORTH:
                // negative Z
                if (WALL_BLOCKS.contains(world.getBlockAt(x, y, z).getRelative(bf).getType())) {
                    wf.stopMinZ = true;
                }
                break;
            case SOUTH:
                // positive Z
                if (WALL_BLOCKS.contains(world.getBlockAt(x, y, z).getRelative(bf).getType())) {
                    wf.stopMaxZ = true;
                }
                break;
            default:
                break;
            }
        }

    }

    int getFloorY(World world, int y, int minX, int maxX, int minZ, int maxZ) {
        // Find the floor - defined as the last y under the roof where there are no wall blocks
        int wallBlockCount;
        do {
            wallBlockCount = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (WALL_BLOCKS.contains(world.getBlockAt(x, y, z).getType())) {
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
