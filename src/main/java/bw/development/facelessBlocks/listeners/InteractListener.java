package bw.development.facelessBlocks.listeners;

import bw.development.facelessBlocks.FacelessBlocks;
import bw.development.facelessBlocks.data.MachineData;
import bw.development.facelessBlocks.gui.MachineGUI;
import bw.development.facelessBlocks.hooks.PointsHook;
import bw.development.facelessBlocks.hooks.VaultHook;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            BlockState state = event.getClickedBlock().getState();
            if (state instanceof Barrel) {
                Barrel barrel = (Barrel) state;
                if (FacelessBlocks.getInstance().getMachineManager().isMachine(barrel.getLocation())) {
                    MachineGUI gui = new MachineGUI(barrel);
                    gui.updateInterface();
                }
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof Barrel) {
            Barrel barrel = (Barrel) inv.getHolder();
            if (FacelessBlocks.getInstance().getMachineManager().isMachine(barrel.getLocation())) {
                if (event.getRawSlot() < inv.getSize()) {
                    int slot = event.getSlot();
                    if (MachineGUI.isSystemSlot(slot)) {
                        event.setCancelled(true);

                        // Lógica de Compra
                        if (slot == MachineGUI.SLOT_SPEED) {
                            handleUpgrade((Player) event.getWhoClicked(), barrel, "SPEED");
                        } else if (slot == MachineGUI.SLOT_LUCK) {
                            handleUpgrade((Player) event.getWhoClicked(), barrel, "LUCK");
                        }
                    }
                } else {
                    if (event.isShiftClick()) event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof Barrel) {
            Barrel barrel = (Barrel) event.getSource().getHolder();
            if (FacelessBlocks.getInstance().getMachineManager().isMachine(barrel.getLocation())) {
                ItemStack item = event.getItem();
                if (item.getType() == Material.GRAY_STAINED_GLASS_PANE ||
                        item.getType() == Material.LIME_STAINED_GLASS_PANE ||
                        item.getType() == Material.RED_STAINED_GLASS_PANE ||
                        item.getType() == Material.EMERALD_BLOCK ||
                        (item.hasItemMeta() && item.getItemMeta().hasDisplayName())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleUpgrade(Player player, Barrel barrel, String type) {
        MachineData data = FacelessBlocks.getInstance().getMachineManager().getMachine(barrel.getLocation());
        if (data == null) return;

        int currentLevel = 0;
        int maxLevel = 5;
        double cost = 0;

        if (type.equals("SPEED")) {
            currentLevel = data.getSpeedLevel();
            cost = MachineGUI.calculateCost(1000, 1.5, currentLevel);
        } else if (type.equals("LUCK")) {
            currentLevel = data.getLuckLevel();
            cost = MachineGUI.calculateCost(2500, 2.0, currentLevel);
        }

        if (currentLevel >= maxLevel) {
            player.sendMessage(Component.text("§c¡Nivel máximo alcanzado!"));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 0.5f);
            return;
        }

        // --- LÓGICA DE COBRO (DUAL) ---
        String ecoType = FacelessBlocks.getInstance().getConfig().getString("economy_type", "VAULT");

        if (ecoType.equalsIgnoreCase("POINTS")) {
            // Usar PlayerPoints
            int costInt = (int) cost;
            if (PointsHook.getAPI().look(player.getUniqueId()) < costInt) {
                player.sendMessage(Component.text("§cNo tienes suficientes puntos. Necesitas §e" + costInt + " Puntos"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }
            PointsHook.getAPI().take(player.getUniqueId(), costInt);
            player.sendMessage(Component.text("§7Se te han cobrado §c" + costInt + " Puntos"));

        } else {
            // Usar Vault (Dinero)
            if (!VaultHook.getEconomy().has(player, cost)) {
                player.sendMessage(Component.text("§cNo tienes suficiente dinero. Necesitas §e$" + (int)cost));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }
            VaultHook.getEconomy().withdrawPlayer(player, cost);
            player.sendMessage(Component.text("§7Se te han cobrado §c$" + (int)cost));
        }

        // Aplicar mejora
        if (type.equals("SPEED")) {
            data.setSpeedLevel(currentLevel + 1);
        } else if (type.equals("LUCK")) {
            data.setLuckLevel(currentLevel + 1);
        }

        FacelessBlocks.getInstance().getMachineManager().saveAsync();
        new MachineGUI(barrel).updateStatusIcon();

        player.sendMessage(Component.text("§a¡Mejora comprada! Nuevo nivel: " + (currentLevel + 1)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
    }
}