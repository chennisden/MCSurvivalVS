package xyz.geometryexplorer.mcSurvivalVS;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class MCSurvivalVS extends JavaPlugin {
	
	public boolean deleteDirectory(File directory) {
		File[] allContents = directory.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directory.delete();
	}
	
	private Events events;
	
	public void passEvents(Events events) {
		this.events = events;
	}
	
	// wow set shit up
	
	private ArrayList<Player> survivors = new ArrayList<Player>();
	private ArrayList<Player> spectators = new ArrayList<Player>();
	
	private ItemStack compass = new ItemStack(Material.COMPASS);
	
	private Difficulty difficulty = Difficulty.EASY;
	
	private int second;
	
	BukkitRunnable borderShrink;
	
	File lobby = new File(Bukkit.getServer().getWorldContainer(), "lobby");
	
	@Override
	public void onEnable() {
		Events events = new Events();
		this.events = events;
		events.passMCVS(this);
		this.getServer().getPluginManager().registerEvents(events, this);
		
		if (!lobby.exists()) {
			WorldCreator wc = new WorldCreator("lobby");
			Bukkit.getServer().createWorld(wc);
		}
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
	
	public ArrayList<Player> getSurvivors() {
		return survivors;
	}
	
	public void removeSurvivor(Player p) {
		for (int i = 0; i < survivors.size(); i++) {
			if (survivors.get(i).equals(p)) {
				survivors.remove(i);
			}
		}
	}
	
	public ItemStack getCompass() {
		return compass;
	}
	
	// wow player state changes
	
	public void playerReset(Player p) {
		p.spigot().respawn();
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
	
	// wow commands
	
	public void resetWorld(World world, String dimension) throws IOException {
		
		for (Player p : Bukkit.getOnlinePlayers()) {
			Location loc = getServer().getWorld("lobby").getSpawnLocation();
			p.teleport(new Location(getServer().getWorld("lobby"), loc.getX(), loc.getY(), loc.getZ()));
		}
		
		Bukkit.unloadWorld(world, false);
		
		if (dimension == "overworld") {
			File overworld = new File(Bukkit.getServer().getWorldContainer(), "world");
			deleteDirectory(overworld);
			
			WorldCreator wc = new WorldCreator("world");
			Bukkit.getServer().createWorld(wc);
		} else if (dimension == "nether") {
			File nether = new File(Bukkit.getServer().getWorldContainer(), "world_nether");
			deleteDirectory(nether);
			
			WorldCreator wc = new WorldCreator("world_nether");
			wc.environment(World.Environment.NETHER);
			Bukkit.getServer().createWorld(wc);
		}
	}
	
	public void newGame() throws IOException {
		World overworld = Bukkit.getWorld("world");
		World nether = Bukkit.getWorld("world_nether");
		
//		resetWorld(overworld, "overworld");
//		resetWorld(nether, "nether");
		
		survivors.clear();
		
		for (Player p : getServer().getOnlinePlayers()) {
			survivors.add(p);
		}
		
		spectators.clear();

		// you can chill out for now cuz i need to test
		
//		if (survivors.size() <= 1) {
//			for (Player p : survivors) {
//				p.sendMessage("Get some friends before trying to play MCSurvivalVS");
//				break;
//			}
//		}
		
		for (int i = 0; i < survivors.size(); i++) {
			Player p = survivors.get(i);
			p.setGameMode(GameMode.SURVIVAL);
			playerReset(p);
			
			int mathX = (int) (100 * Math.cos(i * 2 * Math.PI / survivors.size()));
			int mathZ = (int) (100 * Math.sin(i * 2 * Math.PI / survivors.size()));
			
			int randomX = mathX + (int) (Math.random() * 11) - 5;
			int randomZ = mathZ + (int) (Math.random() * 11) - 5;
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
		
		// does this actually do anything if theres 0 ticks of delay
		
//		overworld.setDifficulty(Difficulty.PEACEFUL);
//		overworld.setDifficulty(difficulty);
		
		if (borderShrink != null) {
			borderShrink.cancel();
		}
		
		World world = Bukkit.getWorld("world");
		WorldBorder border = world.getWorldBorder();
		border.setSize(2048);
		
		borderShrink = new BukkitRunnable() {
			@Override
			public void run() {
				second++;
				
				if (second == 10*60) {
					
					for (Player p : survivors) {
						World playerWorld = p.getWorld();
						Location playerLocation = p.getLocation();
						if (Math.abs(playerLocation.getBlockX()) > 256 || Math.abs(playerLocation.getBlockZ()) > 256) {
							int newX;
							int newY;
							int newZ;
							int centerX;
							int centerZ;
							// centered at about 480 max
							if (Math.abs(playerLocation.getBlockX()) > Math.abs(playerLocation.getBlockZ())) {
								centerX = (480 * playerLocation.getBlockX()) / Math.abs(playerLocation.getBlockX());
								centerZ = (480 * playerLocation.getBlockZ()) / Math.abs(playerLocation.getBlockX());
							} else {
								centerZ = (480 * playerLocation.getBlockZ()) / Math.abs(playerLocation.getBlockZ());
								centerX = (480 * playerLocation.getBlockX()) / Math.abs(playerLocation.getBlockZ());
							}
							newX = centerX + (int) (Math.random()*32) - 16;
							newZ = centerZ + (int) (Math.random()*32) - 16;
							newY = playerWorld.getHighestBlockYAt(newX,newZ);
							
							Location newPlayerLocation = new Location(playerWorld, newX, newY, newZ);
							p.teleport(newPlayerLocation);
							
						}
						
						p.sendMessage("The border has shrunk. It will continue to do so for the next 28 minutes.");
					}
					
					border.setSize(1024);
				} else if (second > 10*60 && second <= (10 + 32) * 60 && second % 60 == 0) {
					border.setSize(1024 - 31 * (second / 60 - 10));
				}
				if (second == (10 + 32) * 60) {
					for (Player p : survivors) {
						p.sendMessage(ChatColor.RED + "The deathmatch has begun.");
						p.sendMessage(ChatColor.RED + "The border will no longer shrink.");
						p.sendMessage(ChatColor.RED + "The glowing effect has been given to every player.");
						p.sendMessage(ChatColor.DARK_RED + "Good luck.");
					}
				}
			}
		};
		
		borderShrink.runTaskTimer(this, 20, 20); // 20 is bc 20 ticks per sec
	}
	
	public void endGame() {
		// remove borders and freeze game timer and shit
		
		events.resetCooldown();
		if (borderShrink != null) {
			borderShrink.cancel();
		}
		World world = Bukkit.getWorld("world");
		WorldBorder border = world.getWorldBorder();
		border.setSize(59999968);
	}
	
	public void updateBorder(WorldBorder border) {
		if (second >= 10 * 60) {
			if (second <= (10 + 28) * 60) {
				border.setSize(512 - (second / 60 - 10) * 16);
			} else {
				border.setSize(64);
			}
		}
	}
	
	// wow cases
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		Player p = (Player) sender;
		World w = p.getWorld();
		
		switch (cmd.getName()) {
			case "newgame": {
				try {
					newGame();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
				break;
			}
			case "survivors": {
				String s = "";
				for (Player player : survivors) {
					s += player.getName() + " ";
				}
				p.sendMessage(s);
				break;
			}
			case "setgametime": {
				if (args.length == 1 && NumberUtils.isNumber(args[0])) {
					second = (int) Integer.parseInt(args[0]);
					p.sendMessage("Time set to " + args[0] + " seconds");
				} else {
					p.sendMessage("You need to pass in a time (in seconds).");
					p.sendMessage("/setgametime [seconds]");
				}
			}
			break;
		}
		
		return true;
	}
}
