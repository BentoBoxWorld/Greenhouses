package world.bentobox.greenhouses.build;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.block.Biome;

/**
 * The look of a test greenhouse for a biome: the door style, the block that fills an
 * otherwise empty floor, and the block laid underneath it.
 *
 * @author tastybento
 */
public final class BiomeStyle {

    /**
     * Door by biome name fragment, checked in order so that e.g. "mangrove_swamp" finds
     * mangrove before swamp. Materials are looked up by name so a door missing from an
     * older server just falls through to the next match.
     */
    private static final List<Map.Entry<String, String>> DOORS = List.of(
            Map.entry("crimson", "CRIMSON_DOOR"), Map.entry("warped", "WARPED_DOOR"),
            Map.entry("soul_sand", "WARPED_DOOR"), Map.entry("basalt", "CRIMSON_DOOR"),
            Map.entry("nether", "CRIMSON_DOOR"), Map.entry("end", "WARPED_DOOR"),
            Map.entry("mangrove", "MANGROVE_DOOR"), Map.entry("cherry", "CHERRY_DOOR"),
            Map.entry("bamboo", "BAMBOO_DOOR"), Map.entry("pale", "PALE_OAK_DOOR"),
            Map.entry("jungle", "JUNGLE_DOOR"), Map.entry("dark_forest", "DARK_OAK_DOOR"),
            Map.entry("birch", "BIRCH_DOOR"), Map.entry("dripstone", "COPPER_DOOR"),
            Map.entry("deep_dark", "COPPER_DOOR"), Map.entry("cave", "COPPER_DOOR"),
            Map.entry("taiga", "SPRUCE_DOOR"), Map.entry("snow", "SPRUCE_DOOR"),
            Map.entry("frozen", "SPRUCE_DOOR"), Map.entry("ice", "SPRUCE_DOOR"),
            Map.entry("grove", "SPRUCE_DOOR"), Map.entry("swamp", "SPRUCE_DOOR"),
            Map.entry("savanna", "ACACIA_DOOR"), Map.entry("badlands", "ACACIA_DOOR"),
            Map.entry("desert", "ACACIA_DOOR"), Map.entry("beach", "BIRCH_DOOR"));

    private final Material door;
    private final Material filler;
    private final Material base;

    private BiomeStyle(Material door, Material filler, Material base) {
        this.door = door;
        this.filler = filler;
        this.base = base;
    }

    /**
     * @param biome the biome, or null for the default style
     * @return the style for it
     */
    public static BiomeStyle of(Biome biome) {
        String key = biome == null ? "" : biome.getKey().getKey().toLowerCase(Locale.ENGLISH);
        Material door = DOORS.stream().filter(e -> key.contains(e.getKey()))
                .map(e -> Material.matchMaterial(e.getValue())).filter(Objects::nonNull).findFirst()
                .orElse(Material.OAK_DOOR);
        if (isNether(key)) {
            return new BiomeStyle(door, Material.NETHERRACK, Material.NETHERRACK);
        }
        if (key.contains("end")) {
            return new BiomeStyle(door, Material.END_STONE, Material.END_STONE);
        }
        if (key.contains("desert") || key.contains("beach")) {
            return new BiomeStyle(door, Material.SAND, Material.SANDSTONE);
        }
        if (key.contains("badlands")) {
            return new BiomeStyle(door, Material.RED_SAND, Material.TERRACOTTA);
        }
        if (key.contains("dripstone") || key.contains("deep_dark") || key.contains("peak")) {
            return new BiomeStyle(door, Material.STONE, Material.STONE);
        }
        return new BiomeStyle(door, Material.GRASS_BLOCK, Material.DIRT);
    }

    private static boolean isNether(String key) {
        return key.contains("nether") || key.contains("crimson") || key.contains("warped")
                || key.contains("soul_sand") || key.contains("basalt");
    }

    /**
     * @return the door, always a door that opens by hand
     */
    public Material getDoor() {
        return door;
    }

    /**
     * @return the block that fills floor spots nothing else needs
     */
    public Material getFiller() {
        return filler;
    }

    /**
     * @return the solid block laid under the floor where there is nothing to hold it up
     */
    public Material getBase() {
        return base;
    }
}
