package com.wasteofplastic.wwarps.panels;

import java.util.*;

import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wasteofplastic.wwarps.WWarps;

public class WarpPanel implements Listener {
    private final WWarps plugin;
    private final List<Inventory> warpPanel;
    private final Map<UUID, ItemStack> signpostCache;

    /**
     * @param plugin
     */
    public WarpPanel(WWarps plugin) {
        this.plugin = plugin;
        warpPanel = new ArrayList<>();
        signpostCache = new HashMap<>();
        updatePanel();
    }

    /**
     * This needs to be called if a warp is added or deleted
     */
    public void updatePanel() {
        warpPanel.clear();
        int panelSize = 45; // Must be a multiple of 9
        // Create the warp panels
        Collection<UUID> warps = plugin.getWarpSignsListener().listSortedWarps();
        int panelNumber = warps.size() / (panelSize-2);
        int remainder = (warps.size() % (panelSize-2)) + 8 + 2;
        remainder -= (remainder % 9);
        int i;
        // TODO: Make panel title a string
        for (i = 0; i < panelNumber; i++) {
            warpPanel.add(Bukkit.createInventory(null, panelSize, plugin.myLocale().warpsTitle + " #" + (i+1)));
        }
        // Make the last panel
        warpPanel.add(Bukkit.createInventory(null, remainder, plugin.myLocale().warpsTitle + " #" + (i+1)));
        panelNumber = 0;
        int slot = 0;
        int count = 0;
        // Add this buttons to each panel
        for (UUID playerUUID : warps) {
            count++;
            // Make a head if the player is known
            String playerName = plugin.getServer().getOfflinePlayer(playerUUID).getName();
            if (playerName != null) {
                ItemStack playerSign = new ItemStack(Material.OAK_SIGN); //Item that is displayed instead of the player head
                if (signpostCache.containsKey(playerUUID)) {
                    playerSign = signpostCache.get(playerUUID);
                } else {
                    ItemMeta meta = playerSign.getItemMeta();
                    assert meta != null;
                    meta.setDisplayName(playerName);
                    //get the sign info
                    Location signLocation = plugin.getWarpSignsListener().getWarp(playerUUID);
                    if (Tag.STANDING_SIGNS.isTagged(signLocation.getBlock().getType())){
                        Sign sign = (Sign)signLocation.getBlock().getState();
                        List<String> lines = new ArrayList<>(Arrays.asList(sign.getLines()));
                        meta.setLore(lines);
                    }
                    playerSign.setItemMeta(meta);
                    signpostCache.put(playerUUID, playerSign);
                }
                // Add item to the panel
                CPItem newButton = new CPItem(playerSign, "wwarp " + playerName);
                warpPanel.get(panelNumber).setItem(slot++, newButton.getItem());
            } else {
                // Just make a blank space
                ItemStack playerSign = new ItemStack(Material.OAK_SIGN);
                ItemMeta meta = playerSign.getItemMeta();
                assert meta != null;
                meta.setDisplayName("#" + count);
                playerSign.setItemMeta(meta);
                warpPanel.get(panelNumber).setItem(slot++,playerSign);
            }
            // Check if the panel is full
            if (slot == panelSize-2) {
                // Add navigation buttons
                if (panelNumber > 0) {
                    warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.ARROW,plugin.myLocale().warpsPrevious,"warps " + (panelNumber-1),"").getItem());
                }
                warpPanel.get(panelNumber).setItem(slot, new CPItem(Material.ARROW,plugin.myLocale().warpsNext,"warps " + (panelNumber+1),"").getItem());
                // Move onto the next panel
                panelNumber++;
                slot = 0;
            }
        }
        if (panelNumber > 0) {
            warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.ARROW,plugin.myLocale().warpsPrevious,"warps " + (panelNumber-1),"").getItem());
        }
    }

    public Inventory getWarpPanel(int panelNumber) {
        if (panelNumber < 0) {
            panelNumber = 0;
        } else if (panelNumber > warpPanel.size()-1) {
            panelNumber = warpPanel.size()-1;
        }
        return warpPanel.get(panelNumber);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory(); // The inventory that was clicked in
        String title = event.getView().getTitle();
        if (!event.getView().getTitle().startsWith(plugin.myLocale().warpsTitle + " #")) {
            return;
        }
        // The player that clicked the item
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if (event.getSlotType().equals(SlotType.OUTSIDE)) {
            player.closeInventory();
            return;
        }
        ItemStack clicked = event.getCurrentItem(); // The item that was clicked
        assert clicked != null;
        if (event.getRawSlot() >= event.getInventory().getSize() || clicked.getType() == Material.AIR) {
            return;
        }
        int panelNumber;
        try {
            panelNumber = Integer.parseInt(title.substring(title.indexOf('#')+ 1));
        } catch (Exception e) {
            panelNumber = 0;
        }
        String command = Objects.requireNonNull(clicked.getItemMeta()).getDisplayName();
        if (command.equalsIgnoreCase(plugin.myLocale().warpsNext)) {
            player.closeInventory();
            player.performCommand("wwarps " + (panelNumber+1));
        } else if (command.equalsIgnoreCase(plugin.myLocale().warpsPrevious)) {
            player.closeInventory();
            player.performCommand("wwarps " + (panelNumber-1));
        } else {
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + plugin.myLocale().warpswarpToPlayersSign.replace("<player>", command));
            player.performCommand("wwarp " + command);
        }
    }
}
