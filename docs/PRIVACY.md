# Multiplayer privacy

All beta decisions and stored data are server-authoritative and keyed by target UUID. Room graphs, Memory Threads, contamination, observation profiles, habits, Panic summaries, Audio Residue context, and significant-event history have no client payload.

When `shared_echoes=false`, a server entity-tracking mixin stops non-target clients before they receive private Echo tracking. Movement therefore follows the same recipient boundary as spawn. Private sounds and spectral door/block updates use the target connection; particles use the target-specific server API; memory fragments and Original text use the target player's message channel. Spectral block updates are restored from current authoritative world state and are never written to world storage.

When `shared_echoes=true`, documented nearby entity visuals and positional audio may be shared. Thread names/steps, room IDs/positions, contamination, observation metrics/style, and personal fragments remain private. Client render cancellation is not the primary privacy mechanism.

Managers never use another player's graph or profile for selection. Disconnect, death/respawn, and dimension transfer clear only the affected UUID's live entities, recording, in-flight observation, transition state, and session counters; the compact thread is paused where appropriate. Two-player recipient rules have pure automated coverage. A real two-client packet/visual check remains separately identified in `VERIFICATION.md` and is never inferred from unit tests.
