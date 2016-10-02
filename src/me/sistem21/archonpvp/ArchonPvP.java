package me.sistem21.archonpvp;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ArchonPvP extends JavaPlugin implements Listener {
	
	private FileConfiguration config;
	private List<Player> warriors;
	private ItemStack pvpOff, pvpOn;
	private int slot;

	public void onEnable() {
		saveDefaultConfig();
		
		Bukkit.getPluginManager().registerEvents(this, this);
		
		this.config = getConfig();
		this.warriors = new ArrayList<>();
		this.pvpOn = getItemStackFromConfig("items.pvp-on");
		this.pvpOff = getItemStackFromConfig("items.pvp-off");
		this.slot = config.getInt("items.slot-on-join") - 1;
	}
	
	@EventHandler
	public void on(PlayerJoinEvent e) {
		e.getPlayer().getInventory().setItem(slot, pvpOff);
		e.getPlayer().getInventory().setHeldItemSlot(0);
	}
	
	@EventHandler
	public void on(PlayerDeathEvent e) {
		if(e.getEntity().getKiller() != null && warriors.contains(e.getEntity().getKiller()) && !config.getString("messages.death-message").isEmpty()) {
			e.setDeathMessage((ChatColor.translateAlternateColorCodes('&', config.getString("messages.death-message")
					.replaceAll("\\{killer\\}", e.getEntity().getKiller().getName())
					.replaceAll("\\{dead\\}", e.getEntity().getName()))));
			e.setDroppedExp(0);
			e.getDrops().clear();
		}
	}
	
	@EventHandler
	public void on(PlayerRespawnEvent e) {
		clearArmor(e.getPlayer());
		e.getPlayer().getInventory().setItem(slot, pvpOff);
		e.getPlayer().getInventory().setHeldItemSlot(0);
	}
	
	@EventHandler
	public void on(EntityDamageByEntityEvent  e) {
		if(e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
			if(!warriors.contains(e.getDamager()) || !warriors.contains(e.getEntity())) e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void on(InventoryClickEvent e) {
		if (e.getCurrentItem() != null && !e.getCurrentItem().getType().equals(Material.AIR) && e.getSlotType().equals(SlotType.ARMOR) || e.getSlot() == slot)
            e.setCancelled(true);
	}
	
	@EventHandler
	public void on(PlayerDropItemEvent e) {
		if(e.getItemDrop().getItemStack().getType() == Material.getMaterial(config.getString("items.pvp-on.id")) || e.getItemDrop().getItemStack().getType() == Material.getMaterial(config.getString("items.pvp-off.id")))
			e.setCancelled(true);
	}
	
	@EventHandler
	public void on(PlayerItemHeldEvent e) {
		Player p = e.getPlayer();
		PlayerInventory inv = p.getInventory();

		if (e.getNewSlot() == slot) {
			inv.setItem(slot, pvpOn);
			
			p.getItemInHand().setDurability((short) -1);
			p.setExp(0.99F);
			
			inv.setBoots(getItemStackFromConfig("items.armor.boots"));
			inv.setLeggings(getItemStackFromConfig("items.armor.leggings"));
			inv.setChestplate(getItemStackFromConfig("items.armor.chestplate"));
			inv.setHelmet(getItemStackFromConfig("items.armor.helmet"));
			
			p.updateInventory();

			if(!warriors.contains(p)) warriors.add(p);
			
			if(!config.getString("messages.pvp-enabled").isEmpty()) p.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.pvp-enabled")));
			if(!config.getString("sounds.pvp-enabled").isEmpty()) p.playSound(p.getLocation(), Sound.valueOf(config.getString("sounds.pvp-enabled")), 5F, 5F);
		} else if(e.getPreviousSlot() == slot){
			clearArmor(p);
			p.setExp(0F);
			
			inv.setItem(slot, pvpOff);
			p.updateInventory();
			
			if(warriors.contains(p)) warriors.remove(p);
			
			if(!config.getString("messages.pvp-disabled").isEmpty()) p.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.pvp-disabled")));
			if(!config.getString("sounds.pvp-disabled").isEmpty()) p.playSound(p.getLocation(), Sound.valueOf(config.getString("sounds.pvp-disabled")), 5F, 5F);
		}
	}

	public void clearArmor(Player player) {
		player.getInventory().setHelmet(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setBoots(null);
	}
	
	@SuppressWarnings("deprecation")
	private ItemStack getItemStackFromConfig(String path) {
		ItemStack is = null;
		String idString = getConfig().getString(path + ".id");
		String[] parts = idString.split(":");
		String name = parts[0];
		int amount = getConfig().contains(path + ".amount") ? getConfig().getInt(path + ".amount") : 1;
		short data = 0;
		if (parts.length == 2) {
			data = Short.parseShort(parts[1]);
		}

		is = data != 0 ? new ItemStack(Material.getMaterial(name), amount, data) : new ItemStack(Material.getMaterial(name) , amount);
		if (getConfig().contains(path + ".estetic")) {
			ItemMeta im = is.getItemMeta();
			String displayName = getConfig().getString(path + ".estetic.displayName");
			im.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
			if (getConfig().contains(path + ".estetic.lore")) {
				List<String> lore = new ArrayList<>();
				for (String s : config.getStringList(path + ".estetic.lore")) {
					lore.add(ChatColor.translateAlternateColorCodes('&', s));
				}

				im.setLore(lore);
			}

			is.setItemMeta(im);
		}
		
		if (getConfig().contains(path + ".enchants")) {
			for (String s : config.getStringList(path + ".enchants")) {
				String[] enParts = s.split(":");
				int enId = Integer.parseInt(enParts[0]);
				if (enParts.length == 2) {
					int level = Integer.valueOf(enParts[1]);
					is.addUnsafeEnchantment(Enchantment.getById(enId), level);
				} else {
					is.addUnsafeEnchantment(Enchantment.getById(enId), 1);
				}
			}
		}

		return is;
	}
}