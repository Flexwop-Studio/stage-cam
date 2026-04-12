package net.cameramod.cameramod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CameraMod implements ModInitializer {
    public static final String MOD_ID = "cameramod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Map<UUID, Map<Integer, CameraPoint>> playerCameras = new HashMap<>();
    public static final Map<UUID, PlayerState> originalStates = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("StageCam loaded!");
        registerCommands();

        // Load cameras when player joins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID uuid = handler.player.getUuid();
            Map<Integer, CameraPoint> loaded = CameraStorage.load(uuid);
            if (!loaded.isEmpty()) {
                playerCameras.put(uuid, loaded);
                handler.player.sendMessage(
                    Text.literal("§a[StageCam] " + loaded.size() + " camera(s) loaded!"), false
                );
            }
        });

        // Save cameras when player disconnects
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            Map<Integer, CameraPoint> cams = playerCameras.get(uuid);
            if (cams != null && !cams.isEmpty()) {
                CameraStorage.save(uuid, cams);
            }
        });

        // Save all cameras when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Map.Entry<UUID, Map<Integer, CameraPoint>> entry : playerCameras.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    CameraStorage.save(entry.getKey(), entry.getValue());
                }
            }
            LOGGER.info("[StageCam] All cameras saved!");
        });
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // /setcam <1-9> [name] - name is optional
            dispatcher.register(CommandManager.literal("setcam")
                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 9))
                    // Without name
                    .executes(ctx -> {
                        return saveCamera(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "slot"), "");
                    })
                    // With name
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            return saveCamera(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "slot"),
                                StringArgumentType.getString(ctx, "name")
                            );
                        })
                    )
                )
            );

            // /renamecam <1-9> <name>
            dispatcher.register(CommandManager.literal("renamecam")
                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 9))
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            if (player == null) return 0;

                            int slot = IntegerArgumentType.getInteger(ctx, "slot");
                            String name = StringArgumentType.getString(ctx, "name");

                            Map<Integer, CameraPoint> cams = playerCameras.get(player.getUuid());
                            if (cams == null || !cams.containsKey(slot)) {
                                player.sendMessage(Text.literal("§c[StageCam] Camera " + slot + " not set!"), false);
                                return 0;
                            }

                            CameraPoint old = cams.get(slot);
                            CameraPoint renamed = new CameraPoint(old.x(), old.y(), old.z(), old.yaw(), old.pitch(), name);
                            cams.put(slot, renamed);
                            CameraStorage.save(player.getUuid(), cams);

                            player.sendMessage(Text.literal("§a[StageCam] Camera " + slot + " renamed to §e\"" + name + "\""), false);
                            return 1;
                        })
                    )
                )
            );

            // /cam <1-9>
            dispatcher.register(CommandManager.literal("cam")
                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 9))
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        ServerPlayerEntity player = source.getPlayer();
                        if (player == null) return 0;

                        int slot = IntegerArgumentType.getInteger(ctx, "slot");
                        Map<Integer, CameraPoint> cams = playerCameras.get(player.getUuid());

                        if (cams == null || !cams.containsKey(slot)) {
                            player.sendMessage(Text.literal("§c[StageCam] Camera " + slot + " not set! Use /setcam " + slot), false);
                            return 0;
                        }

                        CameraPoint cam = cams.get(slot);
                        String displayName = cam.displayName(slot);

                        // Send camera data + name to client
                        player.sendMessage(Text.literal("§0CAMDATA:" + slot + ":" +
                            cam.x() + ":" + cam.y() + ":" + cam.z() + ":" +
                            cam.yaw() + ":" + cam.pitch() + ":" + displayName), false);

                        player.sendMessage(Text.literal("§b[StageCam] " + displayName + " active"), true);
                        return 1;
                    })
                )
            );

            // /camback
            dispatcher.register(CommandManager.literal("camback")
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    if (player == null) return 0;
                    player.sendMessage(Text.literal("§0CAMRESET"), false);
                    return 1;
                })
            );

            // /listcams
            dispatcher.register(CommandManager.literal("listcams")
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    if (player == null) return 0;

                    Map<Integer, CameraPoint> cams = playerCameras.get(player.getUuid());
                    if (cams == null || cams.isEmpty()) {
                        player.sendMessage(Text.literal("§c[StageCam] No cameras set. Use /setcam <1-9>"), false);
                        return 0;
                    }

                    player.sendMessage(Text.literal("§6=== StageCam Cameras ==="), false);
                    for (Map.Entry<Integer, CameraPoint> entry : cams.entrySet()) {
                        CameraPoint cam = entry.getValue();
                        String nameDisplay = (cam.name() != null && !cam.name().isEmpty())
                            ? " §e\"" + cam.name() + "\"" : "";
                        player.sendMessage(Text.literal("§eSlot " + entry.getKey() + nameDisplay + "§7: " +
                            String.format("%.1f", cam.x()) + ", " +
                            String.format("%.1f", cam.y()) + ", " +
                            String.format("%.1f", cam.z())), false);
                    }
                    return 1;
                })
            );

            // /delcam <1-9>
            dispatcher.register(CommandManager.literal("delcam")
                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 9))
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        ServerPlayerEntity player = source.getPlayer();
                        if (player == null) return 0;

                        int slot = IntegerArgumentType.getInteger(ctx, "slot");
                        Map<Integer, CameraPoint> cams = playerCameras.get(player.getUuid());
                        if (cams != null) {
                            cams.remove(slot);
                            CameraStorage.save(player.getUuid(), cams);
                        }
                        player.sendMessage(Text.literal("§c[StageCam] Camera " + slot + " deleted."), false);
                        return 1;
                    })
                )
            );
        });
    }

    private int saveCamera(ServerCommandSource source, int slot, String name) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        CameraPoint cam = new CameraPoint(
            player.getX(),
            player.getY() + player.getEyeHeight(player.getPose()),
            player.getZ(),
            player.getYaw(),
            player.getPitch(),
            name
        );

        playerCameras
            .computeIfAbsent(player.getUuid(), k -> new HashMap<>())
            .put(slot, cam);

        CameraStorage.save(player.getUuid(), playerCameras.get(player.getUuid()));

        String nameDisplay = (!name.isEmpty()) ? " §e\"" + name + "\"§a" : "";
        player.sendMessage(Text.literal("§a[StageCam] Camera " + slot + nameDisplay + " saved! §7(" +
            String.format("%.1f", cam.x()) + ", " +
            String.format("%.1f", cam.y()) + ", " +
            String.format("%.1f", cam.z()) + ")"), false);
        return 1;
    }
}