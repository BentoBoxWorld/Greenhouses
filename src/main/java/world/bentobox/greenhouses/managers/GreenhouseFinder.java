package world.bentobox.greenhouses.managers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.Roof;
import world.bentobox.greenhouses.greenhouse.Walls;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;
import world.bentobox.greenhouses.world.AsyncWorldCache;

public class GreenhouseFinder {

    private Greenhouse gh;
    private final Set<Vector> redGlass = new HashSet<>();
    // Ceiling issue
    private boolean inCeiling = false;
    // The y height where other blocks were found
    // If this is the bottom layer, the player has most likely uneven walls
    private int otherBlockLayer = -1;
    private int wallBlockCount;
    /**
     * This is the count of the various items
     */
    private CounterCheck cc = new CounterCheck();

    static class CounterCheck {
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
            if (Boolean.FALSE.equals(found)) {
                result.add(GreenhouseResult.FAIL_NO_ROOF);
                r.complete(result);
                return;
            }
            // Find the walls
            new Walls(cache).findWalls(roof).thenAccept(walls -> {
                // Make the initial greenhouse
                gh = new Greenhouse(location.getWorld(), walls, roof.getHeight());
                // Set the original biome
                gh.setOriginalBiome(location.getBlock().getBiome());

                // Now check to see if the floor really is the floor and the walls follow the rules
                checkGreenhouse(cache, roof, walls).thenAccept(c -> {
                    result.addAll(c);
                    r.complete(result);
                });
            });

        });
        return r;
    }

    /**
     * Check the greenhouse has the right number of everything
     * @param cache async world cache
     * @param roof - roof object
     * @param walls - walls object
     * @return future set of Greenhouse Results
     */
    CompletableFuture<Set<GreenhouseResult>> checkGreenhouse(AsyncWorldCache cache, Roof roof, Walls walls) {
        CompletableFuture<Set<GreenhouseResult>> r = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(BentoBox.getInstance(), () -> checkGHAsync(r, cache, roof, walls));
        return r;
    }

    private Set<GreenhouseResult> checkGHAsync(CompletableFuture<Set<GreenhouseResult>> r, AsyncWorldCache cache,
            Roof roof, Walls walls) {
        cc = new CounterCheck();
        int y;
        for (y = roof.getHeight(); y > walls.getFloor(); y--) {
            wallBlockCount = 0;
            for (int x = walls.getMinX(); x <= walls.getMaxX(); x++) {
                for (int z = walls.getMinZ(); z <= walls.getMaxZ(); z++) {
                    checkBlock(cc, cache.getBlockType(x,y,z), roof, walls, new Vector(x, y, z));
                }
            }
            if (wallBlockCount == 0 && y < roof.getHeight()) {
                // This is the floor
                break;
            } else {
                if (cc.otherBlock) {
                    if (otherBlockLayer < 0) {
                        otherBlockLayer = y;
                    }
                }
            }
        }

        Set<GreenhouseResult> result = new HashSet<>(checkErrors(roof, y));
        Bukkit.getScheduler().runTask(BentoBox.getInstance(), () -> r.complete(result));
        return result;
    }

    Collection<GreenhouseResult> checkErrors(Roof roof, int y) {
        Set<GreenhouseResult> result = new HashSet<>();
        // Check that the player is vertically in the greenhouse
        if (roof.getLocation().getBlockY() <= y) {
            result.add(GreenhouseResult.FAIL_BELOW);
        }
        // Show errors
        if (isAirHoles() && !inCeiling) {
            result.add(GreenhouseResult.FAIL_HOLE_IN_WALL);
        } else if (isAirHoles() && inCeiling) {
            result.add(GreenhouseResult.FAIL_HOLE_IN_ROOF);
        }
        if (isOtherBlocks() && otherBlockLayer == y + 1) {
            // Walls must be even all the way around
            result.add(GreenhouseResult.FAIL_UNEVEN_WALLS);
        } else if (isOtherBlocks() && otherBlockLayer == roof.getHeight()) {
            // Roof blocks must be glass, glowstone, doors or a hopper.
            result.add(GreenhouseResult.FAIL_BAD_ROOF_BLOCKS);
        } else if (isOtherBlocks()) {
            // "Wall blocks must be glass, glowstone, doors or a hopper.
            result.add(GreenhouseResult.FAIL_BAD_WALL_BLOCKS);
        }
        if (this.getWallDoors() > 8) {
            result.add(GreenhouseResult.FAIL_TOO_MANY_DOORS);
        }
        if (this.getGhHopper() > 1) {
            result.add(GreenhouseResult.FAIL_TOO_MANY_HOPPERS);
        }
        return result;
    }

    /**
     * Check if block is allowed to be in that location
     * @param cc - Counter Check object
     * @param m - material of the block
     * @param roof - roof object
     * @param walls - walls object
     * @param v - vector location of the block
     * @return true if block is acceptable, false if not
     */
    boolean checkBlock(CounterCheck cc, Material m, Roof roof, Walls walls, Vector v) {
        final int x = v.getBlockX();
        final int y = v.getBlockY();
        final int z = v.getBlockZ();
        // Check wall blocks only
        if (y == roof.getHeight() || x == walls.getMinX() || x == walls.getMaxX() || z == walls.getMinZ() || z== walls.getMaxZ()) {
            // Check for non-wall blocks or non-roof blocks at the top of walls
            if ((y != roof.getHeight() && !Walls.wallBlocks(m)) || (y == roof.getHeight() && !Roof.roofBlocks(m))) {
                if (m.equals(Material.AIR)) {
                    // Air hole found
                    cc.airHole = true;
                    if (y == roof.getHeight()) {
                        // Air hole is in ceiling
                        inCeiling = true;
                    }
                } else {
                    // A non-wall or roof block found
                    cc.otherBlock = true;
                }
                // Record the incorrect location
                redGlass.add(v);
                return false;
            } else {
                // Normal wall blocks
                wallBlockCount++;
                return checkDoorsHoppers(cc, m, v);
            }
        }
        return true;
    }

    /**
     * Check the count of doors and hopper and set the hopper location if it is found
     * @param cc counter check
     * @param m material of block
     * @param v vector position of block
     * @return false if there is an error, true if ok
     */
    boolean checkDoorsHoppers(CounterCheck cc, Material m, Vector v) {
        // Count doors
        if (Tag.TRAPDOORS.isTagged(m) || Tag.DOORS.isTagged(m)) {
            cc.doorCount = Tag.TRAPDOORS.isTagged(m) ? cc.doorCount + 2 : cc.doorCount + 1;

            // If we already have 8 doors add these blocks to the red list
            if (cc.doorCount > 8) {
                redGlass.add(v);
                return false;
            }
        }
        // Count hoppers
        if (m.equals(Material.HOPPER)) {
            cc.hopperCount++;
            if (cc.hopperCount > 1) {
                // Problem! Add extra hoppers to the red glass list
                redGlass.add(v);
                return false;
            } else {
                // This is the first hopper
                gh.setRoofHopperLocation(v);
            }
        }
        return true;
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
    public Set<Vector> getRedGlass() {
        return redGlass;
    }

    /**
     * @return the wallDoors
     */
    int getWallDoors() {
        return cc.doorCount;
    }

    /**
     * @return the ghHopper
     */
    int getGhHopper() {
        return cc.hopperCount;
    }

    /**
     * @return the airHoles
     */
    boolean isAirHoles() {
        return cc.airHole;
    }

    /**
     * @return the otherBlocks
     */
    boolean isOtherBlocks() {
        return cc.otherBlock;
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

    /**
     * @param gh the gh to set
     */
    protected void setGh(Greenhouse gh) {
        this.gh = gh;
    }

    public void setGhHopper(int i) {
        cc.hopperCount = i;
    }

    public void setWallDoors(int i) {
        cc.doorCount = i;

    }

    public void setAirHoles(boolean b) {
        cc.airHole = b;

    }

    public void setOtherBlocks(boolean b) {
        cc.otherBlock = b;

    }

}
