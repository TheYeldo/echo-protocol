# Echo Protocol 0.4.1-alpha Verification

Legend: `automatic` means a meaningful assertion in `src/test`; `dedicated` means observed in a real Linux dedicated-server process; `client` and `two-client` require real rendered clients. A build or successful command alone is never treated as gameplay verification.

## Subsystem matrix

| Subsystem | Intended observable pipeline | Cleanup/privacy path | Current verification |
|---|---|---|---|
| Recording | Sample ordered pose, rotation, movement flags, and held-item snapshots into a bounded history | Disconnect and dimension transition remove history; frames never enter custom packets | automatic: empty, one-frame, ordering, maximum bound |
| Memory Echo | Select at least two historical frames, translate the complete route to a safe spawn, replay, observe, fade, discard | target tracking unless shared; active lock clears on removal | automatic: segment bounds and route translation; client visual pending |
| Corrupted Echo | Replay, enter desync, visibly stutter/watch/approach or hide, then fade | bounded state ages and entity removal | source-path audit; client runtime pending |
| Mimic Echo | Copy current motion through a bounded delay queue, deviate, optionally threaten, disappear | disconnect/dimension cleanup; Peaceful blocks hostile state | source-path audit; client runtime pending |
| False Memory | Authentic configured prefix followed by at least one observable bounded fabricated deviation | no inventory/world writes; observation grants only after deviation; discard | automatic: prefix bound and observable-deviation classifier; client runtime pending |
| Panic Imprints | Capture the newest real pre-trigger segment and replay it as harmless frames | bounded oldest-first eviction; clear/disconnect removal | automatic: recent slice and eviction; trigger-hook runtime pending |
| Peripheral Echo | Choose loaded safe point outside central view, require stable observation, reposition once at most, expire | session/cooldown bounds; targeted entity/effects | automatic: view geometry; client runtime pending |
| Audio Residue | Capture allow-listed vanilla event identifier, select same-dimension loaded entry, send positioned sound | 24-entry history, session/cooldown limits, target-only packet when private | source-path audit; client audio pending |
| Borrowed Habits | Real block/item hooks update bounded summaries and influence Original anchor/event/item | no container inspection; session cleanup | source-path audit; gameplay influence pending |
| Familiar locations | Persist bounded dimension-qualified markers and ignore removed typed blocks | persisted world data; invalid entries skipped | source-path audit; restart gameplay pending |
| The Original | Execute one of nine distinct bounded action plans with real acceleration, waypoints, observation and fallback | `shouldSave=false`, maximum age, removal clears lock | automatic stuck logic; all nine visual plans pending |
| Event Director 2.0 | Apply grace/danger/active locks and weighted history-aware selection | failed spawn leaves lock/history untouched; logout cleanup | automatic history/repeat/strong-silence; world suppression runtime pending |
| Stage/config persistence | Persist progression and remaining cooldown delay across process tick reset | malformed config retained; defaults only in memory | automatic config and deadline migration |
| Advancements | Grant impossible criteria only from a reachable server gameplay source | one criterion; forced event paths suppress normal grants | automatic JSON/parent/EN/RU contract; gameplay triggers pending |
| Commands | Permission level 2, validated arguments, result reflects actual start/send/clear | list commands omit frame/private content | source audit; live command matrix pending |
| Skins/models | Resolve bounded Minecraft skin cache and choose classic/slim player model | real S2C cache-clear request; 64-entry client cache | compile/resource audit; client visual pending |
| Particles/sounds/accessibility | Targeted effects, positioned sound, master volume and reduced visual/flashing settings | no empty fake sound registrations; bounded cooldown maps | source audit; sensory client verification pending |
| Dedicated/integrated server | Load common classes without client linkage and run world ticks | lifecycle saves progression and clears session managers | dedicated: final tree reached `Done (1.298s)` and stopped via `stop`; integrated pending |
| Multiplayer privacy | Server tracker excludes unrelated clients when private; targeted particles/sounds use player packets | observer/target dimension and disconnect cleanup | source audit; two-client packet observation pending |
| Resource/localization | Load mod metadata, mixin, advancement JSON, EN/RU strings and vanilla sound identifiers | no generated/runtime files in artifact | automatic contract; dedicated and client resource reload succeeded |

## Advancement trigger map

| Advancement | Server trigger source | Required gameplay condition |
|---|---|---|
| root | `StageManager.grant` | Any genuine child condition is granted |
| deja_vu | event observation callback | First non-forced Echo actually observed |
| that_was_me | `MemoryEchoBehavior` | Historical replay active for 10 ticks and looked at |
| it_saw_me | `CorruptedEchoBehavior` | Watching Corrupted Echo looks back while observed |
| corrupted_memory | `StageManager.tick` | Stage 2 time/event threshold reached |
| out_of_sync | `CorruptedEchoBehavior` | Visible desync movement executes |
| broken_memory | `CorruptedEchoBehavior` | Visible desync movement executes |
| it_looked_back | Corrupted/Mimic behavior | Independent look-at-player action executes |
| perfect_copy | `MimicEchoBehavior` | 80 delayed copies execute and player is looking |
| not_me | `MimicEchoBehavior` | Independent mistake executes |
| do_not_look_away | `MimicEchoBehavior` | Unobserved safe approach step executes |
| copy_is_wrong | `MimicEchoBehavior` | Non-forced hostile state reaches its bounded end |
| familiar_face | event observation callback | Observed Echo has resolvable signed target texture |
| behind_you | Corrupted/Mimic behavior | Safe unobserved approach step executes |
| the_original | `StageManager.tick` | All Stage 3 requirements are met |
| already_home | `OriginalEchoBehavior` | Observed Original uses a bed/home-compatible anchor |
| my_place | `OriginalEchoBehavior` | Observed Original uses a valid familiar/habit anchor |
| which_one_is_real | `OriginalEchoBehavior` | Non-forced confrontation reaches disappearance |
| stop_following_me | `OriginalEchoBehavior` | Non-forced confrontation action executes |
| that_never_happened | `FalseMemoryBehavior` | Player observes fabricated deviation for 8 ticks |
| i_remember_it_differently | `FalseMemoryHistory` via behavior | Observed plan conflicts with a recent route signature |
| you_were_never_there | `FalseMemoryBehavior` | Observed plan enters bounded fabricated route location |
| almost_lost_everything | `FalseMemoryBehavior` | Observed deviation is based on a Panic Imprint |
| out_of_the_corner_of_my_eye | `PeripheralEchoBehavior` | Stable observation grace is satisfied |
| not_my_footsteps | `AudioResidueManager` | Non-forced captured sound packet is actually sent |

## Runtime checklist

- Dedicated server reaches `Done` and is stopped with `stop`: verified; Fabric Loader 0.16.14 loaded Echo Protocol 0.4.1-alpha and Fabric API 0.116.13+1.21.1, then shut down cleanly.
- Linux client reaches title screen without crash: launch verified through renderer/resource/OpenAL initialization; the OS session was locked, so the title screen itself was not visually inspected.
- Integrated single-player world: pending.
- Dedicated-server connection: pending.
- All debug commands and all nine Original kinds: pending.
- 20 TPS and reduced tick rate: pending.
- Peaceful and non-Peaceful; Creative, Survival, Hardcore copy: pending.
- Dimension, logout, death/respawn cleanup: pending.
- Two simultaneous clients with private/public tracking comparison: not verified. A quick-play connection attempt produced zero joined players, so it is not counted as a multiplayer result.

Disposable worlds only. No runtime check may be promoted from “pending” based on build output alone.
