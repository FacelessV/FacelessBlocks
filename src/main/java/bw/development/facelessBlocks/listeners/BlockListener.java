package bw.development.facelessBlocks.listeners;

import bw.development.facelessBlocks.FacelessBlocks;
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

public class BlockListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.BARREL) return;

        String rawName = "Sin Nombre";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            rawName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }

        if (!item.hasItemMeta()) return;
        String name = rawName.trim();
        String type = null;

        // DETECTAR TIPO SEGÚN NOMBRE
        if (name.equalsIgnoreCase("Reciclador")) {
            type = "RECYCLER";
        } else if (name.equalsIgnoreCase("Reparador")) {
            type = "REPAIRER";
        } else if (name.equalsIgnoreCase("Desencantador")) {
            type = "DISENCHANTER";
        }

        if (type != null) {
            BlockState state = event.getBlockPlaced().getState();
            if (state instanceof Barrel) {
                // Crear máquina con el tipo correcto
                FacelessBlocks.getInstance().getMachineManager().createMachine(event.getBlockPlaced().getLocation(), type);
                event.getPlayer().sendMessage(Component.text("§a[FacelessBlocks] ¡Has creado un " + name + " correctamente!"));
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.BARREL) {
            if (FacelessBlocks.getInstance().getMachineManager().isMachine(event.getBlock().getLocation())) {
                FacelessBlocks.getInstance().getMachineManager().removeMachine(event.getBlock().getLocation());
                event.getPlayer().sendMessage(Component.text("§e[FacelessBlocks] Máquina eliminada."));
            }
        }
    }
}