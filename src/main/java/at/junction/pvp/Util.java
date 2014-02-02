package at.junction.pvp;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

class Util {
    private final JunctionPVP plugin;
    private final Map<Player, BukkitTask> pvpCooldownMap = new HashMap<>();

    Util(JunctionPVP plugin){
        this.plugin = plugin;
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
    * Returns true if players are on the same team
     */
    boolean sameTeam(Player one, Player two){
        //noinspection SimplifiableIfStatement
        Team oneTeam = Team.getPlayerTeam(one);
        Team twoTeam = Team.getPlayerTeam(two);
        if (oneTeam != null && twoTeam != null)
            return oneTeam.equals(twoTeam);
        return false;
    }

    boolean blockEquals(Location one, Location two){
        return (one.getWorld().equals(two.getWorld()))
            && (one.getBlockX() == two.getBlockX())
            && (one.getBlockY() == two.getBlockY())
            && (one.getBlockZ() == two.getBlockZ());
    }

    void resetPvpTimer(Player... players){
        for (Player p : players){
            final Player player = p;
            if (pvpCooldownMap.containsKey(p)) {
                pvpCooldownMap.get(p).cancel();
            }
            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(
                plugin, new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (pvpCooldownMap.containsKey(player))
                            pvpCooldownMap.remove(player);
                    }
                }, plugin.config.PVP_COOLDOWN_TICKS);
            pvpCooldownMap.put(p, task);
        }
    }

    boolean hasPvpCooldown(Player p){
        return pvpCooldownMap.containsKey(p);
    }

    void removePvpTimer(Player p){
        if (pvpCooldownMap.containsKey(p))
            pvpCooldownMap.remove(p);
    }
    //Returns a location given world,x,y,z,pitch,yaw
    Location getLocation(String in){
        String[] loc = in.split(",");
        if (loc.length != 6){
            return null;
        }
        return new Location(plugin.getServer().getWorld(loc[0]),
                Double.parseDouble(loc[1]) + 0.5,
                Double.parseDouble(loc[2]),
                Double.parseDouble(loc[3]) + 0.5,
                Float.parseFloat(loc[4]),
                Float.parseFloat(loc[5])
        );
    }
}
