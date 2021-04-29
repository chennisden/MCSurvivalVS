package xyz.geometryexplorer.mcSurvivalVS;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

public class MCSurvivalVS extends JavaPlugin implements Listener {
	
	// wow set shit up
	
	private Collection<? extends Player> survivors = new ArrayList<Player>();
	private ArrayList<Player> spectators = new ArrayList<Player>();
	
	@Override
	public void onEnable() {
		getLogger().info(getName() + " has been enabled.");
	}
	
	@Override
	public void onDisable() {
		getLogger().info(getName() + " has been disabled.");
	}
	
	// wow player state changes
	
	public void playerReset(Player p) {
		p.getInventory().clear();
		p.setExhaustion(0);
		p.setFoodLevel(20);
		p.setHealth(20);
		p.setLevel(0);
		p.setExp(0);
		for (PotionEffect effect : p.getActivePotionEffects()) {
			p.removePotionEffect(effect.getType());
		}
	}
	
	// wow listeners
	
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		
		spectators.add(player);
		
		player.setGameMode(GameMode.SPECTATOR);
	}
	
	public void onPlayerDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();
		if (spectators.indexOf(player) == -1) {
			survivors.removeIf(p -> p.equals(player));
		}
		player.setGameMode(GameMode.SPECTATOR);
		
		if (survivors.size() == 0) {
			endGame();
		}
	}
	
	// wow commands
	
	public void newGame() {
		survivors = getServer().getOnlinePlayers();
		spectators.clear();
		
		for (Player p : survivors) {
			p.setGameMode(GameMode.SURVIVAL);
			playerReset(p);
		}
		
		// set borders and shit
	}
	
	public void endGame() {
		// remove borders and freeze game timer and shit
	}
	
	// wow cases
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		switch (cmd.getName()) {
			case "newgame": {
				newGame();
			}
			case "endgame": {
				endGame();
			}
		}
		
		return true;
	}
}
