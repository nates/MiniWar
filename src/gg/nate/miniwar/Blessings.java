package gg.nate.miniwar;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

public class Blessings implements Listener {
    public enum Blessing {
        PYROMANIAC(
                "Pyromaniac",
                ChatColor.GRAY + "This blessing will give you+" + ChatColor.GRAY + "immunity to fire & lava.",
                ChatColor.RED,
                new String[]{"1  ", " 22", " 20"},
                Material.LAVA_BUCKET,
                Material.FLINT_AND_STEEL,
                Material.PAPER
        ),
        DOCTOR(
                "Doctor",
                ChatColor.BLUE + "This blessing will heal nearby teammates+" + ChatColor.GRAY + "within a short radius while sneaking.",
                ChatColor.YELLOW,
                new String[]{"   ", " 11", " 10"},
                Material.GOLDEN_APPLE,
                Material.PAPER
        ),
        CHAMS(
                "Chams",
                ChatColor.GRAY + "This blessing will outline enemies+" + ChatColor.GRAY + "within a short radius.",
                ChatColor.DARK_GREEN,
                new String[]{"   ", " 11", " 10"},
                Material.ENDER_PEARL,
                Material.PAPER
        ),
        JELLYLEGS(
                "Jellylegs",
                ChatColor.GRAY + "This blessing will give you immunity+" + ChatColor.GRAY + "to fall damage.",
                ChatColor.GREEN,
                new String[]{"1  ", " 22", " 20"},
                Material.WATER_BUCKET,
                Material.EMERALD,
                Material.PAPER
        ),
        RABBIT(
                "Rabbit",
                ChatColor.GRAY + "This blessing will give you a jump boost.+" + ChatColor.GRAY + "Toggle by right clicking in your hand.",
                ChatColor.DARK_GREEN,
                new String[]{"   ", " 11", " 10"},
                Material.RABBIT_FOOT,
                Material.PAPER
        );

        public String name;
        public ChatColor color;
        public ItemStack item;

        Blessing(String name, String lore, ChatColor color, String[] shape, Material... materials) {
            this.name = name;
            this.color = color;
            this.item = createItem(color + name, lore);

            NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("MiniWar"), "blessing_" + this.name.toLowerCase());
            ShapedRecipe blessingRecipe = new ShapedRecipe(key, this.item);
            blessingRecipe.shape(shape);
            for (int i = 0; i < materials.length; i++) {
                blessingRecipe.setIngredient((char) (i + '0'), materials[i]);
            }

            Bukkit.getServer().addRecipe(blessingRecipe);
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return color + name;
        }

        public ChatColor getColor() {
            return color;
        }

        public ItemStack getItem() {
            return item;
        }

        private static ItemStack createItem(String name, String lore) {
            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore.split("\\+", -1)));
            item.setItemMeta(meta);

            return item;
        }
    }

    public static boolean hasBlessing(Player player, Blessing blessing) {
        return Arrays.stream(player.getInventory().getContents()).anyMatch(item -> item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(blessing.getDisplayName()));
    }

    public static Blessing getRandomBlessing() {
        Random rand = new Random();
        Blessing[] blessings = Blessing.values();
        return blessings[rand.nextInt(blessings.length)];
    }

    private static boolean checkWar() {
        return Main.war.getState().equals("GRACE") || Main.war.getState().equals("FIGHT");
    }

    private static boolean checkPlayer(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL && Main.war.getPlayerTeam(player) != 0 && Main.war.getLives(player) > 0;
    }

    public static void loop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!checkWar()) return;

                for (Player player : Main.war.getAllPlayers()) {
                    if (!checkPlayer(player)) continue;

                    // Chams
                    if (hasBlessing(player, Blessing.CHAMS)) {
                        Collection<Entity> nearbyEntities = Main.war.getWorld().getNearbyEntities(player.getLocation(), 15, 15, 15);
                        for (Entity entity : nearbyEntities) {
                            if (entity instanceof Player) {
                                if (Main.war.getPlayerTeam((Player) entity) != Main.war.getPlayerTeam(player) && ((Player) entity).getGameMode() == GameMode.SURVIVAL) {
                                    ((Player) entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 30, 0, false, false));
                                }
                            }
                        }
                    }
                    // Doctor
                    if (hasBlessing(player, Blessing.DOCTOR) && player.isSneaking()) {
                        Collection<Entity> nearbyEntities = Main.war.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2);
                        for (Entity entity : nearbyEntities) {
                            if (entity instanceof Player) {
                                if (Main.war.getPlayerTeam((Player) entity) == Main.war.getPlayerTeam(player) && ((Player) entity).getGameMode() == GameMode.SURVIVAL) {
                                    ((Player) entity).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 1, false, false));
                                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0,1.5,0), 100);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("MiniWar"), 0L, 20L);
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void OnEntityDamage(EntityDamageEvent event) {
        if (!checkWar()) return;

        Entity entity = event.getEntity();
        // Respawn to spectator and strike lightning
        if (entity instanceof Player) {
            Player player = (Player) entity;

            if (!checkPlayer(player)) return;

            // Pyromaniac Blessing
            if (hasBlessing(player, Blessing.PYROMANIAC)) {
                ArrayList<EntityDamageEvent.DamageCause> fireCauses = new ArrayList<>();
                fireCauses.add(EntityDamageEvent.DamageCause.FIRE);
                fireCauses.add(EntityDamageEvent.DamageCause.FIRE_TICK);
                fireCauses.add(EntityDamageEvent.DamageCause.LAVA);
                if (fireCauses.contains(event.getCause())) event.setCancelled(true);
            }

            // JellyLegs Blessing
            if (hasBlessing(player, Blessing.JELLYLEGS)) {
                if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.GRAY + "Damage neglected by " + Blessing.JELLYLEGS.getDisplayName());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!checkWar()) return;

        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (!checkPlayer(player)) return;

        // Check for dropping rabbit blessing while jump effect active
        if (droppedItem.hasItemMeta() && droppedItem.getItemMeta().getDisplayName().equals(Blessing.RABBIT.getDisplayName())) {
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniWar"), () -> {
                if (player.hasPotionEffect(PotionEffectType.JUMP) && !hasBlessing(player, Blessing.RABBIT)) {
                    player.removePotionEffect(PotionEffectType.JUMP);
                    player.sendMessage(Blessing.RABBIT.getDisplayName() + ChatColor.GRAY + " has been toggled off!");
                    player.playSound(player.getLocation(), Sound.BLOCK_STONE_PRESSURE_PLATE_CLICK_OFF, 8, 25);
                }
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!checkWar()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!checkPlayer(player)) return;

        // Rabbit blessing
        if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(Blessing.RABBIT.getDisplayName())) {
            if (player.hasPotionEffect(PotionEffectType.JUMP)) {
                player.removePotionEffect(PotionEffectType.JUMP);
                player.sendMessage(Blessing.RABBIT.getDisplayName() + ChatColor.GRAY + " has been toggled off!");
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_PRESSURE_PLATE_CLICK_OFF, 8, 25);
            } else {
                PotionEffect jumpEffect = new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, false, false);
                player.addPotionEffect(jumpEffect);
                player.sendMessage(Blessing.RABBIT.getDisplayName() + ChatColor.GRAY + " has been toggled on!");
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON, 8, 25);
            }
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 100);
        }
    }
}
