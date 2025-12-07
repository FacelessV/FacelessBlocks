package bw.development.facelessBlocks.listeners;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class BlockListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        // Filtro rápido
        if (item.getType() != Material.BARREL) return;

        // --- DEBUG DE DIAGNOSTICO ---
        String rawName = "Sin Nombre/Meta";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            rawName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        event.getPlayer().sendMessage(Component.text("§e[DEBUG] Barril colocado. Nombre detectado: '" + rawName + "'"));
        // ----------------------------

        if (!item.hasItemMeta()) return;

        // Comprobación de nombre
        if (rawName.trim().equalsIgnoreCase("Reciclador")) {

            BlockState state = event.getBlockPlaced().getState();
            if (state instanceof Barrel) {
                Barrel barrel = (Barrel) state;

                // 1. Marca de identidad en el bloque (Solo el ID, para que sepamos qué es)
                // Ya no guardamos Speed/Luck/Time aquí, eso va a la RAM.
                barrel.getPersistentDataContainer().set(Keys.MACHINE_ID, PersistentDataType.STRING, "RECYCLER");
                barrel.update();

                // 2. REGISTRAR EN EL MANAGER (Memoria RAM)
                // Esto crea la entrada en el mapa con niveles a 0 por defecto.
                FacelessBlocks.getInstance().getMachineManager().createMachine(event.getBlockPlaced().getLocation());

                event.getPlayer().sendMessage(Component.text("§a[FacelessBlocks] ¡Has creado un Reciclador correctamente!"));
            }
        } else {
            if (!rawName.equals("Sin Nombre/Meta")) {
                event.getPlayer().sendMessage(Component.text("§c[DEBUG] Fallo: '" + rawName + "' no es igual a 'Reciclador'"));
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.BARREL) {
            // Usamos el nuevo método isMachine() del Manager
            if (FacelessBlocks.getInstance().getMachineManager().isMachine(event.getBlock().getLocation())) {

                // Borramos los datos de la memoria y del archivo
                FacelessBlocks.getInstance().getMachineManager().removeMachine(event.getBlock().getLocation());

                event.getPlayer().sendMessage(Component.text("§e[FacelessBlocks] Reciclador eliminado."));
            }
        }
    }
}