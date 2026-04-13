package com.paperteleporter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class RoleCommand {
    private final RoleStore roleStore;

    public RoleCommand(RoleStore roleStore) {
        this.roleStore = roleStore;
    }

    public boolean execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("paperteleporter.admin")) {
            sender.sendMessage(Component.text("You need admin permission", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /pt role <player> <Peasant|Lord|King> or /pt role remove <player>", NamedTextColor.YELLOW));
            return true;
        }

        // /pt role remove <player>
        if (args[0].equalsIgnoreCase("remove")) {
            String playerName = args[1];
            Player target = Bukkit.getPlayer(playerName);

            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
                return true;
            }

            roleStore.removeRole(target.getUniqueId());
            sender.sendMessage(Component.text("Removed role from " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text("Your role has been removed", NamedTextColor.YELLOW));
            return true;
        }

        // /pt role <player> <role>
        String playerName = args[0];
        String roleStr = args[1];

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid role. Use: Peasant, Lord, or King", NamedTextColor.RED));
            return true;
        }

        roleStore.setRole(target.getUniqueId(), role);
        sender.sendMessage(Component.text("Set " + target.getName() + " to role " + role.name(), NamedTextColor.GREEN));
        target.sendMessage(Component.text("Your role has been set to " + role.name(), NamedTextColor.GOLD));
        return true;
    }
}
