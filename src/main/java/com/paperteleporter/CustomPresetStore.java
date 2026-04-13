package com.paperteleporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class CustomPresetStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;
    private final Path backupCurrentPath;
    private final Path backupSnapshotsDir;
    private final Logger logger;

    public CustomPresetStore(JavaPlugin plugin) {
        this.filePath = plugin.getDataFolder().toPath().resolve("custom-presets.json");
        Path backupsRoot = plugin.getDataFolder().toPath().getParent().resolve("PaperTeleporter-backups");
        this.backupCurrentPath = backupsRoot.resolve("current").resolve("custom-presets.json");
        this.backupSnapshotsDir = backupsRoot.resolve("snapshots");
        this.logger = plugin.getLogger();
    }

    public Map<Integer, CustomPresetData> load() throws IOException {
        Map<Integer, CustomPresetData> presets = new HashMap<>();
        DataFileBackup.restorePrimaryIfMissing(filePath, backupCurrentPath, logger);
        if (!Files.exists(filePath)) {
            return presets;
        }

        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        CustomPresetData[] array = GSON.fromJson(content, CustomPresetData[].class);
        if (array != null) {
            for (CustomPresetData preset : array) {
                presets.put(preset.getPresetNumber(), preset);
            }
        }
        return presets;
    }

    public void save(Map<Integer, CustomPresetData> presets) throws IOException {
        Files.createDirectories(filePath.getParent());
        List<CustomPresetData> list = new ArrayList<>(presets.values());
        String json = GSON.toJson(list);
        Files.writeString(filePath, json, StandardCharsets.UTF_8);
        DataFileBackup.writeBackups(filePath, backupCurrentPath, backupSnapshotsDir, logger);
    }
}
