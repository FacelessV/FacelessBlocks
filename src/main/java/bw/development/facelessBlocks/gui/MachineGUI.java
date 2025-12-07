package bw.development.facelessBlocks.gui;

import bw.development.facelessBlocks.data.Keys;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.stream.IntStream;

public class MachineGUI {

    private final Barrel barrel;
    private final Inventory inventory;


    // ZONA IZQUIERDA (Inputs)
    public static final int[] INPUT_SLOTS = {0, 1, 2, 9, 10, 18, 19, 20};

    // ZONA DERECHA (Outputs)
    public static final int[] OUTPUT_SLOTS = {6, 7, 8, 16, 17, 24, 25, 26};

    // Slots protegidos (Cristales y botones)
    public static final int[] DECORATION_SLOTS = {3, 4, 5, 12, 14, 21, 22, 23};
    public static final int SLOT_SPEED = 11;
    public static final int SLOT_LUCK = 15;
    public static final int SLOT_STATUS = 13;

    public MachineGUI(Barrel barrel) {
        this.barrel = barrel;
        this.inventory = barrel.getInventory(); // <--- Usamos el inventario REAL
    }

    public void open(Player player) {
        updateInterface(); // Dibuja los botones antes de abrir
        player.openInventory(inventory);
    }

    public void updateInterface() {
        // 1. Decoración Estática
        ItemStack panel = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i : DECORATION_SLOTS) {
            // Solo ponemos el panel si está vacío o es otro panel (para no borrar items del usuario por error)
            ItemStack current = inventory.getItem(i);
            if (current == null || current.getType() == Material.AIR || current.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                inventory.setItem(i, panel);
            }
        }

        // 2. Estado y Botones Dinámicos
        updateStatusIcon();
    }

    public void updateStatusIcon() {
        PersistentDataContainer data = barrel.getPersistentDataContainer();
        int isProcessing = data.getOrDefault(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 0);
        int timeLeft = data.getOrDefault(Keys.PROCESS_TIME, PersistentDataType.INTEGER, 0);

        // --- Estado Central ---
        if (isProcessing == 1) {
            Material mat = (timeLeft % 2 == 0) ? Material.LIME_STAINED_GLASS_PANE : Material.EMERALD_BLOCK;
            inventory.setItem(SLOT_STATUS, createItem(mat,
                    "§a§lPROCESANDO...",
                    "§7Tiempo restante: §f" + timeLeft + "s",
                    "§7¡No rompas el bloque!"
            ));
        } else {
            inventory.setItem(SLOT_STATUS, createItem(Material.RED_STAINED_GLASS_PANE,
                    "§c§lESPERANDO...",
                    "§7Sistema inactivo",
                    "§7Coloca items a la izquierda."
            ));
        }

        // --- Botones de Mejora ---
        int speedLvl = data.getOrDefault(Keys.UPGRADE_SPEED, PersistentDataType.INTEGER, 0);
        int luckLvl = data.getOrDefault(Keys.UPGRADE_LUCK, PersistentDataType.INTEGER, 0);

        inventory.setItem(SLOT_SPEED, createItem(Material.SUGAR,
                "§bMejora Velocidad",
                "§7Nivel: §f" + speedLvl,
                "§eCoste: §6" + (int)getUpgradeCost(speedLvl, 1.5) + "$"
        ));

        inventory.setItem(SLOT_LUCK, createItem(Material.EMERALD,
                "§aMejora Suerte",
                "§7Nivel: §f" + luckLvl,
                "§eCoste: §6" + (int)getUpgradeCost(luckLvl, 2.0) + "$"
        ));
    }

    // Método estático para saber si un slot es del sistema (intocable)
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

    private double getUpgradeCost(int level, double multiplier) {
        return 1000 * Math.pow(multiplier, level);
    }
}