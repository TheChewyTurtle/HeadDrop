package me.rrs.headdrop.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ItemUtils {

    public void addLore(ItemStack head, List<String> rawLore, Player killer) {
        if (rawLore == null || rawLore.isEmpty()) return;

        ItemMeta itemMeta = head.getItemMeta();
        if (itemMeta == null) return;

        // Determine rarity: 1/1000 = Diamond, 1/100 = Gold, otherwise Common
        int rarityRoll = ThreadLocalRandom.current().nextInt(1, 1001);
        String rarity;
        String colorCode;

        if (rarityRoll == 1) { // 1/1000 chance for Diamond
            rarity = "Diamond";
            colorCode = "&b"; // Aqua/Diamond color
        } else if (rarityRoll <= 10) { // 10/1000 = 1/100 chance for Gold
            rarity = "Gold";
            colorCode = "&6"; // Gold color
        } else { // Common
            rarity = "Common";
            colorCode = "&7"; // Gray color
        }

        // Update display name with color code
        String currentName = itemMeta.getDisplayName();
        if (currentName == null || currentName.isEmpty()) {
            currentName = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : "Mob Head";
        }
        // Remove existing color codes and apply new one
        currentName = ChatColor.stripColor(currentName);
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', colorCode + currentName));

        List<String> finalLore = new ArrayList<>();

        // Add rarity line if not common
        if (!rarity.equals("Common")) {
            finalLore.add(ChatColor.translateAlternateColorCodes('&', colorCode + "&l" + rarity.toUpperCase()));
        }

        rawLore.forEach(lore -> {
            // Skip empty lines and {DATE} placeholder entirely
            if (!lore.equalsIgnoreCase("") && !lore.contains("{DATE}")) {
                lore = lore
                        .replace("{KILLER}", killer != null ? killer.getName() : "Unknown")
                        .replace("{WEAPON}", killer != null ? killer.getInventory().getItemInMainHand().getType().toString() : "Unknown");
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    lore = killer != null ? PlaceholderAPI.setPlaceholders(killer, lore) : PlaceholderAPI.setPlaceholders(null, lore);
                }
                finalLore.add(ChatColor.translateAlternateColorCodes('&', lore));
            }
        });

        itemMeta.setLore(finalLore);
        head.setItemMeta(itemMeta);
    }

}