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
        PanelBuilder pb = new PanelBuilder().name("Greenhouses");
        for (BiomeRecipe br : addon.getRecipes().getBiomeRecipes()) {
            if (user.hasPermission(br.getPermission())) {
                pb.item(new PanelItemBuilder()
                        .name(br.getFriendlyName()).icon(br.getIcon())
                        .description(getDescription(br))
                        .clickHandler(new PanelClick(addon, br)).build());
            }
        }
        pb.user(user).build();
    }

    private List<String> getDescription(BiomeRecipe br) {
        List<String> d = new ArrayList<>();
        // Make description
        d.add("Make a " + Util.prettifyText(br.getBiome().toString()));
        d.add("Greenhouse. Requires:");
        d.addAll(br.getRecipeBlocks());
        if (br.getWaterCoverage() > 0) {
            d.add("Water coverage = " + br.getWaterCoverage() + "%");
        }
        if (br.getLavaCoverage() > 0) {
            d.add("Lava coverage = " + br.getLavaCoverage() + "%");
        }
        if (br.getIceCoverage() > 0) {
            d.add("Ice coverage = " + br.getIceCoverage() + "%");
        }
        return d;
    }
}
