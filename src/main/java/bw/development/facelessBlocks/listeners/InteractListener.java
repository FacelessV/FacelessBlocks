package bw.development.facelessBlocks.listeners;

import bw.development.facelessBlocks.FacelessBlocks;
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
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class InteractListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            BlockState state = event.getClickedBlock().getState();
            if (state instanceof Barrel) {
                Barrel barrel = (Barrel) state;

                // Verificar si es Reciclador
                if (FacelessBlocks.getInstance().getMachineManager().isRecycler(barrel.getLocation())) {

                    // 1. Asegurar que los botones visuales existen en el bloque REAL antes de abrir
                    MachineGUI gui = new MachineGUI(barrel);
                    gui.updateInterface();
                    barrel.update(); // <--- IMPORTANTE: Guardamos los botones en el mundo

                    // 2. NO CANCELAMOS EL EVENTO.
                    // Al no poner 'event.setCancelled(true)', Minecraft abre el inventario real.
                    // Esto conecta al jugador con el mismo inventario que ve el Ticker.
                }
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();

        if (inv.getHolder() instanceof Barrel) {
            Barrel barrel = (Barrel) inv.getHolder();
            // Verificar ubicación para asegurar que es nuestra máquina
            if (FacelessBlocks.getInstance().getMachineManager().isRecycler(barrel.getLocation())) {

                if (event.getRawSlot() < inv.getSize()) {
                    int slot = event.getSlot();

                    if (MachineGUI.isSystemSlot(slot)) {
                        event.setCancelled(true); // Bloquear robo de botones

                        if (slot == MachineGUI.SLOT_SPEED) {
                            handleUpgrade((Player) event.getWhoClicked(), barrel, Keys.UPGRADE_SPEED);
                        } else if (slot == MachineGUI.SLOT_LUCK) {
                            handleUpgrade((Player) event.getWhoClicked(), barrel, Keys.UPGRADE_LUCK);
                        }
                    }
                } else {
                    if (event.isShiftClick()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof Barrel) {
            Barrel barrel = (Barrel) event.getSource().getHolder();
            if (FacelessBlocks.getInstance().getMachineManager().isRecycler(barrel.getLocation())) {

                ItemStack item = event.getItem();
                // Bloquear que las tolvas roben los botones de decoración
                if (item.getType() == Material.GRAY_STAINED_GLASS_PANE ||
                        item.getType() == Material.LIME_STAINED_GLASS_PANE ||
                        item.getType() == Material.RED_STAINED_GLASS_PANE ||
                        item.getType() == Material.EMERALD_BLOCK ||
                        (item.hasItemMeta() && item.getItemMeta().hasDisplayName())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleUpgrade(Player player, Barrel barrel, org.bukkit.NamespacedKey key) {
        int currentLevel = barrel.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
        if (currentLevel >= 5) {
            player.sendMessage(Component.text("§c¡Nivel máximo alcanzado!"));
            return;
        }

        barrel.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, currentLevel + 1);
        barrel.update();
        new MachineGUI(barrel).updateStatusIcon(); // Actualizar visualmente
        barrel.update(); // Guardar cambio visual

        player.sendMessage(Component.text("§a¡Mejora aplicada a Nivel " + (currentLevel + 1) + "!"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
    }
}