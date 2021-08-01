package world.bentobox.greenhouses.greenhouse;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

record GreenhouseMob(EntityType mobType, Material mobSpawnOn) { }
