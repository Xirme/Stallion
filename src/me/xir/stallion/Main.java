package me.xir.stallion;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin implements Listener {

	protected final Logger logger = Logger.getLogger("Minecraft");
	protected ItemStack egg;


	public void onEnable() {
		// Register plugin with server
		getServer().getPluginManager().registerEvents(this, this);
	}

	private void giveEgg(Player player) {
		player.getInventory().addItem(egg);
		egg = new ItemStack(Material.MONSTER_EGG, 1, Short.parseShort("100"));
		egg.setAmount(1);
		ItemMeta eggMeta = egg.getItemMeta();
		eggMeta.setDisplayName("Stallion Egg");
		egg.setItemMeta(eggMeta);
		player.getInventory().addItem(egg);
	}

	@EventHandler
	private void u_wot(PlayerLoginEvent e) {
		giveEgg(e.getPlayer());
	}

}