package bw.development.facelessBlocks.data;

import org.bukkit.Material;

public class MachineData {
    // Niveles de mejora
    private int speedLevel = 0;
    private int luckLevel = 0;

    // Estado del proceso
    private boolean processing = false;
    private int timeLeft = 0;
    private Material outputMaterial = null; // Qué va a dar

    public MachineData() {}

    // Getters y Setters simples
    public int getSpeedLevel() { return speedLevel; }
    public void setSpeedLevel(int speedLevel) { this.speedLevel = speedLevel; }

    public int getLuckLevel() { return luckLevel; }
    public void setLuckLevel(int luckLevel) { this.luckLevel = luckLevel; }

    public boolean isProcessing() { return processing; }
    public void setProcessing(boolean processing) { this.processing = processing; }

    public int getTimeLeft() { return timeLeft; }
    public void setTimeLeft(int timeLeft) { this.timeLeft = timeLeft; }

    public Material getOutputMaterial() { return outputMaterial; }
    public void setOutputMaterial(Material outputMaterial) { this.outputMaterial = outputMaterial; }
}