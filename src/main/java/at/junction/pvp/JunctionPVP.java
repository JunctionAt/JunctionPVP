package at.junction.pvp;


import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class JunctionPVP  extends JavaPlugin{
    public Configuration config;
    //Teams SHOULD BE teamName -> team
    public HashMap<String, Team> teams;
    private WorldGuardPlugin wg;

    @Override
    public void onEnable(){
        File file = new File(getDataFolder(), "config.yml");
        if(!file.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if ((plugin == null) || !(plugin instanceof WorldGuardPlugin)){
            getLogger().severe("Worldguard not detected. JunctionPVP not loaded");
            return;
        }
        wg = (WorldGuardPlugin) plugin;
        //Register Listener

        getServer().getPluginManager().registerEvents(new JunctionPVPListener(this), this);

        config = new Configuration(this);
        config.load();

        teams = new HashMap<String, Team>();
        for(String team : config.TEAM_NAMES){
            teams.put(team, new Team(this, team));
        }

        getLogger().info("JunctionPVP Enabled");
    }

    @Override
    public void onDisable(){
        for (Team team : teams.values())
            team.saveTeam();
        config.save();
        this.saveConfig();
        getLogger().info("JunctionPVP Disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (name.equalsIgnoreCase("teams")){
            for (Team team : teams.values()){
                sender.sendMessage(String.format("%s%s (Score: %s)%s:", team.getColor().toString(), team.getName(), team.getScore(), ChatColor.RESET.toString()));
                sender.sendMessage(team.getFormattedPlayerList());
            }
        } else if (name.equalsIgnoreCase("forceteam")){
            if (args.length < 3){
                sender.sendMessage(ChatColor.RED + "Usage: /forceteam <player> <team name>");
                return true;
            }
            Player toSwitch = getServer().getPlayer(args[0]);
            if (toSwitch == null){
                sender.sendMessage(ChatColor.RED + "This player is not online");
                return true;
            }

            String teamName = "";
            for (int i=1; i<args.length; i++){
                teamName += args[i];
                if (i != args.length-1)
                    teamName += " ";
            }
        } else if (name.equalsIgnoreCase("removeteam")){
            if (args.length != 1){
                sender.sendMessage(ChatColor.RED + "Usage: /removeteam <player>");
                return true;
            }
            //Remove player from team objects
            for (Team t : teams.values()){
                if (t.containsPlayer(args[0])){
                    try {
                        t.removePlayer(args[0]);
                        break;
                    } catch (Exception e){
                        //do nothing, won't happen
                    }
                }
            }

            //If player is online, remove metadata
            Player p = getServer().getPlayer(args[0]);
            if (p != null){
                if (p.hasMetadata("JunctionPVP.team"))
                    p.removeMetadata("JunctionPVP.team", this);
            }

            sender.sendMessage("Done");

        } else if (name.equalsIgnoreCase("printteam")){
            for (Team t : teams.values()){
                sender.sendMessage(t.toString());
            }
        } else if (name.equalsIgnoreCase("printMetadata")){
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: printMetadata playername");
                return true;
            }
            Player p = getServer().getPlayer(args[0]);
            if (p != null){
                if (p.hasMetadata("JunctionPVP.team")){
                    sender.sendMessage((String)p.getMetadata("JunctionPVP.team").get(0).value());
                }
            }
        }

        return true;
    }
    /*
    * isPvpRegion(Location location)
    * returns true if location is in pvp region
     */
    public boolean isPvpRegion(Location location) {
        ProtectedRegion pvpRegion = wg.getRegionManager(location.getWorld()).getRegion(config.PVP_REGION);
        boolean inRegion = false;
        if (pvpRegion.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())){
            inRegion = true;
            debugLogger(String.format("%s is in pvp region", location.getBlock().toString()));
        }
        return inRegion;

    }

    /*
    * isTeamRegion(Team t, Location location)
    * return true if location is inside team's region
     */
    public boolean isTeamRegion(Team t, Location location){
        ProtectedRegion teamRegion = wg.getRegionManager(location.getWorld()).getRegion(t.getRegionName());
        boolean inRegion = false;
        if (teamRegion.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())){
            inRegion = true;
            debugLogger(String.format("%s is in team %s region", location.getBlock().toString(), t.getName()));
        }
        return inRegion;
    }

    public void debugLogger(String message){
        if (!config.DEBUG) return;
        getLogger().info(message);
    }
    /*
    * Returns the team object with the lowest score
    * If its tied, returns a new blank team with a null name
     */
    public Team lowestScoreTeam(){
        int lowScore = Integer.MAX_VALUE;
        Team lowTeam = null;
        for (Team t : teams.values()){
            if (t.getScore() < lowScore){
                lowScore = t.getScore();
                lowTeam = t;
            } else if (t.getScore() == lowScore)
                return null;
        }
        debugLogger(String.format("Lowest team is %s", lowTeam == null ? "none" : lowTeam.getName()));
        return lowTeam;
    }

}
