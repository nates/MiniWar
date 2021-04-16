package gg.nate.miniwar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class Events implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        if (Main.war.getState().equals("WAITING")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 2, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION,Integer.MAX_VALUE, 2, false, false));
            player.setGameMode(GameMode.ADVENTURE);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniWar"), () -> {
            // Teleport to spawn and set spawn point
            player.getInventory().clear();
            player.setInvulnerable(true);
            player.teleport(Main.war.getWorld().getSpawnLocation());
            player.setBedSpawnLocation(Main.war.getWorld().getSpawnLocation());
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        if (quitter.getGameMode() != GameMode.SURVIVAL || Main.war.getPlayerTeam(quitter) == 0 || Main.war.getLives(quitter) == 0) return;
        if (Main.war.getState().equals("GRACE") || Main.war.getState().equals("FIGHT")) {
            Main.war.setLives(quitter, 0);
            for (ItemStack stack : quitter.getInventory().getContents()) {
                if (stack != null) quitter.getWorld().dropItem(quitter.getLocation(), stack);
            }
            quitter.setHealth(20.0);
            quitter.getWorld().strikeLightningEffect(quitter.getLocation());
            Player lastAttacker = Main.war.getLastDamaged(quitter);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 10, 29);
                player.sendTitle(ChatColor.GRAY + "[" + ChatColor.YELLOW + "⚔" + ChatColor.GRAY + "] " + ChatColor.RED + quitter.getDisplayName(), "", 10, 60, 20);
                if (lastAttacker != null) {
                    player.sendMessage(ChatColor.RED + quitter.getDisplayName() + ChatColor.GRAY + " has been slain by " + ChatColor.RED + lastAttacker.getDisplayName() + ".");
                }
            }


            String winners = Main.war.getWinner();
            if (winners != null) {
                Main.war.endGame(winners);
            }
        }
        Main.war.removePlayer(quitter);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (Main.war.getState().equals("WAITING")) return;

        Entity attacker = event.getDamager();
        Entity defender = event.getEntity();

        // Disable friendly fire and PVP during Grace period
        if (attacker instanceof Player && defender instanceof Player) {
            // No friendly fire
            if (Main.war.getPlayerTeam(((Player) attacker)) == Main.war.getPlayerTeam(((Player) defender))) {
                event.setCancelled(true);
            } else {
                // No damage on grace period
                if (Main.war.getState().equals("GRACE")) {
                    event.setCancelled(true);
                    return;
                }
                // Set last damaged for death message
                Main.war.setLastDamaged((Player) defender, (Player) attacker);
            }
        }
    }

    @EventHandler
    public void portal(PlayerPortalEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void OnEntityDamage(EntityDamageEvent event) {
        if (Main.war.getState().equals("WAITING")) return;

        Entity entity = event.getEntity();
        // Respawn to spectator and strike lightning
        if (entity instanceof Player) {
            Player defender = (Player) entity;

            // Check if player is on a team
            if (Main.war.getPlayerTeam(defender) == 0) return;

            if (!event.isCancelled() && event.getFinalDamage() >= defender.getHealth()) {
                event.setCancelled(true);
                if (Main.war.getState().equals("FIGHT") || Main.war.getLives(defender) == 1) {
                    Main.war.setLives(defender, 0);
                    for (ItemStack stack : defender.getInventory().getContents()) {
                        if (stack != null) defender.getWorld().dropItem(defender.getLocation(), stack);
                    }
                    defender.getInventory().clear();
                    defender.setHealth(20.0);
                    for (PotionEffect effect : defender.getActivePotionEffects()) defender.removePotionEffect(effect.getType());
                    defender.setGameMode(GameMode.SPECTATOR);
                    defender.getWorld().strikeLightningEffect(defender.getLocation());
                    Player lastAttacker = Main.war.getLastDamaged(defender);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 10, 29);
                        player.sendTitle(ChatColor.GRAY + "[" + ChatColor.YELLOW + "⚔" + ChatColor.GRAY + "] " + ChatColor.RED + defender.getDisplayName(), "", 10, 60, 20);
                        if (lastAttacker == null) {
                            player.sendMessage(ChatColor.RED + defender.getDisplayName() + ChatColor.GRAY + " has died due to natural causes.");
                        } else {
                            player.sendMessage(ChatColor.RED + defender.getDisplayName() + ChatColor.GRAY + " has been slain by " + ChatColor.RED + lastAttacker.getDisplayName() + ".");
                        }
                    }
                    String winners = Main.war.getWinner();
                    if (winners != null) {
                        Main.war.endGame(winners);
                    }
                } else if (Main.war.getState().equals("GRACE")) {
                    Main.war.removeLive(defender);
                    defender.setHealth(1.0);
                    defender.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 3));
                    defender.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 2));
                    defender.sendTitle(ChatColor.GREEN + "Second Chance", ChatColor.GREEN + "You have been given a second chance!", 10, 60, 20);
                    defender.playSound(defender.getLocation(), Sound.ITEM_TOTEM_USE, 10, 29);
                }
            }
        }
    }
}