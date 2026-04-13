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

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /pt <add|remove|rotate|preset|save-preset|spawnpoint|backup> ...");
            return true;
        }

        String action = args[0].toLowerCase();
        String id = args.length >= 2 ? args[1] : "";

        if (args.length >= 2 && !action.equals("add") && !id.matches("[a-zA-Z0-9_-]{3,40}")) {
            player.sendMessage(ChatColor.RED + "Platform name must match [a-zA-Z0-9_-] and be 3-40 chars.");
            return true;
        }

        if ("add".equals(action)) {
            if (args.length < 2 || args.length > 3) {
                player.sendMessage(ChatColor.RED + "Usage: /pt add <platformName> [presetNumber]");
                return true;
            }
            if (!id.matches("[a-zA-Z0-9_-]{3,40}")) {
                player.sendMessage(ChatColor.RED + "Platform name must match [a-zA-Z0-9_-] and be 3-40 chars.");
                return true;
            }
            Block target = player.getTargetBlockExact(20);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Look at a block within 20 blocks.");
                return true;
            }
            if (target.getType() == org.bukkit.Material.SNOW) {
                target = target.getRelative(org.bukkit.block.BlockFace.DOWN);
            }
            Location anchorLocation = target.getLocation().add(0, 1, 0);

            if (args.length == 2) {
                player.sendMessage(platformManager.createPlatform(player, id, anchorLocation));
                return true;
            }

            try {
                int presetNumber = Integer.parseInt(args[2]);
                player.sendMessage(platformManager.createPlatformWithPreset(player, id, anchorLocation, presetNumber));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Preset number must be a number.");
            }
            return true;
        }

        if ("remove".equals(action)) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /pt remove <platformName>");
                return true;
            }
            player.sendMessage(platformManager.removePlatform(id));
            return true;
        }

        if ("rotate".equals(action)) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /pt rotate <platformName>");
                return true;
            }
            player.sendMessage(platformManager.rotatePlatform(id));
            return true;
        }

        if ("preset".equals(action)) {
            if (args.length == 2 && "remove".equalsIgnoreCase(id)) {
                player.sendMessage(ChatColor.RED + "Usage: /pt preset remove <number>");
                return true;
            }
            if (args.length == 3 && "remove".equalsIgnoreCase(id)) {
                try {
                    int presetNumber = Integer.parseInt(args[2]);
                    player.sendMessage(platformManager.removeCustomPreset(presetNumber));
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Preset number must be a number.");
                }
                return true;
            }
            if (args.length != 3) {
                player.sendMessage(ChatColor.RED + "Usage: /pt preset <platformName> <presetNumber>");
                return true;
            }
            try {
                int presetNumber = Integer.parseInt(args[2]);
                player.sendMessage(platformManager.changePreset(id, presetNumber));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Preset must be a number.");
            }
            return true;
        }

        if ("save-preset".equals(action)) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /pt save-preset");
                return true;
            }

            Location centerLocation = player.getLocation().clone().subtract(0, 1, 0).getBlock().getRelative(org.bukkit.block.BlockFace.UP).getLocation();
            player.sendMessage(platformManager.saveCustomPreset(player, centerLocation));
            return true;
        }

        if ("spawnpoint".equals(action)) {
            if (args.length == 1) {
                player.sendMessage(ChatColor.GREEN + "Current minimum spawn distance: " + platformManager.getMinSpawnDistance() + " blocks.");
                return true;
            }
            if (args.length == 2) {
                try {
                    int distance = Integer.parseInt(id);
                    player.sendMessage(platformManager.setMinSpawnDistance(distance));
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Distance must be a number (minimum 20).");
                }
                return true;
            }
            player.sendMessage(ChatColor.RED + "Usage: /pt spawnpoint [distance]");
            return true;
        }

        if ("backup".equals(action)) {
            if (args.length != 2 || !"now".equalsIgnoreCase(args[1])) {
                player.sendMessage(ChatColor.RED + "Usage: /pt backup now");
                return true;
            }
            player.sendMessage(platformManager.createManualBackup());
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown action. Use add, remove, rotate, preset, save-preset, spawnpoint or backup.");
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
            if ("preset".startsWith(args[0].toLowerCase())) {
                options.add("preset");
            }
            if ("save-preset".startsWith(args[0].toLowerCase())) {
                options.add("save-preset");
            }
            if ("spawnpoint".startsWith(args[0].toLowerCase())) {
                options.add("spawnpoint");
            }
            if ("backup".startsWith(args[0].toLowerCase())) {
                options.add("backup");
            }
            return options;
        }

        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if ("preset".equalsIgnoreCase(action)) {
                List<String> options = new ArrayList<>();
                if ("remove".startsWith(args[1].toLowerCase())) {
                    options.add("remove");
                }
                Collection<PlatformData> platforms = platformManager.allPlatforms();
                String prefix = args[1].toLowerCase();
                for (PlatformData platform : platforms) {
                    if (platform.getId().startsWith(prefix)) {
                        options.add(platform.getId());
                    }
                }
                return options;
            }
            if ("remove".equalsIgnoreCase(action) || "rotate".equalsIgnoreCase(action) || "preset".equalsIgnoreCase(action)) {
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
            if ("save-preset".equalsIgnoreCase(action)) {
                return List.of();
            }
            if ("backup".equalsIgnoreCase(action)) {
                if ("now".startsWith(args[1].toLowerCase())) {
                    return List.of("now");
                }
                return List.of();
            }
        }

        if (args.length == 3) {
            String action = args[0].toLowerCase();
            if ("add".equalsIgnoreCase(action)) {
                List<String> presetNumbers = new ArrayList<>();
                String prefix = args[2];
                for (int i = 1; i <= 99; i++) {
                    if (i <= 6 || platformManager.getCustomPreset(i) != null) {
                        if (String.valueOf(i).startsWith(prefix)) {
                            presetNumbers.add(String.valueOf(i));
                        }
                    }
                }
                return presetNumbers;
            }
            if ("preset".equalsIgnoreCase(action)) {
                List<String> presetNumbers = new ArrayList<>();
                String prefix = args[2];
                for (int i = 1; i <= 99; i++) {
                    if (i <= 6 || platformManager.getCustomPreset(i) != null) {
                        if (String.valueOf(i).startsWith(prefix)) {
                            presetNumbers.add(String.valueOf(i));
                        }
                    }
                }
                return presetNumbers;
            }
        }

        return List.of();
    }
}
