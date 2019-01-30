package world.bentobox.greenhouses.data;

import java.awt.Rectangle;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.adapters.Adapter;
import world.bentobox.greenhouses.data.adapters.BiomeRecipeAdapter;
import world.bentobox.greenhouses.data.adapters.RectangleAdapter;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.Walls;

/**
 * Greenhouse object
 * @author tastybento
 *
 */
public class Greenhouse implements DataObject {

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Greenhouse [uniqueId=" + uniqueId + ", location=" + location + ", footprint=" + footprint
                + ", ceilingHeight=" + ceilingHeight + ", originalBiome=" + originalBiome
                + ", roofHopperLocation=" + roofHopperLocation + ", biomeRecipe=" + biomeRecipe.getName()
                + ", broken=" + broken + "]";
    }

    @Expose
    private String uniqueId = UUID.randomUUID().toString();
    @Expose
    private Location location;
    @Expose
    @Adapter(RectangleAdapter.class)
    private Rectangle footprint;
    @Expose
    private int ceilingHeight;
    @Expose
    private Biome originalBiome;
    @Expose
    private Location roofHopperLocation;
    @Expose
    @Adapter(BiomeRecipeAdapter.class)
    private BiomeRecipe biomeRecipe;

    private boolean broken;

    /**
     * Constructor for database
     */
    public Greenhouse() {}

    public Greenhouse(World world, Walls walls, int ceilingHeight) {
        this.location = new Location(world, walls.getMinX(), walls.getFloor(), walls.getMinZ());
        this.ceilingHeight = ceilingHeight;
        this.footprint = new Rectangle(walls.getMinX(), walls.getMinZ(), walls.getWidth(), walls.getLength());
    }

    /**
     * @return the biomeRecipe
     */
    public BiomeRecipe getBiomeRecipe() {
        return biomeRecipe;
    }

    /**
     * @return the ceilingHeight
     */
    public int getCeilingHeight() {
        return ceilingHeight;
    }

    /**
     * @return the floorHeight
     */
    public int getFloorHeight() {
        return location.getBlockY();
    }

    /**
     * @return the floor
     */
    public Rectangle getFootprint() {
        return footprint;
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
     * @param biomeRecipe the biomeRecipe to set
     */
    public void setBiomeRecipe(BiomeRecipe biomeRecipe) {
        this.biomeRecipe = biomeRecipe;
    }

    /**
     * @param broken the broken to set
     */
    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    /**
     * @param ceilingHeight the ceilingHeight to set
     */
    public void setCeilingHeight(int ceilingHeight) {
        this.ceilingHeight = ceilingHeight;
    }

    /**
     * @param floor the floor to set
     */
    public void setFootprint(Rectangle floor) {
        this.footprint = floor;
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

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.objects.DataObject#setUniqueId(java.lang.String)
     */
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;

    }

    /**
     * @return area of greenhouse
     */
    public int getArea() {
        return this.footprint.height * this.footprint.width;
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
        return (location.getWorld().equals(location2.getWorld())
                && location2.getBlockY() <= this.ceilingHeight
                && location2.getBlockY() >= this.getFloorHeight()
                && location2.getBlockX() >= (int)this.footprint.getMinX()
                && location2.getBlockX() <= (int)this.footprint.getMaxX()
                && location2.getBlockZ() >= (int)this.footprint.getMinY()
                && location2.getBlockZ() <= (int)this.footprint.getMaxY());
    }

}
