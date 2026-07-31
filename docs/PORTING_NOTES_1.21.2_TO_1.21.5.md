# Porting notes: Minecraft 1.21.2 through 1.21.5

## Scope and baseline

All branches start from the exact `v0.5.0-beta.2` release commit `c4121bc21abc891af83f5fdcb2c17e29cec87d5e`. The original 1.21.1 tag and artifact were not modified. No gameplay feature, command, configuration field, advancement, item, block, entity type, progression stage, or story content was added.

Original toolchain: Minecraft 1.21.1, Java 21, Yarn 1.21.1+build.3, Fabric Loader 0.16.14, Fabric API 0.116.13+1.21.1, Loom 1.17.14, and Gradle 9.6.1.

## Compatibility changes

- 1.21.2/1.21.3: adapted damage calls that now require `ServerWorld`, keyed `EntityType` construction, renamed attributes/use actions, and the render-state player renderer API. Skin selection, translucent render layers, held items, poses, and head/body rotation remain represented in the renderer.
- 1.21.4: adapted targeted particle sending to the new `important` argument and moved held-item population to `ArmedEntityRenderState`/`ItemModelManager` without dropping translucent rendering.
- 1.21.5: adapted Optional-returning NBT accessors and the codec-based `PersistentStateType` API while preserving existing field names, UUID int arrays, schema version, bounds, and malformed-record isolation. Adapted shield sound registry entries, fall distance, and selected-slot access.
- 1.21.5 removed vanilla `OPTIONAL_UUID` tracked data. An initial custom global handler passed compilation but failed a real client packet decode because handler registration order was not stable across physical sides. The final implementation uses the built-in string handler with validated UUID conversion; the same production JAR then spawned Memory, Corrupted, Mimic, False Memory, Panic, and Original Echo entities without disconnecting the client.

## Mixins and privacy

`ServerEntityTrackingMixin` was retained as a critical mixin; no `require = 0` suppression was introduced. Verbose dedicated-server logs confirmed its runtime application on every target. Privacy remains server-side entity tracking rather than client-only render cancellation.

The automated privacy tests pass. A real two-client packet-recipient test was not completed, so multiplayer privacy is not marked fully verified for upload.

## Persistence and configuration

The logical persistence layout and data version remain unchanged. Unit tests cover round trips, malformed individual records, missing fields, unknown/future data preservation, bounded collections, and thread/contamination state. A 1.21.4 world containing `echo_protocol_memory.dat` loaded under 1.21.5 without reset; after another clean dedicated-server restart, rooms, active thread, contamination, familiar locations, preset, and Panic Imprint were still present with `validation=ok`.

The common `config/echo_protocol.json` format and all beta fields remain unchanged. Temporary test configurations/worlds/logs were kept outside the repository and are not committed.

## Resources and rendering

`fabric.mod.json` declares only exact tested Minecraft versions. The shared build lists `1.21.2` and `1.21.3`; the other builds declare exactly `1.21.4` or `1.21.5`. Java remains 21 and the player-visible mod version remains `0.5.0-beta.2`.

English/Russian language files, textures, sounds, advancements, mixins, entrypoints, and other release resources remain packaged. Servers loaded all resources without parsing failures, and real clients loaded worlds without missing-texture or renderer crashes during the exercised paths.

## Remaining manual verification

- Run the entire gameplay checklist on each target, including visible authentic/deviated routes, Peripheral Echo, Audio Residue, contextual Original movement/event termination, dimension changes, and disconnect-during-event cleanup.
- Visually inspect classic/slim skins, held items, all poses, fade-in/out, translucency, and all Echo types from multiple camera angles.
- Connect two real clients on the required targets and capture actual packet recipients with `shared_echoes=false` and `true`.
- Run comparative performance measurements; startup showed no repeated exceptions or log spam, but no formal tick-time benchmark was recorded.

No Minecraft 1.21.6-or-newer work was performed.
