package bw.development.facelessBlocks.data;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class Keys {

    public static NamespacedKey MACHINE_ID;
    public static NamespacedKey UPGRADE_SPEED;
    public static NamespacedKey UPGRADE_LUCK;
    public static NamespacedKey PROCESS_TIME;
    public static NamespacedKey IS_PROCESSING;

    // --- TIENES QUE TENER ESTO DEFINIDO AQUI ---
    public static NamespacedKey OUTPUT_MATERIAL;

    public static void load(JavaPlugin plugin) {
        MACHINE_ID = new NamespacedKey(plugin, "machine_id");
        UPGRADE_SPEED = new NamespacedKey(plugin, "upgrade_speed");
        UPGRADE_LUCK = new NamespacedKey(plugin, "upgrade_luck");
        PROCESS_TIME = new NamespacedKey(plugin, "process_time");
        IS_PROCESSING = new NamespacedKey(plugin, "is_processing");

        // --- Y TIENES QUE CARGARLO AQUI ABAJO ---
        OUTPUT_MATERIAL = new NamespacedKey(plugin, "output_material"); // <--- SI FALTA ESTO, CRASHEA
    }
}