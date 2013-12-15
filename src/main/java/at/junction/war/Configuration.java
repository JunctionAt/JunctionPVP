package at.junction.war;



import org.bukkit.ChatColor;

import java.util.List;

public class Configuration {
    private JunctionPVP plugin;

    public List<String> TEAM_NAMES;

    public Configuration(JunctionPVP plugin){
        this.plugin = plugin;
        TEAM_NAMES = plugin.getConfig().getStringList("teamNames");
    }
    public void load(){
        plugin.reloadConfig();
    }


}
