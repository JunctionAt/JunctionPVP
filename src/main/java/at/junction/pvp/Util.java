package at.junction.pvp;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Collections;

class Util {
    private final JunctionPVP plugin;
    private final Map<Integer, Player> pvpCooldownMap;

    Util(JunctionPVP plugin){
        this.plugin = plugin;
        pvpCooldownMap = new HashMap<>();

    }
    /*
    * isPvpRegion(Location location)
    * returns true if location is in pvp region
     */
    boolean isPvpRegion(Location location) {
        ProtectedRegion pvpRegion = plugin.wg.getRegionManager(location.getWorld()).getRegion(plugin.config.PVP_REGION);
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
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isTeamRegion(Team t, Location location){
        ProtectedRegion teamRegion = plugin.wg.getRegionManager(location.getWorld()).getRegion(t.getRegionName());
        boolean inRegion = false;
        if (teamRegion.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())){
            inRegion = true;
            debugLogger(String.format("%s is in team %s region", location.getBlock().toString(), t.getName()));
        }
        return inRegion;
    }

    void debugLogger(String message){
        if (!plugin.config.DEBUG) return;
        plugin.getLogger().info(message);
    }
    /*
    * Returns the team object with the lowest score
    * If its tied, returns a new blank team with a null name
     */
    Team lowestScoreTeam(){
        int lowScore = Integer.MAX_VALUE;
        Team lowTeam = null;
        for (Team t : plugin.teams.values()){
            if (t.getScore() < lowScore){
                lowScore = t.getScore();
                lowTeam = t;
            } else if (t.getScore() == lowScore)
                return null;
        }
        debugLogger(String.format("Lowest team is %s", lowTeam == null ? "none" : lowTeam.getName()));
        return lowTeam;
    }
    /*
    * Returns a STRING team name for a player
     */
    String getTeamName(Player p){
        if (p.hasMetadata("JunctionPVP.team"))
            return (String)p.getMetadata("JunctionPVP.team").get(0).value();
        return null;
    }

    /*
    * Returns a TEAM object for a player
     */
    Team getTeam(Player p){
        if (p.hasMetadata("JunctionPVP.team")){
            return (plugin.teams.get(getTeamName(p)));
        }
        return null;
    }

    /*
    * Returns true if players are on the same team
     */
    boolean sameTeam(Player one, Player two){
        //noinspection SimplifiableIfStatement
        if (one.hasMetadata("JunctionPVP.team") && two.hasMetadata("JunctionPVP.team"))
            return (one.getMetadata("JunctionPVP.team").get(0).value().equals(two.getMetadata("JunctionPVP.team").get(0).value()));
        return false;
    }
    /*
    * Changes a player to a new team
     */
    void changeTeam(Player p, Team t) throws Exception{
        getTeam(p).removePlayer(p.getName());
        t.addPlayer(p.getName());
        p.removeMetadata("JunctionPVP.team", plugin);
        p.setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, t.getName()));
    }

    boolean blockEquals(Location one, Location two){
        return (one.getWorld().equals(two.getWorld()))
                && (one.getBlockX() == two.getBlockX())
                && (one.getBlockY() == two.getBlockY())
                && (one.getBlockZ() == two.getBlockZ());
    }

    void resetPvpTimer(Player... players){
        for (Player p : players){
            int key = new Random().nextInt();
            pvpCooldownMap.put(key, p);
            plugin.getServer().getScheduler().runTaskLater(plugin, new pvpCooldownRemoveTask(key, pvpCooldownMap), plugin.config.PVP_COOLDOWN_TICKS);
        }
    }
    boolean hasPvpCooldown(Player p){
        return pvpCooldownMap.values().contains(p);
    }

    void removePvpTimer(Player p){
        pvpCooldownMap.values().removeAll(Collections.singleton(p));
    }

}
