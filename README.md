# Echo Protocol

Echo Protocol is a standalone Fabric mod for Minecraft Java Edition 1.21.1. The world records short, bounded slices of a player's recent movement and later replays distorted memories as translucent Echoes.

This is an alpha release. Features are playable and manually tested, but behavior, configuration defaults, and save data may change before a stable release. The `0.3.0-alpha` development branch adds Stage 3: The Original and should be tested before merging or tagging.

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

The generated mod JAR is written to `build/libs/echo-protocol-0.3.0-alpha.jar`.

## Installation

1. Install Minecraft Java Edition 1.21.1.
2. Install Fabric Loader 0.16.14 or a compatible 1.21.1 loader.
3. Install Fabric API 0.116.13+1.21.1 in the `mods` folder.
4. Place `echo-protocol-0.3.0-alpha.jar` in the same `mods` folder.
5. Launch the client or dedicated server with Java 21.

## Echo Types

- Memory Echoes replay a previous route from the target player's bounded movement history.
- Corrupted Echoes begin as a replay, then may desynchronize, watch, approach, hide, and fade.
- Mimic Echoes copy the target player's current movement with a delay and may rarely enter a scripted hostile encounter when enabled.
- The Original appears only after rare late-game Stage 3 requirements. It uses the target player's skin and familiar locations, moves independently, and suggests replacement without becoming a normal boss.

## Multiplayer and Privacy

Echo Protocol supports dedicated servers and multiplayer. By default, `shared_echoes` is `false`: private Echo entities are server-filtered so unrelated nearby players are not allowed to track the private Echo entity, and private Echo sounds, particles, replay state, Original text events, and chat echoes are sent only to the target player's client. Familiar-location data is stored server-side and is never sent to unrelated clients. When `shared_echoes` is `true`, nearby players may see and hear Echo entities/effects normally, but private text and familiarity data remain target-only.

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
  "echo_sound_effects": true,
  "real_player_skins": true,
  "skin_cache_enabled": true,
  "memory_echo_opacity": 0.42,
  "corrupted_echo_opacity": 0.48,
  "mimic_echo_opacity": 0.58,
  "mimic_threatening_opacity": 0.90,
  "corruption_flicker_enabled": true,
  "corruption_afterimages_enabled": true,
  "corruption_visual_intensity": 0.65,
  "reduced_visual_effects": false,
  "reduced_flashing": true,
  "echo_master_volume": 0.75,
  "memory_echo_volume": 0.55,
  "corrupted_echo_volume": 0.70,
  "mimic_echo_volume": 0.80,
  "breathing_enabled": true,
  "static_effects_enabled": true,
  "safe_spawn_attempts": 16,
  "minimum_echo_spawn_distance": 8,
  "maximum_echo_spawn_distance": 32,
  "stage_three_enabled": true,
  "stage_three_required_playtime_minutes": 180,
  "stage_three_required_memory_events": 8,
  "stage_three_required_corrupted_events": 4,
  "stage_three_required_mimic_events": 2,
  "original_enabled": true,
  "original_first_event_delay_minutes": 20,
  "original_minimum_event_interval_minutes": 60,
  "original_maximum_event_interval_minutes": 150,
  "original_familiar_locations_enabled": true,
  "original_text_events": false,
  "original_damage_enabled": false,
  "original_damage": 4.0,
  "original_maximum_active_per_player": 1,
  "original_maximum_familiar_locations": 16,
  "original_near_full_opacity": 0.94
}
```

Invalid or missing values are replaced with safe defaults.

## Commands

All commands require permission level 2.

- `/echo_protocol stage get <player>`
- `/echo_protocol stage set <player> <0-3>`
- `/echo_protocol spawn <player>`
- `/echo_protocol spawn memory <player>`
- `/echo_protocol spawn corrupted <player>`
- `/echo_protocol spawn mimic <player>`
- `/echo_protocol replay <player>`
- `/echo_protocol event stop <player>`
- `/echo_protocol mimic hostile <player>`
- `/echo_protocol mimic harmless <player>`
- `/echo_protocol original spawn <player>`
- `/echo_protocol original event <player> <occupied_place|already_home|your_bed|wrong_owner|earlier_than_you|familiar_item|waiting|empty_room|confrontation>`
- `/echo_protocol original confront <player>`
- `/echo_protocol original stop <player>`
- `/echo_protocol familiar list <player>`
- `/echo_protocol familiar clear <player>`
- `/echo_protocol familiar add-current <player>`
- `/echo_protocol skin status <player>`
- `/echo_protocol skin clear-cache <player>`
- `/echo_protocol visual memory <player>`
- `/echo_protocol visual corrupted <player>`
- `/echo_protocol visual mimic <player>`
- `/echo_protocol position test <player>`
- `/echo_protocol sound test memory <player>`
- `/echo_protocol sound test corrupted <player>`
- `/echo_protocol sound test mimic <player>`
- `/echo_protocol clear <player>`
- `/echo_protocol reload`
- `/echo_protocol debug on`
- `/echo_protocol debug off`

## Memory and Performance Notes

Recording is bounded per online player. With the default interval of 2 ticks and 10 minutes of history, each player stores at most 6,000 movement frames plus a small bounded set of sound/chat markers. Mimic Echoes use a separate bounded delayed movement queue of about 220 frames while active. Stage 3 adds up to `original_maximum_familiar_locations` lightweight familiar-location entries per player; the default is 16 entries containing only type, dimension, block position, visit count, and last-seen tick. Skin rendering uses Minecraft's client skin provider and a bounded 64-entry client-side resolver cache; the server stores only the target UUID needed for allowed viewers. Safe spawn checks use at most `safe_spawn_attempts` local attempts and never force-load chunks. The implementation avoids pathfinding, chunk force-loading, block entity ticking, and global entity scans. Echo entities are short-lived, non-colliding, server-directed events.

Estimated memory use is roughly 1 to 2 MB per active player depending on JVM object layout and held item NBT size, plus a few kilobytes for Stage 3 familiar-location and progression metadata. The held item visual is copied in a single-item stack form and old data is discarded automatically.

## Known Limitations

- Some Echo behavior is scripted rather than using advanced navigation.
- Recordings are kept in memory and are not preserved across server restarts.
- Sound design currently relies mostly on compatible vanilla sound events.
- Some menu and portal-state detection is limited by server-side information.
- The Original's familiar-location system is heuristic and bounded; it does not inspect container contents or infer detailed base ownership.
- The Original uses scripted safe movement and disappearance instead of normal hostile mob AI.

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
- Real player skin appears when the client has or can resolve it through Minecraft's skin provider.
- Classic and slim skin models render correctly.
- Safe position command finds valid local positions or fails cleanly.
- Sound profile commands play without spam or missing-sound warnings.
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
- Stage 3 unlock persists across restart.
- Familiar-location tracking remains bounded.
- The Original spawns only at loaded safe positions or fails cleanly.
- The Original never changes blocks or inventories.
- Optional Original text is target-only.
- Peaceful disables optional Original damage.

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
13. Run `/echo_protocol familiar add-current <your_name>`, then `/echo_protocol stage set <your_name> 3`.
14. Run `/echo_protocol original spawn <your_name>` and confirm The Original uses your skin, holds a visual familiar item, and leaves without modifying blocks or inventories.
15. Run `/echo_protocol original confront <your_name>` and confirm the confrontation ends without a boss bar or normal chase.
