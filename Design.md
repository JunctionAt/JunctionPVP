*JunctionPVP Design*

Teams:

    An infinite number of teams can exist.
    
    Team stores list of players, as well as methods for adding/removing members, a chat color, and a score
    
    (NYI) Players team is shown in the PvP zone using their nameplate color. Use scoreboards.


How to tell what team a player is on:

    OfflinePlayers are stored in the Team object, and the config.yml.

    Plugin contains a 'teams' map of <TeamName(String), Team(Team)>

    Player has a metadata value of "JunctionPVP.team" set. If it exists, they're on a team.


PVP:

    Requires WorldGuard flag 'pvp' to be set to allow for pvp regions.
    
    Friendly Fire can be disabled/enabled per team. Only effects PVP region (ie, arenas will work elsehwere)

Joining Teams:

    Any player without "JunctionPVP.team" metadata has not joined a team yet. As such, its free.

    First join is used by stepping into portals at spawn

    (NYI) Subsequent joins require the player to place a diamond block at the team they wish to joins spawn
    
        (NYI) Switching teams costs one diamond block.
        
            (NYI) Using BlockPlaceEvent @ coords
            
        (NYI) If you switch to a losing team (one that has less points), you will recieve your block back.
        
            (NYI) BlockPlaceEvent.setCancelled(true)


PvP Timer:

    (NYI) Override PvP using WorldGuard API
    
        (NYI) 30s PvP disabled upon entering pvp zone
        
        (NYI) 30s PvP enabled after last PvP action and leaving zone

Beds:

    Only allowed to spawn via bed in the region defined

Mob Spawns:

    Double in PvP zone
    
    Double drops for mobs at 2x. Ratio out to 1x.
    
    Set Charged Creepers to Creepers (first will be charged, second won't be)
    
    Add Invincible Cow to spawn. (Gene says HI)
    
Commands:

    /teams: list teams. Perm: junctionpvp.teams.

    /forceteam <player> <newteam>: force a player to join a team. Perm: junctionpvp.forceteam
    
    (NYI) /addpoint <team> \[<value>\]: adds points to a team. Defaults to 1, value is optional. Perm: junctionpvp.addpoint




Code:

    Metadata:

        Players:

            JunctionPVP.team: player's team

        Mobs:

            JunctionPVP.spawn: Used in onMobSpawn event, doubles drops/exp if exists