# Beta configuration

`config/echo_protocol.json` remains backward-compatible with 0.2–0.4.1. Missing beta keys receive the values below; valid old values and unknown top-level fields are preserved. Malformed JSON is left untouched and safe in-memory defaults are used. All numeric values are clamped, minimum thread length cannot exceed maximum, graph limits cannot exceed hard bounds, and invalid preset names become `STANDARD`.

```json
{
  "intensity_preset": "STANDARD",
  "persistent_memory_enabled": true,
  "persistent_memory_data_version": 1,
  "persistent_panic_imprint_maximum": 3,
  "persistent_audio_residue_maximum": 16,
  "persistent_habit_maximum": 16,
  "persistent_significant_event_maximum": 24,
  "persistent_false_memory_seed_maximum": 4,
  "room_memory_enabled": true,
  "room_memory_maximum_nodes": 20,
  "room_memory_maximum_edges": 48,
  "room_memory_probe_radius": 8,
  "room_memory_maximum_block_checks": 256,
  "room_memory_update_interval_seconds": 20,
  "room_memory_minimum_confidence": 0.35,
  "room_memory_strong_event_confidence": 0.65,
  "memory_threads_enabled": true,
  "memory_thread_minimum_steps": 2,
  "memory_thread_maximum_steps": 4,
  "memory_thread_maximum_active_per_player": 1,
  "memory_thread_minimum_interval_minutes": 20,
  "memory_thread_strong_finale_silence_minutes": 30,
  "memory_thread_resume_after_restart": true,
  "contradictory_memories_enabled": true,
  "split_memory_enabled": true,
  "repeated_ending_enabled": true,
  "wrong_destination_enabled": true,
  "memory_arrived_first_enabled": true,
  "conflicting_item_enabled": true,
  "missing_segment_enabled": true,
  "contradiction_event_maximum_per_session": 2,
  "memory_contamination_enabled": true,
  "memory_contamination_initial": 0.0,
  "memory_contamination_growth_multiplier": 1.0,
  "memory_contamination_passive_recovery_per_hour": 0.04,
  "memory_contamination_maximum": 1.0,
  "memory_contamination_admin_tests_affect_state": false,
  "observation_profile_enabled": true,
  "observation_profile_minimum_samples": 6,
  "observation_profile_decay_per_hour": 0.08,
  "observation_profile_adaptation_strength": 0.30,
  "thread_aware_original_enabled": true,
  "thread_aware_false_memories_enabled": true,
  "thread_aware_audio_residue_enabled": true,
  "thread_aware_peripheral_echoes_enabled": true,
  "beta_memory_text_fragments_enabled": true,
  "beta_memory_text_minimum_interval_minutes": 45
}
```

Presets are bounded runtime multipliers over the configured values. `SUBTLE` lengthens intervals, favors quiet audio/peripheral presentation, slows contamination, and lowers strong-event pressure. `STANDARD` is the intended default. `INTENSE` moderately raises thread, contradiction, and Original activity while retaining cooldowns and caps. Switching to `CUSTOM` preserves the current configured values. After manually tuning preset-managed pacing, use `CUSTOM` to make that intent explicit.

The existing 0.4.1 settings remain supported and are documented in the root README. No graphical configuration dependency is added.
