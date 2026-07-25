package world.bentobox.greenhouses.ui.admin;

import java.util.List;
import java.util.Map;

import org.bukkit.util.BoundingBox;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;

/**
 * Shows the full detail of one greenhouse, either by ID or the one the admin is standing in.
 *
 * @author tastybento
 */
class AdminInfoCommand extends CompositeCommand {

    private Greenhouse gh;

    /**
     * @param parent - parent command
     */
    public AdminInfoCommand(CompositeCommand parent) {
        super(parent, "info");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup() {
        this.setPermission("greenhouses.admin.info");
        this.setOnlyPlayer(false);
        this.setParametersHelp("greenhouses.commands.admin.info.parameters");
        this.setDescription("greenhouses.commands.admin.info.description");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        Greenhouses addon = this.getAddon();
        if (args.isEmpty()) {
            // No ID given - use the greenhouse the admin is standing in
            if (!user.isPlayer()) {
                user.sendMessage("greenhouses.commands.admin.errors.id-required");
                return false;
            }
            gh = addon.getManager().getMap().getGreenhouse(user.getLocation()).orElse(null);
            if (gh == null) {
                user.sendMessage("greenhouses.errors.not-inside");
                return false;
            }
            return true;
        }
        gh = addon.getManager().getGreenhouseById(args.get(0)).orElse(null);
        if (gh == null) {
            user.sendMessage("greenhouses.commands.admin.errors.unknown-id", "[id]", args.get(0));
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        Greenhouses addon = this.getAddon();
        BoundingBox bb = gh.getBoundingBox();
        user.sendMessage("greenhouses.commands.admin.info.title");
        user.sendMessage("greenhouses.commands.admin.info.id", "[id]", gh.getUniqueId());
        user.sendMessage("greenhouses.commands.admin.info.recipe", "[recipe]", String.valueOf(gh.getBiomeRecipeName()));
        user.sendMessage("greenhouses.commands.admin.info.owner", "[owner]", AdminUtil.ownerName(addon, gh));
        user.sendMessage("greenhouses.commands.admin.info.world", "[world]", AdminUtil.worldName(gh));
        user.sendMessage("greenhouses.commands.admin.info.location", "[xyz]", AdminUtil.xyz(gh));
        user.sendMessage("greenhouses.commands.admin.info.bounding-box", "[min]",
                (int) bb.getMinX() + "," + (int) bb.getMinY() + "," + (int) bb.getMinZ(), "[max]",
                (int) bb.getMaxX() + "," + (int) bb.getMaxY() + "," + (int) bb.getMaxZ());
        user.sendMessage("greenhouses.commands.admin.info.area", "[area]", String.valueOf(gh.getArea()));
        user.sendMessage("greenhouses.commands.admin.info.original-biome", "[biome]",
                gh.getOriginalBiome() == null ? "unknown" : gh.getOriginalBiome().toString());
        user.sendMessage("greenhouses.commands.admin.info.hopper", "[hopper]",
                gh.getRoofHopperLocation() == null ? "none"
                        : gh.getRoofHopperLocation().getBlockX() + "," + gh.getRoofHopperLocation().getBlockY() + ","
                        + gh.getRoofHopperLocation().getBlockZ());
        user.sendMessage("greenhouses.commands.admin.info.broken", "[broken]", String.valueOf(gh.isBroken()));
        // Show why the record is not loaded, if that is the case
        addon.getManager().getUnloaded().stream().filter(u -> u.greenhouse().equals(gh)).findFirst()
        .ifPresent(u -> user.sendMessage("greenhouses.commands.admin.info.not-loaded", "[reason]",
                u.reason().name()));
        Map<org.bukkit.Material, Integer> missing = gh.getMissingBlocks();
        if (!missing.isEmpty()) {
            user.sendMessage("greenhouses.recipe.missing");
            missing.forEach((m, count) -> user.sendMessage("greenhouses.commands.user.make.missing-blocks",
                    "[material]", m.name(), "[number]", String.valueOf(count)));
        }
        return true;
    }

}
