# PaperTeleporter

PaperMC plugin for building protected 7x7 teleport platforms that form a teleport network through NPC interaction.

## Download

- You can download the pre-built `.jar` file from this repository's **Releases** section.

## Feedback and Bug Reports

- Feedback is welcome.
- If you find an issue, please open a bug report in this repository.

## Tech Stack

- Java 25
- Gradle
- Paper API 1.21.5 (Minecraft API line)

## Current Features

- `/pt add <platformName> [presetNumber]` for OP players
  - default preset is 2 (Fenced)
  - supports built-in presets `1-6`
  - supports custom presets `7-99`
- `/pt remove <platformName>` for OP players
- `/pt rotate <platformName>` for OP players (rotates 90 degrees counterclockwise)
- `/pt preset <platformName> <presetNumber>` for OP players (change platform appearance)
- `/pt save-preset` for OP players (auto-saves a custom template to next free slot `7-99`)
- `/pt preset remove <number>` for OP players (remove custom preset)
- `/pt spawnpoint [distance]` for OP players (minimum 20)
- `/pt backup now` for OP players (create manual backup checkpoint before updates)
- 6 preset styles:
  1. **Open**: No railings, fully open platform
  2. **Fenced** (default): Fence railings with opening and entrance stairs
  3. **Enclosed**: Full walls around platform perimeter (uses Brick Wall)
  4. **Towering**: Tall corner pillars for castle-like appearance
  5. **Flat Roof**: Covered platform with flat ceiling
  6. **Gable Roof**: Pitched roof with a solid ridge beam and hanging lanterns at ridge ends
- Builds a 7x7 platform from the **top of targeted block** (`+1`), oriented by player facing
- If target is a thin snow layer, it is ignored for anchor height calculation
- Clears a 7x7x7 area before construction
- Places floor, railings (preset-dependent), spawn block, NPC marker block, and preset-dependent lighting
- Spawns a villager NPC linked to the platform ID
- NPC click opens a GUI with all platform IDs
- GUI pagination supports up to 216 platforms (4 pages)
- Selecting a platform teleports the player to that platform spawn
- Platform area is protected from non-OP block break/place
- OP in creative mode can bypass protection (for admin cleanup/building)
- Prevents overlapping platforms and enforces minimum spawn distance
- Platform data persists to `plugins/PaperTeleporter/platforms.json`
- Material config stored in `plugins/PaperTeleporter/platform-materials.json`
- Custom presets persist to `plugins/PaperTeleporter/custom-presets.json`
- Automatic backup + restore for platform/preset data:
  - current backup: `plugins/PaperTeleporter-backups/current/`
  - timestamp snapshots: `plugins/PaperTeleporter-backups/snapshots/`

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
2. Look at a block near where you want platform center (max 20 blocks away).
3. Run `/pt add my-first-platform` (creates with default fenced preset 2 on top of target block).
4. Right-click the spawned villager NPC to open the teleport GUI.
5. Run `/pt rotate my-first-platform` to rotate a platform 90 degrees counterclockwise.
6. Run `/pt preset my-first-platform 1` to change to open platform (or other preset numbers).
7. Run `/pt remove my-first-platform` to remove one platform.

### Custom Preset Workflow

1. Build a 7x7x7 template in the world.
2. Leave the center spawn position empty (stand in the center hole).
3. Run `/pt save-preset`.
4. Plugin auto-picks next free custom slot (`7-99`) and clears template area.
5. Create new platform from that template with `/pt add my-custom 7` (replace `7` with your saved slot).
6. Remove a custom template with `/pt preset remove 7`.

### Preset Examples

- `/pt add mountain 1` - Open platform for scenic viewpoints
- `/pt add castle 4` - Towering pillars for castle aesthetic  
- `/pt add hub 5` - Flat roof for teleport hub feel
- `/pt add marketplace 7` - Use custom preset slot 7
- `/pt preset mountain 4` - Convert existing to towering style

## Notes

- Code comments are kept in English.
- Paper API dependency uses the Minecraft version line (1.21.x), not Paper server build numbering (for example 26.x.x).
- Designed and tested on PaperMC server build line 26.x.x (tested with Paper 26.1.1).

## Safe Plugin Update

1. Stop server.
2. (Recommended) run `/pt backup now` before stopping, to force a fresh checkpoint.
3. Replace plugin JAR as usual.
4. Do **not** delete `plugins/PaperTeleporter-backups/`.
5. If `plugins/PaperTeleporter/` was deleted accidentally, plugin restores `platforms.json` and `custom-presets.json` from `plugins/PaperTeleporter-backups/current/` on startup.

Important:
- If both `plugins/PaperTeleporter/` and `plugins/PaperTeleporter-backups/` are deleted, metadata is lost and existing world structures become unmanaged. In that case, cleanup must be done manually.

Folder creation behavior:
- `plugins/PaperTeleporter/` is created by plugin normal data writes.
- `plugins/PaperTeleporter-backups/` is created when backup files are written (automatic on save, or immediately via `/pt backup now`).
