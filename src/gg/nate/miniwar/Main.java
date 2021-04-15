package gg.nate.miniwar;

import gg.nate.miniwar.commands.MiniwarCommand;
import gg.nate.miniwar.commands.MiniwarTab;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Collection;

public class Main extends JavaPlugin {
    public static Game war;

    @Override
    public void onEnable(){
        getServer().getPluginManager().registerEvents(new Events(), this);
        getCommand("miniwar").setExecutor(new MiniwarCommand());
        getCommand("miniwar").setTabCompleter(new MiniwarTab());
        war = new Game();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (war.getState().equals("GRACE") || war.getState().equals("FIGHT")) {
                    for (Player player : war.getAllPlayers()) {
                        if (player.getGameMode() != GameMode.SURVIVAL) continue;
                        // Chams Blessing
                        if (Arrays.stream(player.getInventory().getContents()).anyMatch(item -> item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Chams"))) {
                            Collection<Entity> nearbyEntities = war.getWorld().getNearbyEntities(player.getLocation(), 15, 15, 15);
                            for (Entity entity : nearbyEntities) {
                                if (entity instanceof Player) {
                                    if (war.getPlayerTeam((Player) entity) != war.getPlayerTeam(player) && ((Player) entity).getGameMode() == GameMode.SURVIVAL) {
                                        ((Player) entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 30, 0, false, false));
                                    }
                                }
                            }
                        }
                        // Doctor blessing
                        if (Arrays.stream(player.getInventory().getContents()).anyMatch(item -> item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.BLUE + "Doctor")) && player.isSneaking()) {
                            Collection<Entity> nearbyEntities = war.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2);
                            for (Entity entity : nearbyEntities) {
                                if (entity instanceof Player) {
                                    if (war.getPlayerTeam((Player) entity) == war.getPlayerTeam(player) && ((Player) entity).getGameMode() == GameMode.SURVIVAL) {
                                        ((Player) entity).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 0, false, true));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }
}