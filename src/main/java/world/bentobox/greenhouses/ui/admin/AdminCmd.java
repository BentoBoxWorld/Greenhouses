package world.bentobox.greenhouses.ui.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.Greenhouses;

/**
 * This class handles commands for admins
 *
 */
public class AdminCmd extends CompositeCommand {

    public AdminCmd(Greenhouses greenhouses) {
        super(greenhouses, "gadmin");
    }

    @Override
    public void setup() {
        this.setPermission("greenhouses.admin");
        this.setOnlyPlayer(false);
        this.setParametersHelp("greenhouses.admin.parameters");
        this.setDescription("greenhouses.admin.description");

        new GreenhousesAdminReloadCommand(this);
        new GreenhousesAdminInfoCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        return false;

    }

    /*
        case 1:
            if (split[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.loadPluginConfig();
                plugin.loadBiomeRecipes();
                plugin.ecoTick();
                sender.sendMessage(ChatColor.YELLOW + Locale.reloadconfigReloaded);
                return true;
            } else if (split[0].equalsIgnoreCase("info")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + Locale.admininfoerror);
                    return true;
                }
                Player player = (Player)sender;
                Greenhouse greenhouse = players.getInGreenhouse(player);
                if (greenhouse == null) {
                    sender.sendMessage(ChatColor.RED + Locale.admininfoerror2);
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + Locale.infoinfo);
                sender.sendMessage(ChatColor.GREEN + Locale.generalowner + ":" + greenhouse.getPlayerName());
                sender.sendMessage(ChatColor.GREEN + Locale.admininfoflags);
                for (String flag : greenhouse.getFlags().keySet()) {
                    sender.sendMessage(flag + ": " + greenhouse.getFlags().get(flag));
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + Locale.errorunknownCommand);
                return false;
            }
        case 2:
            if (split[0].equalsIgnoreCase("info")) {
                sender.sendMessage(ChatColor.GREEN + Locale.infoinfo);
                int index = 0;
                boolean found = false;
                for (Greenhouse g : plugin.getGreenhouses()) {
                    if (g.getPlayerName().equalsIgnoreCase(split[1])) {
                        if (!found)
                            sender.sendMessage(ChatColor.GREEN + Locale.generalowner + ":" + g.getPlayerName());
                        found = true;
                        sender.sendMessage("Greenhouse #" + (++index));
                        sender.sendMessage("Biome: " + g.getBiome().name());
                        sender.sendMessage("Recipe: " + g.getBiomeRecipe().getFriendlyName());
                        sender.sendMessage(g.getWorld().getName());
                        sender.sendMessage(g.getPos1().getBlockX() + ", " + g.getPos1().getBlockZ() + " to " + g.getPos2().getBlockX() + ", " + g.getPos2().getBlockZ());
                        sender.sendMessage("Base at " + g.getPos1().getBlockY());
                        sender.sendMessage("Height = " + g.getHeight());
                        sender.sendMessage("Area = " + g.getArea());
                    }
                }
                if (found) {
                    if (index == 0) {
                        sender.sendMessage("Player has no greenhouses.");
                    } else {
                        Player player = plugin.getServer().getPlayer(split[1]);
                        if (player != null) {
                            sender.sendMessage("Player has " + index + " greenhouses and is allowed to build " + plugin.getMaxGreenhouses(player));
                        } else {
                            sender.sendMessage("Player has " + index + " greenhouses. Player is offline.");
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Cannot find that player. (May not have logged on recently)");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + Locale.errorunknownCommand);
                return false;
            }
        default:
            return false;
        }
    }*/
}
