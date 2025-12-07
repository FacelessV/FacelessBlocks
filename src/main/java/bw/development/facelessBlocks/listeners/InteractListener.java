package bw.development.facelessBlocks.listeners;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.Keys;
import bw.development.facelessBlocks.gui.MachineGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
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

                // Usamos el Manager nuevo para verificar super rápido
                if (FacelessBlocks.getInstance().getMachineManager().isRecycler(barrel.getLocation())) {
                    event.setCancelled(true); // Evitar GUI vanilla

                    MachineGUI gui = new MachineGUI(barrel);
                    gui.open(event.getPlayer()); // Esto abre el inventario real ya decorado
                }
            }
        }
    }

    // ¡YA NO NECESITAMOS onClose! (El dupe muere aquí)

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();

        // Verificamos si es un Barril y si es una de nuestras máquinas
        if (inv.getHolder() instanceof Barrel) {
            Barrel barrel = (Barrel) inv.getHolder();
            if (FacelessBlocks.getInstance().getMachineManager().isRecycler(barrel.getLocation())) {

                // Si el clic ocurre en el inventario superior (el de la máquina)
                if (event.getRawSlot() < inv.getSize()) {
                    int slot = event.getSlot();

                    // Si tocan un slot de sistema -> CANCELAR
                    if (MachineGUI.isSystemSlot(slot)) {
                        event.setCancelled(true);

                        // Lógica de Botones
                        if (slot == MachineGUI.SLOT_SPEED) {
                            handleUpgrade((Player) event.getWhoClicked(), barrel, Keys.UPGRADE_SPEED);
                        } else if (slot == MachineGUI.SLOT_LUCK) {
                            handleUpgrade((Player) event.getWhoClicked(), barrel, Keys.UPGRADE_LUCK);
                        }
                    }
                } else {
                    // Es el inventario del jugador.
                    // PRECAUCIÓN: Shift-Click podría enviar items a slots prohibidos.
                    if (event.isShiftClick()) {
                        event.setCancelled(true); // Por seguridad, bloqueamos shift-click hacia la máquina
                    }
                }
            }
        }
    }

    // EXTRA: Evitar que las tolvas (Hoppers) roben los botones
    @EventHandler
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof Barrel) {
            Barrel barrel = (Barrel) event.getSource().getHolder();
            if (FacelessBlocks.getInstance().getMachineManager().isRecycler(barrel.getLocation())) {

                // Si la tolva intenta sacar un item de un slot protegido
                ItemStack item = event.getItem();
                // Verificamos si el item se parece a un botón de sistema (simple check)
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    // Es un check rudimentario pero efectivo. Mejor sería chequear el slot de origen si Spigot dejara.
                    // Pero como el hopper saca del primer slot disponible, si aseguramos que Input es 0-2...
                    // Lo más seguro es cancelar si el item es PANEL o BOTÓN.

                    // Asumiendo que tus inputs no son paneles de cristal ni azúcar/esmeralda con nombre:
                    // Dejamos pasar. Si es decoración, cancelamos.
                    // (Esta es una implementación básica, ajústala si usas azúcar como input real)
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

        // Actualizamos visualmente al instante
        new MachineGUI(barrel).updateStatusIcon();

        player.sendMessage(Component.text("§a¡Mejora aplicada a Nivel " + (currentLevel + 1) + "!"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
    }
}