package at.junction.war;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;


public class Team {
    private JunctionPVP plugin;
    private String name;
    private ArrayList<OfflinePlayer> players;
    private int score;

    private ChatColor color;
    private boolean friendlyFire;

    public Team(JunctionPVP plugin, String name){
        players = new ArrayList<OfflinePlayer>();
        for (String player : plugin.getConfig().getStringList(name + ".players.")){
            players.add(getOfflinePlayer(player));
        }
        score = plugin.getConfig().getInt(name + ".score");
        color =  ChatColor.valueOf(plugin.getConfig().getString(name + ".color"));
        friendlyFire = plugin.getConfig().getBoolean(name + ".friendlyFire");
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

    public boolean containsPlayer(String playerName){
        if (players.contains(getOfflinePlayer(playerName))){
            return true;
        }
        return false;

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
            return;
        } else if (amount.length == 1){
            score+= amount[0];
            return;
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

    public void saveTeam(){
        plugin.getConfig().set(name + ".players", players);
        plugin.getConfig().set(name + ".score", score);
    }

    private OfflinePlayer getOfflinePlayer(String playerName){
        return plugin.getServer().getOfflinePlayer(playerName);
    }
}
