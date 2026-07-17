[Modrinth](https://modrinth.com/mod/stage-cam) | [Curseforge](https://www.curseforge.com/minecraft/mc-mods/stage-cam) | [GitHub](https://github.com/Flexwop-Studio/stage-cam) | [Wiki](https://github.com/Flexwop-Studio/stage-cam/wiki) | [Report Issues](https://github.com/Flexwop-Studio/stage-cam/issues)

![StageCam Textcraft Logo](https://cdn.modrinth.com/data/cached_images/e54594d3841e6023646d3740a14b3aa5aa83a53c.png)

# StageCam

A client-side Fabric mod for Minecraft 1.21.1–1.21.4 that adds a professional live camera system — perfect for streaming and content creation.

A second window opens automatically when you load a world. Point OBS at it as a Virtual Camera and you're live.

---

## Features

- **Fixed Cameras** — Place up to 9 camera points per set anywhere with `/setcam <slot> <set> <n>`
- **Camera Sets** — Unlimited sets of 9 cameras each, switch with Page Up / Page Down
- **Live Second Window** — Resizable, native resolution, designed for OBS Virtual Camera
- **Follow Mode** — Fixed camera smoothly rotates to always look at the player (Numpad /)
- **Zoom** — Numpad + / - to zoom in and out in 2° FOV steps
- **Follow Modes** — Handheld and Ego camera modes that follow the player
- **Custom Names** — Name your cameras for easy management
- **Camera Indicator** — HUD element showing active set, camera, follow mode and zoom
- **Camera Markers** — Wireframe markers visible through walls in the main window
- **Persistent** — Camera positions save automatically between sessions

---

## Keybinds

| Key | Action |
|-----|--------|
| Numpad 1-9 | Switch to camera slot |
| Numpad 0 | Turn off active camera |
| Numpad , | Toggle Handheld → Ego → Off |
| Numpad / | Toggle Follow Mode |
| Numpad + | Zoom in |
| Numpad - | Zoom out |
| Page Up | Next camera set |
| Page Down | Previous camera set |

All keybinds are configurable in **Options → Controls → StageCam**.

---

## Commands

| Command | Description |
|---------|-------------|
| `/setcam <1-9> <set> <n>` | Save current position as camera |
| `/renamecam <1-9> <n>` | Rename an existing camera |
| `/cam <1-9>` | Switch to a camera |
| `/delcam <1-9>` | Delete a camera |
| `/listcams` | List cameras in current set |
| `/listcams-s <set>` | List cameras in a specific set |

---

## Requirements

- Minecraft 1.21.1–1.21.4
- Fabric Loader 0.16.5+
- Fabric API

---

## Building from Source

### Prerequisites
- JDK 21 ([Adoptium](https://adoptium.net))
- Git

### Steps

```bash
git clone https://github.com/Flexwop/stage-cam.git
cd stage-cam
./gradlew build
```

The compiled `.jar` will be in `build/libs/`.

To run in a development environment:

```bash
./gradlew runClient
```

---

## Branches

The repository has two active branches, one per supported Minecraft version range:

| Branch | Minecraft Version |
|--------|------------------|
| `1.21.1` | 1.21.1 |
| `1.21.4` | 1.21.2, 1.21.3, 1.21.4 |

When contributing, please make sure to apply your changes to both branches. Bug fixes and new features should first be committed to `1.21.1`, then cherry-picked into `1.21.4`.

---

## Project Structure

```
src/main/java/net/cameramod/cameramod/
├── CameraMod.java                  # Server-side mod init, commands
├── CameraModClient.java            # Client-side init, keybinds
├── CameraPoint.java                # Camera position data
├── CameraStorage.java              # Save/load cameras to JSON
├── CameraMarkerRenderer.java       # Wireframe markers in world
├── CameraIndicatorRenderer.java    # HUD indicator
├── PlayerState.java                # Player state backup
├── SecondWindow.java               # Second GLFW window + framebuffer
└── mixin/
    ├── MixinCamera.java            # Override camera position + follow mode
    ├── MixinGameRenderer.java      # Dual render pass logic + zoom
    ├── MixinCameraFollowMode.java  # Handheld/Ego follow modes
    ├── MixinInGameHud.java         # Hide HUD in camera pass
    ├── MixinWorldRenderer.java     # Hide block outline in camera pass
    └── MixinGameMenuScreen.java    # StageCam button in pause menu
```

---

## How it Works

StageCam renders the game **twice per frame**:

1. **Camera pass** — renders the world from the camera position into a framebuffer, then blits it to the second window
2. **Main pass** — renders normally in First-Person for the main window

Mixins are used to override the camera position, hide HUD elements, and apply zoom during the camera pass.

---

## Shader Compatibility

StageCam works with shader packs but some effects may cause artifacts:

- **Bloom** — may bleed around the player model
- **TAA** — may cause ghosting due to the dual render pass

Disable Bloom and TAA in your shader settings for the best experience. StageCam will automatically warn you on world join if Iris is detected.

---

## Contributing

Pull requests are welcome! If you find a bug or have a feature request, open an issue on GitHub.

Please follow the existing code style and remember to apply changes to both branches.

---

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

## Credits

Built with [Fabric](https://fabricmc.net/) and [Fabric API](https://github.com/FabricMC/fabric).
