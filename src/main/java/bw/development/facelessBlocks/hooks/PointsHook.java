package bw.development.facelessBlocks.hooks;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class PointsHook {
    private static PlayerPointsAPI api = null;

    public static boolean setupPoints(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("PlayerPoints") != null) {
            api = PlayerPoints.getInstance().getAPI();
            return api != null;
        }
        return false;
    }

    public static PlayerPointsAPI getAPI() {
        return api;
    }
}