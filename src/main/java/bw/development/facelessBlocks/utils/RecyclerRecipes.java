package bw.development.facelessBlocks.utils;

import bw.development.facelessBlocks.FacelessBlocks;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RecyclerRecipes {

    private static final Random random = new Random();

    // Estructura simple para guardar el resultado
    public static class RecycleResult {
        public Material material;
        public int min;
        public int max;

        public RecycleResult(Material material, int min, int max) {
            this.material = material;
            this.min = min;
            this.max = max;
        }
    }

    public static RecycleResult getResult(Material input) {
        ConfigurationSection section = FacelessBlocks.getInstance().getConfig().getConfigurationSection("blocks.recycler.recipes");
        if (section == null) return null;

        if (section.contains(input.name())) {
            String matName = section.getString(input.name() + ".result");
            int min = section.getInt(input.name() + ".min");
            int max = section.getInt(input.name() + ".max");

            Material outMat = Material.getMaterial(matName);
            if (outMat != null) {
                return new RecycleResult(outMat, min, max);
            }
        }
        return null;
    }

    public static ItemStack generateOutput(RecycleResult result, int luckLevel) {
        // Logica simple de suerte: cada nivel de suerte aumenta un poco la cantidad maxima potencial
        // o asegura que salga al menos el minimo.
        int amount = random.nextInt((result.max - result.min) + 1) + result.min;

        // Bonus por suerte (ejemplo simple)
        if (luckLevel > 0 && random.nextBoolean()) {
            amount += 1;
        }

        return new ItemStack(result.material, amount);
    }
}