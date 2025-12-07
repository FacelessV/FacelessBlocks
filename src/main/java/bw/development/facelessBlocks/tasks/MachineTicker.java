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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
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

            // Validaciones básicas de carga
            if (loc.getWorld() == null || !loc.getChunk().isLoaded()) continue;

            BlockState state = loc.getBlock().getState();
            if (!(state instanceof Barrel)) continue;

            Barrel barrel = (Barrel) state;
            Inventory inv = barrel.getInventory();

            // DERIVAR AL PROCESO CORRECTO SEGÚN EL ID
            if (data.isProcessing()) {
                continueProcessing(barrel, inv, data);
            } else {
                String id = data.getMachineId();
                if ("RECYCLER".equals(id)) {
                    tryStartRecycler(barrel, inv, data);
                } else if ("REPAIRER".equals(id)) {
                    tryStartRepairer(barrel, inv, data);
                } else if ("DISENCHANTER".equals(id)) {
                    tryStartDisenchanter(barrel, inv, data);
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

    // ========================================================================
    // LOGICA 1: RECICLADOR
    // ========================================================================
    private void tryStartRecycler(Barrel barrel, Inventory inv, MachineData data) {
        for (int slot : MachineGUI.INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                RecyclerRecipes.RecycleResult result = RecyclerRecipes.getResult(item.getType());
                if (result != null) {
                    consumeItem(inv, slot, 1);
                    data.setOutputMaterial(result.material);

                    int baseTime = FacelessBlocks.getInstance().getConfig().getInt("blocks.recycler.base_stats.process_time_seconds", 15);
                    startJob(barrel, data, baseTime);
                    return;
                }
            }
        }
    }

    // ========================================================================
    // LOGICA 2: REPARADOR (CON DEBUG)
    // ========================================================================
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
                        break; // Encontramos una herramienta dañada
                    }
                }
            }
        }

        if (tool == null) return; // No hay nada que reparar, salimos en silencio

        // --- DEBUG START: Si llegamos aquí, es que hay una herramienta rota ---
        // FacelessBlocks.getInstance().getLogger().info("[DEBUG-REPAIR] Intentando reparar: " + tool.getType());

        // 2. Buscar material requerido
        FileConfiguration config = FacelessBlocks.getInstance().getConfig();
        String matName = config.getString("blocks.repairer.repair_materials." + tool.getType().name());

        if (matName == null) {
            FacelessBlocks.getInstance().getLogger().warning("[DEBUG-REPAIR] FALLO: No hay receta en config.yml para " + tool.getType());
            return;
        }

        Material requiredMat = Material.getMaterial(matName);
        if (requiredMat == null) {
            FacelessBlocks.getInstance().getLogger().warning("[DEBUG-REPAIR] FALLO: El material '" + matName + "' en config no es válido.");
            return;
        }

        // 3. Buscar el material en los inputs
        int matSlot = -1;
        for (int slot : MachineGUI.INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() == requiredMat && item.getAmount() > 0) {
                matSlot = slot;
                break;
            }
        }

        if (matSlot != -1) {
            FacelessBlocks.getInstance().getLogger().info("[DEBUG-REPAIR] ¡ÉXITO! Iniciando reparación con " + requiredMat);

            // Consumir Material (Check Eficiencia)
            boolean freeRepair = Math.random() < (data.getEfficiencyLevel() * 0.10);
            if (!freeRepair) {
                consumeItem(inv, matSlot, 1);
            } else {
                FacelessBlocks.getInstance().getLogger().info("[DEBUG-REPAIR] ¡Eficiencia activada! Reparación gratis.");
            }

            // Guardar herramienta en memoria y consumirla del input
            ItemStack toolToRepair = tool.clone();
            toolToRepair.setAmount(1);
            consumeItem(inv, toolSlot, 1);

            data.setStoredItem(toolToRepair);

            int baseTime = config.getInt("blocks.repairer.base_stats.process_time_seconds", 20);
            startJob(barrel, data, baseTime);
        } else {
            // DEBUG: Avisar que falta material (solo una vez para no spamear infinitamente si no lo pones)
            // FacelessBlocks.getInstance().getLogger().info("[DEBUG-REPAIR] Falta material: " + requiredMat + " para reparar " + tool.getType());
        }
    }

    // ========================================================================
    // LOGICA 3: DESENCANTADOR
    // ========================================================================
    private void tryStartDisenchanter(Barrel barrel, Inventory inv, MachineData data) {
        ItemStack enchantedItem = null;
        int itemSlot = -1;

        // 1. Buscar item con encantamientos
        for (int slot : MachineGUI.INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                // Verificar que tenga encantamientos y NO sea un libro encantado
                if (item.getEnchantments().size() > 0 && item.getType() != Material.ENCHANTED_BOOK) {
                    enchantedItem = item;
                    itemSlot = slot;
                    break;
                }
            }
        }
        if (enchantedItem == null) return;

        FileConfiguration config = FacelessBlocks.getInstance().getConfig();
        boolean requiresBook = config.getBoolean("blocks.disenchanter.settings.requires_book", true);
        int bookSlot = -1;

        // 2. Buscar libro normal (si es requerido)
        if (requiresBook) {
            for (int slot : MachineGUI.INPUT_SLOTS) {
                if (slot == itemSlot) continue; // No usar el mismo slot

                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() == Material.BOOK && item.getAmount() > 0) {
                    bookSlot = slot;
                    break;
                }
            }
            if (bookSlot == -1) return; // Falta el libro
        }

        // 3. Iniciar proceso
        FacelessBlocks.getInstance().getLogger().info("[DEBUG-DISENCHANT] Iniciando desencantamiento de " + enchantedItem.getType());

        if (bookSlot != -1) consumeItem(inv, bookSlot, 1);

        ItemStack toProcess = enchantedItem.clone();
        toProcess.setAmount(1);
        consumeItem(inv, itemSlot, 1);

        data.setStoredItem(toProcess);

        int baseTime = config.getInt("blocks.disenchanter.base_stats.process_time_seconds", 60);
        startJob(barrel, data, baseTime);
    }

    // ========================================================================
    // MÉTODOS COMUNES
    // ========================================================================

    private void startJob(Barrel barrel, MachineData data, int baseTimeSeconds) {
        data.setProcessing(true);
        // Calcular tiempo con mejoras
        int time = Math.max(2, baseTimeSeconds - (data.getSpeedLevel() * 2));
        data.setTimeLeft(time);

        barrel.getWorld().playSound(barrel.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        updateVisuals(barrel);
    }

    private void finishJob(Barrel barrel, Inventory inv, MachineData data) {
        data.setProcessing(false);
        ItemStack output = null;
        FileConfiguration config = FacelessBlocks.getInstance().getConfig();
        String id = data.getMachineId();

        // LOGICA DE RESULTADO SEGUN TIPO
        if ("REPAIRER".equals(id)) {
            // --- REPARADOR ---
            output = data.getStoredItem();
            if (output != null) {
                ItemMeta meta = output.getItemMeta();
                if (meta instanceof Damageable) {
                    ((Damageable) meta).setDamage(0); // Reparar totalmente
                    output.setItemMeta(meta);
                }
            }
            data.setStoredItem(null);

        } else if ("DISENCHANTER".equals(id)) {
            // --- DESENCANTADOR ---
            ItemStack stored = data.getStoredItem();
            if (stored != null) {
                // Crear libro encantado
                output = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) output.getItemMeta();

                // Copiar encantamientos
                Map<Enchantment, Integer> enchants = stored.getEnchantments();
                for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                }
                output.setItemMeta(meta);

                // Devolver item limpio (si está config)
                if (!config.getBoolean("blocks.disenchanter.settings.destroy_item", true)) {
                    ItemStack returnedItem = stored.clone();
                    for (Enchantment ench : returnedItem.getEnchantments().keySet()) {
                        returnedItem.removeEnchantment(ench);
                    }
                    deliverOutput(barrel, inv, returnedItem);
                }
            }
            data.setStoredItem(null);

        } else {
            // --- RECICLADOR ---
            Material mat = data.getOutputMaterial();
            if (mat != null) {
                int amount = 1;
                if (Math.random() < (data.getLuckLevel() * 0.10)) amount++;
                output = new ItemStack(mat, amount);
            }
            data.setOutputMaterial(null);
        }

        // Entregar
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
            FacelessBlocks.getInstance().getLogger().info("[DEBUG] Resultado entregado: " + item.getType());
        } else {
            barrel.getWorld().dropItemNaturally(barrel.getLocation().add(0.5, 1.2, 0.5), item);
            FacelessBlocks.getInstance().getLogger().info("[DEBUG] Salida llena. Dropeado.");
        }

        forceUpdate(barrel);
    }

    private void consumeItem(Inventory inv, int slot, int amount) {
        ItemStack item = inv.getItem(slot);
        if (item != null) {
            if (item.getAmount() > amount) {
                item.setAmount(item.getAmount() - amount);
                inv.setItem(slot, item);
            } else {
                inv.setItem(slot, null);
            }
        }
    }

    private void forceUpdate(Barrel barrel) {
        if (barrel.update(true)) {
            updateVisuals(barrel);
        }
    }

    private void updateVisuals(Barrel barrel) {
        Location loc = barrel.getLocation();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        String wName = loc.getWorld().getName();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Inventory top = p.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof Barrel) {
                Barrel b = (Barrel) top.getHolder();
                Location bl = b.getLocation();

                if (bl.getWorld().getName().equals(wName) &&
                        bl.getBlockX() == bx && bl.getBlockY() == by && bl.getBlockZ() == bz) {

                    top.setContents(barrel.getInventory().getContents());
                    p.updateInventory();

                    new MachineGUI(b).updateStatusIcon();
                }
            }
        }
    }
}