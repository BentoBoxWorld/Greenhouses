package world.bentobox.greenhouses.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager.GhResult;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

/**
 * Tells a user how an attempt to make a greenhouse went, shared by the player
 * {@code make} command and the admin {@code build} command.
 *
 * @author tastybento
 */
public final class GreenhouseResults {

    /** How long problem blocks show as red glass, in ticks */
    private static final long RED_GLASS_TICKS = 120L;

    private GreenhouseResults() {
        // Utility class
    }

    /**
     * Reports the result: success, or each failure, with problem blocks shown as red glass
     * and any missing blocks listed.
     *
     * @param user   the user
     * @param recipe the recipe that was asked for, or null if any would do
     * @param result the result
     * @param plugin the plugin to schedule the red glass reset with
     * @return true if the greenhouse was made
     */
    public static boolean report(User user, @Nullable BiomeRecipe recipe, GhResult result, Plugin plugin) {
        if (result.getResults().contains(GreenhouseResult.SUCCESS)) {
            user.sendMessage("greenhouses.commands.user.make.success", "[biome]",
                    result.getFinder().getGh().getBiomeRecipe().getFriendlyName());
            return true;
        }
        result.getResults().forEach(r -> user.sendMessage("greenhouses.commands.user.make.error." + r.name()));
        if (!result.getFinder().getRedGlass().isEmpty() && user.getPlayer() != null) {
            result.getFinder().getRedGlass().forEach(rg -> user.getPlayer()
                    .sendBlockChange(rg.toLocation(user.getWorld()), Material.RED_STAINED_GLASS.createBlockData()));
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> result.getFinder().getRedGlass().stream().map(v -> v.toLocation(user.getWorld()))
                            .forEach(rg -> user.getPlayer().sendBlockChange(rg, rg.getBlock().getBlockData())),
                    RED_GLASS_TICKS);
        }
        if (recipe != null && result.getResults().contains(GreenhouseResult.FAIL_INSUFFICIENT_BLOCKS)) {
            result.getFinder().getGh().getMissingBlocks()
                    .forEach((k, v) -> user.sendMessage("greenhouses.commands.user.make.missing-blocks", "[material]",
                            Util.prettifyText(k.toString()), TextVariables.NUMBER, String.valueOf(v)));
        }
        return false;
    }
}
