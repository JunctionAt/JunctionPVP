package at.junction.pvp;


import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.metadata.FixedMetadataValue;


class JunctionPVPListener implements Listener{
    private final JunctionPVP plugin;

    public JunctionPVPListener(JunctionPVP plugin){
        this.plugin = plugin;
    }

    /*
    * onPlayerMoveEvent
    * Used for initial team joins using portals
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMoveEvent(PlayerMoveEvent event){

        //noinspection LoopStatementThatDoesntLoop
        for (Team t : plugin.teams.values()){
            if (t.isPortalLocation(event.getTo())){
                //Check to see if the player has used their free team change
                if (plugin.config.FREE_JOIN_USED.contains(event.getPlayer().getName()))
                    return;
                try {
                    t.addPlayer(event.getPlayer().getName());
                } catch (Exception e){
                    return;
                }

                //Add player to FREE_JOIN_USED
                plugin.config.FREE_JOIN_USED.add(event.getPlayer().getName());

                //Add metadata to player
                event.getPlayer().setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, t.getName()));
                event.getPlayer().sendMessage(t.getColor() + "Welcome to " + t.getName());
                for (Player p : plugin.getServer().getOnlinePlayers()){
                    if (p.hasMetadata("JunctionPVP.team")){
                        if (p.getMetadata("JunctionPVP.team").get(0).asString().equals(t.getName())){
                            p.sendMessage(t.getColor() + "Please welcome " + event.getPlayer().getName() + "to your team!");
                        }
                    }
                }


                return;
            }
            return;
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event){
        String teamName = plugin.getTeam(event.getPlayer().getName());
        if (teamName != null){
            event.getPlayer().setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, teamName));
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event){
        if (event.isBedSpawn()){

        }
    }
}
