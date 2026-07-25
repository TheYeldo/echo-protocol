# Echo Protocol

Echo Protocol is a standalone Fabric mod for Minecraft Java Edition 1.21.1. The world records short, bounded slices of a player's recent movement and later replays distorted memories as translucent Echoes.

This is an alpha release. Features are playable, but behavior, configuration defaults, and save data may change before a stable release. The `0.4.0-alpha` development branch adds False Memories and must receive manual gameplay testing before merging or tagging.

## Requirements

- Minecraft Java Edition 1.21.1
- Fabric Loader 0.16.14 or compatible
- Fabric API 0.116.13+1.21.1
- Java 21

## Build

Linux:

```sh
chmod +x gradlew
./gradlew clean build
```

Windows:

```powershell
gradlew.bat clean build
```

No global Gradle installation is required. The generated mod JAR is written to `build/libs/echo-protocol-0.4.0-alpha.jar`.

## Installation

1. Install Minecraft Java Edition 1.21.1.
2. Install Fabric Loader 0.16.14 or a compatible 1.21.1 loader.
3. Install Fabric API 0.116.13+1.21.1 in the `mods` folder.
4. Place `echo-protocol-0.4.0-alpha.jar` in the same `mods` folder.
5. Launch the client or dedicated server with Java 21.

## Echo Types

- Memory Echoes replay a previous route from the target player's bounded movement history.
- False Memories begin with authentic recorded frames, then follow a bounded deterministic server-side deviation plan. They keep the target's skin, pose, rotation, and held item during the authentic prefix and are not labelled in normal play.
- Corrupted Echoes begin as a replay, then may desynchronize, watch, approach, hide, and fade.
- Mimic Echoes copy the target player's current movement with a delay and may rarely enter a scripted hostile encounter when enabled.
- Peripheral Echoes briefly appear outside the center of the target's view, use an observation grace period, and disappear or reappear once at another loaded safe location.
- Audio Residue replays only allow-listed Minecraft sound identifiers from bounded interaction history; it never records audio, microphones, voice chat, or private messages.
- The Original appears only after rare late-game Stage 3 requirements. It uses the target player's skin, familiar locations, and bounded habit summaries. Its deliberate local movement uses event-specific action plans, acceleration and deceleration, observation-aware pauses, bounded waypoint planning, and stuck recovery without becoming a normal mob or boss.

## Multiplayer and Privacy

Echo Protocol supports dedicated servers and multiplayer. By default, `shared_echoes` is `false`: a server tracking filter prevents unrelated clients from receiving private Echo spawn and movement packets. Private Echo sounds, particles, False Memory events, Panic Imprint replays, Audio Residue, Original text, and chat echoes are target-only. Full recordings, familiar locations, Panic metadata, habit summaries, and event history remain server-side. When `shared_echoes` is `true`, nearby players may see and hear appropriate public entities/effects, but private metadata and summaries remain target-only.

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
  "original_near_full_opacity": 0.94,
  "original_walk_speed": 0.09,
  "original_slow_walk_speed": 0.065,
  "original_fast_walk_speed": 0.13,
  "original_maximum_speed": 0.16,
  "original_acceleration": 0.012,
  "original_deceleration": 0.018,
  "original_body_turn_speed_degrees": 12.0,
  "original_head_turn_speed_degrees": 18.0,
  "original_arrival_radius": 0.38,
  "original_minimum_movement_distance": 3.0,
  "original_maximum_movement_distance": 12.0,
  "original_maximum_waypoints": 5,
  "original_stuck_window_ticks": 25,
  "original_stuck_minimum_progress": 0.20,
  "original_maximum_replans": 3,
  "original_observation_grace_ticks": 8,
  "original_unobserved_grace_ticks": 14,
  "original_minimum_pause_ticks": 16,
  "original_maximum_pause_ticks": 70,
  "false_memories_enabled": true,
  "false_memory_minimum_stage": 2,
  "false_memory_minimum_real_prefix_seconds": 4,
  "false_memory_maximum_real_prefix_seconds": 10,
  "false_memory_minimum_accuracy": 0.72,
  "false_memory_maximum_deviations": 2,
  "false_memory_event_weight": 8,
  "panic_imprints_enabled": true,
  "panic_imprint_health_threshold": 8.0,
  "panic_imprint_maximum_saved": 4,
  "panic_imprint_minimum_event_interval_minutes": 90,
  "peripheral_echoes_enabled": true,
  "peripheral_echo_maximum_per_session": 2,
  "peripheral_echo_minimum_interval_minutes": 35,
  "peripheral_echo_duration_seconds": 5,
  "audio_residue_enabled": true,
  "audio_residue_maximum_per_session": 3,
  "audio_residue_minimum_interval_minutes": 25,
  "borrowed_habits_enabled": true,
  "maximum_tracked_habits": 12,
  "adaptive_event_director": true,
  "prevent_repeated_events": true,
  "recent_event_history_size": 12,
  "strong_event_silence_minutes": 20,
  "join_event_grace_minutes": 8
}
```

Values are clamped to safe bounds. Missing 0.4 fields are merged into existing 0.3 configuration files without deleting existing supported values. A malformed file is left untouched and safe in-memory defaults are used, so an operator can repair the original file.

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
- `/echo_protocol original movement-test <player> <walk|fast-walk|approach|retreat|patrol|doorway|bed>`
- `/echo_protocol original status <player>`
- `/echo_protocol original stop <player>`
- `/echo_protocol familiar list <player>`
- `/echo_protocol familiar clear <player>`
- `/echo_protocol familiar add-current <player>`
- `/echo_protocol false-memory spawn <player>`
- `/echo_protocol false-memory test-deviation <player>`
- `/echo_protocol panic list <player>`
- `/echo_protocol panic capture-current <player>`
- `/echo_protocol panic replay <player>`
- `/echo_protocol panic clear <player>`
- `/echo_protocol peripheral spawn <player>`
- `/echo_protocol audio-residue play <player>`
- `/echo_protocol habits list <player>`
- `/echo_protocol habits clear <player>`
- `/echo_protocol director status <player>`
- `/echo_protocol director history <player>`
- `/echo_protocol director clear-history <player>`
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

Recording is bounded per online player. With the default interval of 2 ticks and 10 minutes of history, each player stores at most 6,000 movement frames plus a small bounded set of sound/chat markers. Mimic Echoes use a separate bounded delayed movement queue of about 220 frames while active. Stage 3 adds up to `original_maximum_familiar_locations` lightweight familiar-location entries per player; the default is 16 entries containing only type, dimension, block position, visit count, and last-seen tick. An active Original caches at most 12 action segments and 5 local waypoints. Route calculation examines at most 320 already-loaded local positions and runs only when a segment starts or recovery is needed, never every tick. Skin rendering uses Minecraft's client skin provider and a bounded 64-entry client-side resolver cache; the server stores only the target UUID needed for allowed viewers. Safe spawn checks use at most `safe_spawn_attempts` local attempts and never force-load chunks. The implementation avoids full mob navigation, chunk force-loading, block entity ticking, and global entity scans. Echo entities are short-lived, non-colliding, server-directed events.

False Memory plans are capped at 120 authentic and 160 fabricated frames. Each player has at most 4 Panic Imprints by default (120 frames each), 24 lightweight Audio Residue identifiers, 12 habit summaries, and 12 director-history entries. These structures are cleared safely at server shutdown; session counters and active entities are cleaned on disconnect or dimension transfer.

Estimated additional 0.4 overhead is about 150–350 KB per active player at defaults, depending mostly on held-item component data in Panic Imprint frames. Combined with the existing 6,000-frame movement history, total use remains roughly 1.2–2.4 MB per active player on a typical 64-bit JVM. No new system scans the full world or force-loads chunks.

The reworked Original controller retains roughly 3–8 KB per active event for its action plan, observation counters, and waypoints. A route calculation may transiently allocate roughly 50–150 KB for at most 320 local search nodes; that data is released after planning and is not rebuilt every tick.

## Known Limitations

- Echo behavior uses bounded scripted plans rather than general-purpose mob navigation.
- Recordings are kept in memory and are not preserved across server restarts.
- Panic Imprints, Audio Residue history, habit summaries, and adaptive event history are session-scoped in this alpha; progression and familiar locations remain persistent.
- Sound design currently relies mostly on compatible vanilla sound events.
- Some menu and portal-state detection is limited by server-side information.
- The Original's familiar-location system is heuristic and bounded; it does not inspect container contents or infer detailed base ownership.
- The Original uses bounded local waypoint planning rather than advanced navigation. Complex multi-floor routes, closed doors, ladders, and long paths may deliberately fall back or disappear.
- Fabricated movement uses bounded scripted segments rather than general pathfinding; a blocked route may end early and disappear safely.

## Test Checklist

- New Fabric project launches.
- Dedicated server launches.
- Player history is recorded.
- Recording buffer remains bounded.
- Echo entity spawns.
- Echo follows a previous player route.
- False Memory preserves a genuine prefix before the deviation state begins.
- False Memory plans and deviation history stay bounded.
- Panic Imprint capture and replay contain no damage, explosion, fire, or mob recreation.
- Peripheral events respect their per-session cap and observation grace period.
- Audio Residue remains target-only when `shared_echoes=false`.
- Borrowed Habits remain bounded and never interact with blocks or inventories.
- Adaptive history prevents immediate repeats and enforces strong-event silence.
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
- Every Original event finishes within a bounded lifetime and never persists to disk.
- Original walking updates each server tick, accelerates, brakes near its destination, and drives real limb animation from position changes.
- Body rotation, independent head tracking, observation grace, blocked-route replanning, and stuck fallback behave without snapping or endless wall pushing.

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
14. Run every `/echo_protocol original event <your_name> <event>` variant: `occupied_place`, `already_home`, `your_bed`, `wrong_owner`, `earlier_than_you`, `familiar_item`, `waiting`, `empty_room`, and `confrontation`.
15. For each variant, confirm deliberate 3+ block movement, visible limb motion, smooth body/head turns, purposeful pauses, no wall clipping, no world changes, and a bounded clean ending.
16. Run `/echo_protocol original movement-test <your_name> walk`, `fast-walk`, `approach`, `retreat`, `patrol`, `doorway`, and `bed` in a small house, corridor, stairs, outdoors, near a closed doorway, bed, chest, portal, and a blocked direct route. Use `/echo_protocol original status <your_name>` while each test is active.
17. Repeat an Original movement test under temporary low TPS if practical and confirm speed remains tick-based without snapping or endless stuck retries.
18. Run `/echo_protocol original confront <your_name>` and confirm the confrontation stops about 2.5–3.2 blocks away, reacts to approach/retreat, and ends without a boss bar or indefinite chase.
19. Run `/echo_protocol false-memory test-deviation <your_name>` and confirm an authentic route prefix transitions into a harmless fabricated action.
20. At low health, walk for several seconds, run `/echo_protocol panic capture-current <your_name>`, then `/echo_protocol panic replay <your_name>`.
21. Run `/echo_protocol peripheral spawn <your_name>` and turn toward the edge-of-view Echo; confirm it does not flicker and reappears at most once.
22. Interact with a door, chest, crafting table, or furnace, then run `/echo_protocol audio-residue play <your_name>`.
23. Run `/echo_protocol habits list <your_name>` and `/echo_protocol director history <your_name>`; confirm neither command prints movement frames, private messages, or profile data.
