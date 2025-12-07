package bw.development.facelessBlocks.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MachineData {
    private String machineId = "RECYCLER";

    private int speedLevel = 0;
    private int luckLevel = 0;
    private int efficiencyLevel = 0;

    private boolean processing = false;
    private int timeLeft = 0;
    private Material outputMaterial = null;

    // --- NUEVO CAMPO PARA EL REPARADOR ---
    private ItemStack storedItem = null;

    public MachineData() {}

    public MachineData(String machineId) {
        this.machineId = machineId;
    }

    // Getters y Setters básicos
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public int getSpeedLevel() { return speedLevel; }
    public void setSpeedLevel(int speedLevel) { this.speedLevel = speedLevel; }

    public int getLuckLevel() { return luckLevel; }
    public void setLuckLevel(int luckLevel) { this.luckLevel = luckLevel; }

    public int getEfficiencyLevel() { return efficiencyLevel; }
    public void setEfficiencyLevel(int efficiencyLevel) { this.efficiencyLevel = efficiencyLevel; }

    public boolean isProcessing() { return processing; }
    public void setProcessing(boolean processing) { this.processing = processing; }

    public int getTimeLeft() { return timeLeft; }
    public void setTimeLeft(int timeLeft) { this.timeLeft = timeLeft; }

    public Material getOutputMaterial() { return outputMaterial; }
    public void setOutputMaterial(Material outputMaterial) { this.outputMaterial = outputMaterial; }

    // --- GETTER/SETTER NUEVO ---
    public ItemStack getStoredItem() { return storedItem; }
    public void setStoredItem(ItemStack storedItem) { this.storedItem = storedItem; }
}