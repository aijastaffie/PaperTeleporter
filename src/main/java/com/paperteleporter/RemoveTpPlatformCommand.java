package com.paperteleporter;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RemoveTpPlatformCommand implements CommandExecutor {
    private final PlatformManager platformManager;

    public RemoveTpPlatformCommand(PlatformManager platformManager) {
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

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /removetpplatform <id>");
            return true;
        }

        player.sendMessage(platformManager.removePlatform(args[0]));
        return true;
    }
}
