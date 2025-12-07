package bw.development.facelessBlocks.commands;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.Keys;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

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
            // Usamos tu nuevo Manager para limpiar
            for (Location loc : FacelessBlocks.getInstance().getMachineManager().getRecyclers()) {
                if (loc.getBlock().getType() == Material.BARREL) {
                    loc.getBlock().setType(Material.AIR); // Borrar bloque físico
                    count++;
                }
            }
            // Limpiar la lista de memoria/archivo
            FacelessBlocks.getInstance().getMachineManager().getRecyclers().clear();
            // (Opcional: aquí deberías llamar a un método save() forzado en el manager si lo tuvieras público,
            // pero por ahora basta con que al romperse se borren).

            sender.sendMessage(Component.text("§eSe han eliminado " + count + " recicladores activos."));
            return true;
        }

        return false;
    }
}