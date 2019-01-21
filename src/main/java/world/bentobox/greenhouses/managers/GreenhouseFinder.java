package world.bentobox.greenhouses.managers;

import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;

import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.Roof;
import world.bentobox.greenhouses.greenhouse.Walls;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

public class GreenhouseFinder {

    private Greenhouse gh;
    private Set<Location> redGlass = new HashSet<Location>();


    public Set<GreenhouseResult> find(Location location) {
        Set<GreenhouseResult> result = new HashSet<>();
        redGlass.clear();

        // Find the roof
        Roof roof = new Roof(location);
        if (!roof.isRoofFound()) {
            result.add(GreenhouseResult.FAIL_NO_ROOF);
            return result;
        }
        // Find the walls
        Walls walls = new Walls(roof);

        // Make the initial greenhouse
        gh = new Greenhouse(location.getWorld(), new Rectangle(walls.getMinX(), walls.getMinZ(), walls.getMaxX(), walls.getMaxZ()), walls.getFloor(), roof.getHeight());
        // Set the original biome
        gh.setOriginalBiome(location.getBlock().getBiome());

        // Now check again to see if the floor really is the floor and the walls follow the rules
        World world = roof.getLocation().getWorld();
        int minX = walls.getMinX();
        int minZ = walls.getMinZ();
        int maxX = walls.getMaxX();
        int maxZ = walls.getMaxZ();

        // Counts
        int wallDoors = 0;
        // Hoppers
        int ghHopper = 0;
        // Air
        boolean airHoles = false;
        // Other blocks
        boolean otherBlocks = false;
        // Ceiling issue
        boolean inCeiling = false;
        // The y height where other blocks were found
        // If this is the bottom layer, the player has most likely uneven walls
        int otherBlockLayer = -1;
        int wallBlockCount = 0;

        int y = 0;
        for (y = world.getMaxHeight() - 1; y >= walls.getFloor(); y--) {
            int doorCount = 0;
            int hopperCount = 0;
            boolean airHole = false;
            boolean otherBlock = false;
            wallBlockCount = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location thisBlock = new Location(world, x, y, z);
                    Material blockType = world.getBlockAt(x, y, z).getType();
                    // Checking above greenhouse - no blocks allowed
                    if (y > roof.getHeight()) {
                        // We are above the greenhouse
                        if ((world.getEnvironment().equals(Environment.NORMAL) || world.getEnvironment().equals(Environment.THE_END))
                                && blockType != Material.AIR) {
                            result.add(GreenhouseResult.FAIL_BLOCKS_ABOVE);
                            redGlass.add(thisBlock);
                        }
                    } else {
                        // Check just the walls
                        if (y == roof.getHeight() || x == minX || x == maxX || z == minZ || z== maxZ) {
                            //Greenhouses.addon.logDebug("DEBUG: Checking " + x + " " + y + " " + z);
                            if ((y != roof.getHeight() && !Walls.WALL_BLOCKS.contains(blockType))
                                    || (y == roof.getHeight() && !Roof.ROOFBLOCKS.contains(blockType))) {
                                //logger(2,"DEBUG: bad block found at  " + x + "," + y+ "," + z + " " + blockType);
                                if (blockType == Material.AIR) {
                                    airHole = true;
                                    if (y == roof.getHeight()) {
                                        inCeiling = true;
                                    }
                                } else {
                                    otherBlock = true;
                                }
                                redGlass.add(thisBlock);
                            } else {
                                wallBlockCount++;
                                // A string comparison is used to capture 1.8+ door types without stopping pre-1.8
                                // servers from working
                                if (blockType.toString().contains("DOOR")) {
                                    doorCount++;
                                    // If we already have 8 doors add these blocks to the red list
                                    if (wallDoors == 8) {
                                        redGlass.add(thisBlock);
                                    }
                                }
                                if (blockType.equals(Material.HOPPER)) {
                                    hopperCount++;
                                    if (ghHopper > 0) {
                                        // Problem! Add extra hoppers to the red glass list
                                        redGlass.add(thisBlock);
                                    } else {
                                        // This is the first hopper
                                        gh.setRoofHopperLocation(thisBlock);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (wallBlockCount == 0 && y < roof.getHeight()) {
                // This is the floor
                break;
            } else {
                wallBlockCount = 0;
                wallDoors += doorCount;
                ghHopper += hopperCount;
                if (airHole) {
                    airHoles = true;
                }
                if (otherBlock) {
                    otherBlocks = true;
                    if (otherBlockLayer < 0) {
                        otherBlockLayer = y;
                    }
                }
            }
        }
        //addon.logDebug("Floor is at height y = " + y);
        // Check that the player is vertically in the greenhouse
        if (roof.getLocation().getBlockY() <= y) {
            result.add(GreenhouseResult.FAIL_BELOW);
        }
        // Show errors
        if (airHoles & !inCeiling) {
            result.add(GreenhouseResult.FAIL_HOLE_IN_WALL);
        } else if (airHoles & inCeiling) {
            result.add(GreenhouseResult.FAIL_HOLE_IN_ROOF);
        }
        //Greenhouses.addon.logDebug("DEBUG: otherBlockLayer = " + otherBlockLayer);
        if (otherBlocks && otherBlockLayer == y + 1) {
            // Walls must be even all the way around
            result.add(GreenhouseResult.FAIL_UNEVEN_WALLS);
        } else if (otherBlocks && otherBlockLayer == roof.getHeight()) {
            // Roof blocks must be glass, glowstone, doors or a hopper.
            result.add(GreenhouseResult.FAIL_BAD_ROOF_BLOCKS);
        } else if (otherBlocks) {
            // "Wall blocks must be glass, glowstone, doors or a hopper.
            result.add(GreenhouseResult.FAIL_BAD_WALL_BLOCKS);
        }
        if (wallDoors > 8) {
            result.add(GreenhouseResult.FAIL_TOO_MANY_DOORS);
        }
        if (ghHopper > 1) {
            result.add(GreenhouseResult.FAIL_TOO_MANY_HOPPERS);
        }

        return result;
    }

    /**
     * @return the gh
     */
    public Greenhouse getGh() {
        return gh;
    }

    /**
     * @return the redGlass
     */
    public Set<Location> getRedGlass() {
        return redGlass;
    }

}
