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
import java.util.HashSet;
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
        plugin.debugLogger(event.toString());
        //noinspection LoopStatementThatDoesntLoop
        for (Team t : new HashSet<Team>(plugin.teams.values())) {
            System.out.println(t.getName());
            if (t.isPortalLocation(event.getTo())) {
                //Check to see if the player has used their free team change
                if (plugin.config.FREE_JOIN_USED.contains(event.getPlayer().getName()))
                    return;
                try {
                    t.addPlayer(event.getPlayer().getName());
                } catch (Exception e) {
                    return;
                }

                //Add player to FREE_JOIN_USED
                plugin.config.FREE_JOIN_USED.add(event.getPlayer().getName());

                //Add metadata to player
                event.getPlayer().sendMessage(t.getColor() + "Welcome to " + t.getName());
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (plugin.teams.values().contains(p.getName())){
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
        plugin.debugLogger(event.toString());
        if (event.isBedSpawn()) {
            if (!plugin.isTeamRegion(plugin.teams.get(event.getPlayer().getName()), event.getRespawnLocation())) {
                event.setRespawnLocation(plugin.teams.get(event.getPlayer().getName()).getJoinLocation());
                event.getPlayer().sendMessage(ChatColor.RED + "[PVP]You can only spawn in a bed in your team's region. Back to spawn with you...");
            }
        }
    }
    /*
    * onMobSpawnEvent
    * Double mobs iff in pvp region
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobSpawnEvent(CreatureSpawnEvent event) {
        plugin.debugLogger(event.toString());
        if (plugin.isPvpRegion(event.getEntity().getLocation())) {
            if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
                //Set metadata so we know it wasn't spawned in a spawner (important for later)
                event.getEntity().setMetadata("junctionpvp-spawn", new FixedMetadataValue(plugin, "junctionpvp-spawn"));
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
            if (!plugin.teams.get(killer.getName()).equals(plugin.teams.get(((Player) event.getEntity()).getName()))){
                plugin.teams.get(killer.getName()).addPoint();
            }
        }
        else if (plugin.isPvpRegion(event.getEntity().getLocation())) {
            if (hostileEntities.contains(event.getEntityType())) {
                if (event.getEntity().hasMetadata("junctionpvp-spawn")) {
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
                if (plugin.teams.get(damager.getName()).equals(plugin.teams.get(entity.getName()))) {
                    if (plugin.teams.get(damager.getName()).isFriendlyFire())
                        return;
                    event.setCancelled(true);
                    damager.sendMessage("Friendly Fire is disabled!");
                }
            }
        }
    }
}

