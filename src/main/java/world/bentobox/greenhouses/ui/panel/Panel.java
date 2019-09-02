package world.bentobox.greenhouses.ui.panel;

import java.util.ArrayList;
import java.util.List;

import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;

public class Panel {

    private Greenhouses addon;

    public Panel(Greenhouses addon) {
        super();
        this.addon = addon;
    }

    public void ShowPanel(User user) {
        PanelBuilder pb = new PanelBuilder().name(user.getTranslation("greenhouses.general.greenhouses"));
        for (BiomeRecipe br : addon.getRecipes().getBiomeRecipes()) {
            if (user.hasPermission(br.getPermission())) {
                pb.item(new PanelItemBuilder()
                        .name(br.getFriendlyName()).icon(br.getIcon())
                        .description(getDescription(user, br))
                        .clickHandler(new PanelClick(addon, br)).build());
            }
        }
        pb.user(user).build();
    }

    private List<String> getDescription(User user, BiomeRecipe br) {
        List<String> d = new ArrayList<>();
        // Make description
        d.add(user.getTranslation("greenhouses.recipe.title", "[biome]", Util.prettifyText(br.getBiome().toString())));
        if (!br.getRecipeBlocks().isEmpty()) {
            d.add(user.getTranslation("greenhouses.recipe.minimumblockstitle"));
            br.getRecipeBlocks().forEach(b -> d.add(user.getTranslation("greenhouses.recipe.blockscolor") + b));
        }
        if (br.getWaterCoverage() > 0) {
            d.add(user.getTranslation("greenhouses.recipe.watermustbe", "[coverage]", String.valueOf(br.getWaterCoverage())));
        }
        if (br.getLavaCoverage() > 0) {
            d.add(user.getTranslation("greenhouses.recipe.lavamustbe", "[coverage]", String.valueOf(br.getLavaCoverage())));
        }
        if (br.getIceCoverage() > 0) {
            d.add(user.getTranslation("greenhouses.recipe.icemustbe", "[coverage]", String.valueOf(br.getIceCoverage())));
        }
        if (br.getRecipeBlocks().isEmpty()) {
            d.add(user.getTranslation("greenhouses.recipe.nootherblocks"));
        }
        return d;
    }
}
