# PaperTeleporter

PaperMC plugin for building protected 7x7 teleport platforms that form a teleport network through NPC interaction.

## Tech Stack

- Java 25
- Gradle
- Paper API 1.21.5 (Minecraft API line)

## Current Features

- `/pt add <platformName>` for OP players
- `/pt remove <platformName>` for OP players
- `/pt rotate <platformName>` for OP players
- Builds a 7x7 platform from the targeted block, oriented by player facing
- Clears a 7x7x5 area before construction
- Places floor, fence ring with a 3-block opening, spawn block, NPC marker block, and corner lights
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
3. Run `/pt add my-first-platform`.
4. Right-click the spawned villager NPC to open the teleport GUI.
5. Run `/pt rotate my-first-platform` to rotate a platform 90 degrees counterclockwise.
6. Run `/pt remove my-first-platform` to remove one platform.

## Notes

- Code comments are kept in English.
- Paper API dependency uses the Minecraft version line (1.21.x), not Paper server build numbering (for example 26.x.x).
- Designed and tested on PaperMC server build line 26.x.x (tested with Paper 26.1.1).
