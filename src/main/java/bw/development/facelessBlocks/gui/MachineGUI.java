package bw.development.facelessBlocks.gui;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.MachineData;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class MachineGUI {

    private final Barrel barrel;
    private final Inventory inventory;

    // Slots
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

        // Estado
        if (data.isProcessing()) {
            Material mat = (data.getTimeLeft() % 2 == 0) ? Material.LIME_STAINED_GLASS_PANE : Material.EMERALD_BLOCK;
            inventory.setItem(SLOT_STATUS, createItem(mat,
                    "§a§lPROCESANDO...",
                    "§7Tiempo restante: §f" + data.getTimeLeft() + "s",
                    "§7¡No rompas el bloque!"
            ));
        } else {
            inventory.setItem(SLOT_STATUS, createItem(Material.RED_STAINED_GLASS_PANE,
                    "§c§lESPERANDO...",
                    "§7Sistema inactivo",
                    "§7Coloca items a la izquierda."
            ));
        }

        // Botones con PRECIOS REALES
        // Velocidad: Base 1000, Multi 1.5
        double speedCost = calculateCost(1000, 1.5, data.getSpeedLevel());
        inventory.setItem(SLOT_SPEED, createItem(Material.SUGAR,
                "§bMejora Velocidad",
                "§7Nivel: §f" + data.getSpeedLevel(),
                "§eCoste: §6$" + (int)speedCost
        ));

        // Suerte: Base 2500, Multi 2.0
        double luckCost = calculateCost(2500, 2.0, data.getLuckLevel());
        inventory.setItem(SLOT_LUCK, createItem(Material.EMERALD,
                "§aMejora Suerte",
                "§7Nivel: §f" + data.getLuckLevel(),
                "§eCoste: §6$" + (int)luckCost
        ));
    }

    // Helper estático para calcular precios (usado aquí y en InteractListener)
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