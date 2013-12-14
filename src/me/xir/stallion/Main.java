package me.xir.stallion;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public class Main extends JavaPlugin implements Listener {

	private final Logger logger = Logger.getLogger("Minecraft");
	private final ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, Short.parseShort("100"));

	public void onEnable() {
		// Register plugin with server
		getServer().getPluginManager().registerEvents(this, this);

		// Make the egg a magical stallion egg
		ItemMeta eggMeta = egg.getItemMeta();
		eggMeta.setDisplayName("Stallion Egg");
		eggMeta.setLore(new ArrayList<>(Arrays.asList("Spawns a magical stallion!")));
		egg.setItemMeta(eggMeta);
	}

	// Gives the player a magical stallion egg.
	private void giveEgg(Player player) {
		if (!player.getInventory().contains(egg)) {
			player.getInventory().addItem(egg);
		}
	}

	// Checks if any of the horses on the server are owned by the player.
	private boolean hasStallion(Player player) {
		return getStallionByOwner(player) != null;
	}

	// Cycles through all the horses in the server and returns the one who's owner matches the one specified.
	// Returns null if none match.
	// TODO: Figure out a more efficient way to do this.
	private Horse getStallionByOwner(Player owner) {
		for (World w : Bukkit.getServer().getWorlds()) {
			for (Horse h : w.getEntitiesByClass(Horse.class)) {
				if (h.getOwner() != null && h.getOwner().equals(owner)) {
					return h;
				}
			}
		}
		return null;
	}

	// Removes the horse who's owner matches the player.
	private void rmStallion(Player player) {
		Horse stallion = getStallionByOwner(player);

		if (stallion != null) {
			stallion.remove();
		}
	}

	// On join/respawn, kill the player's stallion if it's still alive and give a new egg.
	@EventHandler
	private void giveEggOnJoin(PlayerJoinEvent e) {
		final Player player = e.getPlayer();

		getServer().getScheduler().scheduleSyncDelayedTask(this, new BukkitRunnable() {
			@Override
			public void run() {
				if (player.getVehicle() instanceof Horse) {
					if (!player.getVehicle().hasMetadata("is_stallion")) {
						if (!((Horse) player.getVehicle()).getOwner().equals(player)) {
							rmStallion(player);
							giveEgg(player);

							player.sendMessage(ChatColor.YELLOW + "Your stallion has returned to its egg since you left.");
						}
					}
				} else {
					rmStallion(player);
					giveEgg(player);

					player.sendMessage(ChatColor.YELLOW + "Your stallion has returned to its egg since you left.");
				}
			}

		}, 40l);
	}
	@EventHandler
	private void giveEggOnRespawn(PlayerRespawnEvent e) {
		Player player = e.getPlayer();

		rmStallion(player);
		if (!player.getInventory().contains(egg)) {
			giveEgg(player);
			player.sendMessage(ChatColor.YELLOW + "Your stallion has returned to your inventory because you died.");
		}
	}

	// When the player uses the egg, spawn a new black stallion.
	// (Also murders any existing stallion the player may have because we only want one per player.)
	@EventHandler
	private void eggUseEvent(PlayerInteractEvent e) {
		if (e.getPlayer().getItemInHand().hasItemMeta()) {
			ItemMeta itemMeta = e.getPlayer().getItemInHand().getItemMeta();
			if (itemMeta.getLore() != null && itemMeta.getLore().get(0) != null) {
				if (itemMeta.getLore().get(0).equals("Spawns a magical stallion!")) {

					Player player = e.getPlayer();

					if (e.getClickedBlock() != null) {
						// Remove the player's stallion from the server, if it exists.
						rmStallion(player);

						// Create new horse and make it a stallion.
						Horse stallion = (Horse)player.getLocation().getWorld().spawnEntity(e.getClickedBlock().getRelative(BlockFace.UP).getLocation(), EntityType.HORSE);
						stallion.setColor(Horse.Color.BLACK);
						stallion.setVariant(Horse.Variant.HORSE);
						stallion.setStyle(Horse.Style.NONE);
						stallion.setTamed(true);
						stallion.setOwner(player);
						stallion.setCustomName(player.getName() + "'s Stallion");
						stallion.getInventory().addItem(new ItemStack(Material.SADDLE, 1));
						stallion.getInventory().addItem(new ItemStack(Material.IRON_BARDING, 1));
						stallion.setMetadata("is_stallion", new FixedMetadataValue(this, true));

						// Cancel this event now that the stallion is spawned, and remove the egg from the player's inventory.
						player.getInventory().clear(player.getInventory().getHeldItemSlot());
					}

					// Cancel the event since we don't want the egg to actually be used...
					// TODO: Well shit. It still spawns horses in some circumstances.
					e.setCancelled(true);
				}
			}
		}
	}

	// If a stallion dies, return it to its owner in the form of a magical stallion egg.
	@EventHandler
	private void stallionDeathEvent(EntityDeathEvent e) {
		if (e.getEntity().hasMetadata("is_stallion")) {
			Horse stallion = (Horse)e.getEntity();
			Player owner = (Player)stallion.getOwner();

			// Return the stallion to its owner's inventory.
			owner.getInventory().addItem(egg);
			owner.sendMessage(ChatColor.RED + "Your stallion died and has been returned to your inventory.");

			// We don't want the stallion to drop anything.
			e.setDroppedExp(0);
			e.getDrops().clear();
			stallion.getInventory().clear();
		}
	}
}