package com.paperteleporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PlatformStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<PlatformData>>() {}.getType();

    private final Path path;

    public PlatformStore(JavaPlugin plugin) {
        this.path = plugin.getDataFolder().toPath().resolve("platforms.json");
    }

    public List<PlatformData> load() throws IOException {
        if (Files.notExists(path)) {
            return new ArrayList<>();
        }
        String rawJson = Files.readString(path, StandardCharsets.UTF_8);
        List<PlatformData> data = GSON.fromJson(rawJson, LIST_TYPE);
        return data == null ? new ArrayList<>() : data;
    }

    public void save(List<PlatformData> platforms) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(platforms), StandardCharsets.UTF_8);
    }
}
