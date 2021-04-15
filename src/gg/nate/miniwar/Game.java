package gg.nate.miniwar;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class Game {
    private String state;
    private World world;
    private BukkitTask warningTask;
    private BukkitTask fightTask;
    private ArrayList<Player> teamOne;
    private ArrayList<Player> teamTwo;
    private HashMap<Player, Integer> lives;
    private HashMap<Player, Player> lastDamaged;

    public Game() {
        state = "WAITING";
        Bukkit.getLogger().info("Waiting for players...");
        teamOne = new ArrayList<Player>();
        teamTwo = new ArrayList<Player>();
        lives = new HashMap<>();
        lastDamaged = new HashMap<>();
        for (World w : Bukkit.getWorlds()) if (w.getEnvironment().equals(World.Environment.NORMAL)) world = w;
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(50);
    }

    public Game(World w) {
        state = "WAITING";
        Bukkit.getLogger().info("Waiting for players...");
        teamOne = new ArrayList<Player>();
        teamTwo = new ArrayList<Player>();
        lives = new HashMap<>();
        lastDamaged = new HashMap<>();
        world = w;
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(50);
    }

    public void endGame(String winners) {
        state = "OVER";
        Bukkit.getLogger().info("Game over.");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(ChatColor.RED + "War Over", ChatColor.RED + "Winners: " +  String.join(", ", getTeam(winners).stream().map(Player::getDisplayName).collect(Collectors.toList())), 10, 60, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 10, 29);
            p.setGameMode(GameMode.SPECTATOR);
        }
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniWar"), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) p.setGameMode(GameMode.ADVENTURE);
            stop();
            Main.war = new Game(world);
        }, 20L * 15);
    }

    public void start() {
        state = "STARTING";
        Bukkit.getLogger().info("Starting war...");
        // Set people with no team to spectators
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 10, 29);
            if (getPlayerTeam(player) == 0) player.setGameMode(GameMode.SPECTATOR);
        }

        // Set world borders
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.getWorldBorder().setCenter(0.0, 0.0);
        world.getWorldBorder().setSize(5000);

        // Get spawn for first team and load chunk
        int spawnOneY = world.getHighestBlockYAt(2475, 2475) + 2;
        Location spawnOne = new Location(world, 2475, spawnOneY, 2475);
        spawnOne.getChunk().load(true);

        // Get spawn for second team and load chunk
        int spawnTwoY = world.getHighestBlockYAt(-2475, -2475) + 2;
        Location spawnTwo = new Location(world, -2475, spawnTwoY, -2475);
        spawnTwo.getChunk().load(true);

        // Spawn all players
        for (Player player : getAllPlayers()) {
            lives.put(player, 2);
            lastDamaged.put(player, null);
            if (getPlayerTeam(player) == 1) {
                player.teleport(spawnOne);
            } else if (getPlayerTeam(player) == 2) {
                player.teleport(spawnTwo);
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setInvulnerable(false);
            for(PotionEffect effect:player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
            giveStartingItems(player);

            if (Math.random() > 0.66) {
                int random = (int) (Math.random() * 3) + 1;
                switch (random) {
                    case 1:
                        giveSpecialItem(player);
                        break;
                    case 2:
                        giveEffect(player);
                        break;
                    case 3:
                        giveBlessing(player);
                        break;
                }
            }
            player.sendTitle(ChatColor.GREEN + "Game Started", ChatColor.GREEN + "You have 15 minutes to prepare.", 10, 60, 20);
        }
        state = "GRACE";
        Bukkit.getLogger().info("Grace period started.");
        warningTask = Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniWar"), () -> {
            // Teleport all players
            for (Player player : getAllPlayers()) {
                player.sendTitle(ChatColor.RED + "Warning", ChatColor.RED + "The war will start in 1 minute.", 10, 60, 20);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 10, 29);
            }
        }, 20L * 840);
        fightTask = Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniWar"), () -> {
            state = "FIGHT";
            // Location for first team
            Location locOne = new Location(world, 475, 256, 475);
            while(locOne.getBlock().isEmpty()) locOne.add(0, -1, 0);
            locOne.add(0, 1, 0);

            // Location for second team
            Location locTwo = new Location(world, -475, 256, -475);
            while(locTwo.getBlock().isEmpty()) locTwo.add(0, -1, 0);
            locTwo.add(0, 1, 0);

            // Teleport all players
            for (Player player : getAllPlayers()) {
                if (getPlayerTeam(player) == 1) {
                    player.teleport(locOne);
                } else if (getPlayerTeam(player) == 2) {
                    player.teleport(locTwo);
                }
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 10, 29);
                player.sendTitle(ChatColor.RED + "War", ChatColor.RED + "The war has started.", 10, 60, 20);
            }

            Bukkit.getLogger().info("War period started.");

            world.setTime(0);
            world.getWorldBorder().setSize(1000);
            world.getWorldBorder().setSize(5, 600);
        }, 20L * 900);
    }

    public void stop() {
        state = "STOPPING";

        Bukkit.getLogger().info("Stopping war...");

        Bukkit.getServer().setWhitelist(true);

        Location spawnpoint = world.getSpawnLocation();
        // Teleport to spawn and reset all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
            player.setHealth(20.0);
            world.getWorldBorder().setCenter(spawnpoint.getX(), spawnpoint.getY());
            world.getWorldBorder().setSize(50);
            player.teleport(spawnpoint);
            player.setGameMode(GameMode.ADVENTURE);
            player.kickPlayer(ChatColor.GREEN + "Creating world, please join momentarily.");
        }

        warningTask.cancel();
        fightTask.cancel();

        String worldName = System.currentTimeMillis() / 1000L + "";
        WorldCreator creator = new WorldCreator(worldName).environment(World.Environment.NORMAL);
        world = creator.createWorld();

        for (World w : Bukkit.getWorlds()) if (w.getEnvironment().equals(World.Environment.NORMAL)) {
            if (!w.getName().equals(worldName)) {
                Bukkit.getServer().unloadWorld(w, true);
                Utils.deleteWorld(w.getWorldFolder());
            }
        }

        Bukkit.getLogger().info("Created new world: " + worldName);

        Bukkit.getServer().setWhitelist(false);
    }

    public void giveStartingItems(Player player){
        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD, 1), new ItemStack(Material.STONE_PICKAXE, 1), new ItemStack(Material.STONE_SHOVEL, 1), new ItemStack(Material.STONE_AXE, 1), new ItemStack(Material.TORCH, 16), new ItemStack(Material.COBBLESTONE, 64), new ItemStack(Material.COOKED_BEEF, 16));
    }

    public void giveEffect(Player player) {
        ArrayList<PotionEffect> effects = new ArrayList<>();
        effects.add(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        effects.add(new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 0, false, false));
        effects.add(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, false, false));

        Random rand = new Random();
        PotionEffect effect = effects.get(rand.nextInt(effects.size()));
        player.addPotionEffect(effect);
        player.sendMessage(ChatColor.GRAY + "You were lucky and received a permanent " + ChatColor.RED + effect.getType().getName().toLowerCase() + ChatColor.GRAY + " effect!");
    }

    public void giveBlessing(Player player) {
        ArrayList<ItemStack> blessings = new ArrayList<>();

        // Chams Blessing
        List<String> chamsLore = new ArrayList<>();
        chamsLore.add(ChatColor.GRAY + "This blessing will outline enemies");
        chamsLore.add(ChatColor.GRAY + "within a short radius.");
        blessings.add(createBlessing(ChatColor.YELLOW + "Chams", chamsLore));

        // Doctor Blessing
        List<String> doctorLore = new ArrayList<>();
        doctorLore.add(ChatColor.GRAY + "This blessing will heal nearby teammates");
        doctorLore.add(ChatColor.GRAY + "within a short radius while sneaking.");
        blessings.add(createBlessing(ChatColor.BLUE + "Doctor", doctorLore));

        // Pyromaniac Blessing
        List<String> pyromaniacLore = new ArrayList<>();
        pyromaniacLore.add(ChatColor.GRAY + "This blessing will give you immunity");
        pyromaniacLore.add(ChatColor.GRAY + "to fire & lava.");
        blessings.add(createBlessing(ChatColor.RED + "Pyromaniac", pyromaniacLore));

        // Jelly Legs Blessing
        List<String> jellyLegs = new ArrayList<>();
        jellyLegs.add(ChatColor.GRAY + "This blessing will give you immunity");
        jellyLegs.add(ChatColor.GRAY + "to fall damage.");
        blessings.add(createBlessing(ChatColor.AQUA + "Jelly Legs", jellyLegs));

        // Frog Blessing
        List<String> frogLore = new ArrayList<>();
        frogLore.add(ChatColor.GRAY + "This blessing will give you a jump");
        frogLore.add(ChatColor.GRAY + "boost.");
        frogLore.add(ChatColor.GRAY + "Toggle by right clicking in your hand.");
        blessings.add(createBlessing(ChatColor.GREEN + "Frog", frogLore));

        Random rand = new Random();
        ItemStack blessing = blessings.get(rand.nextInt(blessings.size()));
        player.getInventory().addItem(blessing);
        player.sendMessage(ChatColor.GRAY + "You were lucky and received " + ChatColor.RED + blessing.getItemMeta().getDisplayName() + ChatColor.GRAY + "!");
    }

    public ItemStack createBlessing(String name, List<String> lore) {
        ItemStack blessing = new ItemStack(Material.ENCHANTED_BOOK, 1);
        ItemMeta meta = blessing.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        blessing.setItemMeta(meta);

        return blessing;
    }

    public void giveSpecialItem(Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        // Regular items
        items.add(new ItemStack(Material.ENDER_PEARL));
        items.add(new ItemStack(Material.GOLDEN_APPLE));
        items.add(new ItemStack(Material.LEATHER_CHESTPLATE));

        // Enchanted items
        ItemStack efficiencyPick = new ItemStack(Material.STONE_PICKAXE);
        efficiencyPick.addEnchantment(Enchantment.DIG_SPEED, 1);
        items.add(efficiencyPick);

        ItemStack unbreakingBoots = new ItemStack(Material.IRON_BOOTS);
        unbreakingBoots.addEnchantment(Enchantment.DURABILITY, 1);
        items.add(unbreakingBoots);

        ItemStack protHelm = new ItemStack(Material.CHAINMAIL_HELMET);
        protHelm.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        items.add(protHelm);

        ItemStack unbreakingLeggings = new ItemStack(Material.CHAINMAIL_LEGGINGS);
        unbreakingLeggings.addEnchantment(Enchantment.DURABILITY, 1);
        items.add(unbreakingLeggings);

        Random rand = new Random();
        player.getInventory().addItem(items.get(rand.nextInt(items.size())));
    }

    // Getters, Setters, Adders, Removers, etc.

    public String getState() {
        return state;
    }

    public World getWorld() {
        return world;
    }

    public int getLives(Player player) {
        return lives.get(player);
    }

    public Player getLastDamaged(Player player) {
        return lastDamaged.get(player);
    }

    public ArrayList<Player> getTeam(String team) {
        if (team.equals("1")) return teamOne;
        if (team.equals("2")) return teamTwo;
        return null;
    }

    public int getPlayerTeam(Player player) {
        if (teamOne.contains(player)) return 1;
        if (teamTwo.contains(player)) return 2;
        return 0;
    }

    public ArrayList<Player> getAllPlayers() {
        ArrayList<Player> allPlayers = new ArrayList<Player>();

        allPlayers.addAll(teamOne);
        allPlayers.addAll(teamTwo);

        return allPlayers;
    }

    public void setLastDamaged(Player defender, Player attacker) {
        lastDamaged.replace(defender, attacker);
    }

    public void setLives(Player player, int amount) {
        lives.replace(player, amount);
    }

    public void removeLive(Player player) {
        int value = lives.get(player);
        lives.replace(player, value - 1);
    }

    public void addPlayer(Player player, String team) {
        if (!state.equals("WAITING")) return;

        if (team.equals("1")) teamOne.add(player);
        if (team.equals("2")) teamTwo.add(player);
    }

    public void removePlayer(Player player) {
        if (!state.equals("WAITING")) return;

        teamOne.remove(player);
        teamTwo.remove(player);
    }

    public void shuffleTeams() {
        if (!state.equals("WAITING")) return;

        ArrayList<Player> temp = new ArrayList<Player>();

        // Loop through both teams and add them to the temp array
        temp.addAll(teamOne);
        temp.addAll(teamTwo);

        // Clear original teams
        teamOne = new ArrayList<Player>();
        teamTwo = new ArrayList<Player>();

        // Shuffle the temp array
        Collections.shuffle(temp);

        // Add players back to teams
        boolean toggle = true;
        for (Player player : temp) {
            if (toggle) {
                teamOne.add(player);
            } else {
                teamTwo.add(player);
            }
            toggle = !toggle;
        }
    }
}
