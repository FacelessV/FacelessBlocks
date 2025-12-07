package bw.development.facelessBlocks;

import bw.development.facelessBlocks.commands.MachineCommands;
import bw.development.facelessBlocks.data.Keys;
import bw.development.facelessBlocks.data.MachineManager;
import bw.development.facelessBlocks.hooks.VaultHook;
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

        // 1. Configuración y Datos
        saveDefaultConfig();
        Keys.load(this);

        // 2. Setup de Economía (Vault)
        if (!VaultHook.setupEconomy(this)) {
            getLogger().severe("¡Vault no encontrado! Desactivando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.machineManager = new MachineManager(this);

        // 3. Eventos y Comandos
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new InteractListener(), this);

        MachineCommands cmdExecutor = new MachineCommands();
        getCommand("giverecycler").setExecutor(cmdExecutor);
        getCommand("clearrecyclers").setExecutor(cmdExecutor);

        // 4. Tareas
        new MachineTicker().runTaskTimer(this, 20L, 20L);
        new AutoRefreshTask().runTaskTimer(this, 5L, 5L);

        getLogger().info("FacelessBlocks habilitado con soporte de Economía.");
    }

    @Override
    public void onDisable() {
        getLogger().info("FacelessBlocks desactivado.");
    }

    public static FacelessBlocks getInstance() {
        return instance;
    }

    public MachineManager getMachineManager() {
        return machineManager;
    }
}