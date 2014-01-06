package at.junction.pvp;



import java.util.List;

public class Configuration {
    private final JunctionPVP plugin;
    //List of team names
    public final List<String> TEAM_NAMES;

    //War Region
    public final String PVP_REGION;
    public final boolean DEBUG;

    public final int PVP_COOLDOWN_TICKS;

    public Configuration(JunctionPVP plugin){
        this.plugin = plugin;
        TEAM_NAMES = plugin.getConfig().getStringList("teamNames");
        PVP_REGION = plugin.getConfig().getString("pvpRegionName", "pvp");
        DEBUG = plugin.getConfig().getBoolean("debug", false);
        PVP_COOLDOWN_TICKS = plugin.getConfig().getInt("pvpCooldown", 30) * 20;
    }
    public void load(){
        plugin.reloadConfig();
    }

    public void save(){
    }
}
