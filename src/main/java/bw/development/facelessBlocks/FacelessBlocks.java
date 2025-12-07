package bw.development.facelessBlocks;

import bw.development.facelessBlocks.commands.MachineCommands;
import bw.development.facelessBlocks.data.Keys;
import bw.development.facelessBlocks.data.MachineManager;
import bw.development.facelessBlocks.hooks.PointsHook; // Nuevo
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

        saveDefaultConfig();
        Keys.load(this);

        String ecoType = getConfig().getString("economy_type", "VAULT").toUpperCase();

        // 1. Setup de Economía según Config
        if (ecoType.equals("POINTS")) {
            if (!PointsHook.setupPoints(this)) {
                getLogger().severe("¡PlayerPoints no encontrado! Se requiere para este modo.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("Usando PlayerPoints para economía.");
        } else {
            // Por defecto Vault
            if (!VaultHook.setupEconomy(this)) {
                getLogger().severe("¡Vault no encontrado! Se requiere para este modo.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("Usando Vault (Dinero) para economía.");
        }

        this.machineManager = new MachineManager(this);

        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new InteractListener(), this);

        MachineCommands cmdExecutor = new MachineCommands();
        getCommand("giverecycler").setExecutor(cmdExecutor);
        getCommand("clearrecyclers").setExecutor(cmdExecutor);

        new MachineTicker().runTaskTimer(this, 20L, 20L);
        new AutoRefreshTask().runTaskTimer(this, 5L, 5L);

        getLogger().info("FacelessBlocks habilitado correctamente.");
    }

    // ... resto del archivo igual (onDisable, getInstance, getMachineManager)
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