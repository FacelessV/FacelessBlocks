package bw.development.facelessBlocks.tasks;

import bw.development.facelessBlocks.gui.MachineGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoRefreshTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryView view = player.getOpenInventory();

            // Si el jugador tiene abierta nuestra maquina
            if (view.getTopInventory().getHolder() instanceof MachineGUI) {
                MachineGUI gui = (MachineGUI) view.getTopInventory().getHolder();

                // ¡AQUI! Actualizamos todo de golpe
                gui.refresh();
            }
        }
    }
}