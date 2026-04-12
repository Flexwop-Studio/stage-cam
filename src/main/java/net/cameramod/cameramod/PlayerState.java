package net.cameramod.cameramod;

import net.minecraft.world.GameMode;

public record PlayerState(double x, double y, double z, float yaw, float pitch, GameMode gameMode) {}
