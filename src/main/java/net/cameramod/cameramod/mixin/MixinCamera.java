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
public abstract class MixinCamera {

    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    private float followYaw = 0f;
    private float followPitch = 0f;
    private boolean followInitialized = false;

    private static final float LERP_FACTOR = 0.08f;

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson,
                          boolean inverseView, float tickDelta, CallbackInfo ci) {

        if (!CameraModClient.renderingCameraPass) return;
        if (CameraModClient.activeCamPoint == null) return;

        var cam = CameraModClient.activeCamPoint;

        if (CameraModClient.followMode && focusedEntity != null) {
            double camX = cam.x();
            double camY = cam.y();
            double camZ = cam.z();

            double px = focusedEntity.getX();
            double py = focusedEntity.getEyeY();
            double pz = focusedEntity.getZ();

            double dx = px - camX;
            double dy = py - camY;
            double dz = pz - camZ;

            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDist));

            if (!followInitialized) {
                followYaw = targetYaw;
                followPitch = targetPitch;
                followInitialized = true;
            }

            // Wrap yaw difference to [-180, 180] to avoid spinning the long way around
            float yawDiff = targetYaw - followYaw;
            while (yawDiff > 180f) yawDiff -= 360f;
            while (yawDiff < -180f) yawDiff += 360f;

            followYaw += yawDiff * LERP_FACTOR;
            followPitch += (targetPitch - followPitch) * LERP_FACTOR;

            setPos(camX, camY, camZ);
            setRotation(followYaw, followPitch);

        } else {
            followInitialized = false;
            setPos(cam.x(), cam.y(), cam.z());
            setRotation(cam.yaw(), cam.pitch());
        }
    }
}