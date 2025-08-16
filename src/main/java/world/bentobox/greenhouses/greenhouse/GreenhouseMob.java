package world.bentobox.greenhouses.greenhouse;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public record GreenhouseMob(EntityType mobType, Material mobSpawnOn, @Nullable Consumer<Entity> customizer) {
    public GreenhouseMob(EntityType mobType, Material mobSpawnOn) {
        this(mobType, mobSpawnOn, null);
    }
}
