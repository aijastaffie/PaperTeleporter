package com.paperteleporter;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class ChestAccessListener implements Listener {
    private final PlatformManager platformManager;
    private final RoleStore roleStore;

    public ChestAccessListener(PlatformManager platformManager, RoleStore roleStore) {
        this.platformManager = platformManager;
        this.roleStore = roleStore;
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }

        // Check if it's a chest
        if (!(holder instanceof org.bukkit.block.Chest)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Get the chest location and check if it's a platform chest
        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) holder;
        Block chestBlock = chest.getBlock();
        BlockPoint chestLocation = BlockPoint.fromLocation(chestBlock.getLocation());

        // Check if this chest belongs to a platform
        PlatformData platform = platformManager.getPlatformByChestLocation(chestLocation);
        if (platform == null) {
            // Not a platform chest, allow normally
            return;
        }

        // Check if player is OP or has King role
        boolean isOp = player.isOp();
        boolean isKing = roleStore.getRole(player.getUniqueId()) == UserRole.KING;

        if (!isOp && !isKing) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Only King or OP can access platform chests.", NamedTextColor.RED));
        }
    }
}
