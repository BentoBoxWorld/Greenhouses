package world.bentobox.greenhouses.world;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.BentoBox;
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
     * @return chunk snapshot
     */
    private ChunkSnapshot getSnap(int x, int z) {
        // Convert from block to chunk coords
        Pair<Integer, Integer> key = new Pair<>((x >> 4), (z >> 4));
        // Get from cache if it is available
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        ChunkSnapshot cs;
        try {
            // Block on getting the chunk because this is running async
            cs = getAChunk(key.x, key.z).get();
        } catch (InterruptedException | ExecutionException e) {
            // Try again...
            return getSnap(x,z);
        }
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
     * @return material type
     */
    public Material getBlockType(int x, int y, int z) {
        // Convert block coords to chunk coords
        int xx = x >= 0 ? x % 16 : 15 + (x % 16);
        int zz = z >= 0 ? z % 16 : 15 + (z % 16);
        Material m = getSnap(x,z).getBlockType(xx, y, zz);
        BentoBox.getInstance().logDebug(m);
        return m;
    }

    /**
     * Get block material for block at corresponding coordinates
     * @param v - vector
     * @return Material
     */
    public Material getBlockType(Vector v) {
        return getBlockType(v.getBlockX(), v.getBlockY(), v.getBlockZ());
    }

}
