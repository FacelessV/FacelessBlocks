package bw.development.facelessBlocks.data;

import bw.development.facelessBlocks.FacelessBlocks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class MachineManager {

    private final FacelessBlocks plugin;
    private final File file;
    private FileConfiguration data;

    // EL MAPA PRINCIPAL: Ubicación -> Datos
    private final Map<Location, MachineData> machines = new HashMap<>();

    public MachineManager(FacelessBlocks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "machines.yml");
        load();
    }

    public void createMachine(Location loc) {
        if (!machines.containsKey(loc)) {
            machines.put(loc, new MachineData());
            saveAsync();
        }
    }

    public void removeMachine(Location loc) {
        if (machines.containsKey(loc)) {
            machines.remove(loc);
            saveAsync();
        }
    }

    public MachineData getMachine(Location loc) {
        return machines.get(loc);
    }

    public boolean isMachine(Location loc) {
        return machines.containsKey(loc);
    }

    public Map<Location, MachineData> getAllMachines() {
        return machines;
    }

    // --- CARGA Y GUARDADO ---

    private void load() {
        if (!file.exists()) return;
        data = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection sec = data.getConfigurationSection("machines");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            try {
                Location loc = stringToLoc(key);
                if (loc == null) continue;

                MachineData md = new MachineData();
                md.setSpeedLevel(sec.getInt(key + ".speed"));
                md.setLuckLevel(sec.getInt(key + ".luck"));
                // No cargamos "timeLeft" para que al reiniciar no se bugee el proceso a medias

                machines.put(loc, md);
            } catch (Exception e) {
                plugin.getLogger().warning("Error cargando maquina en " + key);
            }
        }
        plugin.getLogger().info("Cargadas " + machines.size() + " máquinas en memoria.");
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (data == null) data = new YamlConfiguration();
            data.set("machines", null); // Limpiar viejo

            for (Map.Entry<Location, MachineData> entry : machines.entrySet()) {
                String locStr = locToString(entry.getKey());
                String path = "machines." + locStr;
                MachineData md = entry.getValue();

                data.set(path + ".speed", md.getSpeedLevel());
                data.set(path + ".luck", md.getLuckLevel());
            }

            try {
                data.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo guardar machines.yml", e);
            }
        });
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    private Location stringToLoc(String str) {
        try {
            String[] parts = str.split("_");
            World w = Bukkit.getWorld(parts[0]);
            if (w == null) return null;
            return new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }
}