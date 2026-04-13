package com.paperteleporter;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class PaperTeleporterPlugin extends JavaPlugin {
    private PlatformManager platformManager;

    @Override
    public void onEnable() {
        try {
            PlatformMaterials materials = PlatformMaterials.load(this);
            PlatformStore store = new PlatformStore(this);

            platformManager = new PlatformManager(this, store, materials);
            platformManager.load();
        } catch (IOException e) {
            getLogger().severe("Failed to initialize plugin data: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PluginCommand ptCmd = getCommand("pt");
        if (ptCmd != null) {
            PtCommand handler = new PtCommand(platformManager);
            ptCmd.setExecutor(handler);
            ptCmd.setTabCompleter(handler);
        }

        getServer().getPluginManager().registerEvents(new PlatformListener(platformManager), this);
        getLogger().info("PaperTeleporter enabled.");
    }

    @Override
    public void onDisable() {
        if (platformManager == null) {
            return;
        }
        try {
            platformManager.save();
        } catch (IOException e) {
            getLogger().warning("Failed to save platform data: " + e.getMessage());
        }
    }
}
