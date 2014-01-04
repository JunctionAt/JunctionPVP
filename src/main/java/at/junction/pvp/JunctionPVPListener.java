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


class JunctionPVPListener implements Listener {
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

        //noinspection LoopStatementThatDoesntLoop
        for (Team t : plugin.teams.values()) {
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
                event.getPlayer().setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, t.getName()));
                event.getPlayer().sendMessage(t.getColor() + "Welcome to " + t.getName());
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p.hasMetadata("JunctionPVP.team")) {
                        if (p.getMetadata("JunctionPVP.team").get(0).asString().equals(t.getName())) {
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
    public void onPlayerJoin(PlayerJoinEvent event) {
        String teamName = plugin.getTeam(event.getPlayer().getName());
        if (teamName != null) {
            event.getPlayer().setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, teamName));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn()) {
            if (!plugin.isTeamRegion(plugin.teams.get(event.getPlayer().getName()), event.getRespawnLocation())) {
                event.setRespawnLocation(plugin.teams.get(event.getPlayer().getName()).getJoinLocation());
                event.getPlayer().sendMessage(ChatColor.RED + "[PVP]You can only spawn in a bed in your team's region. Back to spawn with you...");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobSpawnEvent(CreatureSpawnEvent event) {

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobDeathEvent(EntityDeathEvent event) {
        if (plugin.isPvpRegion(event.getEntity().getLocation())) {
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

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            if (plugin.isPvpRegion(event.getEntity().getLocation())) {
                Player damager = (Player) event.getDamager();
                Player entity = (Player) event.getEntity();
                if (event.getEntity().hasMetadata("JunctionPVP.team") && event.getDamager().hasMetadata("JunctionPVP.team")) {
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
}
