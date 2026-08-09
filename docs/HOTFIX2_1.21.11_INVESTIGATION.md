# Echo Protocol 0.5.0-beta.2 — Minecraft 1.21.11 hotfix2 investigation

## Scope

This is a corrective candidate for the existing 0.5.0-beta.2 gameplay release. It does not add beta.3 mechanics or
change the published release/tag.

## Second external-feedback pass

The tester clarified that translucency was working in the first hotfix candidate. The remaining visual problem was
readability in dark terrain, not an opaque model. The second pass therefore keeps the existing blended render path and
all configured alpha values unchanged. It applies a restrained cold tint (`RGB 196,224,255`) to the real player skin
and low emission level 5. Normal depth testing is unchanged, so the Echo is not visible through walls.

### Newly reproduced natural-runtime defects

| Report | Finding | Root cause | Correction |
|---|---|---|---|
| Natural Echo usually stands still | REPRODUCED without event-spawn commands | A 5–15 second replay window was selected uniformly from the ten-minute recording buffer. Inventory, crafting, mining pauses, and AFK time therefore dominated natural events even when a valid moving route existed elsewhere in the buffer. | Prefer a meaningful, continuous authentic window; fall back to a continuous idle window only when no moving recording exists. |
| Four or more empty days | REPRODUCED as an indefinite director lock | A False Memory window crossed a teleport/discontinuity, moved 243 blocks on its first tick into an unloaded chunk, disappeared from world tracking without entering the removed state, and remained in `activeEchoes`. | Reject replay windows containing steps over 2.5 blocks and explicitly clean active entities that are no longer tracked by their server world. Rejected and unobserved manifestations still use the bounded retry policy. |
| Mimic does not imitate from its manifestation position | CONFIRMED in code and runtime telemetry | Delayed live frames contain the player's absolute world position. Applying those positions directly from the Mimic's separate spawn point usually creates a blocked long segment. | Rebase delayed frames from the player's capture origin onto the Mimic manifestation origin. |
| Echo keeps looking down | REPRODUCED for idle recorded windows; no render conversion fault found | Natural Memory/Corrupted selection frequently chose a stationary historical interval recorded while mining or looking at grass. That authentic downward pitch was then held for the entire idle event. | Moving-route preference eliminates the accidental combination. Recorded-route pitch remains authentic; independent Peripheral manifestations now ease toward neutral pitch. |
| Peripheral is gone before it can be found | CONFIRMED from its five-second unobserved limit | The same short duration applied even when the target had never observed the manifestation. | Keep the configured five-second observed behavior, but provide an extra three-second unobserved discovery window. |
| The Original remains stationary on slab/cave terrain | REPRODUCED on the QA slab platform | Spawn grounding retained the correct fractional surface (`Y=99.5`), but `OriginalRoutePlanner` converted its goal and waypoints back to integer block-bottom positions. A valid slab route was therefore reported as `NO_ROUTE`, and the behavior entered its look/fade fallback. | Resolve every target and waypoint to the actual collision-shape top surface; retain the fractional Y through direct and graph-planned routes. |

`ReplayPathSafety` did not reject the reproduced stationary Memory Echo: telemetry recorded zero rejected segments. Its
source window simply had zero path distance. Natural and command event creation use the same entity configuration path;
the defect was replay-source selection, not missing natural-only renderer or behavior initialization.

### Instrumented natural scheduler evidence

Testing used the real Event Director selection and manifestation path. No Echo spawn command was used. The disposable
QA server used a 30–45 second interval to gather events; production Standard pacing remains 480–1080 seconds.

- Before the correction, a stationary Memory event used 52 frames, planned and travelled `0.00` blocks, and spent 123
  ticks idle.
- Before the correction, a False Memory selected a 581.20-block discontinuous route, travelled 243.11 blocks in one
  tick, left world tracking, and kept the event lock active indefinitely.
- After the correction, natural Memory manifestations replayed 3.00–4.32 blocks with zero rejected segments.
- A natural Corrupted Echo traversed its replay, desynchronizing, watching, approaching, hiding, and fading states;
  one measured manifestation travelled 8.55 blocks.
- A natural Mimic entered copying, drained 61 delayed frames, then entered independent approach/disappearance states;
  it travelled 21.33 blocks without a rejected segment.
- A natural False Memory began a continuous route and travelled 1.92 blocks before the controlled server shutdown.
- Rejected Peripheral and Panic Imprint selections retried after 15 seconds rather than consuming a successful-event
  interval. No repeating exception or `Can't keep up` entry occurred during the run.

Corrupted Memory remains granted only after a target-facing observation in the Corrupted desynchronizing or watching
state. Selection, spawn attempt, and an unobserved complete lifecycle do not grant it. The earlier audit of the other
advancements remains applicable; this pass did not find a new erroneous trigger.

### Timing and cue decisions

- Memory replay remains 5–15 seconds plus its fade and is long enough to finish the selected meaningful route.
- The measured natural Corrupted and Mimic manifestations each lasted about 19 seconds.
- Peripheral receives an eight-second unobserved window instead of five seconds; its observed behavior is unchanged.
- No further sound increase was made in this pass. The first hotfix already raised the effective default private Memory
  cue from `0.4125` to `0.495`, clamps the cue source to 12 blocks, and added the missing Peripheral cue. External testing
  should reassess that correction before another volume change.
- Default production pacing and gameplay weights are unchanged. Major pacing, gaze/chase behavior, and dialogue remain
  beta.3 work.

## Reproduced code-path defects

| Report | Finding | Root cause | Candidate correction |
|---|---|---|---|
| Natural Echo is opaque | REPRODUCED visually in Prism | The 1.21.11 renderer supplied the right tracked alpha and blended layer, but left the extracted player render state on Minecraft's ordinary visible-player path. The deferred model command therefore rendered the skin as an ordinary player. | Route Echo render states through Minecraft's translucent-entity path and neutralize its hard-coded 15% invisible-entity multiplier for EchoRenderer only, preserving the configured fade-aware alpha. |
| Long stretches with no visible event | REPRODUCED in the director lifecycle | A rejected spawn consumed the full normal 8–18 minute interval; a spawned but unobserved event also retained that full interval. | Rejected or unobserved manifestations use a bounded 15–60 second retry; observed events keep normal pacing. |
| Corrupted Memory without manifestation | REPRODUCED | `corrupted_memory` was granted by the playtime/stage transition, not by a Corrupted Echo observation. | Grant only after a real Corrupted Echo is observed leaving its replay behavior; description now matches that semantic. |
| Very quiet/missing appearance cue | REPRODUCED by distance analysis and natural Peripheral path | Sounds were emitted up to 32 blocks away despite normal attenuation becoming inaudible near 16 blocks; Peripheral Echo skipped the appearance cue entirely. | Preserve direction but clamp the sound source to 12 blocks; raise Memory/Peripheral cue gain by 20%; add the missing Peripheral cue. |
| Echo looking at the ground | REPRODUCED in natural Peripheral/Original initialization | Independent stationary events copied the player's latest pitch/head state, commonly a mining/grass downward look. | Initialize independent Peripheral and Original manifestations with neutral pitch/head alignment. Recorded route Echoes still replay the player's real historical pitch. |
| Cave Echo floating on partial terrain | ROOT CAUSE CONFIRMED; the tester's exact cave scene was not reproduced | Ground placement used integer block Y and only checked for a non-empty floor shape. With gravity disabled, slab/stair/cave-shape offsets never settled. | Place feet at the collision shape's actual top surface at the candidate X/Z and validate that exact support. The final candidate placed both a natural Corrupted Echo and The Original at `Y=99.5` on bottom slabs, with feet visually aligned. |
| Witness advancement without witnessing | REPRODUCED in the natural session (`Behind You` while the client was paused) | Corrupted/Mimic movement outside the view could grant without any earlier confirmed observation; Mimic even marked unseen movement as observed. Original confrontation completion had the same missing observation guard. | Require a confirmed earlier observation for outside-view and confrontation advancements. Audio Residue is moved into audible range before its hearing advancement is granted. |

## Natural Event Director QA timeline

An accelerated QA configuration retained the real scheduler/selection/spawn path and used no event-spawn commands.
The interval was shortened to 30–45 seconds after the mandatory one-minute join grace.

| Wall-clock time | Scheduler result |
|---|---|
| 16:47:55 | Memory manifested |
| 16:48:30 | Contradiction manifested |
| 16:49:04 | Memory manifested |
| 16:49:37 | Peripheral manifested |
| 16:50:09 | Memory Thread event manifested |
| 16:50:40 | Corrupted manifested |

At 16:50:54 the unmodified candidate granted `Behind You` while the Prism client was paused by a system dialog. This
provided a deterministic reproduction of the advancement-with-no-visible-event report and is covered by the final
observation guard.

The observed selected-to-manifested interval in this accelerated run averaged 33 seconds after the first event. The
production Standard preset remains 480–1080 seconds after a successfully observed event; this hotfix does not
redesign that pacing.

With the exact final candidate, a natural Memory manifestation went unobserved, then retried after 15 seconds. A
Contradiction selection was rejected and also retried after 15 seconds instead of consuming the normal interval; the
following Corrupted manifestation used that retry. This confirms both failed and unobserved lifecycle paths.

## Advancement audit

All 32 non-root advancement grant sites were reviewed against their player-visible meaning. The release-blocking
findings were:

- `corrupted_memory` was incorrectly tied to the Stage 2 timer/counter transition. It is now granted only from the
  observed desynchronizing/watching states of an actual Corrupted Echo.
- `behind_you` and `do_not_look_away` could be granted by an unseen Corrupted/Mimic movement without a prior
  observation. They now require an already observed manifestation.
- `not_me` and `it_looked_back` now require the player to be looking during the Mimic's independent action.
- `which_one_is_real` now requires observed confrontation progress before completion can grant it.
- observation-based Memory, False Memory, Peripheral, contradiction, and Original grants were already downstream of
  their observation callbacks. Audio Residue's `not_my_footsteps` remains downstream of an actual sound packet, whose
  private cue position is now kept within audible range.

## Event lifetimes

- Memory Echo: configured replay 5–15 seconds plus its one-second trailing fade.
- Peripheral Echo: five seconds by default and may disappear/reposition after sustained direct observation.
- Corrupted, Mimic, and The Original use behavior-state lifetimes and are longer than the minimum Memory replay.

The short five-to-seven-second sightings reported by the tester match Peripheral and minimum-length Memory events.
Major duration redesign is deferred to beta.3.

## Final local evidence

- Java: Temurin 21.0.12
- Automated tests: 56 passed (52 existing + 4 focused policy/observation/sound tests)
- `./gradlew clean build`: passed
- Dedicated Fabric server: reached `Done` and loaded the persisted QA survival world
- Prism instance: `EchoProtocol-Test-1.21.11`
- Exact final JAR: visually verified with the real custom player skin, configured translucency, fade, held-item replay,
  neutral Original pitch (`0.0`), and precise slab grounding (`Y=99.5`)
- Exact final SHA-256: `73ebccc4f4071095de5032f41cfffb6f1bcf9188ebf06755c05b7d75016094ad`
- Final client/server logs: no ERROR/WARN entries, mixin errors, codec errors, or repeating exceptions

### Second-pass final candidate

- Automated tests: 61 passed (56 prior tests plus five focused selector, relative-replay, and orphan-lock tests).
- Clean test and clean build: passed after the final fractional-surface route-planner correction.
- Artifact: `dist/0.5.0-beta.2-hotfix2-test-2/echo-protocol-0.5.0-beta.2-mc1.21.11-hotfix2-test2.jar`
- SHA-256: `a84ad00aa57bd15925fbd991ccbd3b5cd87876d364e0f1601cf6b2a48dc26a06`
- The exact SHA above was installed as the only Echo Protocol JAR in `EchoProtocol-Test-1.21.11`, launched through
  Prism Launcher 11.0.3, connected to the disposable Fabric server, and visually inspected at night. The target's
  non-default skin remained recognizable under the cold translucent treatment and normal depth testing.
- On the final JAR, the slab `patrol` movement test kept `Y=99.5`, moved The Original 25.83 blocks over 209 moving
  ticks, and completed with zero rejected replay segments. Before the route correction, the same reproduction stayed
  at its spawn position and entered fallback.
- Final server tick query: average 2.7 ms, P95 4.3 ms, P99 5.1 ms at the normal 20 TPS target.
- Final logs contained no errors, mixin/codec failures, `Can't keep up`, or repeating warnings. The client recorded one
  isolated vanilla `Received passengers for unknown entity` warning with no disconnect or repeat.
- The final exact-JAR run used controlled Memory and Original commands for the post-rebuild visual and slab regression
  checks because the restored QA state retained a future natural-event deadline. The preceding natural Event Director
  run covered Memory, Corrupted, Mimic, Peripheral, and False Memory on the same source changes; only the fractional
  Original route correction was added afterward. This distinction is retained rather than calling the controlled
  checks natural scheduling.
