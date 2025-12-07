package bw.development.facelessBlocks.listeners;

import bw.development.facelessBlocks.data.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BlockListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        // Si no es barril, ignoramos (para no spamear chat con otros bloques)
        if (item.getType() != Material.BARREL) return;

        // --- DEBUG DE DIAGNOSTICO ---
        String rawName = "Sin Nombre/Meta";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            // Obtenemos el nombre tal cual lo ve el plugin
            rawName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }

        // ESTE MENSAJE SALDRA SIEMPRE QUE PONGAS UN BARRIL
        event.getPlayer().sendMessage(Component.text("§e[DEBUG] Barril colocado. Nombre detectado: '" + rawName + "'"));
        // ----------------------------

        // Validación
        if (!item.hasItemMeta()) return;

        // Comprobacion flexible
        if (rawName.trim().equalsIgnoreCase("Reciclador")) { // .trim() quita espacios accidentales

            BlockState state = event.getBlockPlaced().getState();
            if (state instanceof Barrel) {
                Barrel barrel = (Barrel) state;

                // Configuracion inicial del bloque
                barrel.getPersistentDataContainer().set(Keys.MACHINE_ID, PersistentDataType.STRING, "RECYCLER");
                barrel.getPersistentDataContainer().set(Keys.UPGRADE_SPEED, PersistentDataType.INTEGER, 0);
                barrel.getPersistentDataContainer().set(Keys.UPGRADE_LUCK, PersistentDataType.INTEGER, 0);
                barrel.getPersistentDataContainer().set(Keys.PROCESS_TIME, PersistentDataType.INTEGER, 0);
                barrel.getPersistentDataContainer().set(Keys.IS_PROCESSING, PersistentDataType.INTEGER, 0);

                barrel.update();

                event.getPlayer().sendMessage(Component.text("§a[FacelessBlocks] ¡Has creado un Reciclador correctamente!"));
            }
        } else {
            // Mensaje para saber por que fallo si tiene nombre pero no es igual
            if (!rawName.equals("Sin Nombre/Meta")) {
                event.getPlayer().sendMessage(Component.text("§c[DEBUG] Fallo: '" + rawName + "' no es igual a 'Reciclador'"));
            }
        }
    }
}