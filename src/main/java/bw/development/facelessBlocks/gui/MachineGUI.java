package bw.development.facelessBlocks.gui;

import bw.development.facelessBlocks.data.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class MachineGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Barrel barrel;

    // DEFINICION DE SLOTS (MAPEO)
    public static final int[] INPUT_SLOTS = {0, 1, 2, 9, 10, 18, 19, 20};
    public static final int[] OUTPUT_SLOTS = {6, 7, 8, 15, 16, 17, 24, 25, 26};

    // Slots protegidos
    public static final int SLOT_SPEED = 11;
    public static final int SLOT_LUCK = 15;
    public static final int SLOT_STATUS = 13;

    public MachineGUI(Barrel barrel) {
        this.barrel = barrel;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Reciclador").color(NamedTextColor.DARK_GRAY));
        setupButtons();
        syncFromBarrelToGui();
    }

    private void setupButtons() {
        // Decoracion estática
        ItemStack panel = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        int[] decorationSlots = {3, 4, 5, 12, 14, 21, 22, 23};

        for (int i : decorationSlots) {
            inventory.setItem(i, panel);
        }

        // El resto se carga dinámicamente en updateStatusIcon para no repetir código
        updateStatusIcon();
    }

    // ==========================================
    // LOGICA DE SINCRONIZACION
    // ==========================================

    public void refresh() {
        syncFromBarrelToGui(); // Mueve items
        updateStatusIcon();    // Mueve estado y botones
    }

    /**
     * Copia los items reales del Barril a la GUI visual.
     * OPTIMIZADO: Solo reemplaza el item si ha cambiado para evitar parpadeo.
     */
    public void syncFromBarrelToGui() {
        Inventory barrelInv = barrel.getInventory();

        // 1. Sincronizar Inputs
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            ItemStack realItem = barrelInv.getItem(i);
            ItemStack guiItem = inventory.getItem(INPUT_SLOTS[i]);

            if (!isSameItem(realItem, guiItem)) {
                inventory.setItem(INPUT_SLOTS[i], (realItem != null) ? realItem.clone() : null);
            }
        }

        // 2. Sincronizar Outputs
        for (int i = 0; i < OUTPUT_SLOTS.length; i++) {
            ItemStack realItem = barrelInv.getItem(i + 9);
            ItemStack guiItem = inventory.getItem(OUTPUT_SLOTS[i]);

            if (!isSameItem(realItem, guiItem)) {
                inventory.setItem(OUTPUT_SLOTS[i], (realItem != null) ? realItem.clone() : null);
            }
        }
    }

    /**
     * Helper para evitar parpadeos visuales
     */
    private boolean isSameItem(ItemStack i1, ItemStack i2) {
        if (i1 == null && i2 == null) return true;
        if (i1 == null || i2 == null) return false;
        return i1.isSimilar(i2) && i1.getAmount() == i2.getAmount();
    }

    /**
     * Guarda la GUI en el barril al cerrar
     */
    public void syncFromGuiToBarrel() {
        Inventory barrelInv = barrel.getInventory();
        barrelInv.clear();

        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            ItemStack item = inventory.getItem(INPUT_SLOTS[i]);
            if (item != null) barrelInv.setItem(i, item);
        }

        for (int i = 0; i < OUTPUT_SLOTS.length; i++) {
            ItemStack item = inventory.getItem(OUTPUT_SLOTS[i]);
            if (item != null) barrelInv.setItem(i + 9, item);
        }
    }

    // ==========================================
    // ACTUALIZACION DE ESTADO Y BOTONES
    // ==========================================

    public void updateStatusIcon() {
        PersistentDataContainer data = barrel.getPersistentDataContainer();
        int isProcessing = data.getOrDefault(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 0);
        int timeLeft = data.getOrDefault(Keys.PROCESS_TIME, PersistentDataType.INTEGER, 0);

        // --- Actualizar Estado Central ---
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

        // --- Actualizar Botones (Precio Dinamico) ---
        // Recuperamos niveles
        int speedLvl = data.getOrDefault(Keys.UPGRADE_SPEED, PersistentDataType.INTEGER, 0);
        int luckLvl = data.getOrDefault(Keys.UPGRADE_LUCK, PersistentDataType.INTEGER, 0);

        // Recreamos los botones para asegurar que el precio y nivel esten al dia
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

    // ==========================================
    // UTILIDADES
    // ==========================================

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

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Barrel getBarrel() {
        return barrel;
    }
}