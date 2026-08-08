# Final QA: Minecraft 1.21.1 through 1.21.11

Release under test: Echo Protocol `0.5.0-beta.2`; immutable source tag `v0.5.0-beta.2` at `c4121bc21abc891af83f5fdcb2c17e29cec87d5e`.

Status words mean `VERIFIED`, `PARTIALLY VERIFIED`, `NOT TESTED`, or `FAILED`. Final client evidence came from Prism Launcher, never Loom `runClient`.

## Compatibility result

| Minecraft | Branch / verified code commit | Prism instance | Yarn | Loader / API | Loom / Gradle / Java | Artifact SHA-256 | Tests / build | Server / Prism | Skin / model | Alpha / fade / collision | Gameplay / advancement | Restart | Privacy | Perf/log | Upload |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1.21.1 | fix/1.21.1-beta.2-runtime `cd467b64` | EchoProtocol-Test-1.21.1 | 1.21.1+build.3 | 0.16.14 / 0.116.13 | 1.17.14 / 9.6.1 / 21 | `0910c7ddf8ea1036f26f5959b9c4a85f824f6ef0dc7333310513001d6882c80b` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | VERIFIED | PARTIALLY VERIFIED | VERIFIED | VERIFIED |
| 1.21.2 | port/1.21.2-1.21.3 `35b4b29f` | EchoProtocol-Test-1.21.2 | 1.21.2+build.1 | 0.16.14 / 0.106.1 | 1.17.14 / 9.6.1 / 21 | `9c531004a702449dc682dbecc6916d3bdef46dad680579edbef7fe7e23cc4022` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | PARTIALLY VERIFIED | PARTIALLY VERIFIED | VERIFIED | VERIFIED |
| 1.21.3 | port/1.21.2-1.21.3 `35b4b29f` | EchoProtocol-Test-1.21.3 | 1.21.2+build.1 | 0.16.14 / 0.114.1 | 1.17.14 / 9.6.1 / 21 | `9c531004a702449dc682dbecc6916d3bdef46dad680579edbef7fe7e23cc4022` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED |
| 1.21.4 | port/1.21.4 `8279225f` | EchoProtocol-Test-1.21.4 | 1.21.4+build.8 | 0.16.14 / 0.119.4 | 1.17.14 / 9.6.1 / 21 | `b653e5bc7c62197953402dbb836ece1a3c36808922362cd7fdd29de5cb025373` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | PARTIALLY VERIFIED | PARTIALLY VERIFIED | VERIFIED | VERIFIED |
| 1.21.5 | port/1.21.5 `2c81be1d` | EchoProtocol-Test-1.21.5 | 1.21.5+build.1 | 0.16.14 / 0.128.2 | 1.17.14 / 9.6.1 / 21 | `4220e7abcd6cdfb57ccb8c8cb70d9c41238ae14f797b01d4eb97f06f05b25760` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | VERIFIED | PARTIALLY VERIFIED | VERIFIED | VERIFIED |
| 1.21.6 | port/1.21.6-1.21.8 `79ed3342` | EchoProtocol-Test-1.21.6 | 1.21.6+build.1 | 0.19.3 / 0.128.2 | 1.17.17 / 9.6.1 / 21 | `71dd1b583f3a80552d726ccfab72431f80045a3e945cdfaac9a08780c339ed54` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | PARTIALLY VERIFIED | PARTIALLY VERIFIED | VERIFIED | VERIFIED |
| 1.21.7 | port/1.21.6-1.21.8 `79ed3342` | EchoProtocol-Test-1.21.7 | 1.21.6+build.1 | 0.19.3 / 0.129.0 | 1.17.17 / 9.6.1 / 21 | `71dd1b583f3a80552d726ccfab72431f80045a3e945cdfaac9a08780c339ed54` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | PARTIALLY VERIFIED | PARTIALLY VERIFIED | VERIFIED | VERIFIED |
| 1.21.8 | port/1.21.6-1.21.8 `79ed3342` | EchoProtocol-Test-1.21.8 | 1.21.6+build.1 | 0.19.3 / 0.136.1 | 1.17.17 / 9.6.1 / 21 | `71dd1b583f3a80552d726ccfab72431f80045a3e945cdfaac9a08780c339ed54` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED |
| 1.21.9 | port/1.21.9-1.21.10 `163168eb` | EchoProtocol-Test-1.21.9 | 1.21.9+build.1 | 0.19.3 / 0.134.1 | 1.17.18 / 9.6.1 / 21 | `448a3bfeae49c30f818b16b9d64b9f40c1416143ed69868aba838ee55ece2226` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | PARTIALLY VERIFIED | PARTIALLY VERIFIED | VERIFIED | VERIFIED |
| 1.21.10 | port/1.21.9-1.21.10 `163168eb` | EchoProtocol-Test-1.21.10 | 1.21.9+build.1 | 0.19.3 / 0.138.4 | 1.17.18 / 9.6.1 / 21 | `448a3bfeae49c30f818b16b9d64b9f40c1416143ed69868aba838ee55ece2226` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | PARTIALLY VERIFIED | PARTIALLY VERIFIED | VERIFIED | VERIFIED |
| 1.21.11 | port/1.21.11 `7c9ff708` | EchoProtocol-Test-1.21.11 | 1.21.11+build.6 | 0.19.3 / 0.141.6 | 1.17.18 / 9.6.1 / 21 | `d38e1bcac0bfad146aaf10d751f53a8bfb4b3b8900b57a45f3ce6bd822e437ad` | VERIFIED 52 / VERIFIED | VERIFIED / VERIFIED | VERIFIED / PARTIALLY VERIFIED | VERIFIED / VERIFIED / VERIFIED | VERIFIED / VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED |

`PARTIALLY VERIFIED` model status means the configured account's actual model was verified and the model is selected from `SkinTextures.model`, but a second authenticated account covering the other wide/slim variant was not available. Non-representative privacy rows have the critical server mixin and recipient-policy tests but were not repeated with two clients. Non-representative restart rows retain automated schema/loading coverage but did not receive another full runtime restart in this pass.

The shared JAR claims are valid: one byte-identical file was used in Prism and server tests for 1.21.2/1.21.3, 1.21.6/1.21.7/1.21.8, and 1.21.9/1.21.10.

## Release-blocking reports

| Report | Result | Affected versions | Root cause | Fix and verification |
|---|---|---|---|---|
| Echo remains Steve/default | REPRODUCED and FIXED | Visually reproduced on 1.21.9; the same modern renderer path affected 1.21.10/1.21.11. A fallback-cache risk was also removed from all branches. | Modern player render commands consume `PlayerEntityRenderState.skinTextures`; only the renderer-private texture was populated. An insecure/fallback response could also be cached too long. | Copy resolved `SkinTextures` into render state, select model from its model type, and cache secure results only. Final 1.21.9–1.21.11 JARs were visually verified with the real custom player skin. |
| Echo becomes opaque | REPRODUCED and FIXED | 1.21.9–1.21.11 renderer architecture | The modern LivingEntity render-command path bypassed EchoRenderer's old typed alpha/layer override. | A required client mixin routes Echo render state through translucent layer, mix color alpha, visibility, and visual offsets. No `require=0` was used. Pixel-background comparison and Prism inspection verified translucency/fades. |
| Echo crosses solid walls | REPRODUCED by implementation audit and focused regression; FIXED | Original 1.21.1 behavior and every inheriting port | Replay used raw positional updates and validated endpoints without a swept segment collision test. Historical routes could therefore cross newly placed blocks; this was server movement, not only client interpolation. | Collision-aware movement plus `ReplayPathSafety` checks the swept segment. A blocked historical route now stops/truncates, enters safe fade/cleanup, and never teleports through the wall. Two focused tests were added; each Prism version received a wall test. |
| Advancement with no visible manifestation | Root cause CONFIRMED; user-visible report not independently reproduced after fix | Original 1.21.1 logic and all ports | Stage 3 transition granted `the_original` before The Original actually manifested. | Stage transition no longer grants it. The Original grants only after successful observation/manifestation. Stage-set smoke testing confirmed no premature grant; actual Original events still grant at the semantic point. |

No P0 crash, disconnect, privacy leak, or persistence corruption remained. No new P1 defect remained after final-JAR retesting.

## Runtime evidence

- Every version reached the main menu, loaded a world, joined its dedicated server, manifested a real Echo, and closed through its dedicated Prism instance.
- The authenticated non-default player skin was confirmed before Echo skin evaluation. The actual player was not Steve while investigating Echo fallback behavior.
- Dedicated servers 1.21.1–1.21.11 reached `Done`, loaded `ServerEntityTrackingMixin`, registered commands/resources, and stopped cleanly.
- Real two-client Prism tests passed on 1.21.3, 1.21.8, and 1.21.11. With `shared_echoes=false`, Player B did not receive/see the target-private entity; with it enabled, B received the documented visual. Disconnecting B did not cancel A; disconnecting A cleaned its Echo. Private room, thread, contamination, profile, familiar-location, panic, and audio state have no observer payload path and remain server-owned.
- Full restart comparisons passed on 1.21.1, 1.21.3, 1.21.5, 1.21.8, and 1.21.11. Nodes, edges, familiar locations, active threads, contamination, preset, panic imprints, habits, and validation survived; no duplicate Echo remained. Audio Residue persistence was present on the 1.21.11 full case; the 1.21.1/1.21.3 focused cases had no capturable allowed sound and did not fabricate one.
- 1.21.11 exercised recording, Memory, Corrupted, Mimic, False Memory, contradiction, Panic, Peripheral, Audio Residue start, Room Graph, Memory Thread, contamination, Observation Profile, The Original movement/context, presets, diagnostics, dimension cleanup, disconnect cleanup, and restart.
- Short 1.21.8/1.21.11 observations found no repeated exception, packet/sound/particle spam, per-tick route planning, stuck lock, retained discarded Echo, or severe TPS warning. The only repeated client-environment warning was Xvfb's unavailable standard cursor shape; it is unrelated to the mod.

## Advancement inventory and semantics

All advancement JSON files use the custom `trigger` criterion, valid parents, and English/Russian translations. Runtime granting is performed only at the code-side event point summarized below.

| ID | Player-visible meaning / completion point |
|---|---|
| root | Root granted with the first actual Echo Protocol advancement. |
| deja_vu | First successfully started Echo event. |
| familiar_face | A manifested Echo uses the remembered target skin. |
| that_was_me | Memory Echo is actually observed replaying a prior route. |
| corrupted_memory | Stage 2 progression. |
| broken_memory | Corrupted Echo visibly leaves the authentic route. |
| out_of_sync | Corrupted Echo enters its visible timing disruption. |
| it_saw_me | Corrupted Echo stops and looks at the target. |
| it_looked_back | Corrupted/Mimic Echo deliberately looks at the target. |
| behind_you | An Echo successfully approaches from outside view. |
| perfect_copy | Mimic successfully repeats current movement. |
| not_me | Mimic performs independent behavior. |
| do_not_look_away | Mimic successfully approaches while unobserved. |
| copy_is_wrong | Hostile Mimic event reaches its survived completion. |
| that_never_happened | False Memory successfully manifests. |
| i_remember_it_differently | Conflicting memory variants manifest. |
| almost_lost_everything | Panic Imprint is actually replayed. |
| you_were_never_there | False Memory reaches its deviated section. |
| out_of_the_corner_of_my_eye | Peripheral Echo is noticed before cleanup. |
| not_my_footsteps | Captured allowed Audio Residue is actually played. |
| a_pattern_emerges | Three valid connected Memory Thread steps are observed. |
| not_forgotten | A remembered event manifests after restart. |
| the_house_remembers | A Memory Thread completes. |
| this_is_not_how_it_happened | Distorted memory/thread manifestation occurs. |
| two_different_endings | Both contradiction outcomes manifest. |
| it_was_waiting_there | A valid event manifests first in a familiar room. |
| you_led_it_here | False route later resolves to the Original room event. |
| the_original | The Original is successfully observed, never merely stage-selected. |
| my_place | The Original manifests at a frequently used location. |
| already_home | The Original manifests inside a familiar base. |
| which_one_is_real | Rare Original confrontation completes. |
| stop_following_me | Enabled Original text event is actually delivered. |

## Known limits

- Both classic and slim code paths are preserved, but only the configured account's real model variant was visually exercised.
- Audible sound output was confirmed by successful playback/state on representative versions, but automated screenshots cannot prove subjective volume.
- Screenshot evidence remains in the external QA workspace and is intentionally not packaged in any JAR.
