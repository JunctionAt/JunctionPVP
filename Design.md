*JunctionPVP Design*

Teams:

    An infinite number of teams can exist.
    
    Team stores list of players, as well as methods for adding/removing members, a chat color, and a score
    
    Players team is shown in the PvP zone using their nameplate color. Uses TagAPI.


How to tell what team a player is on:

    OfflinePlayers are stored in the Team object, and the config.yml.

    Plugin contains a 'teams' map of <TeamName(String), Team(Team)>

    Player has a metadata value of "JunctionPVP.team" set. If it exists, they're on a team.


PVP:

    Requires WorldGuard flag 'pvp' to be set to allow for pvp regions.
    
    Friendly Fire can be disabled/enabled per team. Only effects PVP region (ie, arenas will work elsewhere)

Joining Teams:

    Any player without "JunctionPVP.team" metadata has not joined a team yet. As such, its free.

    First join is used by stepping into portals at spawn

    Subsequent joins require the player to place a diamond block at the team they wish to joins spawn
    
        Switching teams costs one diamond block.

        If you switch to a losing team (one that has less points), you will receive your block back.
        



PvP Timer:

    Override PvP using WorldGuard API
    
        30s PvP enabled after last PvP action

Beds:

    Only allowed to spawn via bed in the region defined

Mob Spawns:

    Double in PvP zone
    
    Double drops for mobs at 2x. Ratio out to 1x.
    
    Set Charged Creepers to Creepers (first will be charged, second won't be)
    
    Add Invincible Cow to spawn. (Gene says HI)
    
Commands:

    /teams: list teams. Perm: junctionpvp.teams.

    /team-set <player> <newteam>: force a player to join a team. Perm: junctionpvp.team.add

    /team-remove [<player>]: remove player from team. Can use on self without arguments. Perm: junction.pvp.team.remove
    
    /addpoint <team> [<value>]: adds points to a team. Defaults to 1, value is optional. Use negative numbers to deduct points if needed. Perm: junctionpvp.team.points




Code:

    Metadata:

        Players:

            JunctionPVP.team: player's team

        Mobs:

            JunctionPVP.spawn: Used in onMobSpawn event, doubles drops/exp if exists