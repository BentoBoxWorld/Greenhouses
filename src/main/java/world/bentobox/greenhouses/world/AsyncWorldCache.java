package world.bentobox.greenhouses.world;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;

/**
 * Provides a thread-safe cache world chunks
 * @author tastybento
 *
 */
public class AsyncWorldCache {

    private final World world;
    private final Map<Pair<Integer, Integer>, ChunkSnapshot> cache;

    /**
     * Chunk cache. This class is designed to be run async and blocks futures
     * @param world - world to cache
     */
    public AsyncWorldCache(World world) {
        this.world = world;
        cache = new HashMap<>();
    }

    /**
     * @return the world's environment
     */
    public Environment getEnvironment() {
        return world.getEnvironment();
    }

    /**
     * @return maximum height of this world
     */
    public int getMaxHeight() {
        return world.getMaxHeight();
    }

    /**
     * Get chunk snapshot from world
     * @param x - coord
     * @param z - coord
     * @return future chunk snapshot
     */
    private CompletableFuture<ChunkSnapshot> getAChunk(int x, int z) {
        CompletableFuture<ChunkSnapshot> r = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(Greenhouses.getInstance().getPlugin(), () ->
        Util.getChunkAtAsync(world, x, z).thenAccept(chunk -> r.complete(chunk.getChunkSnapshot())));
        return r;
    }

    /**
     * Get snapshot from cache or world
     * @param x - block coord
     * @param z - block coord
     * @return chunk snapshot or null if there's an error getting the chunk
     * @throws ExecutionException - if the chunk getting throws an exception
     * @throws InterruptedException  - if the future is interrupted
     */
    @Nullable
    private ChunkSnapshot getSnap(final int x, final int z) throws InterruptedException, ExecutionException {
        // Convert from block to chunk coords
        Pair<Integer, Integer> key = new Pair<>((x >> 4), (z >> 4));
        // Get from cache if it is available
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        // Block on getting the chunk because this is running async
        ChunkSnapshot cs = getAChunk(key.x, key.z).get();
        // Store in cache
        cache.put(key, cs);
        return cs;

    }

    /**
     * Get block material for block at corresponding coordinates
     *
     * @param x block coordinate
     * @param y 0-255
     * @param z block coordinate
     * @return material type or Material.AIR if there is an exception
     */
    public Material getBlockType(final int x, final int y, final int z) {
        // Convert block coords to chunk coords
        // TODO: simplify this - it must be easier than this!
        int xx = x >= 0 ? x % 16 : (16 + (x % 16)) % 16;
        int zz = z >= 0 ? z % 16 : (16 + (z % 16)) % 16;
        try {
            return getSnap(x,z).getBlockType(xx, y, zz);
        } catch (InterruptedException | ExecutionException e) {
            Greenhouses.getInstance().logError("Chunk could not be obtained async! " + e);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            return Material.AIR;
        }
    }

    /**
     * Get block material for block at corresponding coordinates
     * @param v - vector
     * @return Material
     */
    public Material getBlockType(Vector v) {
        return getBlockType(v.getBlockX(), v.getBlockY(), v.getBlockZ());
    }

    /**
     * Check if block is AIR
     * @param vector - vector
     * @return true if AIR
     */
    public boolean isEmpty(Vector vector) {
        return getBlockType(vector).equals(Material.AIR);
    }

}
