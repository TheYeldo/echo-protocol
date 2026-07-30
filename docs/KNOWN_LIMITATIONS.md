# Known limitations

- Room detection is heuristic and does not perfectly understand architectural meaning, room ownership, or complex open plans.
- Complex multi-floor paths, ladders, closed doors, and long or unloaded routes may be truncated, replanned, replaced with a safe fallback, or cancelled.
- Changed interaction blocks do not crash graph logic, but a stale heuristic node may remain until later observations make it irrelevant; event-specific validation still rejects unsafe contexts.
- Some sound design uses vanilla events.
- Memory Threads are generated bounded plans rather than a fixed narrative campaign.
- The mod never inspects container contents, and full movement recordings are not persisted.
- The Observation Profile is deliberately coarse; it may remain `UNCLASSIFIED` for quiet or ambiguous play.
- Subjective timing, peripheral comfort, classic/slim skin appearance, and all contradiction visuals still require real-client review.
- A two-client source/logic audit is not equivalent to a real two-client network observation.
