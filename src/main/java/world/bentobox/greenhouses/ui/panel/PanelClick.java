package world.bentobox.greenhouses.ui.panel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.GreenhouseManager.GhResult;
import world.bentobox.greenhouses.managers.GreenhouseManager.GreenhouseResult;

/**
 * @author tastybento
 *
 */
public class PanelClick implements ClickHandler {

    private final Greenhouses addon;
    private final BiomeRecipe br;

    public PanelClick(Greenhouses addon, BiomeRecipe br) {
        this.addon = addon;
        this.br = br;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.PanelItem.ClickHandler#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)
     */
    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        if (user.hasPermission(br.getPermission())) {
            user.closeInventory();
            makeGreenhouse(user, br);
        }
        return true;
    }

    private boolean makeGreenhouse(User user, BiomeRecipe br) {
        // Check flag
        if (!addon.getIslands().getIslandAt(user.getLocation()).map(i -> i.isAllowed(user, Greenhouses.GREENHOUSES)).orElse(false)) {
            user.sendMessage("greenhouses.errors.no-rank");
            return false;
        }
        // Find the physical the greenhouse
        Location location = user.getLocation().add(new Vector(0,1,0));
        // Check if there's a gh here already
        if (addon.getManager().getMap().getGreenhouse(location).isPresent()) {
            user.sendMessage("greenhouses.commands.user.make.error.already");
            return false;
        }
        addon.getManager().tryToMakeGreenhouse(location, br).thenAccept(r -> processResult(user, r));
        return true;
    }

    void processResult(User user, GhResult result) {
        if (result.getResults().contains(GreenhouseResult.SUCCESS)) {
            // Success
            user.sendMessage("greenhouses.commands.user.make.success", "[biome]", result.getFinder().getGh().getBiomeRecipe().getFriendlyName());
            return;
        }
        result.getResults().forEach(r -> user.sendMessage("greenhouses.commands.user.make.error." + r.name()));
        if (!result.getFinder().getRedGlass().isEmpty()) {
            // Show red glass
            result.getFinder().getRedGlass().stream().map(v -> v.toLocation(user.getWorld())).forEach(rg -> user.getPlayer().sendBlockChange(rg, Material.RED_STAINED_GLASS.createBlockData()));
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> result.getFinder().getRedGlass().stream().map(v -> v.toLocation(user.getWorld())).forEach(rg -> user.getPlayer().sendBlockChange(rg, rg.getBlock().getBlockData())), 120L);
        }
        if (result.getResults().contains(GreenhouseResult.FAIL_INSUFFICIENT_BLOCKS)) {
            result.getFinder().getGh().getMissingBlocks().forEach((k,v) -> user.sendMessage("greenhouses.commands.user.make.missing-blocks", "[material]", Util.prettifyText(k.toString()), TextVariables.NUMBER, String.valueOf(v)));
        }
    }
}
