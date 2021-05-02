package xyz.geometryexplorer.mcSurvivalVS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class Events implements Listener {
	
	private MCSurvivalVS mcvs;
	
	ItemStack compass;
	ItemMeta compassMeta;
	List<String> compassLore;
	
	private Map<String, Long> timeCooldowns = new HashMap<String, Long>();
	private Map<Player, Integer> cooldowns = new HashMap<Player, Integer>();
	
	BukkitRunnable compassCooldownOnName;
	
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
			if (compassCooldownOnName != null) {
				compassCooldownOnName.cancel();
			}
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
		
		Location closestLocation = null;
		
		for (Player player : p.getWorld().getPlayers()) {
			if (player == p) {
				continue;
			}
			
			double oldDistance = 0;
			
			if (closestLocation != null) {
				oldDistance = closestLocation.distance(p.getLocation());
			}
			double newDistance = player.getLocation().distance(p.getLocation());
			
			if (closestLocation == null || newDistance < oldDistance) {
				closestLocation = player.getLocation();
			}
		}
		return closestLocation;
	}
	
	public void updateCompass(Player player, ItemStack compass) {
		
		int playerCounter = 0;
		
		for (Player p : mcvs.getSurvivors()) {
			if (p.getWorld().equals(player.getWorld())) {
				playerCounter++;
			}
		}
		
		if (playerCounter > 1) {
			// compass points to closest player
			
			if (player.getWorld().getName().equals("world")) {
				player.setCompassTarget(closestPlayerLocation(player));
				player.sendMessage(ChatColor.RED + "Compass updated");
			} else if (player.getWorld().getName().equals("world_nether")) {
				CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
				compassMeta.setLodestone(closestPlayerLocation(player));
				compassMeta.setLodestoneTracked(false);
				compass.setItemMeta(compassMeta);
				player.sendMessage(ChatColor.RED + "Compass updated");
			}
		} else {
			player.sendMessage(ChatColor.RED + "There are no other survivors in this dimension");
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteractionEvent(PlayerInteractEvent e) { // wow compass wow
		
		Player player = (Player) e.getPlayer();
		Action action = e.getAction();
		String compassKey = player.getName() + "-compass";
		
		if (mcvs.getSurvivors().contains(player)) {
			if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
		
		if (player.getInventory().getItemInMainHand().getType() == Material.AIR || player.getInventory().getItemInMainHand().getItemMeta().getLore() == null) {
			return;
		}
		
		ItemStack item = player.getInventory().getItemInMainHand();
		
		List<String> itemLore = item.getItemMeta().getLore();
		
		
				if (compassLore.equals(itemLore)) {
					if (!timeCooldowns.containsKey(compassKey) || timeCooldowns.get(compassKey) < System.currentTimeMillis()) {
						timeCooldowns.put(player.getName() + "-compass", System.currentTimeMillis() + 30000);
						updateCompass(player, item);
						cooldowns.put(player,30);
						compassMeta.setDisplayName(ChatColor.RED + ((Integer) cooldowns.get(player)).toString());
						
						compassCooldownOnName = new BukkitRunnable() {
							
							@Override
							public void run() {
								
								cooldowns.put(player, cooldowns.get(player)-1);
								
								compassMeta.setDisplayName(ChatColor.RED + ((Integer) cooldowns.get(player)).toString());
								
								item.setItemMeta(compassMeta);
								
								if (cooldowns.get(player) == 0) {
									item.setItemMeta(mcvs.getCompass().getItemMeta());
									this.cancel();
								}
							}
						};
						
						compassCooldownOnName.runTaskTimer(mcvs, 20, 20);
						
					} else {
						player.sendMessage(ChatColor.RED + "Wait " + ((timeCooldowns.get(compassKey) - System.currentTimeMillis()) + 999)/ 1000 + " more seconds.");
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
		p.setGameMode(GameMode.SPECTATOR);
		mcvs.addSpectator(p);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		mcvs.removeSurvivor(p);
		mcvs.removeSpectator(p);
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();

		List<ItemStack> drops = e.getDrops();
		Iterator<ItemStack> dropsIterator = drops.listIterator();
		
		while (dropsIterator.hasNext() ) {
			ItemStack itemStack = dropsIterator.next();
		
			if (compassLore.equals(itemStack.getItemMeta().getLore())) {
				dropsIterator.remove();
			}
		}
			
		mcvs.removeSurvivor(player);
		
		player.setGameMode(GameMode.SPECTATOR);
		
		if (mcvs.getSurvivors().size() <= 1) {
			mcvs.endGame();
		}
	}
}
