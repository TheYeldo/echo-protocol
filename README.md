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

The generated mod JAR is written to `build/libs/echo-protocol-0.2.0-alpha.jar`.

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
  "debug_logging": false,
  "memory_echo_enabled": true,
  "corrupted_echo_enabled": true,
  "mimic_echo_enabled": true,
  "memory_echo_weight": 70,
  "corrupted_echo_weight": 25,
  "mimic_echo_weight": 5,
  "mimic_minimum_stage_two_minutes": 30,
  "mimic_session_cooldown_minutes": 60,
  "mimic_movement_delay_min_ticks": 40,
  "mimic_movement_delay_max_ticks": 80,
  "mimic_damage_enabled": true,
  "mimic_damage": 4.0,
  "mimic_max_hits_per_event": 1,
  "mimic_chase_enabled": true,
  "mimic_chase_min_seconds": 5,
  "mimic_chase_max_seconds": 10,
  "corrupted_echo_can_approach": true,
  "echo_moves_when_unobserved": true,
  "echo_light_effects": true,
  "echo_sound_effects": true
}
```

Invalid or missing values are replaced with safe defaults.

## Commands

All commands require permission level 2.

- `/echo_protocol stage get <player>`
- `/echo_protocol stage set <player> <0-2>`
- `/echo_protocol spawn <player>`
- `/echo_protocol spawn memory <player>`
- `/echo_protocol spawn corrupted <player>`
- `/echo_protocol spawn mimic <player>`
- `/echo_protocol replay <player>`
- `/echo_protocol event stop <player>`
- `/echo_protocol mimic hostile <player>`
- `/echo_protocol mimic harmless <player>`
- `/echo_protocol clear <player>`
- `/echo_protocol reload`
- `/echo_protocol debug on`
- `/echo_protocol debug off`

## Memory and Performance Notes

Recording is bounded per online player. With the default interval of 2 ticks and 10 minutes of history, each player stores at most 6,000 movement frames plus a small bounded set of sound/chat markers. Mimic Echoes use a separate bounded delayed movement queue of about 220 frames while active. The implementation avoids pathfinding, chunk force-loading, block entity ticking, and global entity scans. Echo entities are short-lived, non-colliding, server-directed events.

Estimated memory use is roughly 1 to 2 MB per active player depending on JVM object layout and held item NBT size. The held item visual is copied in a single-item stack form and old data is discarded automatically.

## Test Checklist

- New Fabric project launches.
- Dedicated server launches.
- Player history is recorded.
- Recording buffer remains bounded.
- Echo entity spawns.
- Echo follows a previous player route.
- Memory Echo accurately replays a route and does not react.
- Corrupted Echo begins with a valid replay, desynchronizes, looks toward the player, and despawns.
- Mimic Echo copies current movement with delay, makes small mistakes, and despawns.
- Hostile Mimic damage respects config, cooldown, max hits, and Peaceful difficulty.
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
6. Run `/echo_protocol spawn memory <your_name>`.
7. Run `/echo_protocol stage set <your_name> 2`, then `/echo_protocol spawn corrupted <your_name>`.
8. Confirm the Echo may stop, look toward you, approach briefly, and fade without changing terrain.
9. Run `/echo_protocol spawn mimic <your_name>` and move for several seconds.
10. Confirm the Mimic copies delayed movement, then desynchronizes.
11. Run `/echo_protocol mimic hostile <your_name>` and confirm the event ends quickly and respects damage limits.
12. Toggle `shared_echoes` in the config, run `/echo_protocol reload`, and compare visibility with another nearby player.
