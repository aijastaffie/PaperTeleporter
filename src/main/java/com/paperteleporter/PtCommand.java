package com.paperteleporter;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PtCommand implements CommandExecutor, TabCompleter {
    private final PlatformManager platformManager;

    public PtCommand(PlatformManager platformManager) {
        this.platformManager = platformManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Only OP can use this command.");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /pt <add|remove|rotate> <platformName>");
            return true;
        }

        String action = args[0].toLowerCase();
        String id = args[1];

        if (!id.matches("[a-zA-Z0-9_-]{3,40}")) {
            player.sendMessage(ChatColor.RED + "Platform name must match [a-zA-Z0-9_-] and be 3-40 chars.");
            return true;
        }

        if ("add".equals(action)) {
            Block target = player.getTargetBlockExact(20);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Look at a block within 20 blocks.");
                return true;
            }

            Location anchor = target.getLocation();
            player.sendMessage(platformManager.createPlatform(player, id, anchor));
            return true;
        }

        if ("remove".equals(action)) {
            player.sendMessage(platformManager.removePlatform(id));
            return true;
        }

        if ("rotate".equals(action)) {
            player.sendMessage(platformManager.rotatePlatform(id));
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown action. Use add, remove or rotate.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if ("add".startsWith(args[0].toLowerCase())) {
                options.add("add");
            }
            if ("remove".startsWith(args[0].toLowerCase())) {
                options.add("remove");
            }
            if ("rotate".startsWith(args[0].toLowerCase())) {
                options.add("rotate");
            }
            return options;
        }

        if (args.length == 2 && ("remove".equalsIgnoreCase(args[0]) || "rotate".equalsIgnoreCase(args[0]))) {
            Collection<PlatformData> platforms = platformManager.allPlatforms();
            List<String> ids = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (PlatformData platform : platforms) {
                if (platform.getId().startsWith(prefix)) {
                    ids.add(platform.getId());
                }
            }
            return ids;
        }

        return List.of();
    }
}
