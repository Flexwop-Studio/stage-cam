package net.cameramod.cameramod.mixin;

import net.cameramod.cameramod.CameraModClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class MixinCameraFollowMode {

    @Shadow protected void setPos(double x, double y, double z) {}
    @Shadow public void setRotation(float yaw, float pitch) {}

    private double smoothX = 0, smoothY = 0, smoothZ = 0;
    private boolean initialized = false;
    private float wackleTime = 0;

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdate(BlockView area, Entity focusedEntity,
                          boolean thirdPerson, boolean inverseView,
                          float tickDelta, CallbackInfo ci) {
        if (!CameraModClient.renderingCameraPass) return;
        if (CameraModClient.activeCamPoint != null) return;
        if (focusedEntity == null) return;
        if (CameraModClient.egoMode) return;

        if (CameraModClient.handcamMode) {
            float yaw   = focusedEntity.getYaw(tickDelta);
            float pitch = focusedEntity.getPitch(tickDelta);
            double eyeY = focusedEntity.getY() + focusedEntity.getEyeHeight(focusedEntity.getPose());

            wackleTime += tickDelta * 0.06f;

            // Blickrichtung des Spielers als 3D-Vektor berechnen (yaw + pitch)
            double yawRad   = Math.toRadians(yaw);
            double pitchRad = Math.toRadians(pitch);

            // Vorwärtsvektor in Blickrichtung
            double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad);
            double forwardY = -Math.sin(pitchRad);
            double forwardZ =  Math.cos(yawRad) * Math.cos(pitchRad);

            // Zielposition: 2 Blöcke VOR dem Spieler in Blickrichtung
            double targetX = focusedEntity.getX() + forwardX * 2.0;
            double targetY = eyeY + forwardY * 2.0;
            double targetZ = focusedEntity.getZ() + forwardZ * 2.0;

            if (!initialized) {
                smoothX = targetX;
                smoothY = targetY;
                smoothZ = targetZ;
                initialized = true;
            }

            // Smooth lerp
            double smoothFactor = 0.15;
            smoothX += (targetX - smoothX) * smoothFactor;
            smoothY += (targetY - smoothY) * smoothFactor;
            smoothZ += (targetZ - smoothZ) * smoothFactor;

            // Sehr leichtes Wackeln
            float wackleYaw   = (float) Math.sin(wackleTime * 0.9f) * 0.15f;
            float wacklePitch = (float) Math.sin(wackleTime * 1.1f) * 0.1f;

            // Kamera schaut zurück auf den Spieler (entgegengesetzte Richtung)
            setPos(smoothX, smoothY, smoothZ);
            setRotation(yaw + 180f + wackleYaw, -pitch + wacklePitch);
        } else {
            initialized = false;
        }
    }
}