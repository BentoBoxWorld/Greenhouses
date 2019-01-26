package world.bentobox.greenhouses.greenhouse;

import org.bukkit.Material;

class GreenhouseBlockConversions {
    private final Material oldMaterial;
    private final Material newMaterial;
    private final double probability;
    private final Material localMaterial;

    public GreenhouseBlockConversions(Material oldMaterial, Material newMaterial, double probability, Material localMaterial) {
        this.oldMaterial = oldMaterial;
        this.newMaterial = newMaterial;
        this.probability = probability;
        this.localMaterial = localMaterial;
    }
    /**
     * @return the oldMaterial
     */
    public Material getOldMaterial() {
        return oldMaterial;
    }
    /**
     * @return the newMaterial
     */
    public Material getNewMaterial() {
        return newMaterial;
    }
    /**
     * @return the probability
     */
    public double getProbability() {
        return probability;
    }
    /**
     * @return the localMaterial
     */
    public Material getLocalMaterial() {
        return localMaterial;
    }

}
