package world.bentobox.greenhouses.data;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.BoundingBox;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.Walls;
import world.bentobox.greenhouses.managers.RecipeManager;

/**
 * Greenhouse object
 * @author tastybento
 *
 */
@Table(name = "Greenhouses")
public class Greenhouse implements DataObject {

    @Expose
    private String uniqueId = UUID.randomUUID().toString();
    @Expose
    private Location location;
    @Expose
    // Min coords are inside, max coords are outside
    private BoundingBox boundingBox;
    @Expose
    private Biome originalBiome;
    @Expose
    private Location roofHopperLocation;
    @Expose
    private String biomeRecipeName;

    private boolean broken;

    private Map<Material, Integer> missingBlocks;

    /**
     * Constructor for database
     */
    public Greenhouse() {}

    public Greenhouse(World world, Walls walls, int ceilingHeight) {
        this.location = new Location(world, walls.getMinX(), walls.getFloor(), walls.getMinZ());
        Location location2 = new Location(world, walls.getMaxX() + 1D, ceilingHeight + 1D, walls.getMaxZ() + 1D);
        this.boundingBox = BoundingBox.of(location, location2);
    }

    /**
     * @return the biomeRecipe
     */
    public String getBiomeRecipeName() {
        return biomeRecipeName;
    }

    /**
     * @return the ceilingHeight
     */
    public int getCeilingHeight() {
        return (int) boundingBox.getMaxY();
    }

    /**
     * @return the floorHeight
     */
    public int getFloorHeight() {
        return location.getBlockY();
    }

    /**
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @return the originalBiome
     */
    public Biome getOriginalBiome() {
        return originalBiome;
    }

    /**
     * @return the roofHopperLocation
     */
    public Location getRoofHopperLocation() {
        return roofHopperLocation;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.objects.DataObject#getUniqueId()
     */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @return the broken
     */
    public boolean isBroken() {
        return broken;
    }

    /**
     * @param biomeRecipeName the biomeRecipe to set
     */
    public void setBiomeRecipeName(String biomeRecipeName) {
        this.biomeRecipeName = biomeRecipeName;
    }

    /**
     * @param broken the broken to set
     */
    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * @param originalBiome the originalBiome to set
     */
    public void setOriginalBiome(Biome originalBiome) {
        this.originalBiome = originalBiome;
    }

    /**
     * @param roofHopperLocation the roofHopperLocation to set
     */
    public void setRoofHopperLocation(Location roofHopperLocation) {
        this.roofHopperLocation = roofHopperLocation;
    }

    /**
     * @return the boundingBox
     */
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    /**
     * @param boundingBox the boundingBox to set
     */
    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.objects.DataObject#setUniqueId(java.lang.String)
     */
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;

    }

    /**
     *
     * @return internal area of greenhouse
     */
    public int getArea() {
        return ((int)boundingBox.getWidthX() - 2) * ((int)boundingBox.getWidthZ() - 2);
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return this.getLocation().getWorld();
    }

    /**
     * Checks if a location is inside the greenhouse
     * @param location2 - location to check
     * @return true if inside the greenhouse
     */
    public boolean contains(Location location2) {
        return location.getWorld().equals(location2.getWorld()) && boundingBox.contains(location2.toVector());
    }

    /**
     * Set the biome recipe
     * @param greenhouseRecipe - biome recipe
     */
    public void setBiomeRecipe(BiomeRecipe greenhouseRecipe) {
        this.biomeRecipeName = greenhouseRecipe.getName();

    }

    /**
     * Get the biome recipe for this greenhouse
     * @return biome recipe or null
     */
    public BiomeRecipe getBiomeRecipe() {
        return RecipeManager.getBiomeRecipies(biomeRecipeName).orElse(null);
    }

    /**
     * @param missingBlocks the missingBlocks to set
     */
    public void setMissingBlocks(Map<Material, Integer> missingBlocks) {
        this.missingBlocks = missingBlocks;
    }

    /**
     * @return the missingBlocks
     */
    public Map<Material, Integer> getMissingBlocks() {
        return missingBlocks;
    }

}
