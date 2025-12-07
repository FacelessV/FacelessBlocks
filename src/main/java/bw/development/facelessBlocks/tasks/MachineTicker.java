package bw.development.facelessBlocks.tasks;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.MachineData;
import bw.development.facelessBlocks.gui.MachineGUI;
import bw.development.facelessBlocks.utils.RecyclerRecipes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class MachineTicker extends BukkitRunnable {

    @Override
    public void run() {
        // Iteramos sobre el MAPA en memoria
        Map<Location, MachineData> machines = FacelessBlocks.getInstance().getMachineManager().getAllMachines();

        for (Map.Entry<Location, MachineData> entry : machines.entrySet()) {
            Location loc = entry.getKey();
            MachineData data = entry.getValue();

            // Chequeo rápido de carga
            if (loc.getWorld() == null || !loc.getChunk().isLoaded()) continue;

            // Obtenemos el bloque DIRECTO (Sin snapshots)
            BlockState state = loc.getBlock().getState();
            if (!(state instanceof Barrel)) continue;

            Barrel barrel = (Barrel) state;
            Inventory inv = barrel.getInventory(); // Inventario en vivo

            processMachine(barrel, inv, data);
        }
    }

    private void processMachine(Barrel barrel, Inventory inv, MachineData data) {
        if (data.isProcessing()) {
            // Logica de cuenta regresiva
            if (data.getTimeLeft() > 0) {
                data.setTimeLeft(data.getTimeLeft() - 1);
            } else {
                finishJob(barrel, inv, data);
            }
        } else {
            // Intentar empezar trabajo
            tryStartJob(barrel, inv, data);
        }
        // ¡NOTA! No llamamos a barrel.update(). No hace falta.
        // Los cambios en 'data' estan en memoria.
        // Los cambios en 'inv' se reflejan en el juego automaticamente.
    }

    private void tryStartJob(Barrel barrel, Inventory inv, MachineData data) {
        for (int slot : MachineGUI.INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);

            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                RecyclerRecipes.RecycleResult result = RecyclerRecipes.getResult(item.getType());

                if (result != null) {
                    // Consumir 1 item
                    int newAmount = item.getAmount() - 1;
                    if (newAmount > 0) {
                        item.setAmount(newAmount);
                        inv.setItem(slot, item); // Actualizamos slot
                    } else {
                        inv.setItem(slot, null); // Borramos slot
                    }

                    // Configurar datos en memoria
                    data.setProcessing(true);
                    data.setOutputMaterial(result.material);

                    // Calcular tiempo
                    int baseTime = FacelessBlocks.getInstance().getConfig().getInt("blocks.recycler.base_stats.process_time_seconds", 15);
                    int time = Math.max(2, baseTime - (data.getSpeedLevel() * 2));
                    data.setTimeLeft(time);

                    barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
                    return; // Solo 1 a la vez
                }
            }
        }
    }

    private void finishJob(Barrel barrel, Inventory inv, MachineData data) {
        data.setProcessing(false);
        Material mat = data.getOutputMaterial();

        if (mat == null) return;

        // Calcular cantidad
        int amount = 1;
        if (Math.random() < (data.getLuckLevel() * 0.10)) amount++;

        ItemStack reward = new ItemStack(mat, amount);
        boolean delivered = false;

        // Entregar
        for (int slot : MachineGUI.OUTPUT_SLOTS) {
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                inv.setItem(slot, reward);
                delivered = true;
                break;
            } else if (current.isSimilar(reward) && current.getAmount() + amount <= current.getMaxStackSize()) {
                current.setAmount(current.getAmount() + amount);
                inv.setItem(slot, current); // Forzamos update del slot
                delivered = true;
                break;
            }
        }

        if (delivered) {
            barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else {
            barrel.getWorld().dropItemNaturally(barrel.getLocation().add(0.5, 1.2, 0.5), reward);
        }

        data.setOutputMaterial(null);
    }
}