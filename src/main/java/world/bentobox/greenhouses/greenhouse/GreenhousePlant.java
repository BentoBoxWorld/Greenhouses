package world.bentobox.greenhouses.greenhouse;

import java.util.Optional;

import org.bukkit.Material;

public class GreenhousePlant {
    private final Material plantMaterial;
    private final Material plantGrownOn;
    /**
     * Describes a recipe plant
     * @param plantMaterial - material
     * @param plantGrownOn - material on which this grows
     */
    public GreenhousePlant(Material plantMaterial,Material plantGrownOn) {
        this.plantMaterial = plantMaterial;
        this.plantGrownOn = plantGrownOn;
    }
    /**
     * @return the plantMaterial
     */
    public Material getPlantMaterial() {
        return plantMaterial;
    }
    /**
     * @return the plantGrownOn
     */
    public Optional<Material> getPlantGrownOn() {
        return Optional.ofNullable(plantGrownOn);
    }

}
