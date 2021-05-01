package xyz.geometryexplorer.mcSurvivalVS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class Events implements Listener {
	
	private MCSurvivalVS mcvs;
	
	ItemStack compass;
	ItemMeta compassMeta;
	List<String> compassLore;
	
	private Map<String, Long> timeCooldowns = new HashMap<String, Long>();
	private Map<Player, Integer> cooldowns = new HashMap<Player, Integer>();
	
	public void passMCVS(MCSurvivalVS mcvs) {
		this.mcvs = mcvs;
	}
	
	public void updateCompassInfo() {
		compass = mcvs.getCompass();
		compassMeta = compass.getItemMeta();
		compassLore = mcvs.getCompass().getItemMeta().getLore();
	}
	
	public void resetCooldown() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			cooldowns.put(p, 0);
			timeCooldowns.clear();
		}
	}
	
	public int FindCompass(Player p) {
		for (int i = 0; i < p.getInventory().getSize(); i++) {
			
			ItemStack itemAtI = p.getInventory().getItem(i);
			
			if (itemAtI != null && itemAtI.getItemMeta().getLore() != null && itemAtI.getItemMeta().getLore().equals(mcvs.getCompass().getItemMeta().getLore())) {
				return i;
			}
		}
		return -1;
	}
	
	public Location closestPlayerLocation(Player p) {
		
		if (p.getWorld().getPlayers().size() == 1) {
			return null;
		}
		
		Location closestLocation = p.getWorld().getPlayers().get(0).getLocation();
		for (Player player : p.getWorld().getPlayers()) {
			if (player == p) {
				continue;
			}
			
			double oldDistance = closestLocation.distance(p.getLocation());
			double newDistance = player.getLocation().distance(p.getLocation());
			
			if (newDistance < oldDistance) {
				closestLocation = player.getLocation();
			}
		}
		return closestLocation;
	}
	
	public void updateCompass(Player player) {
		
		int playerCounter = 0;
		
		for (Player p : mcvs.getSurvivors()) {
			if (p.getWorld().equals(player.getWorld())) {
				playerCounter++;
			}
		}
		
		if (playerCounter > 1) {
			// compass points to closest player
		} else {
			player.sendMessage("There are no other survivors in this dimension.");
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteractionEvent(PlayerInteractEvent e) { // wow compass wow
		
		updateCompassInfo();
		
		Player player = (Player) e.getPlayer();
		Action action = e.getAction();
		String compassKey = player.getName() + "-compass";
		
		if (player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getItemMeta().getLore() == null) {
			return;
		}
		
		ItemStack item = player.getInventory().getItemInMainHand();
		
		List<String> itemLore = item.getItemMeta().getLore();
		
		if (mcvs.getSurvivors().contains(player)) {
			if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
				if (compassLore.equals(itemLore)) {
					if (!timeCooldowns.containsKey(compassKey) || timeCooldowns.get(compassKey) < System.currentTimeMillis()) {
						timeCooldowns.put(player.getName() + "-compass", System.currentTimeMillis() + 30000);
						updateCompass(player);
						cooldowns.put(player,30);
						
						new BukkitRunnable() {
							
							@Override
							public void run() {
								
								compassMeta.setDisplayName(ChatColor.RED + ((Integer) cooldowns.get(player)).toString());
								
								compass.setItemMeta(compassMeta);
								
								player.getInventory().setItem(FindCompass(player), compass);
																
								cooldowns.put(player, cooldowns.get(player)-1);
								
								if (cooldowns.get(player) == 0) {
									this.cancel();
								}
							}
						}.runTaskTimer(mcvs, 0, 20);
					} else {
						player.sendMessage(ChatColor.RED + "Wait " + (timeCooldowns.get(compassKey) - System.currentTimeMillis()) / 1000 + " more seconds.");
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onItemDrop(PlayerDropItemEvent e) {
		
		updateCompassInfo();
		
		if (compassLore.equals(e.getItemDrop().getItemStack().getItemMeta().getLore())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		mcvs.addSpectator(p);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		mcvs.removeSurvivor(p);
		mcvs.removeSpectator(p);
	}
}
