package bw.development.facelessBlocks.data;

import bw.development.facelessBlocks.FacelessBlocks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class MachineManager {

    private final FacelessBlocks plugin;
    private final File file;
    private FileConfiguration data;

    // Aquí guardamos las ubicaciones en memoria RAM para acceso ultra-rápido
    private final Set<Location> recyclers = new HashSet<>();

    public MachineManager(FacelessBlocks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "machines.yml");
        load();
    }

    public void addRecycler(Location loc) {
        recyclers.add(loc);
        saveAsync(); // Guardamos en disco sin congelar el servidor
    }

    public void removeRecycler(Location loc) {
        recyclers.remove(loc);
        saveAsync();
    }

    public boolean isRecycler(Location loc) {
        return recyclers.contains(loc);
    }

    public Set<Location> getRecyclers() {
        return recyclers;
    }

    // --- Lógica de Archivos ---

    private void load() {
        if (!file.exists()) return;
        data = YamlConfiguration.loadConfiguration(file);

        // Cargar lista desde la config
        if (data.contains("recyclers")) {
            for (String locStr : data.getStringList("recyclers")) {
                Location loc = stringToLoc(locStr);
                if (loc != null) recyclers.add(loc);
            }
        }
        plugin.getLogger().info("Cargadas " + recyclers.size() + " máquinas recicladoras.");
    }

    private void saveAsync() {
        // Ejecutamos el guardado en otro hilo para no causar lag
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (data == null) data = new YamlConfiguration();

            // Convertir Locations a String
            Set<String> list = new HashSet<>();
            for (Location loc : recyclers) {
                list.add(locToString(loc));
            }

            data.set("recyclers", list.stream().toList());

            try {
                data.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo guardar machines.yml", e);
            }
        });
    }

    // Helpers simples para serializar Locations
    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLoc(String str) {
        try {
            String[] parts = str.split(",");
            World w = Bukkit.getWorld(parts[0]);
            if (w == null) return null; // Mundo no cargado o borrado
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(w, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }
}