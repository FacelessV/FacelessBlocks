package bw.development.facelessBlocks.tasks;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.Keys;
import bw.development.facelessBlocks.gui.MachineGUI;
import bw.development.facelessBlocks.utils.RecyclerRecipes;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class MachineTicker extends BukkitRunnable {

    @Override
    public void run() {
        // 1. OPTIMIZACION: Iteramos solo sobre la cache de máquinas, no sobre todo el mundo
        for (Location loc : FacelessBlocks.getInstance().getMachineManager().getRecyclers()) {

            // Si el mundo es nulo o el chunk no está cargado, saltamos para no causar lag
            if (loc.getWorld() == null || !loc.getChunk().isLoaded()) continue;

            BlockState state = loc.getBlock().getState();

            // Verificación de seguridad: ¿Sigue siendo un barril?
            if (!(state instanceof Barrel)) {
                // Podríamos eliminarlo del manager aquí, pero mejor dejar que el evento BlockBreak lo maneje
                continue;
            }

            Barrel barrel = (Barrel) state;
            PersistentDataContainer data = barrel.getPersistentDataContainer();

            // Verificamos si es un Reciclador válido
            if (data.has(Keys.MACHINE_ID, PersistentDataType.STRING) &&
                    "RECYCLER".equals(data.get(Keys.MACHINE_ID, PersistentDataType.STRING))) {
                processRecycler(barrel, data);
            }
        }
    }

    private void processRecycler(Barrel barrel, PersistentDataContainer data) {
        Inventory inv = barrel.getInventory();
        int isProcessing = data.getOrDefault(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 0);
        int timeLeft = data.getOrDefault(Keys.PROCESS_TIME, PersistentDataType.INTEGER, 0);
        int speedLvl = data.getOrDefault(Keys.UPGRADE_SPEED, PersistentDataType.INTEGER, 0);

        if (isProcessing == 1) {
            // Lógica de cuenta regresiva
            if (timeLeft > 0) {
                data.set(Keys.PROCESS_TIME, PersistentDataType.INTEGER, timeLeft - 1);

                // Actualizar visualmente el contador (Opcional, consume un poco más de recursos)
                // new MachineGUI(barrel).updateStatusIcon();
            } else {
                finishProcessing(barrel, data);
            }
        } else {
            // Intentar empezar nuevo trabajo
            tryStartProcessing(barrel, data, inv, speedLvl);
        }

        // Guardamos cambios en el bloque (IMPORTANTE)
        barrel.update();
    }

    private void tryStartProcessing(Barrel barrel, PersistentDataContainer data, Inventory inv, int speedLvl) {
        // Solo buscamos en los slots de ENTRADA (Izquierda)
        for (int slot : MachineGUI.INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);

            if (item != null && item.getType() != Material.AIR) {
                RecyclerRecipes.RecycleResult result = RecyclerRecipes.getResult(item.getType());

                if (result != null) {
                    // Consumir 1 item
                    item.setAmount(item.getAmount() - 1);
                    inv.setItem(slot, item); // Actualizar inventario

                    // Configurar datos de proceso
                    data.set(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 1);
                    data.set(Keys.OUTPUT_MATERIAL, PersistentDataType.STRING, result.material.name());

                    // Calcular tiempo (Reducción por nivel de velocidad)
                    int baseTime = FacelessBlocks.getInstance().getConfig().getInt("blocks.recycler.base_stats.process_time_seconds", 15);
                    int time = Math.max(2, baseTime - (speedLvl * 2)); // Mínimo 2 segundos

                    data.set(Keys.PROCESS_TIME, PersistentDataType.INTEGER, time);

                    // Sonido y actualización visual inmediata
                    barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
                    new MachineGUI(barrel).updateStatusIcon();
                    return; // Solo procesamos un item a la vez
                }
            }
        }
    }

    private void finishProcessing(Barrel barrel, PersistentDataContainer data) {
        data.set(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 0);

        String matName = data.get(Keys.OUTPUT_MATERIAL, PersistentDataType.STRING);
        if (matName == null) return;

        Material rewardMat = Material.getMaterial(matName);
        if (rewardMat == null) return;

        // Calcular cantidad (Suerte)
        int luckLvl = data.getOrDefault(Keys.UPGRADE_LUCK, PersistentDataType.INTEGER, 0);
        int amount = 1;
        // Lógica simple de suerte: 10% por nivel de duplicar el premio
        if (Math.random() < (luckLvl * 0.10)) {
            amount++;
        }

        ItemStack output = new ItemStack(rewardMat, amount);
        Inventory inv = barrel.getInventory();
        boolean delivered = false;

        // Solo intentamos poner el item en los slots de SALIDA (Derecha)
        for (int slot : MachineGUI.OUTPUT_SLOTS) {
            ItemStack current = inv.getItem(slot);

            if (current == null || current.getType() == Material.AIR) {
                inv.setItem(slot, output);
                delivered = true;
                break;
            } else if (current.isSimilar(output) && current.getAmount() + amount <= current.getMaxStackSize()) {
                current.setAmount(current.getAmount() + amount);
                delivered = true;
                break;
            }
        }

        if (delivered) {
            barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else {
            // Si está lleno, tiramos el item arriba del barril
            barrel.getWorld().dropItemNaturally(barrel.getLocation().add(0.5, 1.2, 0.5), output);
        }

        // Limpiar memoria y actualizar GUI
        data.remove(Keys.OUTPUT_MATERIAL);
        new MachineGUI(barrel).updateStatusIcon();
    }
}