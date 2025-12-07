package bw.development.facelessBlocks.gui;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.Keys;
import bw.development.facelessBlocks.data.MachineData;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.stream.IntStream;

public class MachineGUI {

    private final Barrel barrel;
    private final Inventory inventory;

    public static final int[] INPUT_SLOTS = {0, 1, 2, 9, 10, 18, 19, 20};
    public static final int[] OUTPUT_SLOTS = {6, 7, 8, 16, 17, 24, 25, 26};
    public static final int[] DECORATION_SLOTS = {3, 4, 5, 12, 14, 21, 22, 23};
    public static final int SLOT_SPEED = 11;
    public static final int SLOT_LUCK = 15;
    public static final int SLOT_STATUS = 13;

    public MachineGUI(Barrel barrel) {
        this.barrel = barrel;
        this.inventory = barrel.getInventory();
    }

    public void open(Player player) {
        updateInterface();
        player.openInventory(inventory);
    }

    public void updateInterface() {
        ItemStack panel = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i : DECORATION_SLOTS) {
            ItemStack current = inventory.getItem(i);
            if (current == null || current.getType() == Material.AIR || current.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                inventory.setItem(i, panel);
            }
        }
        updateStatusIcon();
    }

    public void updateStatusIcon() {
        MachineData data = FacelessBlocks.getInstance().getMachineManager().getMachine(barrel.getLocation());
        if (data == null) return;

        // 1. Detectar Tipo de Máquina
        String machineType = barrel.getPersistentDataContainer().getOrDefault(Keys.MACHINE_ID, PersistentDataType.STRING, "RECYCLER");
        boolean isRepairer = "REPAIRER".equals(machineType);

        // 2. Estado
        if (data.isProcessing()) {
            Material mat = (data.getTimeLeft() % 2 == 0) ? Material.LIME_STAINED_GLASS_PANE : Material.EMERALD_BLOCK;
            inventory.setItem(SLOT_STATUS, createItem(mat,
                    "§a§lPROCESANDO...",
                    "§7Tiempo: §f" + data.getTimeLeft() + "s",
                    "§7¡No toques!"
            ));
        } else {
            inventory.setItem(SLOT_STATUS, createItem(Material.RED_STAINED_GLASS_PANE,
                    "§c§lESPERANDO...",
                    "§7Coloca items a la izquierda."
            ));
        }

        // 3. Botones Adaptativos
        String ecoType = FacelessBlocks.getInstance().getConfig().getString("economy_type", "VAULT");
        String symbol = ecoType.equalsIgnoreCase("POINTS") ? " Puntos" : "$";

        double speedCost = calculateCost(1000, 1.5, data.getSpeedLevel());
        double luckCost = calculateCost(isRepairer ? 5000 : 2500, isRepairer ? 2.5 : 2.0, data.getLuckLevel());

        // Botón Velocidad
        inventory.setItem(SLOT_SPEED, createItem(Material.SUGAR,
                "§bMejora Velocidad",
                "§7Nivel: §f" + data.getSpeedLevel(),
                "§eCoste: §6" + (int)speedCost + symbol
        ));

        // Botón Secundaria (Suerte o Eficiencia)
        String title = isRepairer ? "§aMejora Eficiencia" : "§aMejora Suerte";
        inventory.setItem(SLOT_LUCK, createItem(Material.EMERALD,
                title,
                "§7Nivel: §f" + data.getLuckLevel(),
                "§eCoste: §6" + (int)luckCost + symbol,
                isRepairer ? "§7(Probabilidad de reparar GRATIS)" : "§7(Probabilidad de items extra)"
        ));
    }

    public static double calculateCost(double base, double multiplier, int currentLevel) {
        return base * Math.pow(multiplier, currentLevel);
    }

    public static boolean isSystemSlot(int slot) {
        if (slot == SLOT_SPEED || slot == SLOT_LUCK || slot == SLOT_STATUS) return true;
        return IntStream.of(DECORATION_SLOTS).anyMatch(x -> x == slot);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        if (lore != null && lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }
}