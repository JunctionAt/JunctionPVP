package at.junction.pvp;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class pvpCooldownRemoveTask extends BukkitRunnable {

    int key;
    Map<Integer, Player> cooldownMap;
    public pvpCooldownRemoveTask(int key, Map<Integer, Player> cooldownMap){
        this.key = key;
        this.cooldownMap = cooldownMap;
    }

    public void run(){
        if (cooldownMap.containsKey(key)){
            cooldownMap.remove(key);
        }
    }
}
