package net.cameramod.cameramod;

public record CameraPoint(double x, double y, double z, float yaw, float pitch, String name) {

    // Constructor without name for backwards compatibility
    public CameraPoint(double x, double y, double z, float yaw, float pitch) {
        this(x, y, z, yaw, pitch, "");
    }

    // Returns display name - falls back to "Camera X" if no name set
    public String displayName(int slot) {
        return (name != null && !name.isEmpty()) ? name : "Camera " + slot;
    }
}