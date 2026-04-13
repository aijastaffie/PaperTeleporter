# PaperTeleporter

PaperMC plugin for building protected 7x7 teleport platforms that form a teleport network through NPC interaction.

## Tech Stack

- Java 25
- Gradle
- Paper API 1.21.5 (Minecraft API line)

## Current Features

- `/pt add <platformName>` for OP players (creates platform with default preset 2)
- `/pt remove <platformName>` for OP players
- `/pt rotate <platformName>` for OP players (rotates 90 degrees counterclockwise)
- `/pt preset <platformName> <1-6>` for OP players (change platform appearance)
- 6 preset styles:
  1. **Open**: No railings, fully open platform
  2. **Fenced** (default): Fence railings with opening and entrance stairs
  3. **Enclosed**: Full walls around platform perimeter (uses Brick Wall)
  4. **Towering**: Tall corner pillars for castle-like appearance
  5. **Flat Roof**: Covered platform with flat ceiling
  6. **Gable Roof**: Pitched roof design with gabled entrance
- Automatically handles block replacement in tight/tunneled spaces
- Builds a 7x7 platform from the targeted block, oriented by player facing
- Clears a 7x7x5 area before construction
- Places floor, railings (preset-dependent), spawn block, NPC marker block, and corner lights
- Spawns a villager NPC linked to the platform ID
- NPC click opens a GUI with all platform IDs
- Selecting a platform teleports the player to that platform spawn
- Platform area is protected from non-OP block break/place
- Platform data persists to `plugins/PaperTeleporter/platforms.json`
- Material config stored in `plugins/PaperTeleporter/platform-materials.json`

## Build

Use the project wrapper (recommended):

```bash
./gradlew build
```

Required:

- Java 25 toolchain available on the build machine

The plugin JAR is generated under `build/libs/`.

## Usage

1. Join the server as OP.
2. Look at a block that should be the platform anchor (max 20 blocks away).
3. Run `/pt add my-first-platform` (creates with default fenced preset 2).
4. Right-click the spawned villager NPC to open the teleport GUI.
5. Run `/pt rotate my-first-platform` to rotate a platform 90 degrees counterclockwise.
6. Run `/pt preset my-first-platform 1` to change to open platform (or 1-6 for different styles).
7. Run `/pt remove my-first-platform` to remove one platform.

### Preset Examples

- `/pt add mountain 1` - Open platform for scenic viewpoints
- `/pt add castle 4` - Towering pillars for castle aesthetic  
- `/pt add hub 5` - Flat roof for teleport hub feel
- `/pt preset mountain 4` - Convert existing to towering style

## Notes

- Code comments are kept in English.
- Paper API dependency uses the Minecraft version line (1.21.x), not Paper server build numbering (for example 26.x.x).
- Designed and tested on PaperMC server build line 26.x.x (tested with Paper 26.1.1).
