package gg.nate.miniwar;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class Game {
    private String state;
    private World world;
    private BukkitTask firstWarningTask;
    private BukkitTask secondWarningTask;
    private BukkitTask fightTask;
    private ArrayList<Player> teamOne;
    private ArrayList<Player> teamTwo;
    private ArrayList<Player> teamThree;
    private ArrayList<Player> teamFour;
    private HashMap<Player, Integer> lives;
    private HashMap<Player, Player> lastDamaged;

    public Game() {
        state = "WAITING";
        Bukkit.getLogger().info("Waiting for players...");
        teamOne = new ArrayList<Player>();
        teamTwo = new ArrayList<Player>();
        teamThree = new ArrayList<Player>();
        teamFour = new ArrayList<Player>();
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
        teamThree = new ArrayList<Player>();
        teamFour = new ArrayList<Player>();
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

        // Get spawn for third team and load chunk
        int spawnThreeY = world.getHighestBlockYAt(2475, -2475) + 2;
        Location spawnThree = new Location(world, 2475, spawnThreeY, -2475);
        spawnThree.getChunk().load(true);

        // Get spawn for fourth team and load chunk
        int spawnFourY = world.getHighestBlockYAt(-2475, 2475) + 2;
        Location spawnFour = new Location(world, -2475, spawnFourY, 2475);
        spawnFour.getChunk().load(true);

        // Spawn all players
        for (Player player : getAllPlayers()) {
            lives.put(player, 2);
            lastDamaged.put(player, null);
            switch (getPlayerTeam(player)) {
                case 1:
                    player.teleport(spawnOne);
                    break;
                case 2:
                    player.teleport(spawnTwo);
                    break;
                case 3:
                    player.teleport(spawnThree);
                    break;
                case 4:
                    player.teleport(spawnFour);
                    break;
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
        firstWarningTask = Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniWar"), () -> {
            // Teleport all players
            for (Player player : getAllPlayers()) {
                player.sendTitle(ChatColor.RED + "Warning", ChatColor.YELLOW + "The war will start in 5 minutes.", 10, 60, 20);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 10, 29);
            }
        }, 20L * 600);
        secondWarningTask = Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniWar"), () -> {
            // Teleport all players
            for (Player player : getAllPlayers()) {
                player.sendTitle(ChatColor.RED + "Warning", ChatColor.RED + "The war will start in 1 minute.", 10, 60, 20);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 10, 29);
            }
        }, 20L * 840);
        fightTask = Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniWar"), () -> {
            state = "FIGHT";
            // Get location for first team and load chunk
            int locOneY = world.getHighestBlockYAt(475, 475) + 2;
            Location locOne = new Location(world, 475, locOneY, 475);
            locOne.getChunk().load(true);

            // Get location for second team and load chunk
            int locTwoY = world.getHighestBlockYAt(-475, -475) + 2;
            Location locTwo = new Location(world, -475, locTwoY, -475);
            locTwo.getChunk().load(true);

            // Get location for third team and load chunk
            int locThreeY = world.getHighestBlockYAt(475, -475) + 2;
            Location locThree = new Location(world, 475, locThreeY, -475);
            locThree.getChunk().load(true);

            // Get location for fourth team and load chunk
            int locFourY = world.getHighestBlockYAt(-475, 475) + 2;
            Location locFour = new Location(world, -475, locFourY, 475);
            locFour.getChunk().load(true);

            // Teleport all players
            for (Player player : getAllPlayers()) {
                switch (getPlayerTeam(player)) {
                    case 1:
                        player.teleport(locOne);
                        break;
                    case 2:
                        player.teleport(locTwo);
                        break;
                    case 3:
                        player.teleport(locThree);
                        break;
                    case 4:
                        player.teleport(locFour);
                        break;
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

        firstWarningTask.cancel();
        secondWarningTask.cancel();
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

        Random rand = new Random();
        PotionEffect effect = effects.get(rand.nextInt(effects.size()));
        player.addPotionEffect(effect);
        player.sendMessage(ChatColor.GRAY + "You have received a permanent " + ChatColor.RED + effect.getType().getName().toLowerCase() + ChatColor.GRAY + " effect!");
    }

    public void giveBlessing(Player player) {
        Blessings.Blessing blessing = Blessings.getRandomBlessing();
        player.getInventory().addItem(blessing.item);
        player.sendMessage(ChatColor.GRAY + "You have received a " + blessing.color + blessing.name + ChatColor.GRAY + " blessing!");
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

    public String getWinner() {
        ArrayList<String> aliveTeams = new ArrayList<>();

        for (Player player : Main.war.getTeam("1")) {
            if (Main.war.getLives(player) != 0) {
                aliveTeams.add("1");
                break;
            }
        }

        for (Player player : Main.war.getTeam("2")) {
            if (Main.war.getLives(player) != 0) {
                aliveTeams.add("2");
                break;
            }
        }

        for (Player player : Main.war.getTeam("3")) {
            if (Main.war.getLives(player) != 0) {
                aliveTeams.add("3");
                break;
            }
        }

        for (Player player : Main.war.getTeam("4")) {
            if (Main.war.getLives(player) != 0) {
                aliveTeams.add("4");
                break;
            }
        }

        if (aliveTeams.size() != 1) return null;
        return aliveTeams.get(0);
    }

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
        if (team.equals("3")) return teamThree;
        if (team.equals("4")) return teamFour;
        return null;
    }

    public int getPlayerTeam(Player player) {
        if (teamOne.contains(player)) return 1;
        if (teamTwo.contains(player)) return 2;
        if (teamThree.contains(player)) return 3;
        if (teamFour.contains(player)) return 4;
        return 0;
    }

    public ArrayList<Player> getAllPlayers() {
        ArrayList<Player> allPlayers = new ArrayList<Player>();

        allPlayers.addAll(teamOne);
        allPlayers.addAll(teamTwo);
        allPlayers.addAll(teamThree);
        allPlayers.addAll(teamFour);

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
        if (team.equals("3")) teamThree.add(player);
        if (team.equals("4")) teamFour.add(player);
    }

    public void removePlayer(Player player) {
        if (!state.equals("WAITING")) return;

        teamOne.remove(player);
        teamTwo.remove(player);
        teamThree.remove(player);
        teamFour.remove(player);
    }

    public void shuffleTeams() {
        if (!state.equals("WAITING")) return;

        ArrayList<Player> temp = new ArrayList<Player>();

        // Loop through both teams and add them to the temp array
        temp.addAll(teamOne);
        temp.addAll(teamTwo);
        temp.addAll(teamThree);
        temp.addAll(teamFour);

        // Clear original teams
        teamOne = new ArrayList<Player>();
        teamTwo = new ArrayList<Player>();
        teamThree = new ArrayList<Player>();
        teamFour = new ArrayList<Player>();

        // Shuffle the temp array
        Collections.shuffle(temp);

        // Add players back to teams
        int cycle = 1;
        for (Player player : temp) {
            switch (cycle) {
                case 1:
                    teamOne.add(player);
                    break;
                case 2:
                    teamTwo.add(player);
                    break;
                case 3:
                    teamThree.add(player);
                    break;
                case 4:
                    teamFour.add(player);
                    break;
            }
            cycle++;
            if (cycle == 5) cycle = 1;
        }
    }

    public boolean enoughPlayers() {
        int teamsWithPlayers = 0;
        if (teamOne.size() > 0) teamsWithPlayers++;
        if (teamTwo.size() > 0) teamsWithPlayers++;
        if (teamThree.size() > 0) teamsWithPlayers++;
        if (teamFour.size() > 0) teamsWithPlayers++;

        return teamsWithPlayers >= 2;
    }
}
