package me.rrs.headdrop.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class SkullCreator {

	private SkullCreator() {}

	public static ItemStack createSkull() {
		return new ItemStack(Material.PLAYER_HEAD);
	}

	public static ItemStack createSkullWithName(String name) {
		ItemStack skull = createSkull();

		// Null check - if name is null, return blank skull and log error
		if (name == null) {
			Bukkit.getLogger().severe("[HeadDrop] Failed to create player head: Player name is null!");
			return skull;
		}

		return itemWithName(skull, name);
	}

	public static ItemStack createSkullWithBase64(String base64, UUID uuid) {
		ItemStack skull = createSkull();
		return itemWithBase64(skull, base64, uuid);
	}

	public static ItemStack itemWithName(ItemStack item, String name) {
		notNull(item, "item");
		notNull(name, "name");

		// Additional null/empty check for name
		if (name.trim().isEmpty()) {
			Bukkit.getLogger().severe("[HeadDrop] Failed to create player head: Player name is empty!");
			return item;
		}

		SkullMeta meta = (SkullMeta) item.getItemMeta();
		if (meta == null) {
			Bukkit.getLogger().severe("[HeadDrop] Failed to create player head: ItemMeta is null!");
			return item;
		}

		try {
			meta.setOwningPlayer(Bukkit.getOfflinePlayer(name));
			item.setItemMeta(meta);
		} catch (Exception e) {
			Bukkit.getLogger().severe("[HeadDrop] Failed to set player head owner for player '" + name + "': " + e.getMessage());
			e.printStackTrace();
		}

		return item;
	}

	private static ItemStack itemWithBase64(ItemStack item, String base64, UUID uuid) {
		notNull(item, "item");
		notNull(base64, "base64");

		if (!(item.getItemMeta() instanceof SkullMeta meta)) {
			return null;
		}

		try {
			String json = new String(Base64.getDecoder().decode(base64));

			JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
			String textureUrl = jsonObject.getAsJsonObject("textures")
					.getAsJsonObject("SKIN")
					.get("url").getAsString();

			PlayerProfile profile = Bukkit.createProfile(uuid);
			PlayerTextures textures = profile.getTextures();
			textures.setSkin(new URL(textureUrl));
			profile.setTextures(textures);

			meta.setOwnerProfile(profile);
			item.setItemMeta(meta);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return item;
	}

	private static void notNull(Object o, String name) {
		if (o == null) {
			throw new NullPointerException(name + " should not be null!");
		}
	}
}