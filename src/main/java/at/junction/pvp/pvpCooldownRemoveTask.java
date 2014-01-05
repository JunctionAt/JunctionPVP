package at.junction.pvp;

import org.bukkit.scheduler.BukkitRunnable;

public class pvpCooldownRemoveTask extends BukkitRunnable {

    JunctionPVPListener listener;
    int key;
    public pvpCooldownRemoveTask(JunctionPVPListener listener, int key){
        this.listener = listener;
        this.key = key;
    }

    public void run(){
        if (listener.pvpTimes.containsKey(key)){
            listener.pvpTimes.remove(key);
        }

    }
}
