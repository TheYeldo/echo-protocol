# Echo Protocol v0.2.0-alpha

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

## Previous Releases

### 0.1.0

- Initial standalone Fabric implementation for Minecraft 1.21.1.
- Added bounded player recording, per-player stages, server-directed Echo replays, admin commands, localization, placeholder assets, and advancements.
