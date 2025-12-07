package bw.development.facelessBlocks.commands;

import bw.development.facelessBlocks.FacelessBlocks;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class MachineCommands implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Comando: /giverecycler
        if (command.getName().equalsIgnoreCase("giverecycler")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cSolo jugadores.");
                return true;
            }
            Player player = (Player) sender;

            // Crear el ítem del Reciclador
            ItemStack item = new ItemStack(Material.BARREL);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("Reciclador")); // El nombre exacto que busca tu BlockListener
            item.setItemMeta(meta);

            player.getInventory().addItem(item);
            player.sendMessage(Component.text("§aHas recibido un Reciclador."));
            return true;
        }

        // Comando: /clearrecyclers (Emergencia)
        if (command.getName().equalsIgnoreCase("clearrecyclers")) {
            if (!sender.hasPermission("facelessblocks.admin")) {
                sender.sendMessage("§cNo tienes permiso.");
                return true;
            }

            int count = 0;
            // 1. Obtener todas las ubicaciones actuales
            // Creamos una copia del Set para evitar errores al modificar mientras iteramos (si fuera el caso)
            Set<Location> locations = new HashSet<>(FacelessBlocks.getInstance().getMachineManager().getAllMachines().keySet());

            for (Location loc : locations) {
                // Borrar el bloque físico del mundo
                if (loc.getWorld() != null && loc.getBlock().getType() == Material.BARREL) {
                    loc.getBlock().setType(Material.AIR);
                    count++;
                }
            }

            // 2. Limpiar la memoria del Manager
            FacelessBlocks.getInstance().getMachineManager().getAllMachines().clear();

            // 3. Guardar el archivo vacío para que no reaparezcan al reiniciar
            FacelessBlocks.getInstance().getMachineManager().saveAsync();

            sender.sendMessage(Component.text("§eSe han eliminado " + count + " recicladores activos y limpiado la base de datos."));
            return true;
        }

        return false;
    }
}