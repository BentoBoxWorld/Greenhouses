package world.bentobox.greenhouses.greenhouse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

public class Walls extends MinMaxXZ {
    public static final List<Material> WALL_BLOCKS;
    static {
        List<Material> w = Arrays.stream(Material.values())
                .filter(Material::isBlock) // Blocks only, no items
                .filter(m -> !m.name().contains("TRAPDOOR")) // No trap doors
                .filter(m -> m.name().contains("DOOR") // All doors
                        || m.name().contains("GLASS") // All glass blocks
                        || m.equals(Material.HOPPER) // Hoppers
                        || m.equals(Material.GLOWSTONE)) // Glowstone
                .collect(Collectors.toList());
        WALL_BLOCKS = Collections.unmodifiableList(w);
    }

    private int floor;

    private static final List<BlockFace> ORDINALS = Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    public Walls(Roof roof) {
        // The player is under the roof
        // Assume the player is inside the greenhouse they are trying to create
        Location loc = roof.getLocation();
        World world = roof.getLocation().getWorld();
        floor = getFloorY(world, roof.getHeight(), roof.getMinX(), roof.getMaxX(), roof.getMinZ(), roof.getMaxZ());
        // Now start with the player's x and z location
        int radiusMinX = 0;
        int radiusMaxX = 0;
        int radiusMinZ = 0;
        int radiusMaxZ = 0;
        boolean stopMinX = false;
        boolean stopMaxX = false;
        boolean stopMinZ = false;
        boolean stopMaxZ = false;
        minX = loc.getBlockX();
        maxX = loc.getBlockX();
        minZ = loc.getBlockZ();
        maxZ = loc.getBlockZ();
        do {
            // Look around player in an ever expanding cube
            minX = loc.getBlockX() - radiusMinX;
            maxX = loc.getBlockX() + radiusMaxX;
            minZ = loc.getBlockZ() - radiusMinZ;
            maxZ = loc.getBlockZ() + radiusMaxZ;
            int y;
            for (y = roof.getHeight() - 1; y > floor; y--) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        // Only look around outside edge
                        if (!((x > minX && x < maxX) && (z > minZ && z < maxZ))) {
                            // Look at block faces
                            for (BlockFace bf: ORDINALS) {
                                switch (bf) {
                                case EAST:
                                    // positive x
                                    if (WALL_BLOCKS.contains(world.getBlockAt(x, y, z).getRelative(bf).getType())) {
                                        stopMaxX = true;
                                    }
                                    break;
                                case WEST:
                                    // negative x
                                    if (WALL_BLOCKS.contains(world.getBlockAt(x, y, z).getRelative(bf).getType())) {
                                        stopMinX = true;
                                    }
                                    break;
                                case NORTH:
                                    // negative Z
                                    if (WALL_BLOCKS.contains(world.getBlockAt(x, y, z).getRelative(bf).getType())) {
                                        stopMinZ = true;
                                    }
                                    break;
                                case SOUTH:
                                    // positive Z
                                    if (WALL_BLOCKS.contains(world.getBlockAt(x, y, z).getRelative(bf).getType())) {
                                        stopMaxZ = true;
                                    }
                                    break;
                                default:
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (minX < roof.getMinX()) {
                stopMinX = true;
            }
            if (maxX > roof.getMaxX()) {
                stopMaxX = true;
            }
            if (minZ < roof.getMinZ()) {
                stopMinZ = true;
            }
            if (maxZ > roof.getMaxZ()) {
                stopMaxZ = true;
            }
            // Expand the edges
            if (!stopMinX) {
                radiusMinX++;
            }
            if (!stopMaxX) {
                radiusMaxX++;
            }
            if (!stopMinZ) {
                radiusMinZ++;
            }
            if (!stopMaxZ) {
                radiusMaxZ++;
            }
        } while (!stopMinX || !stopMaxX || !stopMinZ || !stopMaxZ);
        // We should have the largest cube we can make now
        minX--;
        maxX++;
        minZ--;
        maxZ++;
        // Find the floor again, only looking within the walls
        floor = getFloorY(world, roof.getHeight(), minX, maxX, minZ,maxZ);
    }

    private int getFloorY(World world, int y, int minX, int maxX, int minZ, int maxZ) {
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
