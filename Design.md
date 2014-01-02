*JunctionPVP Design*

Teams:

    An infinite number of teams can exist.
    
    Team stores list of players, as well as methods for adding/removing members, a chat color, and a score
    
    (NYI) Players team is shown in the PvP zone using their nameplate color (TagAPI? Scoreboards?). Possibly override helmets with an item?


How to tell what team a player is on:

    OfflinePlayers are stored in the Team object, and the config.yml.

    Each player has a metadata value, JunctionPVP.team, stored at login.

    Metadata changed when the player changes teams.




PVP:

    Requires WorldGuard
    
    (NYI) Friendly Fire can be disabled/enabled per team. Only affects defined regions in friendlyFireDisabledRegions. To disable globally, set to \_\_GLOBAL\_\_.  Defaults to 'pvp'.
    

Joining Teams:

    Each player is given one free team join. Players name is stored upon using this

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
    (NYI) Only allowed to spawn via bed in the region defined

Mob Spawns:
    Double in PvP zone
    
    Double drops for mobs at 2x. Ratio out to 1x.
    
    Set Charged Creepers to Creepers (first will be charged, second won't be)
    
    Add Invincible Cow to spawn.
    
Nether:
    (NYI) Team spawn portals go back to main spawn portal & Main goes to correct team's portal

Commands:

    /teams: list teams. Perm: junctionpvp.teams.
    
    /score: list score. Perm: junctionpvp.score
    
    /forceteam <player> <newteam>: force a player to join a team. Perm: junctionpvp.forceteam
    
    /addpoint <team> \[<value>\]: adds points to a team. Defaults to 1, value is optional. Perm: junctionpvp.addpoint


