# Echo Protocol v0.3.0-alpha

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

## Previous Releases

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
