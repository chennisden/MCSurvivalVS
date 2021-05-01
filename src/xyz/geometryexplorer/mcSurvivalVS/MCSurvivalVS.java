package xyz.geometryexplorer.mcSurvivalVS;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

public class MCSurvivalVS extends JavaPlugin {
	
	private Events events;
	
	public void passEvents(Events events) {
		this.events = events;
	}
	
	// wow set shit up
	
	private Collection<? extends Player> survivors = new ArrayList<Player>();
	private ArrayList<Player> spectators = new ArrayList<Player>();
	
	private ItemStack compass = new ItemStack(Material.COMPASS);
	
	private Difficulty difficulty = Difficulty.EASY;
	
	@Override
	public void onEnable() {
		Events events = new Events();
		this.events = events;
		events.passMCVS(this);
		this.getServer().getPluginManager().registerEvents(events, this);
	}
	
	@Override
	public void onDisable() {
	}
	
	// wow change data in events dot java
	
	public void addSpectator(Player p) {
		spectators.add(p);
	}
	
	public void removeSpectator(Player p) {
		for (int i = 0; i < spectators.size(); i++) {
			if (spectators.get(i).equals(p)) {
				spectators.remove(i);
			}
		}
	}
	
	public Collection<? extends Player> getSurvivors() {
		return survivors;
	}
	
	public void removeSurvivor(Player p) {
		survivors.removeIf(player -> player.equals(p));
	}
	
	public ItemStack getCompass() {
		return compass;
	}
	
	// wow player state changes
	
	public void playerReset(Player p) {
		p.getInventory().clear();
		p.setExhaustion(0);
		p.setSaturation(5);
		p.setFoodLevel(20);
		p.setHealth(20);
		p.setLevel(0);
		p.setExp(0);
		for (PotionEffect effect : p.getActivePotionEffects()) {
			p.removePotionEffect(effect.getType());
		}
		giveCompass(p);
	}
	
	public void giveCompass(Player p) {
		Inventory inv = p.getInventory();
		
		ItemMeta compassMeta = compass.getItemMeta();
		
		compassMeta.setDisplayName(ChatColor.RED + "Player Hunter");
		ArrayList<String> lore = new ArrayList<String>();
		lore.add(ChatColor.WHITE + "Right click to point towards the nearest player.");
		lore.add(ChatColor.GRAY + "(Cooldown is 30 seconds.)");
		compassMeta.setLore(lore);
		
		compass.setItemMeta(compassMeta);
		
		inv.setItem(8, compass);
		
		events.updateCompassInfo();
	}
	
	// wow listeners and event handlers
	
	public void onPlayerDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();

		removeSurvivor(player);
		
		player.setGameMode(GameMode.SPECTATOR);
		
		if (survivors.size() <= 1) {
			endGame();
		}
	}
	
	// wow commands
	
	public void newGame() {
		World overworld = Bukkit.getWorld("world");
		
		survivors = getServer().getOnlinePlayers();
		spectators.clear();

		// you can chill out for now cuz i need to test
		
//		if (survivors.size() <= 1) {
//			for (Player p : survivors) {
//				p.sendMessage("Get some friends before trying to play MCSurvivalVS");
//				break;
//			}
//		}
		
		for (Player p : survivors) {
			p.setGameMode(GameMode.SURVIVAL);
			playerReset(p);
			int randomX = (int) (Math.random() * 100);
			int randomZ = (int) (Math.random() * 100);
			int Y = overworld.getHighestBlockYAt(randomX, randomZ) + 1;
			
			Location spawn = new Location(overworld, randomX, Y, randomZ);
			p.teleport(spawn);
			p.sendMessage("Start!");
		}
		
		// set borders and shit
		
		events.resetCooldown();
		
		overworld.setFullTime(0);
		overworld.setStorm(false);
		overworld.setThundering(false);
		overworld.setDifficulty(Difficulty.PEACEFUL);
		overworld.setDifficulty(Difficulty.EASY);
	}
	
	public void endGame() {
		// remove borders and freeze game timer and shit
	}
	
	// wow cases
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		Player p = (Player) sender;
		World w = p.getWorld();
		
		switch (cmd.getName()) {
			case "newgame": {
				newGame();
				break;
			}
			case "endgame": {
				endGame();
				break;
			}
			case "setdifficulty": {
				
				boolean valid = true;
				
				if (args.length == 1) {
					switch (args[0]) {
					case "easy": {
						difficulty = Difficulty.EASY;
						break;
					}
					case "normal": {
						difficulty = Difficulty.NORMAL;
						break;
					}
					case "hard": {
						difficulty = Difficulty.HARD;
						break;
					}
					default: {
						valid = false;
					}
					}
					
					if (valid) {
						p.sendMessage("Difficulty set to " + difficulty);
					} else {
						p.sendMessage("Please choose one of the following difficulties: easy, medium, hard");
					}
					
					w.setDifficulty(difficulty);
				} else {
					p.sendMessage("You need to pass in a difficulty.");
					p.sendMessage("/setdifficulty [difficulty]");
				}
			}
			break;
		}
		
		return true;
	}
}
