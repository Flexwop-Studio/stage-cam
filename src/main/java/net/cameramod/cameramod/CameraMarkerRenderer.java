package net.cameramod.cameramod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import net.minecraft.client.gl.ShaderProgramKeys;
import java.util.Map;

public class CameraMarkerRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(CameraMarkerRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        // Do not render in the camera viewport, only in the main window
        if (CameraModClient.renderingCameraPass) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

Map<Integer, CameraPoint> cams = CameraMod.getCamerasForSet(
    client.player.getUuid(), CameraModClient.activeSet);
        if (cams == null || cams.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
            VertexFormat.DrawMode.LINES,
            VertexFormats.LINES
        );

        RenderSystem.lineWidth(2.0f);
        RenderSystem.disableDepthTest(); // Visible through walls
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (Map.Entry<Integer, CameraPoint> entry : cams.entrySet()) {
            CameraPoint cam = entry.getValue();
            int slot = entry.getKey();
            float x = (float) cam.x();
            float y = (float) cam.y();
            float z = (float) cam.z();
            float size = 0.3f;

            // Color based on slot (1=red, 2=green, 3=blue, etc.)
            float r = slot % 3 == 1 ? 1f : 0.2f;
            float g = slot % 3 == 2 ? 1f : 0.2f;
            float b = slot % 3 == 0 ? 1f : 0.2f;

            // Cross in the X-direction
            drawLine(buffer, matrix, x - size, y, z, x + size, y, z, r, g, b);
            // Cross in the Y-direction
            drawLine(buffer, matrix, x, y - size, z, x, y + size, z, r, g, b);
            // Cross in the Z-direction
            drawLine(buffer, matrix, x, y, z - size, x, y, z + size, r, g, b);

            // Small label with slot number (as a line box)
            drawBox(buffer, matrix, x, y, z, size * 0.5f, r, g, b);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        matrices.pop();
    }

    private static void drawLine(BufferBuilder buffer, Matrix4f matrix,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float r, float g, float b) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, 1f).normal(dx/len, dy/len, dz/len);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, 1f).normal(dx/len, dy/len, dz/len);
    }

    private static void drawBox(BufferBuilder buffer, Matrix4f matrix,
                                 float x, float y, float z, float s,
                                 float r, float g, float b) {
        // Bottom square
        drawLine(buffer, matrix, x-s, y-s, z-s, x+s, y-s, z-s, r, g, b);
        drawLine(buffer, matrix, x+s, y-s, z-s, x+s, y-s, z+s, r, g, b);
        drawLine(buffer, matrix, x+s, y-s, z+s, x-s, y-s, z+s, r, g, b);
        drawLine(buffer, matrix, x-s, y-s, z+s, x-s, y-s, z-s, r, g, b);
        // Top square
        drawLine(buffer, matrix, x-s, y+s, z-s, x+s, y+s, z-s, r, g, b);
        drawLine(buffer, matrix, x+s, y+s, z-s, x+s, y+s, z+s, r, g, b);
        drawLine(buffer, matrix, x+s, y+s, z+s, x-s, y+s, z+s, r, g, b);
        drawLine(buffer, matrix, x-s, y+s, z+s, x-s, y+s, z-s, r, g, b);
        // Vertical lines
        drawLine(buffer, matrix, x-s, y-s, z-s, x-s, y+s, z-s, r, g, b);
        drawLine(buffer, matrix, x+s, y-s, z-s, x+s, y+s, z-s, r, g, b);
        drawLine(buffer, matrix, x+s, y-s, z+s, x+s, y+s, z+s, r, g, b);
        drawLine(buffer, matrix, x-s, y-s, z+s, x-s, y+s, z+s, r, g, b);
    }
}