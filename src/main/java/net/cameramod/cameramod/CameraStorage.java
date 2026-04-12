package net.cameramod.cameramod;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CameraStorage {

    private static final Path STORAGE_DIR = FabricLoader.getInstance()
        .getConfigDir().resolve("cameramod");

    private static Path getFile(UUID playerUuid) {
        return STORAGE_DIR.resolve(playerUuid + ".json");
    }

    public static void save(UUID playerUuid, Map<Integer, CameraPoint> cameras) {
        try {
            Files.createDirectories(STORAGE_DIR);

            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();

            for (Map.Entry<Integer, CameraPoint> entry : cameras.entrySet()) {
                JsonObject cam = new JsonObject();
                cam.addProperty("slot", entry.getKey());
                cam.addProperty("x", entry.getValue().x());
                cam.addProperty("y", entry.getValue().y());
                cam.addProperty("z", entry.getValue().z());
                cam.addProperty("yaw", entry.getValue().yaw());
                cam.addProperty("pitch", entry.getValue().pitch());
                cam.addProperty("name", entry.getValue().name() != null ? entry.getValue().name() : "");
                array.add(cam);
            }

            root.add("cameras", array);

            try (Writer writer = Files.newBufferedWriter(getFile(playerUuid))) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }

            CameraMod.LOGGER.info("[StageCam] Cameras saved for " + playerUuid);
        } catch (Exception e) {
            CameraMod.LOGGER.error("[StageCam] Error saving cameras: " + e.getMessage());
        }
    }

    public static Map<Integer, CameraPoint> load(UUID playerUuid) {
        Map<Integer, CameraPoint> cameras = new HashMap<>();
        Path file = getFile(playerUuid);

        if (!Files.exists(file)) return cameras;

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("cameras");

            for (JsonElement element : array) {
                JsonObject cam = element.getAsJsonObject();
                int slot    = cam.get("slot").getAsInt();
                double x    = cam.get("x").getAsDouble();
                double y    = cam.get("y").getAsDouble();
                double z    = cam.get("z").getAsDouble();
                float yaw   = cam.get("yaw").getAsFloat();
                float pitch = cam.get("pitch").getAsFloat();
                // Backwards compatible - name is optional
                String name = cam.has("name") ? cam.get("name").getAsString() : "";
                cameras.put(slot, new CameraPoint(x, y, z, yaw, pitch, name));
            }

            CameraMod.LOGGER.info("[StageCam] Loaded " + cameras.size() + " camera(s) for " + playerUuid);
        } catch (Exception e) {
            CameraMod.LOGGER.error("[StageCam] Error loading cameras: " + e.getMessage());
        }

        return cameras;
    }
}