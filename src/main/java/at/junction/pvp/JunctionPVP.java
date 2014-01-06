package at.junction.pvp;


import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import java.io.File;
import java.util.HashMap;

@SuppressWarnings("WeakerAccess")
public class JunctionPVP  extends JavaPlugin{
    Configuration config;
    Util util;
    //Teams SHOULD BE teamName -> team
    HashMap<String, Team> teams;
    WorldGuardPlugin wg;
    private boolean loadError = false;

    @Override
    public void onEnable(){
        File file = new File(getDataFolder(), "config.yml");
        if(!file.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }

        //Load WorldGuard
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if ((plugin == null) || !(plugin instanceof WorldGuardPlugin)){
            getLogger().severe("Worldguard not detected. JunctionPVP unloading");
            loadError = true;
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        wg = (WorldGuardPlugin) plugin;

        //Register Listener
        getServer().getPluginManager().registerEvents(new JunctionPVPListener(this), this);

        config = new Configuration(this);
        config.load();

        util = new Util(this);

        teams = new HashMap<>();
        for(String team : config.TEAM_NAMES){
            teams.put(team, new Team(this, team));
        }

        getLogger().info("JunctionPVP Enabled");
    }

    @Override
    public void onDisable(){
        if (!loadError){
            for (Team team : teams.values())
                team.saveTeam();
            this.saveConfig();
        }
        getLogger().info("JunctionPVP Disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (name.equalsIgnoreCase("teams")){
            for (Team team : teams.values()){
                sender.sendMessage(String.format("%s%s (Score: %s)%s:", team.getColor().toString(), team.getName(), team.getScore(), ChatColor.RESET.toString()));
                sender.sendMessage(team.getFormattedPlayerList());
            }
        } else if (name.equalsIgnoreCase("team-set")){
            if (args.length < 2){
                sender.sendMessage(ChatColor.RED + "Usage: /team-set <player> <team name>");
                return true;
            }
            Player toSwitch = getServer().getPlayer(args[0]);
            if (toSwitch == null){
                sender.sendMessage(ChatColor.RED + "This player is not online");
                return true;
            }
            try {
                util.changeTeam(toSwitch, teams.get(args[1]));
                sender.sendMessage("Done");
            } catch (Exception e){
                sender.sendMessage(e.getMessage());
            }

        } else if (name.equalsIgnoreCase("team-remove")){
            String playerName;
            if (args.length >= 1){
                playerName = args[0];
            } else {
                playerName = sender.getName();
            }
            //Remove player from team objects
            //Doesn't use util.getTeam(), as it won't work on offlinePlayers
            for (Team t : teams.values()){
                if (t.containsPlayer(playerName)){
                    try {
                        t.removePlayer(playerName);
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
        } else if (name.equalsIgnoreCase("team-print")){
            for (Team t : teams.values()){
                sender.sendMessage(t.toString());
            }
        } else if (name.equalsIgnoreCase("team-addpoints")){
            int amount;
            if (args.length == 0){
                sender.sendMessage(ChatColor.RED + "Usage: /team-addpoints <team> <value>");
                return true;
            } else if (args.length == 1){
                amount = 1;
            } else {
                amount = Integer.parseInt(args[1]);
            }
            teams.get(args[0]).addPoint(amount);
            sender.sendMessage("Done");
        }

        return true;
    }
}
