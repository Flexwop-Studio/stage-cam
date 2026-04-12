package net.cameramod.cameramod.mixin;

import net.cameramod.cameramod.CameraModClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
        // HUD im Kamera-Pass immer ausblenden
        if (CameraModClient.renderingCameraPass) {
            ci.cancel();
        }
    }
}