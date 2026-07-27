# Echo Protocol v0.4.1-alpha — Stability Update

## Fixed

- Replay routes now translate to their validated safe spawn as a whole instead of jumping back to the old absolute coordinates on the first tick.
- A rejected world entity spawn no longer sets the active-event lock, event history, counters, or command success state.
- Dimension changes now discard incompatible movement history before any new replay can select it.
- Private Audio Residue and recorded-memory sounds now originate from the validated world position while remaining target-only.
- Panic Imprints retain the newest bounded capture rather than truncating away the moments nearest the trigger.
- False Memory prefixes honor the configured maximum, and generated plans fall back to an observable head deviation if selected actions would be no-ops.
- Hostile Mimics are excluded in Peaceful and terminate within their configured bounded state even when chase movement is disabled.
- Forced administrative events no longer grant normal gameplay progression; observation advancements now require an actual observed behavior transition.
- The previously unreachable `it_saw_me` and `out_of_sync` conditions now have real Corrupted Echo trigger paths.
- Original confrontation and familiar-location advancements now require their corresponding observed event instead of any spawn or disappearance.
- `/echo_protocol clear` now ends active entities and clears all per-player session managers, and `skin clear-cache` now sends a real client cache-clear packet.
- Stage cooldown persistence now stores remaining delays, avoiding stale process-local tick deadlines after a server restart.
- Muted sound attempts no longer consume their cooldown.

## Improved

- Added JUnit 5 regression coverage for bounded recordings, replay translation, Panic eviction, False Memory deviation visibility, view-angle geometry, event history, stuck detection, config migration, advancement resources, and persisted delays.
- Original event selection now prefers a matching valid bed, storage, workstation, doorway, portal, or frequently used item and ignores removed block anchors.
- Event Director selection excludes Peripheral Echo and Audio Residue candidates while their real cooldown/session prerequisites are unavailable.
- Config migration preserves unknown top-level fields, clamps non-finite values safely, and never overwrites malformed JSON.
- Removed empty custom-sound declarations; the mod intentionally plays registered vanilla sound events.

## Verification

- Java 21 unit tests and clean Loom builds cover the pure logic and resource contracts listed in `docs/VERIFICATION.md`.
- Runtime results for this branch are recorded in `docs/VERIFICATION.md`; unperformed client or multiplayer checks are explicitly marked rather than inferred from compilation.

## Known Limitations

- Recordings and the 0.4 bounded event histories remain session-scoped.
- Original and False Memory navigation remains intentionally local, loaded-chunk-only, and bounded; obstructed plans may end safely.
- Human visual verification is still required for subjective opacity, animation quality, classic/slim skins, and peripheral comfort.

# Echo Protocol v0.4.0-alpha — False Memories

## Added

- False Memories with authentic recorded prefixes and bounded deterministic deviations
- Panic Imprints with per-player bounded short movement segments and minimal safe metadata
- Rare Peripheral Echo events with observation grace and at most one reposition
- Audio Residue using validated vanilla sound identifiers only
- Bounded Borrowed Habit summaries for The Original
- Adaptive Event Director history, repetition suppression, combat gating, and strong-event silence
- Six hidden spoiler-aware advancements with English and Russian localization
- Permission-level-2 testing and inspection commands for every new subsystem
- Server entity-tracking filter for target-only private Echo packets
- Linux and Windows wrapper build documentation

## Improved

- Completely reworked The Original movement from intermittent teleport-like micro-steps to continuous server-tick movement with acceleration, braking, and real position-driven walk animation
- Added bounded local waypoint planning, swept collision/hazard checks, gradual body rotation, independent head tracking, observation grace, and stuck recovery
- Added distinct action plans for all nine Original event kinds, with purposeful pauses and bounded endings
- Added seven Original movement test modes and a live operator status command
- Added backward-compatible, clamped Original movement configuration while keeping version `0.4.0-alpha`
- Event pacing and deterministic weighted selection
- Disconnect and dimension-transfer cleanup
- Existing configuration migration and safe clamping
- Pose preservation during authentic replay prefixes
- The Original's use of familiar locations and frequently observed items
- Dedicated-server privacy boundaries for movement, particles, sounds, and metadata

## Safety

- False Memories, Panic Imprints, Peripheral Echoes, Audio Residue, and Borrowed Habits are harmless by default
- New systems never modify blocks, inventories, containers, world rules, chunks, or Hardcore state
- No raw audio, voice chat, private chat, or full recording history is transmitted by the new systems

## Known Limitations

- New bounded histories are session-scoped in this alpha
- Fabricated movement is scripted and stops safely when a loaded route is obstructed
- Manual gameplay testing is required before merge, tag, or release
- The Original's navigation is intentionally local and bounded; complex multi-floor routes, closed doors, ladders, and long paths may use a deliberate fallback and disappear

## Previous Releases

### Echo Protocol v0.3.0-alpha

## Added

- Stage 3: The Original
- Independent Original Echo behavior
- Familiar-location tracking
- Bed, storage, crafting, furnace, doorway, portal, idle-location, and manual location markers
- Scripted observing, inhabiting, recognizing, replacing, confronting, and leaving states
- Rare replacement events in familiar player locations
- Optional target-only Original text events
- Optional non-lethal confrontation damage
- New administrator testing commands
- Five new Stage 3 advancements
- Persistent Stage 3 progress and cooldowns

## Improved

- Late-game psychological horror progression
- Player familiarity tracking
- Safe scripted Echo positioning
- Server-authoritative privacy
- Persistence of event counters and familiar locations

## Known Limitations

- The Original uses scripted movement rather than advanced navigation
- Familiar locations are bounded heuristics and do not detect actual ownership
- Mimic advancement progress completed before v0.3.0-alpha is not retroactively imported into Stage 3 progression
- Recordings themselves are not preserved across server restarts

### Echo Protocol v0.2.0-alpha

## Added

- Memory Echoes that replay previous player routes
- Corrupted Echoes with desynchronized behavior
- Mimic Echoes that copy current movement with a delay
- Rare scripted hostile Mimic encounters
- Real player skins
- Classic and slim player model support
- Safe Echo spawning and hidden repositioning
- Server-authoritative private Echo tracking
- Configurable atmospheric sounds and visual effects
- Reduced flashing and reduced visual effects settings
- New advancements
- New administrator and testing commands
- English and Russian localization
- Dedicated server and multiplayer support

## Improved

- Movement and rotation interpolation
- Echo fade-in and fade-out
- Line-of-sight handling
- Multiplayer privacy
- Echo skin caching
- Spawn position validation
- Configuration validation

## Known Limitations

- Some Echo behavior is scripted rather than using advanced navigation
- Recordings are not preserved across server restarts
- Sound design currently relies mostly on compatible vanilla sound events
- Some menu and portal-state detection is limited by server-side information

### 0.1.0

- Initial standalone Fabric implementation for Minecraft 1.21.1.
- Added bounded player recording, per-player stages, server-directed Echo replays, admin commands, localization, placeholder assets, and advancements.
