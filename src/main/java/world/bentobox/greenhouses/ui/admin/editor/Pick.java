package world.bentobox.greenhouses.ui.admin.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.panels.PickerPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * The pickers the recipe editor uses, built on BentoBox's {@link PickerPanel}.
 *
 * @author tastybento
 */
final class Pick {

    private Pick() {
        // Utility class
    }

    /**
     * @param m a block or liquid
     * @return what to show it as: liquids are not items, so they show as buckets
     */
    static Material icon(Material m) {
        return switch (m) {
        case WATER -> Material.WATER_BUCKET;
        case LAVA -> Material.LAVA_BUCKET;
        default -> m.isItem() ? m : Material.BARRIER;
        };
    }

    /**
     * @param m a block or liquid
     * @return its name for people
     */
    static String name(Material m) {
        return Util.prettifyText(m.name());
    }

    /**
     * Picks any item, e.g. a plant or an icon. Clicking an item in the inventory picks it.
     *
     * @param user  the admin
     * @param title the title
     * @param onPick what to do with it
     * @param back  what Back does
     */
    static void item(User user, String title, Consumer<Material> onPick, Runnable back) {
        PickerPanel.materials(user).title(title).onPick(onPick).onCancel(back).open();
    }

    /**
     * Picks a block that can be placed, or water or lava, e.g. what a plant grows on. Water
     * and lava show as buckets, and clicking a bucket in the inventory picks its liquid.
     *
     * @param user  the admin
     * @param title the title
     * @param onPick what to do with it
     * @param back  what Back does
     */
    static void blockOrLiquid(User user, String title, Consumer<Material> onPick, Runnable back) {
        List<Material> blocks = new ArrayList<>(List.of(Material.WATER, Material.LAVA));
        Arrays.stream(Material.values()).filter(m -> !m.name().startsWith("LEGACY_") && m.isBlock() && m.isItem()
                && !m.isAir()).forEach(blocks::add);
        PickerPanel.of(user, blocks, m -> new ItemStack(icon(m)), Pick::name).title(title)
                .fromInventory(Pick::fromItem).onPick(onPick).onCancel(back).open();
    }

    /**
     * @param item an item clicked in the inventory
     * @return the block or liquid it stands for, if any
     */
    static Optional<Material> fromItem(ItemStack item) {
        return switch (item.getType()) {
        case WATER_BUCKET -> Optional.of(Material.WATER);
        case LAVA_BUCKET -> Optional.of(Material.LAVA);
        default -> item.getType().isBlock() ? Optional.of(item.getType()) : Optional.empty();
        };
    }
}
