package net.cameramod.cameramod;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32;

public class SecondWindow {

    private long windowHandle = -1;
    private int framebuffer = -1;
    private int framebufferTexture = -1;
    private int depthBuffer = -1;

    // Dynamic size - starts at 854x480, resizes with window
    public int fbWidth = 854;
    public int fbHeight = 480;

    private GLFWFramebufferSizeCallback resizeCallback;

    public void init() {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE); // Resizable!

        long mainWindow = MinecraftClient.getInstance().getWindow().getHandle();
        windowHandle = GLFW.glfwCreateWindow(fbWidth, fbHeight, "StageCam - Camera View", 0, mainWindow);

        if (windowHandle == 0) {
            CameraMod.LOGGER.error("Second window could not be created!");
            return;
        }

        // Register resize callback
        resizeCallback = GLFWFramebufferSizeCallback.create((window, width, height) -> {
            if (width > 0 && height > 0) {
                fbWidth = width;
                fbHeight = height;
                // Recreate framebuffer with new size
                deleteFramebuffer();
                initFramebuffer();
                CameraMod.LOGGER.info("StageCam window resized to " + width + "x" + height);
            }
        });
        GLFW.glfwSetFramebufferSizeCallback(windowHandle, resizeCallback);

        CameraMod.LOGGER.info("StageCam second window created!");
    }

    public void initFramebuffer() {
        // Use the actual Minecraft framebuffer size for best quality
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() != null) {
            fbWidth = Math.max(fbWidth, client.getWindow().getFramebufferWidth());
            fbHeight = Math.max(fbHeight, client.getWindow().getFramebufferHeight());
        }

        framebuffer = GL32.glGenFramebuffers();
        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, framebuffer);

        framebufferTexture = GL32.glGenTextures();
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, framebufferTexture);
        GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, GL32.GL_RGB,
            fbWidth, fbHeight, 0, GL32.GL_RGB, GL32.GL_UNSIGNED_BYTE, 0);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_LINEAR);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_LINEAR);
        GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0,
            GL32.GL_TEXTURE_2D, framebufferTexture, 0);

        depthBuffer = GL32.glGenRenderbuffers();
        GL32.glBindRenderbuffer(GL32.GL_RENDERBUFFER, depthBuffer);
        GL32.glRenderbufferStorage(GL32.GL_RENDERBUFFER, GL32.GL_DEPTH_COMPONENT, fbWidth, fbHeight);
        GL32.glFramebufferRenderbuffer(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT,
            GL32.GL_RENDERBUFFER, depthBuffer);

        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, 0);
        CameraMod.LOGGER.info("StageCam framebuffer created at " + fbWidth + "x" + fbHeight);
    }

    private void deleteFramebuffer() {
        if (framebuffer != -1) {
            GL32.glDeleteFramebuffers(framebuffer);
            framebuffer = -1;
        }
        if (framebufferTexture != -1) {
            GL32.glDeleteTextures(framebufferTexture);
            framebufferTexture = -1;
        }
        if (depthBuffer != -1) {
            GL32.glDeleteRenderbuffers(depthBuffer);
            depthBuffer = -1;
        }
    }

    public void blitToSecondWindow() {
        if (windowHandle == -1 || framebuffer == -1) return;

        long mainWindow = MinecraftClient.getInstance().getWindow().getHandle();

        // Switch to second window context
        GLFW.glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();

        // Get actual second window framebuffer size
        int[] winW = new int[1], winH = new int[1];
        GLFW.glfwGetFramebufferSize(windowHandle, winW, winH);

        if (winW[0] <= 0 || winH[0] <= 0) {
            GLFW.glfwMakeContextCurrent(mainWindow);
            GL.createCapabilities();
            return;
        }

        GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, framebuffer);
        GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, 0);
        GL32.glBlitFramebuffer(
            0, 0, fbWidth, fbHeight,
            0, 0, winW[0], winH[0],
            GL32.GL_COLOR_BUFFER_BIT, GL32.GL_LINEAR
        );

        GLFW.glfwSwapBuffers(windowHandle);
        GLFW.glfwPollEvents();

        // Switch back to main window
        GLFW.glfwMakeContextCurrent(mainWindow);
        GL.createCapabilities();
    }

    public long getWindowHandle() { return windowHandle; }
    public int getFramebuffer() { return framebuffer; }
    public int getFramebufferTexture() { return framebufferTexture; }

    // Used by MixinGameRenderer for blit source dimensions
    public int getFbWidth() { return fbWidth; }
    public int getFbHeight() { return fbHeight; }

    public boolean isOpen() {
        if (windowHandle == -1) return false;
        return !GLFW.glfwWindowShouldClose(windowHandle);
    }

    public void close() {
        if (resizeCallback != null) {
            resizeCallback.free();
            resizeCallback = null;
        }
        deleteFramebuffer();
        if (windowHandle != -1) {
            GLFW.glfwDestroyWindow(windowHandle);
            windowHandle = -1;
        }
    }
}