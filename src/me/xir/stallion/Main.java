package me.xir.stallion;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

public class Main extends JavaPlugin implements Listener {

	private final Logger logger = Logger.getLogger("Minecraft");
	private final ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, Short.parseShort("100"));

	public void onEnable() {
		// Register plugin with server
		getServer().getPluginManager().registerEvents(this, this);
	}

	// Gives the player a magical stallion egg.
	private void giveEgg(Player player) {
		ItemMeta eggMeta = egg.getItemMeta();
		eggMeta.setDisplayName("Stallion Egg");
		eggMeta.setLore(new ArrayList<>(Arrays.asList("stallion_egg", player.getName())));
		egg.setItemMeta(eggMeta);
		player.getInventory().addItem(egg);
	}

	// Basically just checks if a player has "stallion_id" metadata.
	private boolean hasStallion(Player player) {
		return player.hasMetadata("stallion_id");
	}

	// Cycles through all the horses in the server and returns the one that matches the UUID.
	// Returns null if none match.
	// TODO: Figure out a more efficient way to do this.
	private Horse getStallionById(UUID stallionId) {
		for (World w : Bukkit.getServer().getWorlds()) {
			for (Horse h : w.getEntitiesByClass(Horse.class)) {
				if (h.getUniqueId().equals(stallionId)) {
					return h;
				}
			}
		}
		return null;
	}

	// Removes the horse that matches the UUID from metadata then removes the metadata from the player.
	private void rmStallion(Player player) {
		getStallionById(UUID.fromString(player.getMetadata("stallion_id").get(0).asString())).remove();
		player.removeMetadata("stallion_id", this);
	}

	// On join/respawn, kill the player's stallion if it's still alive and give a new egg.
	@EventHandler
	private void giveEggOnJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		PlayerInventory pInv = player.getInventory();

		if (hasStallion(player)) {
			rmStallion(player);
		}
		giveEgg(player);
	}
	@EventHandler
	private void giveEggOnRespawn(PlayerRespawnEvent e) {
		Player player = e.getPlayer();
		PlayerInventory pInv = player.getInventory();

		if (hasStallion(player)) {
			rmStallion(player);
		}
		giveEgg(player);
	}

	// When the player uses the egg, spawn a new black stallion and put its UUID in the player's metadata.
	// (Also murders any existing stallion the player may have because we only want on per player.)
	@EventHandler
	private void eggUseEvent(PlayerInteractEvent e) {
		if (e.getPlayer().getItemInHand().hasItemMeta()) {
			ItemMeta itemMeta = e.getPlayer().getItemInHand().getItemMeta();
			if (itemMeta.getLore().get(0).equals("stallion_egg")) {
				Player player = e.getPlayer();
				if (itemMeta.getLore().get(1).equals(player.getName())) {

					// Create new horse and make it a stallion.
					Horse stallion = (Horse)player.getLocation().getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);
					stallion.setColor(Horse.Color.BLACK);
					stallion.setVariant(Horse.Variant.HORSE);
					stallion.setStyle(Horse.Style.NONE);
					stallion.setTamed(true);
					stallion.setOwner((player));
					stallion.setCustomName(player.getName() + "'s Stallion");

					// Remove the player's stallion from the server, if it exists.
					if (player.hasMetadata("stallion_id")) {
						rmStallion(player);
					}

					// Invalidate the metadata cache and replace it with the new stallion's UUID.
					player.getMetadata("stallion_id").get(0).invalidate();
					player.setMetadata("stallion_id", new FixedMetadataValue(this, stallion.getUniqueId().toString()));

					// Cancel this event now that the stallion is spawned, and remove the egg from the player's inventory.
					e.setCancelled(true);
					player.getInventory().clear(player.getInventory().getHeldItemSlot());

				} else {
					// Yell at the player for having someone else's egg.
					player.sendMessage(ChatColor.RED + "Why are you trying to use someone else's stallion egg?");
				}
			}
		}
	}

}