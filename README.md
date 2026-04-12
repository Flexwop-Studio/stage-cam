# StageCam

A client-side Fabric mod for Minecraft 1.21.1 that adds a professional live camera system — perfect for streaming and content creation.

A second window opens automatically when you load a world. Point OBS at it as a Virtual Camera and you're live.

---

## Features

- **Fixed Cameras** — Place up to 9 camera points anywhere with `/setcam <1-9>`
- **Live Second Window** — Resizable, native resolution, designed for OBS Virtual Camera
- **Follow Modes** — Handheld and Ego camera modes that follow the player
- **Custom Names** — Name your cameras for easy management
- **Camera Indicator** — HUD element showing the active camera or mode
- **Camera Markers** — Wireframe markers visible through walls in the main window
- **Persistent** — Camera positions save automatically between sessions

---

## Keybinds

| Key | Action |
|-----|--------|
| Numpad 1-9 | Switch to camera slot |
| Numpad 0 | Turn off active camera |
| Numpad , | Toggle Handheld → Ego → Off |

All keybinds are configurable in **Options → Controls → StageCam**.

---

## Commands

| Command | Description |
|---------|-------------|
| `/setcam <1-9>` | Save current position as camera |
| `/setcam <1-9> <name>` | Save with a custom name |
| `/renamecam <1-9> <name>` | Rename an existing camera |
| `/cam <1-9>` | Switch to a camera |
| `/delcam <1-9>` | Delete a camera |
| `/listcams` | List all saved cameras |

---

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.5+
- Fabric API 0.102.0+1.21.1

---

## Building from Source

### Prerequisites
- JDK 21 ([Adoptium](https://adoptium.net))
- Git

### Steps

```bash
git clone https://github.com/yourusername/stagecam.git
cd stagecam
./gradlew build
```

The compiled `.jar` will be in `build/libs/`.

To run in a development environment:

```bash
./gradlew runClient
```

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
    ├── MixinCamera.java            # Override camera position
    ├── MixinGameRenderer.java      # Dual render pass logic
    ├── MixinCameraFollowMode.java  # Handheld/Ego follow modes
    ├── MixinInGameHud.java         # Hide HUD in camera pass
    ├── MixinWorldRenderer.java     # Hide block outline in camera pass
    └── MixinGameMenuScreen.java    # StageCam button in pause menu
```

---

## How it Works

StageCam renders the game **twice per frame**:

1. **Camera pass** — renders the world from the camera position (Third-Person) into a framebuffer, then blits it to the second window
2. **Main pass** — renders normally in First-Person for the main window

Mixins are used to override Minecraft's camera position and hide HUD elements during the camera pass.

---

## Shader Compatibility

StageCam works with shader packs but some effects may cause artifacts:

- **Bloom** — may bleed around the player model
- **TAA** — may cause ghosting due to the dual render pass

Disable Bloom and TAA in your shader settings for the best experience.

---

## Contributing

Pull requests are welcome! If you find a bug or have a feature request, open an issue on GitHub.

Please follow the existing code style — no unnecessary dependencies, keep it simple.

---

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

## Credits

Built with [Fabric](https://fabricmc.net/) and [Fabric API](https://github.com/FabricMC/fabric).