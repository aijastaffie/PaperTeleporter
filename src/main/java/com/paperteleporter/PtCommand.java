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
            player.sendMessage(ChatColor.RED + "Usage: /pt <add|remove|rotate|preset|spawnpoint> <platformName> [preset]");
            return true;
        }

        String action = args[0].toLowerCase();
        String id = args.length >= 2 ? args[1] : "";

        if (args.length >= 2 && !action.equals("add") && !id.matches("[a-zA-Z0-9_-]{3,40}")) {
            player.sendMessage(ChatColor.RED + "Platform name must match [a-zA-Z0-9_-] and be 3-40 chars.");
            return true;
        }

        if ("add".equals(action)) {
            if (!id.matches("[a-zA-Z0-9_-]{3,40}")) {
                player.sendMessage(ChatColor.RED + "Platform name must match [a-zA-Z0-9_-] and be 3-40 chars.");
                return true;
            }
            Block target = player.getTargetBlockExact(20);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Look at a block within 20 blocks.");
                return true;
            }
            player.sendMessage(platformManager.createPlatform(player, id, target.getLocation()));
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
                player.sendMessage(ChatColor.RED + "Usage: /pt preset <platformName> <1-6>");
                return true;
            }
            try {
                int presetNumber = Integer.parseInt(args[2]);
                if (!Preset.isValid(presetNumber)) {
                    player.sendMessage(ChatColor.RED + "Preset must be 1-6.");
                    return true;
                }
                player.sendMessage(platformManager.changePreset(id, presetNumber));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Preset must be a number 1-6.");
            }
            return true;
        }

        if ("save-preset".equals(action)) {
            if (args.length != 3) {
                player.sendMessage(ChatColor.RED + "Usage: /pt save-preset <name> <number>");
                return true;
            }
            String presetName = id;
            try {
                int presetNumber = Integer.parseInt(args[2]);
                if (presetNumber < 1 || presetNumber > 99) {
                    player.sendMessage(ChatColor.RED + "Custom preset number must be 1-99.");
                    return true;
                }
                Block targetBlock = player.getTargetBlockExact(20);
                if (targetBlock == null) {
                    player.sendMessage(ChatColor.RED + "Look at a block within 20 blocks. This will be the center of the 7x7x7.");
                    return true;
                }
                player.sendMessage(platformManager.saveCustomPreset(player, presetName, presetNumber, targetBlock.getLocation()));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Preset number must be a number.");
            }
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

        player.sendMessage(ChatColor.RED + "Unknown action. Use add, remove, rotate, preset or spawnpoint.");
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
                return new ArrayList<>(); // Name suggestions could be empty or populated from history
            }
        }

        if (args.length == 3) {
            String action = args[0].toLowerCase();
            if ("preset".equalsIgnoreCase(action)) {
                List<String> presetNumbers = new ArrayList<>();
                String prefix = args[2];
                for (int i = 1; i <= 6; i++) {
                    if (String.valueOf(i).startsWith(prefix)) {
                        presetNumbers.add(String.valueOf(i));
                    }
                }
                return presetNumbers;
            }
            if ("save-preset".equalsIgnoreCase(action)) {
                List<String> presetNumbers = new ArrayList<>();
                String prefix = args[2];
                for (int i = 1; i <= 99; i++) {
                    if (String.valueOf(i).startsWith(prefix)) {
                        presetNumbers.add(String.valueOf(i));
                    }
                }
                return presetNumbers;
            }
        }

        return List.of();
    }
}
