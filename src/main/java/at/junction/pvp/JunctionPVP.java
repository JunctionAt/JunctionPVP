package at.junction.pvp;


import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import java.io.File;
import org.bukkit.OfflinePlayer;

@SuppressWarnings("WeakerAccess")
public class JunctionPVP  extends JavaPlugin{
    Configuration config;
    final Util util = new Util(this);
    //Teams SHOULD BE teamName -> team
    WorldGuardPlugin wg;

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
            this.setEnabled(false);
            return;
        }
        wg = (WorldGuardPlugin) plugin;

        //Register Listener
        getServer().getPluginManager().registerEvents(new JunctionPVPListener(this), this);

        config = new Configuration(this);
        config.load();

        for(String team : config.TEAM_NAMES){
            Team.create(this, team);
        }

        getLogger().info("JunctionPVP Enabled");
    }

    @Override
    public void onDisable(){
        config.load(); //reload config before we save it, so we don't lose data if anything changed

        this.saveConfig();

        getLogger().info("JunctionPVP Disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (name.equalsIgnoreCase("teams")) {
            for (Team team : Team.getAll()) {
                sender.sendMessage(String.format("%s%s (Score: %s)%s:", team.getColor().toString(), team.getFriendlyName(), team.getScore(), ChatColor.RESET.toString()));
                sender.sendMessage(team.getFormattedPlayerList());
            }
        } else if (name.equalsIgnoreCase("team-set")) {
            if (args.length < 2){
                sender.sendMessage(ChatColor.RED + "Usage: /team-set <player> <team name>");
                return true;
            }
            Player toSwitch = getServer().getPlayer(args[0]);
            if (toSwitch == null) {
                sender.sendMessage(ChatColor.RED + "This player is not online");
                return true;
            }
            try {
                Team.get(args[1]).addPlayer(toSwitch);
                sender.sendMessage("Done");
            } catch (Exception e) {
                sender.sendMessage(e.getMessage());
            }

        } else if (name.equalsIgnoreCase("team-remove")) {
            String playerName;
            if (args.length >= 1) {
                playerName = args[0];
            } else {
                playerName = sender.getName();
            }
            //Remove player from team objects
            //Doesn't use util.getTeam(), as it won't work on offlinePlayers
            OfflinePlayer op = getServer().getOfflinePlayer(playerName);
            Team.getPlayerTeam(op).removePlayer(op);
            sender.sendMessage("Done");
        } else if (name.equalsIgnoreCase("team-print")) {
            for (Team t : Team.getAll()) {
                sender.sendMessage(t.toString());
            }
        } else if (name.equalsIgnoreCase("team-addpoints")) {
            int amount;
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Usage: /team-addpoints <team> <value>");
                return true;
            } else if (args.length == 1) {
                amount = 1;
            } else {
                amount = Integer.parseInt(args[1]);
            }
            Team.get(args[0]).addPoint(amount);
            sender.sendMessage("Done");
        } else if (name.equalsIgnoreCase("player-printmetadata")){
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /player-printmetadata <player>");
                return true;
            }
            Player player = getServer().getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player doesn't exist");
                return true;
            }

            if (player.hasMetadata("JunctionPVP.team")) {
                sender.sendMessage("Team: " + player.getMetadata("JunctionPVP.team").get(0).value());
            } else {
                sender.sendMessage("Team metadata missing");
            }
        } else if (name.equalsIgnoreCase("pvp")){
            if (!(sender instanceof Player)){
                sender.sendMessage(ChatColor.RED + "This command is only usable by players");
            } else {
                Player player = (Player)sender;
                if (player.hasMetadata("pvp")){
                    //Remove pvp metadata
                    player.removeMetadata("pvp", this);
                } else {
                    //Add pvp metadata
                    player.setMetadata("pvp", new FixedMetadataValue(this, "on"));
                }
            }
        }

        return true;
    }
}
