package world.bentobox.greenhouses.greenhouse;

/**
 * Holds min, max x and z coords and provides the area
 * @author tastybento
 *
 */
public abstract class MinMaxXZ {

    protected int minX;
    protected int maxX;
    protected int minZ;
    protected int maxZ;

    /**
     * @return the minX
     */
    public int getMinX() {
        return minX;
    }

    /**
     * @return the maxX
     */
    public int getMaxX() {
        return maxX;
    }

    /**
     * @return the minZ
     */
    public int getMinZ() {
        return minZ;
    }

    /**
     * @return the maxZ
     */
    public int getMaxZ() {
        return maxZ;
    }

    /**
     * @return the area
     */
    public int getArea() {
        return (maxX - minX) * (maxZ - minZ);
    }

    @Override
    public String toString() {
        return "MinMaxXZ [minX=" + minX + ", maxX=" + maxX + ", minZ=" + minZ + ", maxZ=" + maxZ + "]";
    }


}
