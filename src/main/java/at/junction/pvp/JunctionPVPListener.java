package at.junction.pvp;


import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;


public class JunctionPVPListener implements Listener {
    private final JunctionPVP plugin;
    //Semaphore for mob spawning - if true, mob will spawn without firing EntitySpawnEvent
    private boolean _MOB_SPAWN = false;
    //Equipment/Weapon for spawned mobs
    private ItemStack[] _EQUIPMENT;
    private ItemStack _WEAPON;
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
        //Player is on a team, we ONLY care if they're going through their portal
        if (event.getPlayer().hasMetadata("JunctionPVP.team")){
            Team t = plugin.teams.get(event.getPlayer().getMetadata("JunctionPVP.team").get(0).value());
            if (t.isPortalLocation(event.getTo())){
                event.getPlayer().teleport(t.getJoinLocation());
            }
            return;
        }

        for (Team t : plugin.teams.values()) {
            if (t.isPortalLocation(event.getTo())) {
                try {
                    t.addPlayer(event.getPlayer().getName());
                } catch (Exception e) {
                    return;
                }

                //Add metadata to player
                plugin.debugLogger(String.format("Adding metadata to %s", event.getPlayer().getName()));
                event.getPlayer().setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, t.getName()));
                plugin.debugLogger(String.format("%s %s the correct metadata", event.getPlayer().getName(), event.getPlayer().hasMetadata("JunctionPVP.team") ? "has" : "doesn't have"));

                return;
            }
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
            if (!plugin.isTeamRegion(plugin.teams.get(event.getPlayer().getMetadata("JunctionPVP.team").get(0).value()), event.getRespawnLocation())) {

                event.setRespawnLocation(plugin.teams.get(event.getPlayer().getMetadata("JunctionPVP.team").get(0).value()).getJoinLocation());
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
                if (_MOB_SPAWN) {
                    event.getEntity().getEquipment().setArmorContents(_EQUIPMENT);
                    event.getEntity().getEquipment().setItemInHand(_WEAPON);
                    _EQUIPMENT = null;
                    _WEAPON = null;
                    return;
                }
                if (hostileEntities.contains(event.getEntityType())) {
                    if (event.getEntityType().equals(EntityType.CREEPER)) {
                        ((Creeper) event.getEntity()).setPowered(true);
                    }
                    _MOB_SPAWN = true;
                    _EQUIPMENT = event.getEntity().getEquipment().getArmorContents();
                    _WEAPON = event.getEntity().getEquipment().getItemInHand();
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
        if (event.getEntity() instanceof Player) {
            Player killer = event.getEntity().getKiller();
            //Not killed by player, return
            if (killer == null) return;
            //If they aren't on the same team, add a point ot the killer's team
            if (!event.getEntity().getMetadata("JunctionPVP.team").get(0).value()
                    .equals(killer.getMetadata("JunctionPVP.team").get(0).value())) {
                plugin.teams.get(killer.getMetadata("JunctionPVP.team").get(0).value()).addPoint();
            }
        } else if (plugin.isPvpRegion(event.getEntity().getLocation())) {
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
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            if (plugin.isPvpRegion(event.getEntity().getLocation())) {
                Player damager = (Player) event.getDamager();
                Player entity = (Player) event.getEntity();
                //If players are on the same team
                if (damager.getMetadata("JunctionPVP.team").get(0).value()
                        .equals(entity.getMetadata("JunctionPVP.team").get(0).value())) {
                    //return iff friendly fire is enabled
                    if (plugin.teams.get(damager.getMetadata("JunctionPVP.team").get(0).value()).isFriendlyFire())
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
        for (Team t : plugin.teams.values()) {
            if (t.containsPlayer(event.getPlayer().getName())) {
                event.getPlayer().setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, t.getName()));
                return;
            }
        }
    }

    /*
    * Used for team swapping
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType().equals(Material.DIAMOND_BLOCK)) {
            plugin.debugLogger(String.format("%s placed DIAMOND_BLOCK", event.getPlayer().getName()));
            if (!event.getPlayer().hasMetadata("JunctionPVP.team")){
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You must join a team first, go back to spawn");
                return;
            }
            for (Team t : plugin.teams.values()) {
                Location teamJoinLoc = t.getJoinLocation();
                Location blockPlacedLocation = event.getBlockPlaced().getLocation();

                if ((teamJoinLoc.getWorld().getName().equals(blockPlacedLocation.getWorld().getName())
                        && (teamJoinLoc.getBlockX() == blockPlacedLocation.getBlockX())
                        && (teamJoinLoc.getBlockY() == blockPlacedLocation.getBlockY())
                        && (teamJoinLoc.getBlockZ() == blockPlacedLocation.getBlockZ()))){
                    if (event.isCancelled()){
                        event.setCancelled(false);
                    }
                    plugin.debugLogger("Block coords match");
                    //Same Block, Swap Teams
                    if (t.containsPlayer(event.getPlayer().getName())){
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "You're already on that team!");
                    } else {
                        if (t.equals(plugin.lowestScoreTeam())){
                            //Give them their block back
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(String.format("%sThis team swap was free, as %s is losing!", t.getColor(), t.getName()));
                        }
                        //Swap Team
                        try {
                            plugin.teams.get(event.getPlayer().getMetadata("JunctionPVP.team").get(0).value()).removePlayer(event.getPlayer().getName());
                            t.addPlayer(event.getPlayer().getName());
                            event.getPlayer().removeMetadata("JunctionPVP.team", plugin);
                            event.getPlayer().setMetadata("JunctionPVP.team", new FixedMetadataValue(plugin, t.getName()));
                        } catch (Exception e){
                            //Checked earlier, shouldn't happen ever
                            plugin.getLogger().severe("You broke the universe. How could you?");
                        }


                        //Finally, remove the block (replace with air)
                        event.getBlockPlaced().setType(Material.AIR);

                    }

                }
            }
        }
    }
}

