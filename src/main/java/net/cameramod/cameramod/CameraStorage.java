package net.cameramod.cameramod;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CameraStorage {

    private static Path getDir() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("cameramod");
        if (!Files.exists(dir)) {
            try { Files.createDirectories(dir); } catch (IOException ignored) {}
        }
        return dir;
    }

    // File per player per set: <uuid>_set<N>.json
    private static Path getFile(UUID uuid, int set) {
        return getDir().resolve(uuid + "_set" + set + ".json");
    }

    public static void saveSet(UUID uuid, int set, Map<Integer, CameraPoint> cams) {
        JsonObject root = new JsonObject();
        cams.forEach((slot, cam) -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", cam.x());
            obj.addProperty("y", cam.y());
            obj.addProperty("z", cam.z());
            obj.addProperty("yaw", cam.yaw());
            obj.addProperty("pitch", cam.pitch());
            obj.addProperty("name", cam.name() != null ? cam.name() : "");
            root.add(String.valueOf(slot), obj);
        });

        try (Writer writer = new FileWriter(getFile(uuid, set).toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        } catch (IOException e) {
            CameraMod.LOGGER.error("Failed to save cameras: " + e.getMessage());
        }
    }

    public static Map<Integer, CameraPoint> loadSet(UUID uuid, int set) {
        Map<Integer, CameraPoint> cams = new HashMap<>();
        Path file = getFile(uuid, set);
        if (!Files.exists(file)) return cams;

        try (Reader reader = new FileReader(file.toFile())) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                int slot = Integer.parseInt(entry.getKey());
                JsonObject obj = entry.getValue().getAsJsonObject();
                double x     = obj.get("x").getAsDouble();
                double y     = obj.get("y").getAsDouble();
                double z     = obj.get("z").getAsDouble();
                float yaw    = obj.get("yaw").getAsFloat();
                float pitch  = obj.get("pitch").getAsFloat();
                String name  = obj.has("name") ? obj.get("name").getAsString() : "";
                cams.put(slot, new CameraPoint(x, y, z, yaw, pitch, name));
            }
        } catch (Exception e) {
            CameraMod.LOGGER.error("Failed to load cameras: " + e.getMessage());
        }
        return cams;
    }

    // Load all sets for a player — finds all files matching <uuid>_set*.json
    public static void loadAllSets(UUID uuid) {
        Path dir = getDir();
        String prefix = uuid + "_set";
        try {
            Files.list(dir).forEach(path -> {
                String filename = path.getFileName().toString();
                if (filename.startsWith(prefix) && filename.endsWith(".json")) {
                    try {
                        String setStr = filename.substring(prefix.length(), filename.length() - 5);
                        int set = Integer.parseInt(setStr);
                        Map<Integer, CameraPoint> cams = loadSet(uuid, set);
                        CameraMod.getCamerasForSet(uuid, set).putAll(cams);
                    } catch (NumberFormatException ignored) {}
                }
            });
        } catch (IOException e) {
            CameraMod.LOGGER.error("Failed to scan camera sets: " + e.getMessage());
        }
    }

    // Legacy: save/load without set (set 1)
    public static void save(UUID uuid, Map<Integer, CameraPoint> cams) {
        saveSet(uuid, 1, cams);
    }

    public static Map<Integer, CameraPoint> load(UUID uuid) {
        return loadSet(uuid, 1);
    }
}