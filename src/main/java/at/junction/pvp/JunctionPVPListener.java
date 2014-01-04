package at.junction.pvp;


import org.bukkit.ChatColor;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;


public class JunctionPVPListener implements Listener {
    private final JunctionPVP plugin;
    //Semaphore for mob spawning - if true, mob will spawn without firing EntitySpawnEvent
    private boolean _MOB_SPAWN = false;
    private ArrayList<EntityType> hostileEntities = new ArrayList<EntityType>();

    public JunctionPVPListener(JunctionPVP plugin) {
        this.plugin = plugin;

        hostileEntities.add(EntityType.CREEPER);
        hostileEntities.add(EntityType.ZOMBIE);
        hostileEntities.add(EntityType.SKELETON);
        hostileEntities.add(EntityType.WITCH);
        hostileEntities.add(EntityType.SPIDER);
    }

    /*
    * onPlayerMoveEvent
    * Used for initial team joins using portals
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        //Player is on a team, we don't care where they are
        if (event.getPlayer().hasMetadata("JunctionPVP.team")) return;

        for (Team t : plugin.teams.values()) {
            System.out.println(t.getName());
            if (t.isPortalLocation(event.getTo())) {
                try {
                    t.addPlayer(event.getPlayer().getName());
                } catch (Exception e) {
                    return;
                }

                //Add metadata to player
                event.getPlayer().sendMessage(t.getColor() + "Welcome to " + t.getName());
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p.getMetadata("JunctionPVP.team").get(0).toString().equals(t.getName())){
                        p.sendMessage(t.getColor() + "Please welcome " + event.getPlayer().getName() + "to your team!");
                    }
                }
                return;
            }
            return;
        }
    }

    /*
    * onPlayerRespawn
    * Cancel bed spawns in bad areas
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        if (event.isBedSpawn()) {
            plugin.debugLogger("Player tried to spawn in bed...");
            //If player isn't in their team region, disable bed spawns
            if (!plugin.isTeamRegion(plugin.teams.get(event.getPlayer().getMetadata("JunctionPVP.team").get(0).toString()), event.getRespawnLocation())) {
                plugin.debugLogger(String.format("sending %s to %s join location", event.getPlayer().getName(), plugin.teams.get(event.getPlayer().getName()).getName()));
                event.setRespawnLocation(plugin.teams.get(event.getPlayer().getMetadata("JunctionPVP.team").get(0).toString()).getJoinLocation());
                event.getPlayer().sendMessage("You can only spawn in a bed in your team's region. Back to your team's spawn with you...");
            }
        }
    }
    /*
    * onMobSpawnEvent
    * Double mobs iff in pvp region
    * If mob is a creeper, make it charged (first only)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobSpawnEvent(CreatureSpawnEvent event) {
        if (plugin.isPvpRegion(event.getEntity().getLocation())) {
            if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
                //Set metadata so we know it wasn't spawned in a spawner (important for later)
                event.getEntity().setMetadata("JunctionPVP.spawn", new FixedMetadataValue(plugin, "JunctionPVP.spawn"));
                if (_MOB_SPAWN) return;
                if (hostileEntities.contains(event.getEntityType())) {
                    if (event.getEntityType().equals(EntityType.CREEPER)) {
                        ((Creeper) event.getEntity()).setPowered(true);
                    }
                    _MOB_SPAWN = true;
                    event.getLocation().getWorld().spawnEntity(event.getLocation(), event.getEntityType());
                    _MOB_SPAWN = false;
                }
            }
        }
    }
    /*
    * onEntityDeathEvent
    * Add points to teams on player deaths
    * Double drops in pvp zone on death
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeathEvent(EntityDeathEvent event) {
        plugin.debugLogger(event.toString());
        if (event.getEntity() instanceof Player){

            Player killer = event.getEntity().getKiller();
            //Not killed by player, return
            if (killer == null) return;
            //If they aren't on the same team, add a point ot the killer's team
            if (!event.getEntity().getMetadata("JunctionPVP.team").toString()
                    .equals(killer.getMetadata("JunctionPVP.team").toString())){
                plugin.teams.get(killer.getMetadata("JunctionPVP.team").get(0).toString()).addPoint();
            }
        }
        else if (plugin.isPvpRegion(event.getEntity().getLocation())) {
            if (hostileEntities.contains(event.getEntityType())) {
                if (event.getEntity().hasMetadata("JunctionPVP.spawn")) {
                    //Double EXP
                    event.setDroppedExp(event.getDroppedExp() * 2);
                    //Double drops
                    List<ItemStack> drops = new ArrayList<ItemStack>(event.getDrops());
                    event.getDrops().addAll(drops);
                }
            }
        }
    }
    /*
    * onEntityDamageByEntityEvent
    * If players are on the same team and team has friendly fire disabled, cancel damage
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        plugin.debugLogger(event.toString());
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            if (plugin.isPvpRegion(event.getEntity().getLocation())) {
                Player damager = (Player) event.getDamager();
                Player entity = (Player) event.getEntity();
                //If players are on the same team
                if (damager.getMetadata("JunctionPVP.team").get(0).toString()
                        .equals(entity.getMetadata("JunctionPVP.team").get(0).toString())){
                    //return iff friendly fire is enabled
                    if (plugin.teams.get(damager.getMetadata("JunctionPVP.team").get(0).toString()).isFriendlyFire())
                        return;

                    //Cancel event - FriendlyFire is disabled, players are on the same team
                    event.setCancelled(true);
                    damager.sendMessage("Friendly Fire is disabled!");
                }
            }
        }
    }

    /*
    * Add metadata to player on join
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (Team t : plugin.teams.values()){
            if (t.containsPlayer(event.getPlayer().getName())){
                event.getPlayer().setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, t.getName()));
                return;
            }
        }
    }
}

