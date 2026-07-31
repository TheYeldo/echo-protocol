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

The automated privacy tests pass. Real two-client dedicated-server tests were completed on Minecraft 1.21.3 with the unchanged shared JAR and on Minecraft 1.21.5. Temporary client packet-handler instrumentation counted actual received entity-spawn/tracker, particle, sound, text, and custom-payload packets; it was kept outside the repository.

- With `shared_echoes=false`, only the target received Memory Echo, False Memory, Peripheral Echo, Panic Imprint replay, The Original, and Audio Residue traffic on 1.21.5. The 1.21.3 target likewise exclusively received Memory Echo, Mimic Echo, False Memory, and The Original traffic. Observer counters stayed at zero for private Echo spawns/effects and Echo Protocol payloads.
- With `shared_echoes=true`, both clients received the intentionally shared Echo entity, while target-only particles and player memory metadata remained private.
- Room Graph, Memory Thread, contamination, Observation Profile, familiar-location, and Panic Imprint records remained server-owned and UUID-separated; no network payload exposes them.
- Disconnecting the observer did not cancel the target's running Original event. Disconnecting the target cleaned its Echo while leaving the observer connected with valid state.

## Persistence and configuration

The logical persistence layout and data version remain unchanged. Unit tests cover round trips, malformed individual records, missing fields, unknown/future data preservation, bounded collections, and thread/contamination state. Focused clean-stop restart tests passed on Minecraft 1.21.3, 1.21.4, and previously on 1.21.5.

On 1.21.3 and 1.21.4, familiar locations, Room Graph nodes/edges, active Memory Thread state, contamination, selected preset, Panic Imprint, Audio Residue, and bounded habit summaries survived restart with `validation=ok`. Expected passive contamination decay was limited to 0.001 during the restart interval. No duplicate Echo entity or stuck director lock appeared, and records remained separated by player UUID.

The common `config/echo_protocol.json` format and all beta fields remain unchanged. Temporary test configurations/worlds/logs were kept outside the repository and are not committed.

## Resources and rendering

`fabric.mod.json` declares only exact tested Minecraft versions. The shared build lists `1.21.2` and `1.21.3`; the other builds declare exactly `1.21.4` or `1.21.5`. Java remains 21 and the player-visible mod version remains `0.5.0-beta.2`.

English/Russian language files, textures, sounds, advancements, mixins, entrypoints, and other release resources remain packaged. Servers loaded all resources without parsing failures, and real clients loaded worlds without missing-texture or renderer crashes during the exercised paths.

## Final focused verification

- Minecraft 1.21.5 passed the representative pipeline: Memory, Corrupted, Mimic, False Memory with deviation, Panic capture/replay, Peripheral cleanup, actual Audio Residue playback, Room Graph creation, Memory Thread advancement, contamination change, moving/contextual Original, and event cleanup.
- Minecraft 1.21.4 passed Memory, deviating False Memory, actual Audio Residue playback, moving Original/event cleanup, and restart persistence.
- Minecraft 1.21.3 passed Memory, delayed Mimic, deviating False Memory, moving/contextual Original, private entity tracking, and restart persistence using the exact shared production JAR.
- Short controlled 1.21.3/1.21.5 sessions covered idle, Memory, False Memory, active Memory Thread, and Original movement. Server logs contained no post-`Done` warning/error, repeated exception, `Can't keep up`, packet/effect spam symptom, retained entity, or stuck event lock. Collections shown by diagnostics stayed bounded. No formal MSPT benchmark was recorded.

## Remaining non-blocking manual coverage

- Exhaustive visual inspection of every classic/slim skin, held-item combination, pose, fade, render angle, and every variation of all Original events was not repeated on every target.
- Two-client privacy was intentionally tested only on required representative targets 1.21.3 and 1.21.5, not separately on 1.21.2 or 1.21.4.
- No formal profiler or comparative MSPT benchmark was run; the performance result is a short release-blocker/log check.

No Minecraft 1.21.6-or-newer work was performed.
