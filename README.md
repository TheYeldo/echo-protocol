# Echo Protocol

Echo Protocol is a standalone Fabric mod for Minecraft Java Edition 1.21.1. The world records short, bounded slices of a player's recent movement and later replays distorted memories as translucent Echoes.

## Requirements

- Minecraft Java Edition 1.21.1
- Fabric Loader 0.16.14 or compatible
- Fabric API 0.116.13+1.21.1
- Java 21

## Build

```powershell
.\gradlew.bat build
```

On Unix-like systems:

```sh
./gradlew build
```

The generated mod JAR is written to `build/libs/echo-protocol-0.1.0.jar`.

## Configuration

The config file is created at `config/echo_protocol.json` on first server start.

```json
{
  "enabled": true,
  "recording_sample_interval_ticks": 2,
  "maximum_history_minutes": 10,
  "minimum_replay_seconds": 5,
  "maximum_replay_seconds": 15,
  "stage_zero_minutes": 10,
  "stage_two_minutes": 60,
  "minimum_event_interval_seconds": 480,
  "maximum_event_interval_seconds": 1080,
  "shared_echoes": false,
  "chat_echoes": false,
  "torch_flicker": true,
  "sound_echoes": true,
  "echo_opacity": 0.45,
  "debug_logging": false
}
```

Invalid or missing values are replaced with safe defaults.

## Commands

All commands require permission level 2.

- `/echo_protocol stage get <player>`
- `/echo_protocol stage set <player> <0-2>`
- `/echo_protocol spawn <player>`
- `/echo_protocol replay <player>`
- `/echo_protocol clear <player>`
- `/echo_protocol reload`
- `/echo_protocol debug on`
- `/echo_protocol debug off`

## Memory and Performance Notes

Recording is bounded per online player. With the default interval of 2 ticks and 10 minutes of history, each player stores at most 6,000 movement frames plus a small bounded set of sound/chat markers. The implementation avoids pathfinding, chunk force-loading, block entity ticking, and global entity scans. Echo entities are short-lived, non-colliding, server-directed replays.

Estimated memory use is roughly 1 to 2 MB per active player depending on JVM object layout and held item NBT size. The held item visual is copied in a single-item stack form and old data is discarded automatically.

## Test Checklist

- New Fabric project launches.
- Dedicated server launches.
- Player history is recorded.
- Recording buffer remains bounded.
- Echo entity spawns.
- Echo follows a previous player route.
- Skin fallback works.
- Fade-in and fade-out work.
- Echo has no collision.
- Echo cannot damage players.
- Targeted visibility works.
- `shared_echoes` config works.
- Stage progression works.
- Cooldowns work.
- Data survives a world restart.
- Commands work.
- Russian and English localization load.
- Advancements load.
- No missing textures.
- No missing model warnings.
- No client-only class crash on dedicated server.

## Manual In-Game Smoke Test

1. Install the built JAR and Fabric API in a Minecraft 1.21.1 Fabric instance.
2. Create a world or join a dedicated server.
3. Walk a visible route for at least 10 seconds while changing direction and held items.
4. Run `/echo_protocol replay <your_name>`.
5. Confirm a translucent Echo follows a previous segment and fades out.
6. Run `/echo_protocol stage set <your_name> 2`, then `/echo_protocol spawn <your_name>`.
7. Confirm the Echo may stop and look toward you or produce corrupted timing.
8. Toggle `shared_echoes` in the config, run `/echo_protocol reload`, and compare visibility with another nearby player.
