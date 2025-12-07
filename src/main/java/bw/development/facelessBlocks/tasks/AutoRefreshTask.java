package bw.development.facelessBlocks.tasks;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.gui.MachineGUI;
import org.bukkit.Bukkit;
import org.bukkit.block.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoRefreshTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryView view = player.getOpenInventory();
            Inventory top = view.getTopInventory();

            // Verificamos si el jugador está mirando un Barril real
            if (top.getHolder() instanceof Barrel) {
                Barrel barrel = (Barrel) top.getHolder();

                // Verificamos si ese barril es un Reciclador
                if (FacelessBlocks.getInstance().getMachineManager().isRecycler(barrel.getLocation())) {
                    // Actualizamos solo la interfaz (iconos de estado y progreso)
                    // No tocamos los items de input/output para no molestar al jugador
                    new MachineGUI(barrel).updateStatusIcon();
                }
            }
        }
    }
}