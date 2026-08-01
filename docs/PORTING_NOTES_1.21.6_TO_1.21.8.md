# Porting notes: Minecraft 1.21.6 through 1.21.8

## Scope and baseline

The port preserves mod version `0.5.0-beta.2` and starts from the released feature set at tag `v0.5.0-beta.2` / commit `c4121bc21abc891af83f5fdcb2c17e29cec87d5e`. The clean 1.21.5 port was reviewed as the newest Yarn-based reference. No gameplay system, command, config field, advancement, entity type, item, block, dimension, progression, or story content was added.

## API and toolchain changes

- Updated the compile baseline to Minecraft 1.21.6, Yarn `1.21.6+build.1`, Loader `0.19.3`, Fabric API `0.128.2+1.21.6`, Loom `1.17.17`, Gradle `9.6.1`, and Java 21.
- Replaced `ServerPlayerEntity#getServerWorld()` calls with the covariant 1.21.6 `getWorld()` API.
- Migrated entity custom NBT hooks to `writeCustomData(WriteView)` and `readCustomData(ReadView)` without changing saved field names or the mod data version.
- Runtime compatibility was tested with Yarn `1.21.7+build.8` / Fabric API `0.129.0+1.21.7` and Yarn `1.21.8+build.1` / Fabric API `0.136.1+1.21.8`.

## Rendering, networking, and mixins

The 1.21.5 extracted render-state implementation compiled and ran on the 1.21.6–1.21.8 pipeline without a version-specific fork. Renderer registration, skin lookup, classic/slim state, held-item state, poses, yaw/pitch, and translucent alpha were retained. A real 1.21.8 screenshot confirmed a skinned translucent Echo without missing/black texture; exhaustive pose/skin combinations were not repeated on every runtime.

The stable built-in string tracked-data codec for UUIDs was preserved with UUID validation. Real clients received Echo spawn, tracker, movement, sound, and particle packets without codec failures or disconnects.

`ServerEntityTrackingMixin` remains critical and unchanged. Verbose logs confirmed that it applied on all three dedicated servers; no `require = 0` suppression was added.

## Persistence, configuration, and resources

The logical persistence schema remains version 1. Existing 0.5.0-beta.2 data restored Familiar Locations, Room Graph nodes/edges, a Memory Thread, contamination, Observation Profile, Panic Imprint, Audio Residue, habits, recent events, and the selected preset on a clean 1.21.8 restart with `validation=ok`. No duplicate Echo or stuck event lock appeared.

`config/echo_protocol.json` and all public field semantics remain unchanged. English/Russian localization, textures, sounds, advancement JSON, mixin metadata, and entrypoints remain packaged. `fabric.mod.json` names exactly 1.21.6, 1.21.7, and 1.21.8 and requires Java 21.

## Runtime verification

- 1.21.6: Memory, delayed Mimic, deviating False Memory, Panic capture/replay, moving Original, cleanup, server, and client/world.
- 1.21.7: Memory, distinct Corrupted, Peripheral, moving Original, cleanup, server, and client/world. Audio capture/play was also exercised, but not against the final byte-identical artifact, so this row is documented as partial.
- 1.21.8: complete representative pipeline including real route replay, all representative Echo variants, contradiction, Panic, Peripheral, actual Audio Residue, Room Graph nodes/edges, Memory Thread advancement, contamination, Observation Profile, moving/contextual Original, preset/diagnostics, disconnect cleanup, real dimension cleanup, and persistence restart.
- Two real 1.21.8 clients were instrumented outside the repository. With `shared_echoes=false`, the observer received no private Echo spawn/movement, sound, particle, text, or custom payload while the target did. With `shared_echoes=true`, documented shared visuals were received while memory metadata remained server-only. Disconnecting either side did not corrupt the other player's state.
- The short 1.21.8 log/performance inspection found no repeated exception, mixin/API failure, stuck event, retained discarded Echo, or unbounded diagnostic state. One `Can't keep up` warning occurred only while first generating the End during the explicit dimension test; it did not repeat and is not attributed to Echo Protocol. No formal benchmark or profiler run was performed.

## Known limitations

- Exhaustive classic/slim skins, every held item, every pose/fade angle, and every Original event variation were not manually repeated on all three versions.
- Restart persistence and two-client privacy were tested on the required representative target 1.21.8, not separately on 1.21.6 and 1.21.7.
- The performance result is a controlled release-blocker/log check rather than a comparative MSPT benchmark.

No work was performed on Minecraft 1.21.9 or newer.
