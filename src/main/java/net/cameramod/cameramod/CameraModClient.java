package net.cameramod.cameramod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class CameraModClient implements ClientModInitializer {

    private static final KeyBinding[] camKeys = new KeyBinding[10];

    public static CameraPoint activeCamPoint = null;
    public static int activeCamSlot = 0;
    public static String activeCamName = "";
    public static SecondWindow secondWindow = null;

    public static boolean renderingCameraPass = false;
    public static boolean blittedThisPass = false;
    public static Perspective savedPerspective = null;

    public static KeyBinding handcamKey;
    public static KeyBinding followKey;
    public static KeyBinding zoomInKey;
    public static KeyBinding zoomOutKey;
    public static KeyBinding setNextKey;
    public static KeyBinding setPrevKey;

    public static boolean handcamMode = false;
    public static boolean egoMode = false;
    public static boolean followMode = false;  // Fixed cam looks at player

    // Zoom
    public static float cameraFov = -1f; // -1 = use default
    private static final float FOV_MIN = 10f;
    private static final float FOV_MAX = 110f;
    private static final float FOV_STEP = 2f;

    // Camera sets
    public static int activeSet = 1; // 1-based

    private static boolean irisWarningShown = false;

    public static boolean isCameraActive() {
        return activeCamPoint != null || handcamMode || egoMode;
    }

    public static void deactivateCamera(MinecraftClient client) {
        activeCamPoint = null;
        activeCamSlot = 0;
        activeCamName = "";
        handcamMode = false;
        egoMode = false;
        followMode = false;
        renderingCameraPass = false;
        blittedThisPass = false;
        cameraFov = -1f;
        if (client != null && client.options != null) {
            client.options.setPerspective(
                savedPerspective != null ? savedPerspective : Perspective.FIRST_PERSON
            );
            savedPerspective = null;
            try { client.options.bobView.setValue(true); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onInitializeClient() {
        CameraMarkerRenderer.register();
        CameraIndicatorRenderer.register();

        // Numpad 0-9
        String[] keyNames = {"0","1","2","3","4","5","6","7","8","9"};
        int[] glfwKeys = {
            GLFW.GLFW_KEY_KP_0,
            GLFW.GLFW_KEY_KP_1, GLFW.GLFW_KEY_KP_2, GLFW.GLFW_KEY_KP_3,
            GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_5, GLFW.GLFW_KEY_KP_6,
            GLFW.GLFW_KEY_KP_7, GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_9
        };
        for (int i = 0; i <= 9; i++) {
            camKeys[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cameramod.cam" + keyNames[i],
                InputUtil.Type.KEYSYM, glfwKeys[i], "category.cameramod"
            ));
        }

        // Numpad , = Toggle Handheld -> Ego -> Off
        handcamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cameramod.handcam",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_DECIMAL, "category.cameramod"
        ));

        // Numpad / = Toggle Follow Mode
        followKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cameramod.follow",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_DIVIDE, "category.cameramod"
        ));

        // Numpad + / - = Zoom
        zoomInKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cameramod.zoomin",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_ADD, "category.cameramod"
        ));
        zoomOutKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cameramod.zoomout",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_SUBTRACT, "category.cameramod"
        ));

        // Page Up / Page Down = Switch camera sets
        setNextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cameramod.setnext",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_UP, "category.cameramod"
        ));
        setPrevKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cameramod.setprev",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_DOWN, "category.cameramod"
        ));

        // Iris warning
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (!irisWarningShown && FabricLoader.getInstance().isModLoaded("iris")) {
                client.player.sendMessage(
                    Text.literal("§e[StageCam] §fIris/Shaders detected! For best results disable §eBLOOM§f and §eTAA§f in your shader settings."),
                    false
                );
                irisWarningShown = true;
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Numpad 0 = turn everything off
            if (camKeys[0].wasPressed()) {
                deactivateCamera(client);
            }

            // Numpad 1-9 = fixed camera
            for (int i = 1; i <= 9; i++) {
                if (camKeys[i].wasPressed()) {
                    handcamMode = false;
                    egoMode = false;
                    followMode = false;
                    cameraFov = -1f;
                    client.player.networkHandler.sendChatCommand("cam " + i + " " + activeSet);
                }
            }

            // Numpad , = Toggle Handheld -> Ego -> Off
            if (handcamKey.wasPressed()) {
                if (!handcamMode && !egoMode) {
                    handcamMode = true;
                    egoMode = false;
                    followMode = false;
                    activeCamPoint = null;
                    activeCamSlot = 0;
                    activeCamName = "";
                    cameraFov = -1f;
                } else if (handcamMode) {
                    handcamMode = false;
                    egoMode = true;
                    followMode = false;
                    activeCamPoint = null;
                    cameraFov = -1f;
                } else {
                    deactivateCamera(client);
                }
            }

            // Numpad / = Toggle Follow Mode (only when fixed cam active)
            if (followKey.wasPressed() && activeCamPoint != null) {
                followMode = !followMode;
                client.player.sendMessage(
                    Text.literal("§e[StageCam] §fFollow mode: " + (followMode ? "§aON" : "§cOFF")),
                    true
                );
            }

            // Numpad + / - = Zoom
            if (zoomInKey.wasPressed() && isCameraActive()) {
                float base = cameraFov < 0 ? client.options.getFov().getValue() : cameraFov;
                cameraFov = Math.max(FOV_MIN, base - FOV_STEP);
            }
            if (zoomOutKey.wasPressed() && isCameraActive()) {
                float base = cameraFov < 0 ? client.options.getFov().getValue() : cameraFov;
                cameraFov = Math.min(FOV_MAX, base + FOV_STEP);
            }

            // Page Up / Page Down = switch camera sets
            if (setNextKey.wasPressed()) {
                activeSet++;
                activeCamPoint = null;
                activeCamSlot = 0;
                activeCamName = "";
                followMode = false;
                cameraFov = -1f;
                client.player.sendMessage(
                    Text.literal("§e[StageCam] §fCamera Set: §a" + activeSet),
                    true
                );
            }
            if (setPrevKey.wasPressed()) {
                if (activeSet > 1) {
                    activeSet--;
                    activeCamPoint = null;
                    activeCamSlot = 0;
                    activeCamName = "";
                    followMode = false;
                    cameraFov = -1f;
                    client.player.sendMessage(
                        Text.literal("§e[StageCam] §fCamera Set: §a" + activeSet),
                        true
                    );
                }
            }
        });

        // Chat intercept for CAMDATA / CAMRESET
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String raw = message.getString();

            if (raw.contains("CAMDATA:")) {
                try {
                    String data = raw.substring(raw.indexOf("CAMDATA:") + "CAMDATA:".length());
                    String[] parts = data.split(":");
                    activeCamSlot = Integer.parseInt(parts[0]);
                    double x     = Double.parseDouble(parts[1]);
                    double y     = Double.parseDouble(parts[2]);
                    double z     = Double.parseDouble(parts[3]);
                    float  yaw   = Float.parseFloat(parts[4]);
                    float  pitch = Float.parseFloat(parts[5]);
                    activeCamName = (parts.length > 6) ? parts[6] : "";

                    activeCamPoint = new CameraPoint(x, y, z, yaw, pitch, activeCamName);
                    handcamMode = false;
                    egoMode = false;
                    followMode = false;
                } catch (Exception e) {
                    CameraMod.LOGGER.error("Error parsing CAMDATA: " + e.getMessage());
                }
                return false;
            }

            if (raw.contains("CAMRESET")) {
                deactivateCamera(MinecraftClient.getInstance());
                return false;
            }

            return true;
        });

        // Second window init
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (secondWindow == null && client.world != null) {
                secondWindow = new SecondWindow();
                secondWindow.init();
                secondWindow.initFramebuffer();
            }
            if (secondWindow != null && !secondWindow.isOpen()) {
                deactivateCamera(client);
                secondWindow = null;
            }
        });
    }
}