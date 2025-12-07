package bw.development.facelessBlocks.utils;

import bw.development.facelessBlocks.FacelessBlocks;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class RepairUtils {

    // Verifica si el item es reparable (tiene daño y está configurado)
    public static Material getRepairMaterial(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        // Verificar si tiene daño
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable)) return null;
        if (((Damageable) meta).getDamage() == 0) return null; // Está nuevo

        // Buscar en config
        ConfigurationSection section = FacelessBlocks.getInstance().getConfig().getConfigurationSection("blocks.repairer.repair_materials");
        if (section == null) return null;

        String matName = section.getString(item.getType().name());
        if (matName != null) {
            return Material.getMaterial(matName);
        }

        return null; // No configurado
    }

    // Repara el item totalmente
    public static void repairItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            ((Damageable) meta).setDamage(0);
            item.setItemMeta(meta);
        }
    }
}