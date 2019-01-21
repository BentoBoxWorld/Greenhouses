/**
 *
 */
package world.bentobox.greenhouses.data;

import java.awt.Rectangle;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;

/**
 * @author tastybento
 *
 */
public class Greenhouse implements DataObject {

    private String uniqueId = UUID.randomUUID().toString();
    private Location location;
    private Rectangle footprint;
    private int ceilingHeight;
    private Biome originalBiome;
    private Biome greenhouseBiome;
    private Location roofHopperLocation;
    private BiomeRecipe biomeRecipe;
    private boolean broken;

    public Greenhouse() {}

    /**
     * @param world
     * @param footprint
     * @param ceilingHeight
     */
    public Greenhouse(World world, Rectangle footprint, int floorHeight, int ceilingHeight) {
        this.location = new Location(world, footprint.getMinX(), floorHeight, footprint.getMinY());
        this.footprint = footprint;
        this.ceilingHeight = ceilingHeight;
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
     * @return the greenhouseBiome
     */
    public Biome getGreenhouseBiome() {
        return greenhouseBiome;
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
     * @param greenhouseBiome the greenhouseBiome to set
     */
    public void setGreenhouseBiome(Biome greenhouseBiome) {
        this.greenhouseBiome = greenhouseBiome;
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

}
