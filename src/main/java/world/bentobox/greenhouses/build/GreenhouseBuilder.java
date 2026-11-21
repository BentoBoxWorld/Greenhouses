package world.bentobox.greenhouses.build;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.greenhouses.build.GreenhousePlan.Placement;

/**
 * Places the blocks of a {@link GreenhousePlan} in a world.
 *
 * @author tastybento
 */
public final class GreenhouseBuilder {

    /** Bone meal put in the roof hopper when the recipe grows plants */
    static final int BONE_MEAL = 64;

    private GreenhouseBuilder() {
        // Utility class
    }

    /**
     * Builds the plan. Blocks are placed without physics, so water does not flow and sand
     * does not fall while the build is half done.
     *
     * @param world        the world
     * @param plan         the plan
     * @param fillBoneMeal true to put bone meal in the roof hopper
     * @return the number of blocks changed
     */
    public static int build(@NonNull World world, @NonNull GreenhousePlan plan, boolean fillBoneMeal) {
        int changed = 0;
        Block hopper = null;
        for (Placement p : plan.getPlacements()) {
            Block b = world.getBlockAt(p.x(), p.y(), p.z());
            if (p.onlyIfAir() && !b.isEmpty()) {
                continue;
            }
            BlockData data = p.material().createBlockData();
            if (data instanceof Bisected bisected) {
                bisected.setHalf(p.upper() ? Bisected.Half.TOP : Bisected.Half.BOTTOM);
            }
            if (p.facing() != null && data instanceof Directional directional
                    && directional.getFaces().contains(p.facing())) {
                directional.setFacing(p.facing());
            }
            if (data instanceof Leaves leaves) {
                // Placed leaves far from a log would decay
                leaves.setPersistent(true);
            }
            b.setBlockData(data, false);
            changed++;
            if (p.material() == Material.HOPPER) {
                hopper = b;
            }
        }
        if (fillBoneMeal && hopper != null && hopper.getState() instanceof Hopper h) {
            h.getInventory().addItem(new ItemStack(Material.BONE_MEAL, BONE_MEAL));
        }
        return changed;
    }
}
