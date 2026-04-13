package com.paperteleporter;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetTpPlatformCommand implements CommandExecutor {
    private final PlatformManager platformManager;

    public SetTpPlatformCommand(PlatformManager platformManager) {
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

        String id = args.length > 0 ? args[0] : "platform-" + System.currentTimeMillis();
        if (!id.matches("[a-zA-Z0-9_-]{3,40}")) {
            player.sendMessage(ChatColor.RED + "ID must match [a-zA-Z0-9_-] and be 3-40 chars.");
            return true;
        }

        Block target = player.getTargetBlockExact(20);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Look at a block within 20 blocks.");
            return true;
        }

        Location anchor = target.getLocation();
        player.sendMessage(platformManager.createPlatform(player, id, anchor));
        return true;
    }
}
