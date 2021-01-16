package world.bentobox.greenhouses.managers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.Roof;
import world.bentobox.greenhouses.greenhouse.Walls;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.world.AsyncWorldCache;

public class GreenhouseFinder {

    private Greenhouse gh = new Greenhouse();
    private final Set<Location> redGlass = new HashSet<>();
    // Counts
    private int wallDoors = 0;
    // Hoppers
    private int ghHopper = 0;
    // Air
    private boolean airHoles = false;
    // Other blocks
    private boolean otherBlocks = false;
    // Ceiling issue
    private boolean inCeiling = false;
    // The y height where other blocks were found
    // If this is the bottom layer, the player has most likely uneven walls
    private int otherBlockLayer = -1;
    private int wallBlockCount;

    class CounterCheck {
        int doorCount;
        int hopperCount;
        boolean airHole;
        boolean otherBlock;
    }

    /**
     * Find out if there is a greenhouse here
     * @param location - start location
     * @return future GreenhouseResult class
     */
    public CompletableFuture<Set<GreenhouseResult>> find(Location location) {
        CompletableFuture<Set<GreenhouseResult>> r = new CompletableFuture<>();
        Set<GreenhouseResult> result = new HashSet<>();
        redGlass.clear();

        // Get a world cache
        AsyncWorldCache cache = new AsyncWorldCache(location.getWorld());
        // Find the roof
        Roof roof = new Roof(cache, location);
        roof.findRoof().thenAccept(found -> {
            if (!found) {
                result.add(GreenhouseResult.FAIL_NO_ROOF);
                r.complete(result);
                return;
            }
            BentoBox.getInstance().logDebug(roof);
            // Find the walls
            new Walls().findWalls(roof).thenAccept(walls -> {
                // Make the initial greenhouse
                gh = new Greenhouse(location.getWorld(), walls, roof.getHeight());
                // Set the original biome
                gh.setOriginalBiome(location.getBlock().getBiome());

                // Now check to see if the floor really is the floor and the walls follow the rules
                checkGreenhouse(gh, roof, walls).thenAccept(c -> {
                    result.addAll(c);
                    r.complete(result);
                });
            });

        });
        return r;
    }

    /**
     * Check the greenhouse has the right number of everything
     * @param gh2 - greenhouse
     * @param roof - roof object
     * @param walls - walls object
     * @return future set of Greenhouse Results
     */
    CompletableFuture<Set<GreenhouseResult>> checkGreenhouse(Greenhouse gh2, Roof roof, Walls walls) {
        Set<GreenhouseResult> result = new HashSet<>();
        World world = roof.getLocation().getWorld();
        int y;
        for (y = world.getMaxHeight() - 1; y >= walls.getFloor(); y--) {
            CounterCheck cc = new CounterCheck();
            wallBlockCount = 0;
            for (int x = walls.getMinX(); x <= walls.getMaxX(); x++) {
                for (int z = walls.getMinZ(); z <= walls.getMaxZ(); z++) {
                    result.addAll(checkBlock(cc, roof, walls, world.getBlockAt(x, y, z)));
                }
            }
            if (wallBlockCount == 0 && y < roof.getHeight()) {
                // This is the floor
                break;
            } else {
                wallDoors += cc.doorCount;
                ghHopper += cc.hopperCount;
                if (cc.airHole) {
                    airHoles = true;
                }
                if (cc.otherBlock) {
                    otherBlocks = true;
                    if (otherBlockLayer < 0) {
                        otherBlockLayer = y;
                    }
                }
            }
        }

        result.addAll(checkErrors(roof, y));
        return CompletableFuture.completedFuture(result);
    }

    Collection<GreenhouseResult> checkErrors(Roof roof, int y) {
        Set<GreenhouseResult> result = new HashSet<>();
        // Check that the player is vertically in the greenhouse
        if (roof.getLocation().getBlockY() <= y) {
            result.add(GreenhouseResult.FAIL_BELOW);
        }
        // Show errors
        if (airHoles && !inCeiling) {
            result.add(GreenhouseResult.FAIL_HOLE_IN_WALL);
        } else if (airHoles && inCeiling) {
            result.add(GreenhouseResult.FAIL_HOLE_IN_ROOF);
        }
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

    Set<GreenhouseResult> checkBlock(CounterCheck cc, Roof roof, Walls walls, Block block) {
        Set<GreenhouseResult> result = new HashSet<>();
        World world = block.getWorld();
        // Checking above greenhouse - no blocks allowed
        if (block.getY() > roof.getHeight()) {
            // We are above the greenhouse
            if (!world.getEnvironment().equals(Environment.NETHER) && !block.isEmpty()) {
                result.add(GreenhouseResult.FAIL_BLOCKS_ABOVE);
                redGlass.add(block.getLocation());
            }
        } else {
            // Check just the walls
            checkWalls(block, roof, walls, cc);
        }
        return result;
    }

    /**
     * Check a wall block
     * @param block - block
     * @param roof - roof object
     * @param walls - wall object
     * @param cc - count
     * @return true if block was in the wall
     */
    boolean checkWalls(Block block, Roof roof, Walls walls, CounterCheck cc) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        // Check wall blocks only
        if (y == roof.getHeight() || x == walls.getMinX() || x == walls.getMaxX() || z == walls.getMinZ() || z== walls.getMaxZ()) {
            // Check for non-wall blocks or non-roof blocks at the top of walls
            if ((y != roof.getHeight() && !Walls.wallBlocks(block.getType())) || (y == roof.getHeight() && !Roof.roofBlocks(block.getType()))) {
                if (block.isEmpty()) {
                    cc.airHole = true;
                    if (y == roof.getHeight()) {
                        inCeiling = true;
                    }
                } else {
                    cc.otherBlock = true;
                }
                redGlass.add(block.getLocation());
            } else {
                // Normal wall blocks
                wallBlockCount++;
                checkDoorsHoppers(cc, block);
            }
            return true;
        }
        return false;
    }

    void checkDoorsHoppers(CounterCheck cc, Block block) {
        // Count doors
        if (Tag.DOORS.isTagged(block.getType())) {
            cc.doorCount++;
            // If we already have 8 doors add these blocks to the red list
            if (wallDoors == 8) {
                redGlass.add(block.getLocation());
            }
        }
        // Count hoppers
        if (block.getType().equals(Material.HOPPER)) {
            cc.hopperCount++;
            if (ghHopper > 0) {
                // Problem! Add extra hoppers to the red glass list
                redGlass.add(block.getLocation());
            } else {
                // This is the first hopper
                gh.setRoofHopperLocation(block.getLocation());
            }
        }
    }

    /**
     * @return the greenhouse
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

    /**
     * @return the wallDoors
     */
    int getWallDoors() {
        return wallDoors;
    }

    /**
     * @param wallDoors the wallDoors to set
     */
    void setWallDoors(int wallDoors) {
        this.wallDoors = wallDoors;
    }

    /**
     * @return the ghHopper
     */
    int getGhHopper() {
        return ghHopper;
    }

    /**
     * @return the airHoles
     */
    boolean isAirHoles() {
        return airHoles;
    }

    /**
     * @return the otherBlocks
     */
    boolean isOtherBlocks() {
        return otherBlocks;
    }

    /**
     * @return the inCeiling
     */
    boolean isInCeiling() {
        return inCeiling;
    }

    /**
     * @return the otherBlockLayer
     */
    int getOtherBlockLayer() {
        return otherBlockLayer;
    }

    /**
     * @return the wallBlockCount
     */
    int getWallBlockCount() {
        return wallBlockCount;
    }

    /**
     * @param ghHopper the ghHopper to set
     */
    void setGhHopper(int ghHopper) {
        this.ghHopper = ghHopper;
    }

    /**
     * @param airHoles the airHoles to set
     */
    void setAirHoles(boolean airHoles) {
        this.airHoles = airHoles;
    }

    /**
     * @param otherBlocks the otherBlocks to set
     */
    void setOtherBlocks(boolean otherBlocks) {
        this.otherBlocks = otherBlocks;
    }

    /**
     * @param inCeiling the inCeiling to set
     */
    void setInCeiling(boolean inCeiling) {
        this.inCeiling = inCeiling;
    }

    /**
     * @param otherBlockLayer the otherBlockLayer to set
     */
    void setOtherBlockLayer(int otherBlockLayer) {
        this.otherBlockLayer = otherBlockLayer;
    }

    /**
     * @param wallBlockCount the wallBlockCount to set
     */
    void setWallBlockCount(int wallBlockCount) {
        this.wallBlockCount = wallBlockCount;
    }

}
