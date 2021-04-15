package gg.nate.miniwar.commands;

import gg.nate.miniwar.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MiniwarTab implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1:
                if (hasPermission(sender, "teams")) completions.add("teams");
                if (hasPermission(sender, "start")) completions.add("start");
                if (hasPermission(sender, "stop")) completions.add("stop");
                break;
            case 2:
                switch (args[0].toLowerCase()) {
                    case "teams":
                        if (hasPermission(sender, "teams")) {
                            completions.add("add");
                            completions.add("remove");
                            completions.add("shuffle");
                            completions.add("list");
                        }
                        break;
                }
                break;
            case 3:
                switch (args[0].toLowerCase()) {
                    case "teams":
                        switch (args[1].toLowerCase()) {
                            case "add":
                                for (Player player : Bukkit.getOnlinePlayers()) if (Main.war.getPlayerTeam(player) == 0) completions.add(player.getDisplayName());
                                break;
                            case "remove":
                                for (Player player : Main.war.getAllPlayers()) if (Main.war.getPlayerTeam(player) != 0) completions.add(player.getDisplayName());
                                break;
                        }
                        break;
                }
                break;
            case 4:
                switch (args[0].toLowerCase()) {
                    case "teams":
                        switch (args[1].toLowerCase()) {
                            case "add":
                                completions.add("1");
                                completions.add("2");
                                completions.add("3");
                                completions.add("4");
                        }
                        break;
                }
                break;
        }

        return completions;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission("miniwars." + permission) || sender.isOp();
    }
}
