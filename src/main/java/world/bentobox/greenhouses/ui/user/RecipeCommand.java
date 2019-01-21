/**
 *
 */
package world.bentobox.greenhouses.ui.user;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
public class RecipeCommand extends CompositeCommand {

    /**
     * @param parent
     * @param label
     * @param aliases
     */
    public RecipeCommand(CompositeCommand parent) {
        super(parent, "make");
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        /*
        // Second arg
        int recipeNumber = 0;
        try {
            recipeNumber = Integer.valueOf(split[1]);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + Locale.recipehint);
            return true;
        }
        List<BiomeRecipe> recipeList = plugin.getBiomeRecipes();
        if (recipeNumber <1 || recipeNumber > recipeList.size()) {
            player.sendMessage(ChatColor.RED + Locale.recipewrongnumber.replace("[size]", String.valueOf(recipeList.size())));
            return true;
        }
        BiomeRecipe br = recipeList.get(recipeNumber-1);
        if (br.getFriendlyName().isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "[" + Util.prettifyText(br.getBiome().toString()) + " recipe]");
        } else {
            player.sendMessage(ChatColor.GREEN + "[" + br.getFriendlyName() + " recipe]");
        }
        player.sendMessage(ChatColor.YELLOW + "Biome: " + Util.prettifyText(br.getBiome().toString()));
        if (br.getWaterCoverage() == 0) {
            player.sendMessage(Locale.recipenowater);
        } else if (br.getWaterCoverage() > 0) {
            player.sendMessage(Locale.recipewatermustbe.replace("[coverage]", String.valueOf(br.getWaterCoverage())));
        }
        if (br.getIceCoverage() == 0) {
            player.sendMessage(Locale.recipenoice);
        } else if (br.getIceCoverage() > 0) {
            player.sendMessage(Locale.recipeicemustbe.replace("[coverage]", String.valueOf(br.getIceCoverage())));
        }
        if (br.getLavaCoverage() == 0) {
            player.sendMessage(Locale.recipenolava);
        } else if (br.getLavaCoverage() > 0) {
            player.sendMessage(Locale.recipelavamustbe.replace("[coverage]", String.valueOf(br.getLavaCoverage())));
        }
        List<String> reqBlocks = br.getRecipeBlocks();
        if (reqBlocks.size() > 0) {
            player.sendMessage(ChatColor.YELLOW + Locale.recipeminimumblockstitle);
            int index = 1;
            for (String list : reqBlocks) {
                player.sendMessage(Locale.lineColor + (index++) + ": " + list);
            }
        } else {
            player.sendMessage(ChatColor.YELLOW + Locale.recipenootherblocks);
        }*/
        return true;
    }

}
