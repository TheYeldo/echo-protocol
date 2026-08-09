# Echo Protocol 0.5.0-beta.2 — Minecraft 1.21.11 hotfix2 investigation

## Scope

This is a corrective candidate for the existing 0.5.0-beta.2 gameplay release. It does not add beta.3 mechanics or
change the published release/tag.

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
