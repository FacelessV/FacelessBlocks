package bw.development.facelessBlocks.listeners;

import bw.development.facelessBlocks.data.Keys;
import bw.development.facelessBlocks.gui.MachineGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class InteractListener implements Listener {

    // Abrir GUI
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            BlockState state = event.getClickedBlock().getState();
            if (state instanceof Barrel) {
                Barrel barrel = (Barrel) state;
                if (barrel.getPersistentDataContainer().has(Keys.MACHINE_ID, PersistentDataType.STRING)) {
                    event.setCancelled(true); // Bloquear GUI vanilla
                    MachineGUI gui = new MachineGUI(barrel);
                    event.getPlayer().openInventory(gui.getInventory());
                }
            }
        }
    }

    // Guardar items al cerrar
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MachineGUI) {
            MachineGUI gui = (MachineGUI) event.getInventory().getHolder();
            gui.syncFromGuiToBarrel(); // <--- EL MOMENTO MAGICO
        }
    }

    // Manejar Clics (Anti-robo y Mejoras)
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof MachineGUI) {
            MachineGUI gui = (MachineGUI) event.getInventory().getHolder();
            int slot = event.getRawSlot();
            Player player = (Player) event.getWhoClicked();

            // Si el clic es en el inventario superior (la GUI)
            if (slot < event.getInventory().getSize()) {

                // 1. Permitir clic en slots de Input/Output
                boolean isInput = Arrays.stream(MachineGUI.INPUT_SLOTS).anyMatch(i -> i == slot);
                boolean isOutput = Arrays.stream(MachineGUI.OUTPUT_SLOTS).anyMatch(i -> i == slot);

                if (isInput || isOutput) {
                    return; // Dejar pasar el evento (pueden mover items)
                }

                // 2. Bloquear todo lo demas (Botones)
                event.setCancelled(true);

                // 3. Logica de Botones
                if (slot == MachineGUI.SLOT_SPEED) {
                    handleUpgrade(player, gui.getBarrel(), Keys.UPGRADE_SPEED);
                } else if (slot == MachineGUI.SLOT_LUCK) {
                    handleUpgrade(player, gui.getBarrel(), Keys.UPGRADE_LUCK);
                }
            } else {
                // Es el inventario del jugador, permitir clic (salvo Shift-Click)
                if (event.isShiftClick()) {
                    event.setCancelled(true); // Bloquear shift-click por seguridad simple
                }
            }
        }
    }

    private void handleUpgrade(Player player, Barrel barrel, org.bukkit.NamespacedKey key) {
        // AQUI IRIA LA LOGICA DE ECONOMIA (VAULT)
        // Por ahora, solo subimos el nivel gratis para probar
        int currentLevel = barrel.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);

        if (currentLevel >= 5) {
            player.sendMessage(Component.text("§c¡Nivel máximo alcanzado!"));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
            return;
        }

        // Subir nivel
        barrel.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, currentLevel + 1);
        barrel.update();

        player.sendMessage(Component.text("§a¡Mejora aplicada a Nivel " + (currentLevel + 1) + "!"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);

        // Cerrar y reabrir para actualizar botones (o actualizar items en caliente)
        player.closeInventory();
    }
}