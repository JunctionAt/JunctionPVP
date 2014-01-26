package at.junction.pvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;


class Team {
    private final JunctionPVP plugin;
    private final List<Location> portalLocation;
    private final HashSet<OfflinePlayer> players;
    private org.bukkit.scoreboard.Team team;

    private final String name;
    public String getName(){
        return name;
    }

    private int score;
    public int getScore() {
        return score;
    }

    private final ChatColor color;
    public ChatColor getColor() {
        return color;
    }

    private final boolean friendlyFire;
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getFriendlyFire() {
        return friendlyFire;
    }

    private final Location joinLocation;
    public Location getJoinLocation() {
        return joinLocation;
    }

    private final Location spawnLocation;
    public Location getSpawnLocation(){
        return spawnLocation;
    }



    private final String regionName;
    public String getRegionName() {
        return regionName;
    }



    public Team(JunctionPVP plugin, String name){
        this.plugin = plugin;
        this.name = name;
        this.score = plugin.getConfig().getInt(name + ".score");
        this.color =  ChatColor.valueOf(plugin.getConfig().getString(name + ".color"));
        this.friendlyFire = plugin.getConfig().getBoolean(name + ".friendlyFire");
        this.joinLocation = getLocation(plugin.getConfig().getString(name + ".joinLocation"));
        this.spawnLocation = getLocation(plugin.getConfig().getString(name+".spawnLocation"));
        this.regionName = plugin.getConfig().getString(name + ".regionName");

        //Scoreboard init & team prefix
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        if (sb.getTeam(name) == null) {
            team = sb.registerNewTeam(name);
            team.setPrefix(color + "");
        }

        List<String> portalCoords = plugin.getConfig().getStringList(name+".portalLocation");
        portalLocation = new ArrayList<>();
        for (String coord : portalCoords){
            Location temp = getLocation(coord);
            if (temp != null)
                this.portalLocation.add(temp);
        }
        this.players = new HashSet<>();
        for (String player : plugin.getConfig().getStringList(name + ".players")){
            plugin.util.debugLogger(String.format("Adding %s to team %s", player, this.getName()));
            System.out.println(plugin.getServer().getOfflinePlayer(player));
            this.players.add(plugin.getServer().getOfflinePlayer(player));
            this.team.addPlayer(plugin.getServer().getOfflinePlayer(player));
        }
    }

    public boolean isPortalLocation(Location loc){
        for (Location l: portalLocation){
            if (plugin.util.blockEquals(l, loc)){
                return true;
            }
        }
        return false;
    }

    public boolean containsPlayer(String playerName){
        return players.contains(plugin.getServer().getOfflinePlayer(playerName));
    }

    public void addPlayer(String playerName) throws Exception{
        if (this.containsPlayer(playerName)) throw new Exception("Player is already on this team");

        players.add(plugin.getServer().getOfflinePlayer(playerName));

        plugin.getServer().getPlayer(playerName).teleport(spawnLocation);

        for (Player p : plugin.getServer().getOnlinePlayers()){
            if (this.containsPlayer(p.getName())){
                p.sendMessage(String.format("%sWelcome %s to the %s!", this.getColor(), playerName, this.getName()));
            }
        }

        plugin.getServer().getPlayer(playerName).setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, getName()));

        if (!team.hasPlayer(plugin.getServer().getOfflinePlayer(playerName))){
            team.addPlayer(plugin.getServer().getOfflinePlayer(playerName));
        }

    }

    public void removePlayer(String playerName) throws Exception{
        if (!containsPlayer(playerName)) throw new Exception("Player is not on that team");
        players.remove(plugin.getServer().getOfflinePlayer(playerName));

        for (Player p : plugin.getServer().getOnlinePlayers()){
            if (this.containsPlayer(p.getName())){
                p.sendMessage(String.format("%s%s has left the %s :(", this.getColor(), playerName, this.getName()));
            }
        }
        if (team.hasPlayer(plugin.getServer().getOfflinePlayer(playerName))){
            team.removePlayer(plugin.getServer().getPlayer(playerName));
        }
    }
    /*
    * Add single or multiple points to a team
     */
    public void addPoint(Integer... amount) {
        if (amount.length == 0) {
            plugin.util.debugLogger(String.format("%s scored a point", name));
            score++;
        } else if (amount.length == 1){
            plugin.util.debugLogger(String.format("%s scored %s points", name, amount[0].toString()));
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

    //Returns a location given world,x,y,z
    private Location getLocation(String in){
        String[] loc = in.split(",");
        if (loc.length != 4){
            return null;
        }
        return new Location(plugin.getServer().getWorld(loc[0]), Double.parseDouble(loc[1]), Double.parseDouble(loc[2]), Double.parseDouble(loc[3]));


    }

    //Prints out teamdata. Debugging stuff. Use with /printteam.
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

        data.append("spawnLocation: ").append(spawnLocation);
        data.append("\n");

        data.append("portal location: ");

        for (Location l : portalLocation){
            data.append(l.getWorld().getName()).append(l.getBlockX()).append(" ").append(l.getBlockY()).append(" ").append(l.getBlockZ()).append(" | ");
        }
        data.append("\n");

        return data.toString();
    }

    //Override equality - if team names are the same, teams are the same.
    public boolean equals(Team t){
        return t != null && name.equals(t.getName());
    }
}
