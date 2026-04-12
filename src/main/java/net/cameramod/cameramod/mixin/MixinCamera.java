package net.cameramod.cameramod.mixin;

import net.cameramod.cameramod.CameraModClient;
import net.cameramod.cameramod.CameraPoint;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class MixinCamera {

    @Shadow protected void setPos(double x, double y, double z) {}
    @Shadow public void setRotation(float yaw, float pitch) {}

    @Inject(method = "update", at = @At("TAIL"))
    private void onCameraUpdate(BlockView area, Entity focusedEntity,
                                boolean thirdPerson, boolean inverseView,
                                float tickDelta, CallbackInfo ci) {
        // Cameraposition only overwrite when were in Camera pass
        if (!CameraModClient.renderingCameraPass) return;
        if (!CameraModClient.isCameraActive()) return;

        CameraPoint cam = CameraModClient.activeCamPoint;
        if (cam == null) return;

        setPos(cam.x(), cam.y(), cam.z());
        setRotation(cam.yaw(), cam.pitch());
    }
}