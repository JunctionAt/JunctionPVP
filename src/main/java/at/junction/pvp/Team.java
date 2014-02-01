package at.junction.pvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;


class Team {
    private final JunctionPVP plugin;
    private final List<Location> portalLocation;
    private final HashSet<OfflinePlayer> players;
    private org.bukkit.scoreboard.Team team;

    private static HashMap<String, Team> teamNameMap = new HashMap<>();

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

    public Team(JunctionPVP plugin, String name) {
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
        } else {
            team = sb.getTeam(name);
        }
        team.setPrefix(color + "");

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

        teamNameMap.put(name, this);
    }

    public static Team getPlayerTeam(OfflinePlayer p) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team st = sb.getPlayerTeam(p);
        if (st != null)
            return teamNameMap.get(st.getName());
        else
            return null;
    }

    public static Team getPlayerTeam(String playerName) throws Exception {
        return getPlayerTeam(Bukkit.getServer().getOfflinePlayer(playerName));
    }

    public static String getPlayerTeamName(OfflinePlayer p) {
        Team t = getPlayerTeam(p);
        return (t == null) ? null : t.getName();
    }

    public static String getPlayerTeamName(String playerName) throws Exception {
        return getPlayerTeamName(Bukkit.getServer().getOfflinePlayer(playerName));
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

    public void addPlayer(OfflinePlayer player) throws Exception {
        if (this.players.contains(player)) throw new Exception("Player is already on this team");

        if (getPlayerTeam(player) != null) {
            removePlayer(player);
        }

        players.add(player);

        if (player.isOnline()) {
            player.getPlayer().teleport(spawnLocation);
        }

        for (Player p : plugin.getServer().getOnlinePlayers()){
            if (this.containsPlayer(p.getName())){
                p.sendMessage(String.format("%sWelcome %s to the %s!", this.getColor(), player.getName(), this.getName()));
            }
        }

        if (!team.hasPlayer(player))
            team.addPlayer(player);
    }

    public void addPlayer(String playerName) throws Exception {
        addPlayer(Bukkit.getServer().getOfflinePlayer(name));
    }

    public void removePlayer(OfflinePlayer player) {
        if (!players.contains(player))
            return;

        players.remove(player);

        for (Player p : plugin.getServer().getOnlinePlayers()){
            if (this.containsPlayer(p.getName())){
                p.sendMessage(String.format("%s%s has left the %s :(", this.getColor(), player.getName(), this.getName()));
            }
        }
        if (team.hasPlayer(player)){
            team.removePlayer(player);
        }
    }

    public void removePlayer(String playerName) throws Exception {
        removePlayer(Bukkit.getServer().getOfflinePlayer(name));
    }

    /*
    * Add single or multiple points to a team
     */
    public void addPoint(Integer amount) {
        if (amount == 0) {
            return;
        } else if (amount == 1) {
            plugin.util.debugLogger(String.format("%s scored a point", name));
        } else {
            plugin.util.debugLogger(String.format("%s scored %s points", name, amount.toString()));
        }
        score += amount;
    }

    public void addPoint() {
        addPoint(1);
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
