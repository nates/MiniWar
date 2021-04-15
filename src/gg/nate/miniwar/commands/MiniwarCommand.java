package gg.nate.miniwar.commands;

import gg.nate.miniwar.Game;
import gg.nate.miniwar.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class MiniwarCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: miniwar [teams | start | stop]");
            return false;
        }
        switch (args[0]) {
            case "teams":
                if (!sender.hasPermission("miniwar.teams")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return false;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: miniwar teams [add | shuffle]");
                    return false;
                }
                Player player;
                switch (args[1]) {
                    case "shuffle":
                        // Shuffle teams

                        // Check game state
                        if (!Main.war.getState().equals("WAITING")) {
                            sender.sendMessage(ChatColor.RED + "Cannot shuffle teams!");
                            return false;
                        }

                        // Shuffle teams
                        Main.war.shuffleTeams();
                        sender.sendMessage(ChatColor.GRAY + "Shuffled teams!");
                        break;
                    case "add":
                        // Add player to a team

                        // Check for arguments
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Usage: miniwar teams add <player> [1 | 2 | 3 | 4]");
                            return false;
                        }

                        // Check game state
                        if (!Main.war.getState().equals("WAITING")) {
                            sender.sendMessage(ChatColor.RED + "Cannot add players!");
                            return false;
                        }

                        // Check if valid player
                        player = Bukkit.getServer().getPlayer(args[2].toLowerCase());
                        if (player == null) {
                            sender.sendMessage(ChatColor.RED + "Invalid player!");
                            return false;
                        }

                        // Check if player is already on a team
                        if (Main.war.getPlayerTeam(player) != 0) {
                            sender.sendMessage(ChatColor.RED + player.getDisplayName() + " is already on a team!");
                            return false;
                        }

                        // Check for team
                        if (!args[3].equals("1") && !args[3].equals("2") && !args[3].equals("3") && !args[3].equals("4")) {
                            sender.sendMessage(ChatColor.RED + "Usage: miniwars teams add <player> [1 | 2 | 3 | 4]");
                            return false;
                        }

                        Main.war.addPlayer(player, args[3]);
                        sender.sendMessage(ChatColor.GRAY + "Added " + player.getDisplayName() + " to team " + args[3] + "!");
                        break;
                    case "remove":
                        // Remove player from a team

                        // Check for arguments
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Usage: miniwar teams remove <player>");
                            return false;
                        }

                        // Check game state
                        if (!Main.war.getState().equals("WAITING")) {
                            sender.sendMessage(ChatColor.RED + "Cannot remove players!");
                            return false;
                        }

                        // Check if player is valid
                        player = Bukkit.getServer().getPlayer(args[2].toLowerCase());
                        if (player == null) {
                            sender.sendMessage(ChatColor.RED + "Invalid player!");
                            return false;
                        }

                        // Check if player is on a team
                        if (Main.war.getPlayerTeam(player) == 0) {
                            sender.sendMessage(ChatColor.RED + player.getDisplayName() + " is not on a team!");
                            return false;
                        }

                        Main.war.removePlayer(player);
                        sender.sendMessage(ChatColor.GRAY + "Removed " + player.getDisplayName() + "!");
                        break;
                    case "list":
                        sender.sendMessage(ChatColor.GRAY + "Team one: " + Main.war.getTeam("1").stream().map(Player::getDisplayName).collect(Collectors.joining(", ")));
                        sender.sendMessage(ChatColor.GRAY + "Team two: " + Main.war.getTeam("2").stream().map(Player::getDisplayName).collect(Collectors.joining(", ")));
                        sender.sendMessage(ChatColor.GRAY + "Team three: " + Main.war.getTeam("3").stream().map(Player::getDisplayName).collect(Collectors.joining(", ")));
                        sender.sendMessage(ChatColor.GRAY + "Team four: " + Main.war.getTeam("4").stream().map(Player::getDisplayName).collect(Collectors.joining(", ")));
                        break;
                }
                break;
            case "start":
                if (!sender.hasPermission("miniwar.start")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return false;
                }
                if (!Main.war.enoughPlayers()) {
                    sender.sendMessage(ChatColor.RED + "There must be at least 1 person on two teams!");
                    return false;
                }
                Bukkit.broadcastMessage(ChatColor.GREEN + "Starting war!");
                Main.war.start();
                break;
            case "stop":
                if (!sender.hasPermission("miniwar.stop")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return false;
                }

                // Check game state
                if (!Main.war.getState().equals("GRACE") && !Main.war.getState().equals("FIGHT")) {
                    sender.sendMessage(ChatColor.RED + "Cannot stop until war starts!");
                    return false;
                }
                Bukkit.broadcastMessage(ChatColor.GREEN + "Stopping war!");
                Main.war.stop();

                Main.war = new Game(Main.war.getWorld());
                break;
            default:
                break;
        }

        return false;
    }
}