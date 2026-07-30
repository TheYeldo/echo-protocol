# Echo Protocol 0.5.0-beta.1 verification

Legend: `automated` is a meaningful assertion in `src/test`; `dedicated-server runtime`, `real client`, and `two-client` require those real processes. Source review, mocks, a build, and a one-client launch are never promoted to a stronger label.

## Subsystem matrix

| Subsystem | Verification | Evidence / remaining work |
|---|---|---|
| Legacy migration | automated | Representative 0.2/0.3/0.4.1 state, familiar locations, malformed-player isolation |
| Beta NBT persistence | automated | Round-trip of graph/thread/profile/contamination, malformed child isolation, future version read-only preservation |
| Room graph | automated | Merge, dimension separation, edge reinforcement, node/edge eviction; live interaction generation still needs client runtime |
| Memory Threads | automated | Seeded 2–4-step plans, feature adaptation/cancellation, failed event non-advance, observed advance, completion-history bound, paused codec restart |
| Contradictions | automated | Every seeded variant is bounded/deterministic; Split group clears two members exactly once; visuals still need real client |
| Contamination | automated | Growth, anti-farming, decay interval, clamping, authentic non-zero weight, authentic recovery |
| Observation Profile | automated | Minimum samples, style confidence, decay, bounded counters; gameplay presentation still needs client runtime |
| Presets/config | automated | Defaults, clamps, old values, malformed-file preservation, unknown fields, distinct runtime multipliers |
| Advancements/localization | automated | JSON parent/criterion contract and non-empty English/Russian translations |
| Privacy recipients | automated; source audit | Target-only vs shared visual and always-private metadata rules; server tracking mixin and targeted sound/particle/text paths inspected |
| Event Director 3.0 | automated for pure selectors; source audit | Thread-first success/failure semantics and active lock audited; world safety gates need runtime matrix |
| The Original context | source audit | Uses loaded room/Panic/audio anchors and existing movement controller; client action-plan verification pending |
| Disconnect/death/dimension cleanup | source audit | UUID-scoped managers, thread pause, recording/entity/in-flight cleanup; runtime scenarios pending |
| Dedicated server | dedicated-server runtime | Final-code beta reached `Done (1.459s)` and stopped cleanly with `stop`; a later Mojang public-key request timed out without stopping the server |
| Linux client | real client (partial) | Java 21 client initialized the renderer, OpenAL, atlases, and Echo Protocol resources without a crash; integrated-world gameplay was not run |
| Two-client privacy | not fully verified | Requires two real connected clients under both sharing settings |
| Persistence restart sequence | not fully verified | Automated codecs are complete; real 0.4.1 → beta disposable-world sequence remains separate |

## Beta advancement trigger map

| Advancement | Normal gameplay trigger |
|---|---|
| The House Remembers | First completed Memory Thread |
| Two Different Endings | Stable direct observation of a non-admin Split Memory |
| It Was Waiting There | Stable observation of Memory Arrived First |
| A Pattern Emerges | Third connected observed step of one normal thread |
| Not Forgotten | First accepted observation in a thread loaded from a previous server session |
| This Is Not How It Happened | Crossing into `DISTORTED` through normal thread outcomes |
| You Led It Here | The Original uses a thread room with a prior followed False Memory event |

Admin-created threads, admin advances, and forced contradictions carry `awardsProgress=false` and do not grant these by default.

## Runtime checklist

- `./gradlew clean test`: passed, 49 tests, 0 failures/errors/skips.
- `./gradlew clean build`: passed; remapped release and sources JARs generated.
- Dedicated server `Done` / clean `stop`: passed on the current beta runtime.
- Linux client common/resource initialization: passed on the current beta runtime; the process was closed after resource loading.
- Integrated world and dedicated connection: not verified. A quick-play connection attempt produced no joined player on the server and is not counted as a pass.
- Real 0.4.1 migration plus beta restart: pending.
- Normal and reduced TPS, Peaceful/Survival/Creative/Hardcore copy, Overworld/Nether, logout/death/respawn: pending.
- Two real clients, `shared_echoes=false` and `true`: not verified.
- Idle dedicated-server sampling (no connected players): at target 20 TPS, mean 4.1 ms/tick, P50 1.6 ms, P95 25.7 ms, P99 66.4 ms over 100 ticks; at target 10 TPS, mean 1.0 ms/tick, P50 1.0 ms, P95 1.4 ms, P99 1.8 ms over 100 ticks. This is a startup/idle observation, not a measurement of beta event load.
- Classic/slim skins and subjective peripheral/contradiction visuals: manual visual tests required.

Disposable worlds only. Unperformed checks remain pending even when compilation and pure tests pass.
