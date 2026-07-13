# Echo Protocol v0.3.0-alpha

## Added

- Stage 3: The Original, a rare late-game psychological horror stage.
- The Original Echo type with independent scripted behavior.
- Familiar-location tracking for beds, chests, crafting areas, furnaces, doors, idle spots, portals, and manual admin markers.
- Rare Original replacement events, including occupied-place, already-home, wrong-owner, waiting, empty-room, and confrontation sequences.
- Optional target-only Original text events, disabled by default.
- Optional controlled Original confrontation damage, disabled by default and never lethal.
- Stage 3 progression persistence across logout, world reload, and server restart.
- New Stage 3 administrator commands and familiar-location commands.
- New Stage 3 advancements and English/Russian localization.

## Improved

- Stage tracking now records bounded per-type Echo event counts.
- Existing Memory, Corrupted, and Mimic systems remain available after Stage 3 unlock.
- Multiplayer privacy rules now explicitly cover Original sounds, particles, text, and familiar-location data.

## Known Limitations

- The Original uses scripted safe-position movement rather than advanced navigation.
- Familiar locations are lightweight heuristics and do not infer protected claims or detailed room ownership.
- Original sound design currently relies on compatible vanilla sound events.
- The feature branch is intended for manual gameplay testing before merge, tag, or release.

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
