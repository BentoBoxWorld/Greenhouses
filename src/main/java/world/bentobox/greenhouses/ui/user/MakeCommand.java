/**
 *
 */
package world.bentobox.greenhouses.ui.user;

import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.Greenhouse;
import world.bentobox.greenhouses.ui.Locale;

/**
 * @author tastybento
 *
 */
public class MakeCommand extends CompositeCommand {

    /**
     * @param parent
     * @param label
     * @param aliases
     */
    public MakeCommand(CompositeCommand parent) {
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
        // Sets up a greenhouse
        final Greenhouse greenhouseN = players.getInGreenhouse(player);
        if (greenhouseN != null) {
            // alreadyexists
            player.sendMessage(ChatColor.RED + Locale.erroralreadyexists);
            return true;
        }
        // Check if they are at their limit
        if (plugin.players.isAtLimit(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Locale.infonomore));
        } else {
            // Try to make greenhouse
            Greenhouse g = plugin.tryToMakeGreenhouse(player);
            if (g == null) {
                // norecipe
                player.sendMessage(ChatColor.RED + Locale.errornorecipe);
                return true;
            }
            // Greenhouse is made
        }
        return true;

        // Second arg
    case 2:
        if (split[0].equalsIgnoreCase("make")) {
            // Sets up a greenhouse for a specific biome
            if (players.getInGreenhouse(player) != null) {
                // alreadyexists
                player.sendMessage(ChatColor.RED + Locale.erroralreadyexists);
                return true;
            }
            // Check if they are at their limit
            if (plugin.players.isAtLimit(player)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Locale.infonomore));
            } else {
                // Check we are in a greenhouse
                try {
                    if (NumberUtils.isNumber(split[1])) {
                        int recipeNum = Integer.valueOf(split[1]);
                        List<BiomeRecipe> recipeList = plugin.getBiomeRecipes();
                        if (recipeNum < 1 || recipeNum > recipeList.size()) {
                            player.sendMessage(ChatColor.RED + Locale.errornorecipe);
                            return true;
                        }
                        if (plugin.tryToMakeGreenhouse(player,recipeList.get(recipeNum)) == null) {
                            // Failed for some reason - maybe permissions
                            player.sendMessage(ChatColor.RED + Locale.errornorecipe);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + Locale.errornorecipe);
                    return true;
                }
            }
            return true;
        }

    }
