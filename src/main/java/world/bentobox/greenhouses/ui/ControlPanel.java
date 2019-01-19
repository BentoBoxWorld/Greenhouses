package world.bentobox.greenhouses.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.greenhouse.Greenhouse;
import world.bentobox.greenhouses.util.Util;
import world.bentobox.greenhouses.util.VaultHelper;


/**
 * @author tastybento
 * Provides a handy control panel 
 */
public class ControlPanel implements Listener {

    private Greenhouses plugin;

    private HashMap<UUID, HashMap<Integer,BiomeRecipe>> biomePanels = new HashMap<UUID, HashMap<Integer,BiomeRecipe>>();
    /**
     * @param store
     */
    public ControlPanel(Greenhouses plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked(); // The player that clicked the item
        Inventory inventory = event.getInventory(); // The inventory that was clicked in
        int slot = event.getRawSlot();
        HashMap<Integer,BiomeRecipe> panel = biomePanels.get(player.getUniqueId());
        if (panel == null) {
            // No panel, so just return
            return;
        }
        // Check this is the Greenhouse Biome Panel 
        if (inventory.getName().equals(ChatColor.translateAlternateColorCodes('&', Locale.controlpaneltitle))) {
            //String message = "";
            event.setCancelled(true); // Don't let them pick anything up
            if (slot < 0)  {
                player.closeInventory();
                return;
            }
            if (slot == 0) {
                player.performCommand("greenhouse info");
                player.closeInventory();
                return;
            }
            if (panel.containsKey(slot)) {
                BiomeRecipe biomeRecipe = panel.get(slot);
                // Sets up a greenhouse
                Greenhouse oldg = plugin.players.getInGreenhouse(player);
                // Check ownership
                if (oldg != null && !oldg.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(Locale.errornotyours);
                    player.closeInventory();
                    return;
                }
                if (oldg != null) {
                    // Player wants to try and change biome
                    //player.closeInventory(); // Closes the inventory
                    // error.exists
                    //player.sendMessage(ChatColor.RED + Locale.erroralreadyexists);
                    //return;
                    plugin.removeGreenhouse(oldg);
                } 
                // Check if they are at their limit
                if (plugin.players.isAtLimit(player)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', Locale.infonomore));
                } else {
                    // Make greenhouse
                    Greenhouse g = plugin.tryToMakeGreenhouse(player, biomeRecipe);
                    if (g == null) {
                        player.closeInventory(); // Closes the inventory
                        //error.norecipe
                        player.sendMessage(ChatColor.RED + Locale.errornorecipe);
                        return;
                    }
                    player.closeInventory(); // Closes the inventory
                }
                //player.performCommand("greenhouse make");
                //player.sendMessage(message);		
            }
        }
    }

    /**
     * Creates a player-specific biome panel based on permissions
     * @param player
     * @return
     */
    public Inventory getPanel(Player player) {
        HashMap<Integer, BiomeRecipe> store = new HashMap<Integer,BiomeRecipe>();
        // Index 0 is reserved for instructions
        int index = 1;
        // Run through biomes and add to the inventory if this player is allowed to use them
        for (BiomeRecipe br : plugin.getBiomeRecipes()) {
            // Gather the info
            if (br.getPermission().isEmpty() || VaultHelper.checkPerm(player, br.getPermission())) {
                // Add this biome recipe to the list
                store.put(index++, br);
            }
        }
        // Now create the panel
        //int panelSize = store.size() + 9 - 1;
        int panelSize = store.size() + 9;
        panelSize -= ( panelSize % 9);
        Inventory biomePanel = Bukkit.createInventory(player, panelSize, ChatColor.translateAlternateColorCodes('&', Locale.controlpaneltitle));
        // Add the instructions item
        ItemStack item = new ItemStack(Material.THIN_GLASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Locale.generalgreenhouses);
        List<String> lore = new ArrayList<String>();
        lore.addAll(new ArrayList<String>(Arrays.asList(Locale.infowelcome.split("\\|")))); 
        if (plugin.players.isAtLimit(player)) {
            lore.add(ChatColor.translateAlternateColorCodes('&', Locale.infonomore));
        } else {
            if (plugin.players.getRemainingGreenhouses(player) > 0) {
                if (plugin.players.getRemainingGreenhouses(player) == 1) {
                    lore.addAll(new ArrayList<String>(Arrays.asList(Locale.infoonemore.split("\\|")))); 
                } else {
                    String temp = Locale.infoyoucanbuild.replace("[number]", String.valueOf(plugin.players.getRemainingGreenhouses(player)));
                    lore.addAll(new ArrayList<String>(Arrays.asList(temp.split("\\|")))); 
                }
            } else {
                lore.addAll(new ArrayList<String>(Arrays.asList(Locale.infounlimited.split("\\|"))));
            }   
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        biomePanel.setItem(0, item);
        // Now add the biomes

        index = 1;
        for (BiomeRecipe br : store.values()) {
            // Create an itemStack
            item = new ItemStack(br.getIcon());
            meta = item.getItemMeta();
            if (br.getFriendlyName().isEmpty()) {
                meta.setDisplayName(Util.prettifyText(br.getBiome().toString()));
            } else {
                meta.setDisplayName(br.getFriendlyName());
            }
            lore.clear();
            List<String> reqBlocks = br.getRecipeBlocks();
            if (reqBlocks.size() > 0) {
                lore.add(ChatColor.YELLOW + Locale.recipeminimumblockstitle);
                int i = 1;
                for (String list : reqBlocks) {
                    lore.add(Locale.lineColor + (i++) + ": " + list);
                }
            } else {
                lore.add(ChatColor.YELLOW + Locale.recipenootherblocks);
            }
            if (br.getWaterCoverage() == 0) {
                lore.add(Locale.recipenowater);
            } else if (br.getWaterCoverage() > 0) {
                lore.add(Locale.recipewatermustbe.replace("[coverage]", String.valueOf(br.getWaterCoverage())));
            }
            if (br.getIceCoverage() == 0) {
                lore.add(Locale.recipenoice);
            } else if (br.getIceCoverage() > 0) {
                lore.add(Locale.recipeicemustbe.replace("[coverage]", String.valueOf(br.getIceCoverage())));
            }
            if (br.getLavaCoverage() == 0) {
                lore.add(Locale.recipenolava);
            } else if (br.getLavaCoverage() > 0) {
                lore.add(Locale.recipelavamustbe.replace("[coverage]", String.valueOf(br.getLavaCoverage())));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            biomePanel.setItem(index++,item);
        }
        // Stash the panel for later use when clicked
        biomePanels.put(player.getUniqueId(), store);
        return biomePanel;
    }
}
