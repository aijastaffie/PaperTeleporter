package com.paperteleporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoleStore {
    private static final String ROLES_FILE = "roles.json";
    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, RoleData> roles = new HashMap<>();
    private final File dataDir;
    private final File rolesFile;

    public RoleStore(JavaPlugin plugin, File dataDir) {
        this.plugin = plugin;
        this.dataDir = dataDir;
        this.rolesFile = new File(dataDir, ROLES_FILE);
        loadRoles();
    }

    private void loadRoles() {
        if (!rolesFile.exists()) {
            return;
        }

        try {
            String content = Files.readString(rolesFile.toPath());
            Map<String, String> rawRoles = gson.fromJson(content, new TypeToken<Map<String, String>>() {}.getType());

            if (rawRoles != null) {
                for (Map.Entry<String, String> entry : rawRoles.entrySet()) {
                    try {
                        roles.put(entry.getKey(), new RoleData(entry.getKey(), UserRole.valueOf(entry.getValue())));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid role '" + entry.getValue() + "' for player " + entry.getKey());
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load roles: " + e.getMessage());
        }
    }

    public void saveRoles() {
        try {
            Map<String, String> rawRoles = new HashMap<>();
            for (Map.Entry<String, RoleData> entry : roles.entrySet()) {
                rawRoles.put(entry.getKey(), entry.getValue().getRole().name());
            }

            String jsonContent = gson.toJson(rawRoles);
            Files.writeString(rolesFile.toPath(), jsonContent);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save roles: " + e.getMessage());
        }
    }

    public void setRole(UUID playerUuid, UserRole role) {
        String uuidStr = playerUuid.toString();
        roles.put(uuidStr, new RoleData(uuidStr, role));
        saveRoles();
    }

    public UserRole getRole(UUID playerUuid) {
        RoleData data = roles.get(playerUuid.toString());
        return data != null ? data.getRole() : null;
    }

    public void removeRole(UUID playerUuid) {
        roles.remove(playerUuid.toString());
        saveRoles();
    }

    public boolean hasRole(UUID playerUuid) {
        return roles.containsKey(playerUuid.toString());
    }
}
