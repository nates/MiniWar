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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;


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
        if (Main.war.getPlayerTeam(quitter) == 0) return;
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
            if (Main.war.getPlayerTeam(((Player) attacker)) == Main.war.getPlayerTeam(((Player) defender))) {
                event.setCancelled(true);
            } else {
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
    public void OnEntityDamage(EntityDamageEvent event) {
        if (Main.war.getState().equals("WAITING")) return;

        Entity entity = event.getEntity();
        // Respawn to spectator and strike lightning
        if (entity instanceof Player) {
            Player defender = (Player) entity;

            // Check if player is on a team
            if (Main.war.getPlayerTeam(defender) == 0) return;

            // Pyromaniac blessing
            if (Arrays.stream(defender.getInventory().getContents()).anyMatch(item -> item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.RED + "Pyromaniac"))) {
                ArrayList<EntityDamageEvent.DamageCause> fireCauses = new ArrayList<>();
                fireCauses.add(EntityDamageEvent.DamageCause.FIRE);
                fireCauses.add(EntityDamageEvent.DamageCause.FIRE_TICK);
                fireCauses.add(EntityDamageEvent.DamageCause.LAVA);
                if (fireCauses.contains(event.getCause())) event.setCancelled(true);
            }
                if (Arrays.stream(defender.getInventory().getContents()).anyMatch(item -> item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Jelly Legs"))) {
                    if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                        event.setCancelled(true);
                        defender.sendMessage(ChatColor.GRAY + "Damage neglected by " + ChatColor.AQUA + "Jelly Legs");
                    }
                }


            if (event.getFinalDamage() >= defender.getHealth() && !event.isCancelled()) {
                if (Main.war.getState().equals("FIGHT") || Main.war.getLives(defender) == 1) {

                    event.setCancelled(true);
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
                    event.setCancelled(true);
                    defender.setHealth(1.0);
                    defender.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 3));
                    defender.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 2));
                    defender.sendTitle(ChatColor.GREEN + "Second Chance", ChatColor.GREEN + "You have been given a second chance!", 10, 60, 20);
                    defender.playSound(defender.getLocation(), Sound.ITEM_TOTEM_USE, 10, 29);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Frog blessing
        if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Frog")) {
            if (player.hasPotionEffect(PotionEffectType.JUMP)) {
                player.removePotionEffect(PotionEffectType.JUMP);
                player.sendMessage(ChatColor.GREEN + "Frog" + ChatColor.GRAY + " has been toggled off!");
            } else {
                PotionEffect jumpEffect = new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, false, false);
                player.addPotionEffect(jumpEffect);
                player.sendMessage(ChatColor.GREEN + "Frog" + ChatColor.GRAY + " has been toggled on!");
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        // Check for dropping frog blessing while jump effect active
        if (droppedItem.hasItemMeta() && droppedItem.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Frog")) {
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniWar"), () -> {
                if (player.hasPotionEffect(PotionEffectType.JUMP) && !Arrays.stream(player.getInventory().getContents()).anyMatch(item -> item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Frog"))) {
                    player.removePotionEffect(PotionEffectType.JUMP);
                    player.sendMessage(ChatColor.GREEN + "Frog" + ChatColor.GRAY + " has been toggled off!");
                }
            }, 5L);
        }
    }
}