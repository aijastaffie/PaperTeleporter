package com.paperteleporter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;

public final class PlatformListener implements Listener {
    private static final String GUI_TITLE = ChatColor.DARK_AQUA + "Teleport Platforms";

    private final PlatformManager platformManager;

    public PlatformListener(PlatformManager platformManager) {
        this.platformManager = platformManager;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (platformManager.isProtectedBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This teleport platform is protected. Use /pt remove <id> to remove it.");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (platformManager.isProtectedBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This teleport platform is protected. Use /pt remove <id> to remove it.");
        }
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        PlatformData clickedPlatform = platformManager.findByNpc(entity.getUniqueId());
        if (clickedPlatform == null) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        Inventory inventory = Bukkit.createInventory(null, 54, GUI_TITLE);

        Collection<PlatformData> platforms = platformManager.allPlatforms();
        int slot = 0;
        for (PlatformData platform : platforms) {
            if (slot >= inventory.getSize()) {
                break;
            }
            ItemStack item = new ItemStack(Material.ENDER_PEARL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + platform.getId());
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = currentItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        String id = ChatColor.stripColor(meta.getDisplayName());
        for (PlatformData platform : platformManager.allPlatforms()) {
            if (platform.getId().equalsIgnoreCase(id)) {
                platformManager.teleport(player, platform);
                player.closeInventory();
                return;
            }
        }

        player.sendMessage(ChatColor.RED + "Platform not found anymore.");
    }
}
