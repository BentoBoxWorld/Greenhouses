package world.bentobox.greenhouses.greenhouse;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

class GreenhouseMob {
    private final EntityType mobType;
    private final Material mobSpawnOn;
    /**
     * @param mobType - entity type of mob
     * @param mobSpawnOn - material on which it much spawn, or null if any
     */
    public GreenhouseMob(EntityType mobType, Material mobSpawnOn) {
        this.mobType = mobType;
        this.mobSpawnOn = mobSpawnOn;
    }
    /**
     * @return the mobType
     */
    public EntityType getMobType() {
        return mobType;
    }
    /**
     * @return the mobSpawnOn
     */
    public Optional<Material> getMobSpawnOn() {
        return Optional.of(mobSpawnOn);
    }
}
