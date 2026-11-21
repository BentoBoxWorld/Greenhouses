package world.bentobox.greenhouses.greenhouse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MobHabitat} (issue #8).
 */
class MobHabitatTest {

    private static MobHabitat of(EntityType type, Material on) {
        return MobHabitat.of(new GreenhouseMob(type, on));
    }

    @Test
    void testWater() {
        assertEquals(MobHabitat.WATER, of(EntityType.SQUID, Material.WATER));
        assertEquals(MobHabitat.WATER, of(EntityType.COD, Material.BUBBLE_COLUMN));
    }

    @Test
    void testLava() {
        assertEquals(MobHabitat.LAVA, of(EntityType.STRIDER, Material.LAVA));
    }

    @Test
    void testAir() {
        assertEquals(MobHabitat.AIR, of(EntityType.BAT, Material.STONE));
        assertEquals(MobHabitat.AIR, of(EntityType.PARROT, Material.GRASS_BLOCK));
        assertEquals(MobHabitat.AIR, of(EntityType.GHAST, Material.SOUL_SAND));
        assertEquals(MobHabitat.AIR, of(EntityType.BEE, Material.GRASS_BLOCK));
    }

    @Test
    void testGround() {
        assertEquals(MobHabitat.GROUND, of(EntityType.COW, Material.GRASS_BLOCK));
        assertEquals(MobHabitat.GROUND, of(EntityType.TURTLE, Material.SAND));
    }

    /**
     * What a mob spawns on wins: a mob placed on water lives in the water.
     */
    @Test
    void testSpawnOnLiquidWins() {
        assertEquals(MobHabitat.WATER, of(EntityType.BAT, Material.WATER));
    }

    @Test
    void testConfigKeys() {
        assertEquals("moblimit", MobHabitat.GROUND.getConfigKey());
        assertEquals("moblimit-water", MobHabitat.WATER.getConfigKey());
        assertEquals("moblimit-lava", MobHabitat.LAVA.getConfigKey());
        assertEquals("moblimit-air", MobHabitat.AIR.getConfigKey());
    }
}
