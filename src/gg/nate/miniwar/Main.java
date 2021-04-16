package gg.nate.miniwar;

import gg.nate.miniwar.commands.MiniwarCommand;
import gg.nate.miniwar.commands.MiniwarTab;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public static Game war;

    @Override
    public void onEnable(){
        war = new Game();

        getServer().getPluginManager().registerEvents(new Events(), this);
        getServer().getPluginManager().registerEvents(new Blessings(), this);
        getCommand("miniwar").setExecutor(new MiniwarCommand());
        getCommand("miniwar").setTabCompleter(new MiniwarTab());
        Blessings.loop();
    }
}