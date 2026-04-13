package com.paperteleporter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlatformListener implements Listener {
    private static final String GUI_TITLE_PREFIX = ChatColor.DARK_AQUA + "Teleport Platforms";
    private static final int GUI_SIZE = 54;
    private static final int MAX_GUI_PAGES = 4;

    private final Map<UUID, Integer> currentPageByPlayer = new HashMap<>();

    private final PlatformManager platformManager;

    public PlatformListener(PlatformManager platformManager) {
        this.platformManager = platformManager;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (p.isOp() && p.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (platformManager.isProtectedBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "This teleport platform is protected. Use /pt remove <id> to remove it.");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.isOp() && p.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (platformManager.isProtectedBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "This teleport platform is protected. Use /pt remove <id> to remove it.");
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
        currentPageByPlayer.put(player.getUniqueId(), 0);
        openPage(player, 0);
    }

    private void openPage(Player player, int requestedPage) {
        Collection<PlatformData> platforms = platformManager.allPlatforms();
        List<PlatformData> platformList = new ArrayList<>(platforms);

        int totalPages = Math.max(1, (platformList.size() + GUI_SIZE - 1) / GUI_SIZE);
        totalPages = Math.min(MAX_GUI_PAGES, totalPages);

        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        currentPageByPlayer.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE_PREFIX + ChatColor.GRAY + " [" + (page + 1) + "/" + totalPages + "]");

        int start = page * GUI_SIZE;
        int end = Math.min(start + GUI_SIZE, platformList.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            PlatformData platform = platformList.get(i);
            ItemStack item = new ItemStack(Material.ENDER_PEARL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + platform.getId());
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }

        player.openInventory(inventory);
        player.sendMessage(ChatColor.GRAY + "GUI pages: press hotbar 1 = previous, hotbar 9 = next.");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(GUI_TITLE_PREFIX)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbar = event.getHotbarButton();
            Integer currentPage = currentPageByPlayer.getOrDefault(player.getUniqueId(), 0);
            if (hotbar == 0) {
                openPage(player, currentPage - 1);
                return;
            }
            if (hotbar == 8) {
                openPage(player, currentPage + 1);
                return;
            }
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
