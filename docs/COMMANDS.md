# Operator commands

Every command below requires permission level 2. Spawn commands return success only after the sound was sent or every required entity was accepted by the world. Typical honest failures include a disabled feature, missing authentic recording, missing room, active event, incompatible dimension, unsafe or unloaded spawn, unavailable contradiction, or insufficient samples.

Forced Original event and movement-test commands prefer a safe visible reveal in front of the target. To verify beta.2 interactions, hold a placeable block, record a hand swing, and replay it; or run `waiting`, `your_bed`, and `confrontation` beside a closed door. Door/block reactions are target-only visuals and restore without editing the world.

## Beta memory

```text
/echo_protocol memory status <player>
/echo_protocol memory rooms <player>
/echo_protocol memory threads <player>
/echo_protocol memory contamination <player>
/echo_protocol memory profile <player>
/echo_protocol memory validate <player>
/echo_protocol memory clear-thread <player>
/echo_protocol memory clear-profile <player>
/echo_protocol memory reset-beta-data <player>
```

The reset preserves Stage progression and familiar locations. There is no wildcard reset-all command. Diagnostics never print movement frames, full routes, chat, IP addresses, inventory history, profiles/skins, container contents, or other players' private metadata.

## Threads and contradictions

```text
/echo_protocol thread start <player> <type>
/echo_protocol thread advance <player>
/echo_protocol thread cancel <player>
/echo_protocol thread status <player>
/echo_protocol contradiction spawn <player> split_memory
/echo_protocol contradiction spawn <player> repeated_ending
/echo_protocol contradiction spawn <player> wrong_destination
/echo_protocol contradiction spawn <player> memory_arrived_first
/echo_protocol contradiction spawn <player> conflicting_item
/echo_protocol contradiction spawn <player> missing_segment
```

Thread types are `bedroom`, `storage`, `entrance`, `portal`, `panic`, `missing_route`, `empty_room`, `followed_echo`, and `ignored_sound`. Admin-started and admin-advanced threads do not grant normal gameplay advancements or contamination by default.

## Contamination and presets

```text
/echo_protocol contamination get <player>
/echo_protocol contamination set <player> <0.0-1.0>
/echo_protocol contamination add <player> <-1.0-1.0>
/echo_protocol contamination reset <player>
/echo_protocol preset subtle
/echo_protocol preset standard
/echo_protocol preset intense
/echo_protocol preset custom
/echo_protocol preset status
```

Preset selection is persisted to the existing config, reloads immediately, and preserves unrelated and unknown config fields where the JSON can be parsed. Existing 0.4.1 commands remain unchanged; see `README.md` for their list.
