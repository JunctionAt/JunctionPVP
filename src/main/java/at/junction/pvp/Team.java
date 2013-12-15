package at.junction.pvp;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;


class Team {
    private final JunctionPVP plugin;
    private final String name;
    private final List<OfflinePlayer> players;
    private int score;

    private final ChatColor color;
    private final boolean friendlyFire;

    private final Location joinLocation;
    private final List<Location> portalLocation;

    public Team(JunctionPVP plugin, String name){
        this.plugin = plugin;
        this.name = name;
        players = new ArrayList<OfflinePlayer>();
        for (String player : plugin.getConfig().getStringList(name + ".players.")){
            players.add(getOfflinePlayer(player));
        }
        score = plugin.getConfig().getInt(name + ".score");
        color =  ChatColor.valueOf(plugin.getConfig().getString(name + ".color"));
        friendlyFire = plugin.getConfig().getBoolean(name + ".friendlyFire");
        joinLocation = getLocation(plugin.getConfig().getString(name+".joinLocation"));
        List<String> portalCoords = plugin.getConfig().getStringList(name+".portalLocation");

        portalLocation = new ArrayList<Location>();
        for (String coord : portalCoords){
            Location temp = getLocation(coord);
            if (temp != null)
                portalLocation.add(temp);
        }

    }

    public String getName(){
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public Location getJoinLocation() {
        return joinLocation;
    }

    public boolean isPortalLocation(Location loc){
        return portalLocation.contains(loc);
    }

    public boolean containsPlayer(String playerName){
        return players.contains(getOfflinePlayer(playerName));

    }
    public void addPlayer(String playerName) throws Exception{
        if (containsPlayer(playerName)) throw new Exception("Player exists already on that team");
        players.add(getOfflinePlayer(playerName));
    }

    public void removePlayer(String playerName) throws Exception{
        if (!containsPlayer(playerName)) throw new Exception("Player is not on that team");
        players.remove(getOfflinePlayer(playerName));
    }

    public void addPoint(Integer... amount) {
        if (amount.length == 0) {
            score++;
        } else if (amount.length == 1){
            score+= amount[0];
        }
    }
    public String getFormattedPlayerList(){
        StringBuilder sb = new StringBuilder();
        for (OfflinePlayer op : players){
            if (op.isOnline()){
                sb.append(op.getName());
                sb.append(", ");
            }
        }
        return sb.substring(0, sb.length()-2);
    }

    //Saves team to config file
    public void saveTeam(){
        plugin.getConfig().set(name + ".players", players);
        plugin.getConfig().set(name + ".score", score);
    }

    //Returns an offlinePlayer from a playername
    private OfflinePlayer getOfflinePlayer(String playerName){
        return plugin.getServer().getOfflinePlayer(playerName);
    }

    //Returns a location given world,x,y,z
    private Location getLocation(String in){
        String[] loc = in.split(",");
        if (loc.length != 4){
            return null;
        }
        return new Location(plugin.getServer().getWorld(loc[0]), Double.parseDouble(loc[1]), Double.parseDouble(loc[2]), Double.parseDouble(loc[3]));


    }
}
