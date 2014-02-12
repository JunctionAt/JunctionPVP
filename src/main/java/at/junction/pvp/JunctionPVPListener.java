package at.junction.pvp;


import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.sk89q.worldguard.protection.events.DisallowedPVPEvent;


@SuppressWarnings("WeakerAccess")
public class JunctionPVPListener implements Listener {
    final JunctionPVP plugin;
    //Semaphore for mob spawning - if true, mob will spawn without firing EntitySpawnEvent
    private boolean _MOB_SPAWN = false;
    //Equipment/Weapon for spawned mobs
    private ItemStack[] _EQUIPMENT;
    private ItemStack _WEAPON;
    private final ArrayList<EntityType> hostileEntities = new ArrayList<>();

    public JunctionPVPListener(JunctionPVP plugin) {
        this.plugin = plugin;

        hostileEntities.add(EntityType.CREEPER);
        hostileEntities.add(EntityType.ZOMBIE);
        hostileEntities.add(EntityType.SKELETON);
        hostileEntities.add(EntityType.WITCH);
        hostileEntities.add(EntityType.SPIDER);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Team.updatePlayer(event.getPlayer());
    }

    /*
    * onPlayerMoveEvent
    * Used for initial team joins using portals
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Team playerTeam = Team.getPlayerTeam(event.getPlayer());
        for (Team t : Team.getAll()){
            if (t.isPortalLocation((event.getTo()))){
                //Player is joining a team
                if (playerTeam == null) {
                    try {
                        t.addPlayer(event.getPlayer().getName());
                    } catch (Exception e){
                        return;
                    }
                //Player is already on a team and entering a portal
                } else {
                    if (t.containsPlayer(event.getPlayer().getName())){
                        event.getPlayer().teleport(t.getSpawnLocation());
                    } else {
                        event.getPlayer().teleport(plugin.config.PLAYER_PORTAL_TP_FAIL_LOCATION);
                        event.getPlayer().sendMessage("You cannot go through another team's portal. If you are trying to switch teams, bring a diamond block back to spawn");
                    }
                }
            }
        }
    }

    /*
    * onPlayerRespawn
    * Moves players to correct location on spawn
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.isBedSpawn() || event.getPlayer().getBedSpawnLocation() == null) {
            Team team = Team.getPlayerTeam(event.getPlayer());
            if (team != null) {
                event.setRespawnLocation(team.getSpawnLocation());
            }
        }
    }

    /*
    * onPlayerEnterbed
    * Deals with players sleeping in beds
    * Cancels event if not in their teams region
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Team team = Team.getPlayerTeam(event.getPlayer());
        if (team != null) {
            if (!plugin.util.isTeamRegion(team, event.getBed().getLocation())){
                event.getPlayer().sendMessage("You can only sleep in a bed in your teams region");
                event.setCancelled(true);
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
        if (plugin.util.isPvpRegion(event.getEntity().getLocation())) {
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
            Player victim = (Player)event.getEntity();
            Player killer = event.getEntity().getKiller();
            //Not killed by player, return
            if (killer == null) return;
            //Remove player pvp cooldown on death
            plugin.util.removePvpTimer((Player)event.getEntity());
            //If they aren't on the same team, add a point ot the killer's team
            if (Team.getPlayerTeam(killer) != null && !plugin.util.sameTeam(victim, killer))
                Team.getPlayerTeam(killer).addPoint();
        } else if (plugin.util.isPvpRegion(event.getEntity().getLocation())) {
            if (hostileEntities.contains(event.getEntityType())) {
                if (event.getEntity().hasMetadata("JunctionPVP.spawn")) {
                    //Double EXP
                    event.setDroppedExp(event.getDroppedExp() * 2);
                    //Double drops
                    List<ItemStack> drops = new ArrayList<>(event.getDrops());
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
            Player damager = (Player) event.getDamager();
            Player entity = (Player) event.getEntity();
            //If both players are in the PvP region
            if (plugin.util.isPvpRegion(entity.getLocation()) && plugin.util.isPvpRegion(damager.getLocation())) {
                //If players are on the same team AND friendly fire is not enabled
                if (plugin.util.sameTeam(damager, entity) && !Team.getPlayerTeam(damager).getFriendlyFire()) {
                    event.setCancelled(true);
                    damager.sendMessage("Friendly Fire is disabled!");
                    return;
                }
                plugin.util.resetPvpTimer(damager, entity);
            }
        }
    }

    /*
    * Used for team swapping
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.DIAMOND_BLOCK){
            return;
        }
        for (Team t : Team.getAll()){
            Location teamJoinLocation = t.getJoinLocation();
            Location blockedPlacedLocation = event.getBlockPlaced().getLocation();
            if (plugin.util.blockEquals(teamJoinLocation, blockedPlacedLocation)){
                if (Team.getPlayerTeam(event.getPlayer()) == null) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "You must join a team first, go back to spawn");
                    return;
                } else {
                    if (event.isCancelled()) {
                        event.setCancelled(false);
                    }
                    //Same Block, Swap Teams
                    if (t.containsPlayer(event.getPlayer().getName())) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "You're already on that team!");
                    } else {
                        //Give them their block back, as they joined the losing team
                        if (t.equals(plugin.util.lowestScoreTeam())) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(String.format("%sThis team swap was free, as %s is losing!", t.getColor(), t.getFriendlyName()));
                        }
                        //Swap Team
                        try {
                            t.addPlayer(event.getPlayer());
                        } catch (Exception e) {
                            //Shouldn't happen, as we've checked all conditions.
                            plugin.getLogger().severe("You broke the universe. How could you?");
                            e.printStackTrace();
                        }
                        //Reset player inventory, so they see the item again
                        //noinspection deprecation
                        event.getPlayer().updateInventory();
                        //Finally, remove the block (replace with air)
                        event.getBlockPlaced().setType(Material.AIR);

                    }
                }
            }
        }
    }

    //Used for PvP Cooldowns
    @EventHandler(priority = EventPriority.MONITOR)
    private void onPVPDamage(DisallowedPVPEvent event) {
        Player defender = event.getDefender();
        Player attacker = event.getAttacker();

        if (plugin.util.hasPvpCooldown(defender)){
            if (plugin.util.sameTeam(defender, attacker)) {
                if (!Team.getPlayerTeam(defender).getFriendlyFire()) {
                    attacker.sendMessage("Friendly fire is disabled for your team!");
                    return;
                }
            }
            plugin.util.resetPvpTimer(attacker, defender);
            event.setCancelled(true);
        }
    }
}

