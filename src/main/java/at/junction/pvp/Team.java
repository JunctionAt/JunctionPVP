package at.junction.pvp;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;


public class Team {
    private final JunctionPVP plugin;
    private final String name;
    private final List<OfflinePlayer> players;

    private int score;

    private final ChatColor color;
    private final boolean friendlyFire;

    private final Location joinLocation;
    private final List<Location> portalLocation;

    private final String regionName;

    public Team(JunctionPVP plugin, String name){
        this.plugin = plugin;
        this.name = name;
        players = new ArrayList<OfflinePlayer>();
        for (String player : plugin.getConfig().getStringList(name + ".players")){
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

        regionName = plugin.getConfig().getString(name + ".regionName");

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

    public int getScore() {
        return score;
    }

    public String getRegionName() {
        return regionName;
    }


    public boolean isPortalLocation(Location loc){
        for (Location l: portalLocation){
            if ((l.getWorld().getName().equals(loc.getWorld().getName())) &&
                    (l.getBlockX() == loc.getBlockX()) &&
                    (l.getBlockY() == loc.getBlockY()) &&
                    (l.getBlockZ() == loc.getBlockZ())){
                plugin.debugLogger(String.format("enetered %s spawn portal", name));
                return true;
            }
        }
        return false;
    }

    public boolean containsPlayer(String playerName){
        return players.contains(getOfflinePlayer(playerName));

    }

    public void addPlayer(String playerName) throws Exception{
        if (containsPlayer(playerName)) throw new Exception("Player exists already on that team");
        players.add(getOfflinePlayer(playerName));


        plugin.getServer().getPlayer(playerName).teleport(joinLocation);

    }

    public void removePlayer(String playerName) throws Exception{
        if (!containsPlayer(playerName)) throw new Exception("Player is not on that team");
        players.remove(getOfflinePlayer(playerName));
        plugin.teams.remove(playerName);
    }
    /*
    * Add single or multiple points to a team
     */
    public void addPoint(Integer... amount) {

        if (amount.length == 0) {
            plugin.debugLogger(String.format("%s scored a point", name));
            score++;
        } else if (amount.length == 1){
            plugin.debugLogger(String.format("%s scored %s points", name, amount[0].toString()));
            score+= amount[0];
        }
    }
    /*
    * Returns list of players as 'player1, player2, player3'
     */
    public String getFormattedPlayerList(){
        StringBuilder sb = new StringBuilder();
        for (OfflinePlayer op : players){
            if (op.isOnline()){
                sb.append(op.getName());
                sb.append(", ");
            }
        }
        if (sb.length() == 0) return null;
        return sb.substring(0, sb.length()-2);
    }

    /*
    * Save team config
     */
    public void saveTeam(){
        List<String> tempPlayerList = new ArrayList<>();
        for (OfflinePlayer op : players){
            tempPlayerList.add(op.getName());
        }
        plugin.getConfig().set(name + ".players", tempPlayerList);
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

    public String toString(){
        StringBuilder data = new StringBuilder();
        data.append(color).append(name);
        data.append("\n");

        data.append("region name: ").append(regionName);
        data.append("\n");

        data.append("players: ");
        for (OfflinePlayer p : players){
            data.append(p).append(", ");
        }
        data.append("\n");

        data.append("score: ").append(score);
        data.append("\n");

        data.append("friendly fire: ").append(friendlyFire);
        data.append("\n");

        data.append("joinLocation: ").append(joinLocation);
        data.append("\n");

        data.append("portal location: ");

        for (Location l : portalLocation){
            data.append(l.getWorld().getName()).append(l.getBlockX()).append(" ").append(l.getBlockY()).append(" ").append(l.getBlockZ()).append(" | ");
        }
        data.append("\n");

        return data.toString();
    }

    public boolean equals(Team t){
        if (t == null) return false;
        return name.equals(t.getName());
    }
}
