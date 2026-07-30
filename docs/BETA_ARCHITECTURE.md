# Echo Protocol 0.5.0-beta.1 Architecture Plan

## Ownership and persistence

`StageManager` remains the server-authoritative owner of per-player progression. It is extended with one
`PersistentMemoryManager`; beta data is not mirrored into a second gameplay scheduler. The manager stores a
single `PersistentEchoMemory` through Minecraft 1.21.1 `PersistentState` in the Overworld save data manager.
Each UUID maps to one bounded `PlayerMemoryState`, alongside the existing stage/progression snapshot.

The old `data/echo_protocol_state.json` is read only as a migration source when no PersistentState import marker
exists. It is not deleted or rewritten. Valid 0.2–0.4.1 stage counters and familiar locations are imported,
malformed players and malformed child entries are skipped independently, and a future unsupported beta data
version is logged and left read-only rather than being overwritten.

Meaningful beta mutations mark PersistentState dirty: room discovery/reinforcement, graph edge changes, thread
creation/advance/cancel/complete, persistent panic/audio/habit changes, contamination changes above a bounded
threshold, profile bucket changes, and significant-event records. Per-tick recording, entity state, camera
samples, and playtime increments do not mark it dirty. Server stop and explicit migration synchronize stage
progress once.

## Session-only information

- the ten-minute `PlayerRecording` ring buffer;
- raw chat and sound-marker history;
- live Echo entities, render state, action cursors, and navigation working sets;
- damage/fire/drowning detectors, join/dimension/sleep grace maps, and per-session event counts;
- raw observation samples and camera direction;
- active route-search nodes and temporary spawn candidates.

## Persistent bounded information

- up to 3 Panic Imprints, each reduced to at most 48 evenly selected visual movement frames;
- up to 16 allow-listed Audio Residues;
- up to 16 habit summaries;
- a room graph of at most 20 nodes and 48 edges;
- one active primary Memory Thread and up to 8 completed thread types;
- contamination in `[0, 1]` and one fixed-size decaying observation profile;
- up to 24 significant observed-event records;
- up to 4 compact False Memory seeds;
- the last strong-event time and beta schema version.

Eviction is deterministic. Recent/frequently reinforced rooms and habits are preferred; otherwise the oldest,
lowest-confidence entry is removed. Invalid identifiers, dimensions, positions, edges, enum names, and individual
player records are rejected locally. Lists and maps are clamped both when mutated and when decoded.

## Room inference

Room awareness is incremental and player-driven. Explicit bed, chest, crafting, furnace, door, and portal
interactions produce immediate observations. A rate-limited transition tracker samples the player's already-loaded
local area and current familiar-location neighborhood. A probe checks at most 256 blocks inside an 8-block radius,
using nearby walls/ceiling, sky visibility, depth, and known interaction blocks to assign a heuristic type and
confidence. It never scans chunks, flood-fills a structure, opens containers, force-loads chunks, or infers
ownership.

Nodes merge only within the same dimension and local merge radius when their activity types are compatible.
Edges are reinforced when the same player transitions between two known nodes; doorway interactions may annotate
the transition position. Low-confidence nodes cannot drive strong events until reinforced. Changed blocks are
handled as missing heuristic evidence and event-specific safety validation rejects an unusable loaded context;
the graph never assumes block ownership or mutates the site.

## Memory Threads and Event Director 3.0

A `MemoryThread` is a cached plan of 2–4 event steps sharing a room, prior panic location, sound, habit, or item.
Only one primary thread is active by default. Thread planning consumes compact persisted context and feature
toggles. Disabled or invalid steps are skipped or replaced with a compatible subtle step; a thread that has no
valid continuation is cancelled without holding the active-event lock.

`EchoEventDirector` remains the scheduler. Its order is: hard safety gates, active lock, grace/cooldowns, eligible
thread step, context validation, thread attempt, optional unrelated subtle fallback, then normal deterministic
weighted selection. A spawn is recorded only after `spawnEntity` or sound delivery succeeds. Observation callbacks
distinguish spawn, meaningful observation, completion, and failure; only the outcome required by a step advances
the thread.

False Memory contradiction plans reuse the existing recording, `EchoEntity`, safe-position validation, and
behavior controllers. Split pairs share a bounded group cleanup handle. The Original continues to use
`OriginalMovementController` and its local route planner; thread context selects an anchor and action metadata but
never bypasses collision, loaded-chunk, or fallback checks.

## Contamination and observation profile

Contamination changes only from discrete gameplay outcomes or hourly recovery. Admin tests do not affect it by
default. It adjusts bounded candidate weights and deviation planning while preserving a non-zero authentic Memory
candidate at every tier.

The observation profile stores only fixed decaying counters and aggregate preferred distance. It requires minimum
samples and confidence before deriving `FOLLOWER`, `AVOIDANT`, `OBSERVER`, `INVESTIGATOR`, or `DISTANT`; otherwise
it remains `UNCLASSIFIED`. It changes weights and presentation limits only—never input, camera, movement, damage,
or player state.

## Multiplayer privacy

All graphs, threads, contamination, profiles, and persistent summaries remain server-only. Existing server entity
tracking remains the primary privacy boundary for private Echo entities. Sounds, particles, and text use target-only
server packet APIs when `shared_echoes=false`. With sharing enabled, only documented nearby entity visuals/audio
may be public; no beta metadata is serialized to clients. Every manager is keyed by target UUID, and cleanup never
iterates into another player's state.
