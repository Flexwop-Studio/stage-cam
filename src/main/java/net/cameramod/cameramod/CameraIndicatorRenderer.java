package net.cameramod.cameramod;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class CameraIndicatorRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;
            if (client.options.hudHidden) return;
            if (CameraModClient.renderingCameraPass) return;

            String text = null;
            int color = 0xFFFFFF;

            if (CameraModClient.activeCamPoint != null) {
                int slot = CameraModClient.activeCamSlot;
                String name = CameraModClient.activeCamName;
                if (name != null && !name.isEmpty()) {
                    text = "⬤ CAM " + slot + "  " + name;
                } else {
                    text = "⬤ CAM " + slot;
                }
                color = 0x4A9EFF;
            } else if (CameraModClient.handcamMode) {
                text = "⬤ HANDHELD";
                color = 0x50C878;
            } else if (CameraModClient.egoMode) {
                text = "⬤ EGO";
                color = 0xFF6B6B;
            }

            if (text == null) return;

            int screenW = client.getWindow().getScaledWidth();
            int textW = client.textRenderer.getWidth(text);

            // Background
            context.fill(
                screenW - textW - 14, 6,
                screenW - 6, 23,
                0x88000000
            );

            // Text
            context.drawTextWithShadow(
                client.textRenderer,
                text,
                screenW - textW - 10,
                9,
                color
            );
        });
    }
}