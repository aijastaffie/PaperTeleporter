package com.paperteleporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PlatformMaterials {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Material platform;
    private final Material fence;
    private final Material spawn;
    private final Material npc;
    private final Material light;

    private PlatformMaterials(Material platform, Material fence, Material spawn, Material npc, Material light) {
        this.platform = platform;
        this.fence = fence;
        this.spawn = spawn;
        this.npc = npc;
        this.light = light;
    }

    public static PlatformMaterials load(JavaPlugin plugin) throws IOException {
        Path path = plugin.getDataFolder().toPath().resolve("platform-materials.json");
        if (Files.notExists(path)) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("default-platform-materials.json", false);
            Path defaultPath = plugin.getDataFolder().toPath().resolve("default-platform-materials.json");
            Files.move(defaultPath, path);
        }

        String rawJson = Files.readString(path, StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();

        Material platform = parseMaterial(json, "platform", Material.SMOOTH_STONE);
        Material fence = parseMaterial(json, "fence", Material.OAK_FENCE);
        Material spawn = parseMaterial(json, "spawn", Material.GOLD_BLOCK);
        Material npc = parseMaterial(json, "npc", Material.EMERALD_BLOCK);
        Material light = parseMaterial(json, "light", Material.SOUL_LANTERN);

        JsonObject normalized = new JsonObject();
        normalized.addProperty("platform", platform.name());
        normalized.addProperty("fence", fence.name());
        normalized.addProperty("spawn", spawn.name());
        normalized.addProperty("npc", npc.name());
        normalized.addProperty("light", light.name());
        Files.writeString(path, GSON.toJson(normalized), StandardCharsets.UTF_8);

        return new PlatformMaterials(platform, fence, spawn, npc, light);
    }

    private static Material parseMaterial(JsonObject json, String key, Material fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        String value = json.get(key).getAsString().toUpperCase();
        Material material = Material.matchMaterial(value);
        return material == null ? fallback : material;
    }

    public Material platform() {
        return platform;
    }

    public Material fence() {
        return fence;
    }

    public Material spawn() {
        return spawn;
    }

    public Material npc() {
        return npc;
    }

    public Material light() {
        return light;
    }
}
