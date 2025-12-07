package bw.development.facelessBlocks;

import bw.development.facelessBlocks.data.Keys;
import bw.development.facelessBlocks.data.MachineManager;
import bw.development.facelessBlocks.listeners.BlockListener;
import bw.development.facelessBlocks.listeners.InteractListener;
import bw.development.facelessBlocks.tasks.AutoRefreshTask;
import bw.development.facelessBlocks.tasks.MachineTicker;
import org.bukkit.plugin.java.JavaPlugin;

public final class FacelessBlocks extends JavaPlugin {

    private static FacelessBlocks instance;
    private MachineManager machineManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Cargar Configuración
        saveDefaultConfig();

        // 2. Cargar Llaves de Datos
        Keys.load(this);

        this.machineManager = new MachineManager(this);

        // 3. Registrar Eventos
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new InteractListener(), this);

        // 4. Iniciar la tarea repetitiva (El Ticker)
        // Se ejecuta cada 20 ticks (1 segundo)
        new MachineTicker().runTaskTimer(this, 20L, 20L);
        new AutoRefreshTask().runTaskTimer(this, 5L, 5L);

        getLogger().info("FacelessBlocks ha sido habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("FacelessBlocks se ha desactivado.");
    }

    public static FacelessBlocks getInstance() {
        return instance;
    }

    public MachineManager getMachineManager() {
        return machineManager;
    }
}