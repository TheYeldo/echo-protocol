# Persistent memory format

Echo Protocol uses one Overworld `PersistentState` named `echo_protocol_memory` under Minecraft's supported world-data mechanism. It does not write gameplay data to arbitrary sidecar files. The old `data/echo_protocol_state.json` is used only as an untouched migration source.

When `persistent_memory_enabled` is `false`, beta memory is kept in an unbound session-only state while the existing Stage snapshot continues to use world persistence. Previously saved beta records are retained rather than erased, and become available again if persistence is re-enabled.

The root and every player record carry data version 1. Each UUID record contains the existing Stage snapshot and one bounded `PlayerMemoryState`. Child lists are decoded independently: a malformed residue, room, edge, event, seed, or thread is skipped and logged without invalidating valid siblings. A malformed player is skipped without discarding other players. A future root version is logged, retained read-only, and written back byte-for-byte at the custom-data level; known Stage fields are recovered for the running session where possible.

## Hard bounds

| Data | Maximum per player |
|---|---:|
| Panic Imprints | 3 × 48 sampled visual frames |
| Audio Residue summaries | 16 |
| Habit summaries | 16 |
| Room nodes / edges | 20 / 48 |
| Associated positions per room | 8 |
| Active primary threads | 1 × 4 steps |
| Completed thread types | 8 |
| Significant event records | 24 |
| False Memory seeds | 4 |
| Observation counters | one fixed enum-sized array |

The full ten-minute recording, chat, raw sound/audio, microphone or voice-chat data, complete inventories, container contents, entity instances, navigation searches, and world/chunk scans are never persisted.

Rooms and habits retain frequently reinforced entries; low-confidence/old graph entries and old event-like entries are evicted first. Dirty state is marked for meaningful mutations, not recording samples or every server tick. Active threads serialize their compact plan cursor, load paused, and resume through the thread manager after safety/context checks. Persistent decay and event timestamps use saved world age so a process restart does not reset their reference.

Migration recognizes representative 0.2, 0.3, 0.4.0-alpha, and 0.4.1-alpha JSON shapes. Valid Stage counts, remaining cooldown delays, mimic progression, and familiar locations are imported. The source file is not deleted or rewritten; an unrecoverable file leaves the import marker unset so an operator can repair and retry it.

At hard bounds, compact NBT is estimated at roughly 25–70 KiB per player, dominated by up to 144 sampled Panic frames and dimension/item identifier strings. Exact compressed disk size depends on identifiers and NBT compression.
