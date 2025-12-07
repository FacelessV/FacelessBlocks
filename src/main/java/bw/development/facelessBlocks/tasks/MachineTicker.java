package bw.development.facelessBlocks.tasks;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.Keys;
import bw.development.facelessBlocks.utils.RecyclerRecipes;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
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
        // Envolvemos todo en un TRY-CATCH para capturar el error maldito
        try {
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state instanceof Barrel) {
                            Barrel barrel = (Barrel) state;
                            PersistentDataContainer data = barrel.getPersistentDataContainer();

                            if (data.has(Keys.MACHINE_ID, PersistentDataType.STRING) &&
                                    "RECYCLER".equals(data.get(Keys.MACHINE_ID, PersistentDataType.STRING))) {
                                processRecycler(barrel, data);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // SI ALGO FALLA, LO IMPRIMIMOS AL CHAT GLOBAL PARA QUE LO VEAS
            Bukkit.broadcast(Component.text("§c[ERROR CRÍTICO EN TICKER]: " + e.getMessage()));
            e.printStackTrace(); // Esto manda el error completo a la consola
            this.cancel(); // Detenemos el ticker para no spamear el error 20 veces por segundo
        }
    }

    private void processRecycler(Barrel barrel, PersistentDataContainer data) {
        Inventory inv = barrel.getInventory();
        int isProcessing = data.getOrDefault(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 0);
        int timeLeft = data.getOrDefault(Keys.PROCESS_TIME, PersistentDataType.INTEGER, 0);
        int speedLvl = data.getOrDefault(Keys.UPGRADE_SPEED, PersistentDataType.INTEGER, 0);

        if (isProcessing == 1) {
            if (timeLeft > 0) {
                data.set(Keys.PROCESS_TIME, PersistentDataType.INTEGER, timeLeft - 1);
            } else {
                finishProcessing(barrel, data);
            }
        } else {
            tryStartProcessing(barrel, data, inv, speedLvl);
        }
        barrel.update();
    }

    private void tryStartProcessing(Barrel barrel, PersistentDataContainer data, Inventory inv, int speedLvl) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                RecyclerRecipes.RecycleResult result = RecyclerRecipes.getResult(item.getType());

                if (result != null) {
                    // Validacion de seguridad extra
                    if (result.material == null) {
                        Bukkit.broadcast(Component.text("§c[ERROR] La receta devuelve Material NULL para " + item.getType()));
                        return;
                    }

                    if (Keys.OUTPUT_MATERIAL == null) throw new IllegalArgumentException("Keys.OUTPUT_MATERIAL es NULL (Revisa Keys.java)");

                    // 1. Quitar item
                    item.setAmount(item.getAmount() - 1);

                    // 2. Guardar estado
                    data.set(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 1);
                    data.set(Keys.OUTPUT_MATERIAL, PersistentDataType.STRING, result.material.name());

                    // 3. Tiempo
                    int baseTime = FacelessBlocks.getInstance().getConfig().getInt("blocks.recycler.base_stats.process_time_seconds");
                    if (baseTime == 0) baseTime = 15; // Fallback por si la config falla

                    int time = Math.max(2, baseTime - (speedLvl * 2));
                    data.set(Keys.PROCESS_TIME, PersistentDataType.INTEGER, time);

                    barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
                    return;
                }
            }
        }
    }

    private void finishProcessing(Barrel barrel, PersistentDataContainer data) {
        // 1. Apagamos la maquina
        data.set(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 0);

        // 2. Recuperamos que teniamos que dar
        String matName = data.get(Keys.OUTPUT_MATERIAL, PersistentDataType.STRING);

        // --- DEBUG: Ver que intenta dar ---
        if (matName == null) {
            Bukkit.broadcast(Component.text("§c[ERROR] ¡La máquina olvidó el premio! (matName is NULL)"));
            return;
        }
        Bukkit.broadcast(Component.text("§e[DEBUG] Intentando entregar: " + matName));
        // ----------------------------------

        Material rewardMat = Material.getMaterial(matName);
        if (rewardMat == null) {
            Bukkit.broadcast(Component.text("§c[ERROR] El material '" + matName + "' no existe en Minecraft. Revisa la config."));
            return;
        }

        // 3. Calculamos cantidad (Suerte)
        int luckLvl = data.getOrDefault(Keys.UPGRADE_LUCK, PersistentDataType.INTEGER, 0);
        int amount = 1;
        // Si el success es 100% en tu mente, aqui aseguramos cantidad minima 1
        // Si quieres bonus por suerte:
        if (luckLvl > 0 && Math.random() > 0.5) amount++;

        ItemStack output = new ItemStack(rewardMat, amount);

        // 4. LOGICA DE ENTREGA CORREGIDA (Solo Slots 9-26)
        Inventory inv = barrel.getInventory();
        boolean delivered = false;

        // Recorremos SOLO los slots que corresponden a la SALIDA (Derecha en GUI)
        // El barril tiene 27 slots. Reservamos 0-8 para entrada. 9-26 para salida.
        for (int i = 9; i < 27; i++) {
            ItemStack current = inv.getItem(i);

            if (current == null || current.getType() == Material.AIR) {
                // Slot vacio encontrado, poner item
                inv.setItem(i, output);
                delivered = true;
                break;
            } else if (current.isSimilar(output) && current.getAmount() + amount <= current.getMaxStackSize()) {
                // Stackear con existente
                current.setAmount(current.getAmount() + amount);
                delivered = true;
                break;
            }
        }

        if (delivered) {
            barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            Bukkit.broadcast(Component.text("§a[EXITO] Item entregado en zona de salida."));
        } else {
            // Si la salida esta llena, lo tiramos al suelo
            barrel.getWorld().dropItemNaturally(barrel.getLocation().add(0, 1, 0), output);
            Bukkit.broadcast(Component.text("§6[ALERTA] Salida llena, item tirado al suelo."));
        }

        // Limpiamos la memoria
        data.remove(Keys.OUTPUT_MATERIAL);
    }
}