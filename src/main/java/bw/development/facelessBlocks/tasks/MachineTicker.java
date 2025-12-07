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
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class MachineTicker extends BukkitRunnable {

    @Override
    public void run() {
        for (Location loc : FacelessBlocks.getInstance().getMachineManager().getRecyclers()) {
            if (loc.getWorld() == null || !loc.getChunk().isLoaded()) continue;

            BlockState state = loc.getBlock().getState();
            if (!(state instanceof Barrel)) continue;

            Barrel barrel = (Barrel) state;
            PersistentDataContainer data = barrel.getPersistentDataContainer();

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
            if (timeLeft > 0) {
                data.set(Keys.PROCESS_TIME, PersistentDataType.INTEGER, timeLeft - 1);
            } else {
                finishProcessing(barrel, data);
            }
        } else {
            tryStartProcessing(barrel, data, inv, speedLvl);
        }

        // USAMOS EL MÉTODO DE GUARDADO FORZADO
        forceUpdate(barrel);
    }

    private void tryStartProcessing(Barrel barrel, PersistentDataContainer data, Inventory inv, int speedLvl) {
        for (int slot : MachineGUI.INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);

            // Verificamos amount > 0 para evitar fantasmas
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                RecyclerRecipes.RecycleResult result = RecyclerRecipes.getResult(item.getType());

                if (result != null) {
                    Bukkit.getPlayer("cheinsowman").sendMessage(Component.text("§e[DEBUG] Consumiendo 1 " + item.getType()));

                    // --- CONSUMO SEGURO ---
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                        inv.setItem(slot, item);
                    } else {
                        inv.setItem(slot, null); // Borrar totalmente
                    }
                    // ----------------------

                    data.set(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 1);
                    data.set(Keys.OUTPUT_MATERIAL, PersistentDataType.STRING, result.material.name());

                    int baseTime = FacelessBlocks.getInstance().getConfig().getInt("blocks.recycler.base_stats.process_time_seconds", 15);
                    int time = Math.max(2, baseTime - (speedLvl * 2));
                    data.set(Keys.PROCESS_TIME, PersistentDataType.INTEGER, time);

                    barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
                    new MachineGUI(barrel).updateStatusIcon();
                    return;
                }
            }
        }
    }

    private void finishProcessing(Barrel barrel, PersistentDataContainer data) {
        data.set(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 0);

        String matName = data.get(Keys.OUTPUT_MATERIAL, PersistentDataType.STRING);
        if (matName == null) return;

        Material rewardMat = Material.getMaterial(matName);
        if (rewardMat == null) {
            data.remove(Keys.OUTPUT_MATERIAL);
            return;
        }

        int luckLvl = data.getOrDefault(Keys.UPGRADE_LUCK, PersistentDataType.INTEGER, 0);
        int amount = 1;
        if (Math.random() < (luckLvl * 0.10)) amount++;

        ItemStack output = new ItemStack(rewardMat, amount);
        Inventory inv = barrel.getInventory();
        boolean delivered = false;

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
            Bukkit.getPlayer("cheinsowman").sendMessage(Component.text("§a[DEBUG] Entregado: " + output.getType() + " x" + amount));
        } else {
            barrel.getWorld().dropItemNaturally(barrel.getLocation().add(0.5, 1.2, 0.5), output);
            Bukkit.getPlayer("cheinsowman").sendMessage(Component.text("§6[DEBUG] Salida llena. Item dropeado."));
        }

        data.remove(Keys.OUTPUT_MATERIAL);
        new MachineGUI(barrel).updateStatusIcon();
    }

    /**
     * Guarda los cambios forzosamente y actualiza la vista de los jugadores.
     */
    private void forceUpdate(Barrel barrel) {
        // 1. Forzar guardado del bloque (true = force)
        if (barrel.update(true)) {

            // 2. Sincronización visual manual
            // Buscamos si hay jugadores con este inventario abierto y se lo refrescamos
            Location machineLoc = barrel.getLocation();

            for (Player p : Bukkit.getOnlinePlayers()) {
                Inventory top = p.getOpenInventory().getTopInventory();

                // Si el jugador está mirando un Barril
                if (top.getHolder() instanceof Barrel) {
                    Barrel openBarrel = (Barrel) top.getHolder();

                    // Y es ESTE barril
                    if (openBarrel.getLocation().equals(machineLoc)) {
                        // Le pegamos el contenido nuevo directamente en la cara
                        top.setContents(barrel.getInventory().getContents());
                    }
                }
            }
        } else {
            Bukkit.getPlayer("cheinsowman").sendMessage(Component.text("§c[CRITICAL ERROR] No se pudo guardar el barril."));
        }
    }
}