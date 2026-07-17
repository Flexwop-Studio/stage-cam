package net.cameramod.cameramod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CameraMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("cameramod");

    // Key: UUID, Value: Map<setNumber, Map<slot, CameraPoint>>
    public static final Map<UUID, Map<Integer, Map<Integer, CameraPoint>>> playerCameraSets = new HashMap<>();

    // Helper: get cameras for a specific set
    public static Map<Integer, CameraPoint> getCamerasForSet(UUID uuid, int set) {
        return playerCameraSets
            .computeIfAbsent(uuid, k -> new HashMap<>())
            .computeIfAbsent(set, k -> new HashMap<>());
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // /setcam <slot> [name] — saves to current set (set 1 default, client sends set number)
            dispatcher.register(CommandManager.literal("setcam")
                .then(CommandManager.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
                    .executes(ctx -> setCamera(ctx.getSource(),
                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot"), 1, ""))
                .then(CommandManager.argument("set", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                    .executes(ctx -> setCamera(ctx.getSource(),
                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot"),
                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "set"), ""))
                    .then(CommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(ctx -> setCamera(ctx.getSource(),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot"),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "set"),
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name")))))));


            // /cam <slot> [set] — activate camera
            dispatcher.register(CommandManager.literal("cam")
                .then(CommandManager.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
                    .executes(ctx -> activateCamera(ctx.getSource(),
                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot"), 1))
                    .then(CommandManager.argument("set", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(ctx -> activateCamera(ctx.getSource(),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot"),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "set"))))));

            // /camback
            dispatcher.register(CommandManager.literal("camback")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player != null)
                        player.sendMessage(Text.literal("§0CAMRESET"), false);
                    return 1;
                }));

            // /renamecam <slot> <name>
            dispatcher.register(CommandManager.literal("renamecam")
                .then(CommandManager.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
                    .then(CommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(ctx -> renameCam(ctx.getSource(),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot"), 1,
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name"))))));

            // /delcam <slot>
            dispatcher.register(CommandManager.literal("delcam")
                .then(CommandManager.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
                    .executes(ctx -> deleteCam(ctx.getSource(),
                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot"), 1))));

            // /listcams
            dispatcher.register(CommandManager.literal("listcams")
                .executes(ctx -> listCams(ctx.getSource(), 1)));

            // /listcams-s <set>
            dispatcher.register(CommandManager.literal("listcams-s")
                .then(CommandManager.argument("set", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                    .executes(ctx -> listCams(ctx.getSource(),
                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "set")))));
        });

        // Load all cameras on init
        LOGGER.info("StageCam initialized");
    }

    private static int setCamera(ServerCommandSource source, int slot, int set, String name) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        double x = player.getX();
        double y = player.getY() + player.getEyeHeight(player.getPose());
        double z = player.getZ();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        Map<Integer, CameraPoint> cams = getCamerasForSet(player.getUuid(), set);
        cams.put(slot, new CameraPoint(x, y, z, yaw, pitch, name));
        CameraStorage.saveSet(player.getUuid(), set, cams);

        String display = name.isEmpty() ? "Camera " + slot : name;
        source.sendFeedback(() -> Text.literal("§aSaved §f" + display + " §ato slot §f" + slot + " §a(Set " + set + ")"), false);
        return 1;
    }

    private static int activateCamera(ServerCommandSource source, int slot, int set) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        Map<Integer, CameraPoint> cams = getCamerasForSet(player.getUuid(), set);
        CameraPoint cam = cams.get(slot);

        if (cam == null) {
            source.sendFeedback(() -> Text.literal("§cNo camera in slot " + slot + " (Set " + set + ")"), false);
            return 0;
        }

        String name = cam.name() != null && !cam.name().isEmpty() ? cam.name() : "";
        String msg = "§0CAMDATA:" + slot + ":" + cam.x() + ":" + cam.y() + ":" + cam.z()
            + ":" + cam.yaw() + ":" + cam.pitch() + (name.isEmpty() ? "" : ":" + name);
        player.sendMessage(Text.literal(msg), false);
        return 1;
    }

    private static int renameCam(ServerCommandSource source, int slot, int set, String name) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        Map<Integer, CameraPoint> cams = getCamerasForSet(player.getUuid(), set);
        CameraPoint cam = cams.get(slot);
        if (cam == null) {
            source.sendFeedback(() -> Text.literal("§cNo camera in slot " + slot), false);
            return 0;
        }
        cams.put(slot, new CameraPoint(cam.x(), cam.y(), cam.z(), cam.yaw(), cam.pitch(), name));
        CameraStorage.saveSet(player.getUuid(), set, cams);
        source.sendFeedback(() -> Text.literal("§aRenamed slot " + slot + " to §f" + name), false);
        return 1;
    }

    private static int deleteCam(ServerCommandSource source, int slot, int set) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        Map<Integer, CameraPoint> cams = getCamerasForSet(player.getUuid(), set);
        cams.remove(slot);
        CameraStorage.saveSet(player.getUuid(), set, cams);
        source.sendFeedback(() -> Text.literal("§aDeleted slot " + slot + " (Set " + set + ")"), false);
        return 1;
    }

    private static int listCams(ServerCommandSource source, int set) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        Map<Integer, CameraPoint> cams = getCamerasForSet(player.getUuid(), set);
        if (cams.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§eNo cameras in Set " + set), false);
            return 1;
        }
        source.sendFeedback(() -> Text.literal("§eCameras in Set " + set + ":"), false);
        cams.forEach((slot, cam) ->
            source.sendFeedback(() -> Text.literal("  §f" + slot + ". §7" + cam.displayName(slot)
                + " §8(" + String.format("%.0f", cam.x()) + ", "
                + String.format("%.0f", cam.y()) + ", "
                + String.format("%.0f", cam.z()) + ")"), false));
        return 1;
    }
}