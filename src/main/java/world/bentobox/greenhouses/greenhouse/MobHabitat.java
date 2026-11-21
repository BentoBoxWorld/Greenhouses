package world.bentobox.greenhouses.greenhouse;

import java.util.Set;

import org.bukkit.Material;

/**
 * Where a greenhouse mob lives, which decides what space its mob limit is measured against
 * (issue #8). Ground mobs use the floor area, as they always have. Water and lava mobs use the
 * number of water or lava blocks, and flying mobs use the internal volume, so deep oceans and
 * tall greenhouses can hold more of them.
 * <p>
 * A recipe only uses a separate limit for a habitat when it sets one ({@code moblimit-water},
 * {@code moblimit-lava}, {@code moblimit-air}). Otherwise those mobs share the ground limit,
 * which is how every recipe worked before.
 */
public enum MobHabitat {
    /** Measured against the floor area. Uses the recipe's {@code moblimit}. */
    GROUND("moblimit"),
    /** Measured against the number of water blocks. */
    WATER("moblimit-water"),
    /** Measured against the number of lava blocks. */
    LAVA("moblimit-lava"),
    /** Measured against the internal volume. */
    AIR("moblimit-air");

    /**
     * Mobs that fly. The Bukkit API has no way to tell from an {@code EntityType}, and the
     * {@code Flying} interface misses most of these, so they are listed by name. Names are used
     * rather than constants so that types missing from older servers do not break loading.
     */
    private static final Set<String> FLYING = Set.of("ALLAY", "BAT", "BEE", "GHAST", "HAPPY_GHAST",
            "PARROT", "PHANTOM", "VEX");

    private final String configKey;

    MobHabitat(String configKey) {
        this.configKey = configKey;
    }

    /**
     * @return the biomes.yml key that holds this habitat's mob limit
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Work out a mob's habitat. A mob that spawns on water or lava belongs to that liquid,
     * whatever it is; otherwise flying mobs live in the air and everything else on the ground.
     * @param mob - greenhouse mob
     * @return habitat
     */
    public static MobHabitat of(GreenhouseMob mob) {
        Material on = mob.mobSpawnOn();
        if (on == Material.WATER || on == Material.BUBBLE_COLUMN) {
            return WATER;
        }
        if (on == Material.LAVA) {
            return LAVA;
        }
        if (mob.mobType() != null && FLYING.contains(mob.mobType().name())) {
            return AIR;
        }
        return GROUND;
    }
}
