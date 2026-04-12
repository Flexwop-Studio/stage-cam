package net.cameramod.cameramod;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class CameraIndicatorRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register(CameraIndicatorRenderer::render);
    }

    private static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options == null) return;

        boolean active = CameraModClient.activeCamPoint != null
            || CameraModClient.handcamMode
            || CameraModClient.egoMode;

        if (!active) return;

        // Build display text and color
        String camText;
        int color;

        if (CameraModClient.activeCamPoint != null) {
            String name = CameraModClient.activeCamName != null && !CameraModClient.activeCamName.isEmpty()
                ? CameraModClient.activeCamName
                : "Camera " + CameraModClient.activeCamSlot;
            camText = "S" + CameraModClient.activeSet + " · " + name;
            color = 0x4A9EFF; // blue
        } else if (CameraModClient.handcamMode) {
            camText = "Handheld";
            color = 0x50C878; // green
        } else {
            camText = "Ego";
            color = 0xFF6B6B; // red
        }

        // Append follow mode indicator
        if (CameraModClient.followMode) {
            camText += " ⟳";
        }

        // Append zoom indicator if not default
        if (CameraModClient.cameraFov >= 0) {
            int fovInt = Math.round(CameraModClient.cameraFov);
            camText += " " + fovInt + "°";
        }

        int screenW = context.getScaledWindowWidth();
        int textW = client.textRenderer.getWidth(camText);

        // Background box
        context.fill(
            screenW - textW - 14, 5,
            screenW - 6, 23,
            0x88000000
        );

        // Text
        context.drawTextWithShadow(
            client.textRenderer,
            camText,
            screenW - textW - 10,
            8,
            color
        );
    }
}