package net.cameramod.cameramod.mixin;

import net.cameramod.cameramod.CameraModClient;
import net.cameramod.cameramod.SecondWindow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.opengl.GL32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Shadow private MinecraftClient client;

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (client.options == null) return;

        boolean screenOpen = client.currentScreen != null
            && !(client.currentScreen instanceof ChatScreen);

        if (screenOpen) {
            if (CameraModClient.renderingCameraPass) {
                CameraModClient.renderingCameraPass = false;
                CameraModClient.blittedThisPass = false;
                if (CameraModClient.savedPerspective != null) {
                    client.options.setPerspective(CameraModClient.savedPerspective);
                    CameraModClient.savedPerspective = null;
                }
                setBobView(true);
            }
            return;
        }

        if (!CameraModClient.isCameraActive()) {
            if (CameraModClient.renderingCameraPass) {
                CameraModClient.renderingCameraPass = false;
                CameraModClient.blittedThisPass = false;
                client.options.setPerspective(
                    CameraModClient.savedPerspective != null ?
                    CameraModClient.savedPerspective : Perspective.FIRST_PERSON
                );
                CameraModClient.savedPerspective = null;
                setBobView(true);
            }
            return;
        }

        if (CameraModClient.renderingCameraPass) {
            CameraModClient.savedPerspective = client.options.getPerspective();
            if (CameraModClient.egoMode) {
                client.options.setPerspective(Perspective.FIRST_PERSON);
            } else {
                client.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
            }
            setBobView(false);
        }
    }

    // 1.21.4 returns Float not Double
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (CameraModClient.renderingCameraPass) {
            float fov = CameraModClient.cameraFov >= 0
                ? CameraModClient.cameraFov
                : (float) client.options.getFov().getValue();
            cir.setReturnValue(fov);
        }
    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;",
        ordinal = 0
    ))
    private void beforeScreenRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (!CameraModClient.renderingCameraPass) return;
        SecondWindow sw = CameraModClient.secondWindow;
        if (sw == null || !sw.isOpen()) return;
        blitToSecondWindow(sw);
        CameraModClient.blittedThisPass = true;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void afterRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (client.options == null) return;
        if (!CameraModClient.isCameraActive()) return;

        boolean screenOpen = client.currentScreen != null
            && !(client.currentScreen instanceof ChatScreen);
        if (screenOpen) return;

        SecondWindow sw = CameraModClient.secondWindow;

        if (CameraModClient.renderingCameraPass) {
            if (!CameraModClient.blittedThisPass && sw != null && sw.isOpen()) {
                blitToSecondWindow(sw);
            }
            CameraModClient.blittedThisPass = false;
            CameraModClient.renderingCameraPass = false;
            if (CameraModClient.savedPerspective != null) {
                client.options.setPerspective(CameraModClient.savedPerspective);
            }
            setBobView(true);
            ((GameRenderer)(Object)this).render(tickCounter, tick);
        } else {
            CameraModClient.renderingCameraPass = true;
        }
    }

    private void blitToSecondWindow(SecondWindow sw) {
        int mainFb = client.getFramebuffer().fbo;
        int srcW = client.getWindow().getFramebufferWidth();
        int srcH = client.getWindow().getFramebufferHeight();
        int dstW = sw.getFbWidth();
        int dstH = sw.getFbHeight();

        GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, mainFb);
        GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, sw.getFramebuffer());
        GL32.glBlitFramebuffer(0, 0, srcW, srcH, 0, 0, dstW, dstH,
            GL32.GL_COLOR_BUFFER_BIT, GL32.GL_LINEAR);
        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, mainFb);
        sw.blitToSecondWindow();
    }

    private void setBobView(boolean value) {
        client.options.bobView.setValue(value);
    }
}