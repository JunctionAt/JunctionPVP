package at.junction.pvp;



import java.util.List;

public class Configuration {
    private final JunctionPVP plugin;
    //List of team names
    public final List<String> TEAM_NAMES;
    //List of players who have used their free team join
    public final List<String> FREE_JOIN_USED;
    //War Region
    public final String PVP_REGION;

    public Configuration(JunctionPVP plugin){
        this.plugin = plugin;
        TEAM_NAMES = plugin.getConfig().getStringList("teamNames");
        FREE_JOIN_USED = plugin.getConfig().getStringList("freeJoinUsed");
        PVP_REGION = plugin.getConfig().getString("pvpRegionName");
    }
    public void load(){
        plugin.reloadConfig();
    }

    public void save(){
        plugin.getConfig().set("freeJoinUsed", FREE_JOIN_USED);
    }
}
