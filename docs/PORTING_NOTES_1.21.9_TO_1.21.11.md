# Porting notes: Minecraft 1.21.9-1.21.11

## Scope and baseline

- Echo Protocol version remains `0.5.0-beta.2`.
- Source release remains tag `v0.5.0-beta.2` at `c4121bc21abc891af83f5fdcb2c17e29cec87d5e`.
- The verified 1.21.6-1.21.8 implementation was used as the technical reference.
- No gameplay mechanics, balance, configuration fields, commands, advancements, or persistence semantics were added or removed.
- Java remains 21.

## Minecraft 1.21.9 and 1.21.10

The main source migration was the Yarn/API world-access rename from `Entity#getWorld()` to `Entity#getEntityWorld()`, together with `getPos()` to `getEntityPos()` at entity call sites. Game-profile accessors and client renderer types were updated for the current mappings.

The Echo renderer continues using extracted `PlayerEntityRenderState` values. It was adapted to the modern ordered render-command queue, camera render state, player skin types, translucent entity layer, tint/alpha handling, and current player model-layer keys. Render state does not retain a mutable Echo entity reference. Classic/slim models, player skin, held items, movement poses, body/head rotation, fade and translucency were preserved.

The stable built-in string tracked-data codec remains in use for target and skin UUIDs. UUID strings are validated before use. No custom physical-side-dependent tracked-data handler was reintroduced.

The same production JAR was tested on real 1.21.9 and 1.21.10 Fabric servers and clients. Both servers reached `Done`, both clients connected and loaded worlds, and the critical tracking mixin applied. Representative events included Memory Echo, Mimic/Corrupted Echo, False Memory, Peripheral Echo, Audio Residue and The Original across the two runtimes.

## Minecraft 1.21.11

Minecraft 1.21.11 required three focused source adaptations:

- command permission predicates now use `CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)`;
- translucent entity render-layer creation moved to `RenderLayers.entityTranslucent(...)`;
- the player skin cache now stores only secure textures so a temporary unsigned default returned during asynchronous download cannot permanently replace the real skin.

The skin issue was reproduced visually: the initial default texture could remain cached after the signed player skin became available. The fix is intentionally limited to cache admission and does not change gameplay or networking.

Networking payload structure and persistence schema did not change. The server-authoritative `ServerEntityTrackingMixin` still uses a required injection; no `require = 0` suppression was introduced. The modern `ReadView`/`WriteView` persistence implementation remains logically compatible with 0.5.0-beta.2 state.

## Runtime verification highlights

On 1.21.11 the representative pipeline exercised real route recording/replay, distinct Corrupted behavior, delayed Mimic following, False Memory deviation, contradiction, Panic Imprint capture/replay, Peripheral Echo cleanup, Audio Residue capture/playback, Room Graph nodes/edges, Memory Thread advancement, contamination, Observation Profile samples, The Original movement/context event, presets, diagnostics, disconnect cleanup and dimension cleanup.

After a clean server stop and restart, validation reported `ok`; familiar locations, a bounded 5/5 Room Graph, thread step 2/3, expected contamination decay, observer profile, Panic Imprints, Audio Residue, habits and the intense preset were restored. No duplicate Echo or stuck event lock appeared.

Two real clients verified private entity packet recipients on 1.21.11. False Memory, Peripheral Echo and The Original were received by the target only when sharing was disabled. When sharing was enabled, the same Echo ID reached both clients. Disconnecting either player did not corrupt the other player or leave an active event lock. Sounds/particles/text use target recipients, while Room Graph, thread, contamination, profile and familiar-location records remain server-only and have no synchronization payload.

## Resources and limitations

Fabric metadata is bounded to exactly `1.21.9`/`1.21.10` for the shared artifact and exactly `1.21.11` for the separate artifact. Existing advancements, sounds, textures, English/Russian localization, mixin configuration and entrypoints remain packaged.

No reduced restart test or separate two-client test was run on 1.21.9/1.21.10; those are recorded as `NOT TESTED` because the required restart/privacy target for this batch was 1.21.11. The performance check was short and intended to find release-blocking repetition or leaks, not to produce benchmark numbers.
