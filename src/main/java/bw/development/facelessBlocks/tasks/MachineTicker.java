package bw.development.facelessBlocks.tasks;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.MachineData;
import bw.development.facelessBlocks.gui.MachineGUI;
import bw.development.facelessBlocks.utils.RecyclerRecipes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class MachineTicker extends BukkitRunnable {

    @Override
    public void run() {
        Map<Location, MachineData> machines = FacelessBlocks.getInstance().getMachineManager().getAllMachines();

        for (Map.Entry<Location, MachineData> entry : machines.entrySet()) {
            Location loc = entry.getKey();
            MachineData data = entry.getValue();

            if (loc.getWorld() == null || !loc.getChunk().isLoaded()) continue;

            BlockState state = loc.getBlock().getState();
            if (!(state instanceof Barrel)) continue;

            Barrel barrel = (Barrel) state;
            Inventory inv = barrel.getInventory();

            // Lógica Central
            if (data.isProcessing()) {
                continueProcessing(barrel, inv, data);
            } else {
                if ("RECYCLER".equals(data.getMachineId())) {
                    tryStartRecycler(barrel, inv, data);
                } else if ("REPAIRER".equals(data.getMachineId())) {
                    tryStartRepairer(barrel, inv, data);
                }
            }
        }
    }

    private void continueProcessing(Barrel barrel, Inventory inv, MachineData data) {
        if (data.getTimeLeft() > 0) {
            data.setTimeLeft(data.getTimeLeft() - 1);
        } else {
            finishJob(barrel, inv, data);
        }
    }

    // --- RECICLADOR ---
    private void tryStartRecycler(Barrel barrel, Inventory inv, MachineData data) {
        for (int slot : MachineGUI.INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                RecyclerRecipes.RecycleResult result = RecyclerRecipes.getResult(item.getType());
                if (result != null) {
                    // Consumir
                    consumeItem(inv, slot, 1);

                    // Configurar
                    data.setOutputMaterial(result.material);
                    startJob(barrel, data, 15); // Tiempo base
                    return;
                }
            }
        }
    }

    // --- REPARADOR ---
    private void tryStartRepairer(Barrel barrel, Inventory inv, MachineData data) {
        ItemStack tool = null;
        int toolSlot = -1;

        // 1. Buscar herramienta dañada
        for (int slot : MachineGUI.INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof Damageable) {
                    if (((Damageable) meta).hasDamage()) {
                        tool = item;
                        toolSlot = slot;
                        break;
                    }
                }
            }
        }
        if (tool == null) return;

        // 2. Buscar material requerido en config
        FileConfiguration config = FacelessBlocks.getInstance().getConfig();
        String matName = config.getString("blocks.repairer.repair_materials." + tool.getType().name());
        if (matName == null) return;
        Material requiredMat = Material.getMaterial(matName);
        if (requiredMat == null) return;

        // 3. Buscar material en inventario
        int matSlot = -1;
        for (int slot : MachineGUI.INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() == requiredMat && item.getAmount() > 0) {
                matSlot = slot;
                break;
            }
        }

        if (matSlot != -1) {
            // Consumir Material (Check Eficiencia)
            boolean freeRepair = Math.random() < (data.getEfficiencyLevel() * 0.10);
            if (!freeRepair) {
                consumeItem(inv, matSlot, 1);
            }

            // Consumir Herramienta del input y guardarla en memoria
            ItemStack toolToRepair = tool.clone();
            toolToRepair.setAmount(1); // Asegurar que es 1
            consumeItem(inv, toolSlot, 1);

            data.setStoredItem(toolToRepair); // <--- AQUÍ GUARDAMOS EL ÍTEM CON ENCHANTS

            startJob(barrel, data, 20); // Tiempo base reparador
        }
    }

    // --- MÉTODOS COMUNES ---

    private void startJob(Barrel barrel, MachineData data, int baseTime) {
        data.setProcessing(true);
        // Calcular tiempo con mejoras
        int time = Math.max(2, baseTime - (data.getSpeedLevel() * 2));
        data.setTimeLeft(time);

        barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        updateVisuals(barrel);
    }

    private void finishJob(Barrel barrel, Inventory inv, MachineData data) {
        data.setProcessing(false);
        ItemStack output = null;

        if ("REPAIRER".equals(data.getMachineId())) {
            // Lógica Reparador
            output = data.getStoredItem();
            if (output != null) {
                // Reparar el item
                ItemMeta meta = output.getItemMeta();
                if (meta instanceof Damageable) {
                    ((Damageable) meta).setDamage(0); // Vida al 100%
                    output.setItemMeta(meta);
                }
            }
            data.setStoredItem(null); // Limpiar memoria
        } else {
            // Lógica Reciclador
            Material mat = data.getOutputMaterial();
            if (mat != null) {
                int amount = 1;
                // Bonus de suerte
                if (Math.random() < (data.getLuckLevel() * 0.10)) amount++;
                output = new ItemStack(mat, amount);
            }
            data.setOutputMaterial(null);
        }

        if (output != null) {
            deliverOutput(barrel, inv, output);
        }

        updateVisuals(barrel);
    }

    private void deliverOutput(Barrel barrel, Inventory inv, ItemStack item) {
        boolean delivered = false;
        for (int slot : MachineGUI.OUTPUT_SLOTS) {
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                inv.setItem(slot, item);
                delivered = true;
                break;
            } else if (current.isSimilar(item) && current.getAmount() + item.getAmount() <= current.getMaxStackSize()) {
                current.setAmount(current.getAmount() + item.getAmount());
                inv.setItem(slot, current);
                delivered = true;
                break;
            }
        }

        if (delivered) {
            barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else {
            barrel.getWorld().dropItemNaturally(barrel.getLocation().add(0.5, 1.2, 0.5), item);
        }
    }

    private void consumeItem(Inventory inv, int slot, int amount) {
        ItemStack item = inv.getItem(slot);
        if (item.getAmount() > amount) {
            item.setAmount(item.getAmount() - amount);
            inv.setItem(slot, item);
        } else {
            inv.setItem(slot, null);
        }
    }

    // Sincronización visual simple
    private void updateVisuals(Barrel barrel) {
        Location loc = barrel.getLocation();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Inventory top = p.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof Barrel) {
                Barrel b = (Barrel) top.getHolder();
                if (b.getLocation().equals(loc)) {
                    p.updateInventory(); // Forzar refresco
                }
            }
        }
    }
}