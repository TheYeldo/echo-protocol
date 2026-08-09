# Beta.3 external tester feedback

Source: an external tester's approximately three-hour Minecraft 1.21.11 survival session with Echo Protocol
0.5.0-beta.2. These are design candidates, not part of the beta.2 hotfix.

The tester later clarified that the Echo was translucent, but the effect was hard to notice at night and in caves.
The beta.2 hotfix may restore restrained cold readability without changing mechanics; broader visual direction and
art tuning should be evaluated for beta.3.

## Gaze reaction

- After several seconds of continuous observation, an Echo could notice the player and slowly turn its head/body.
- A rarer escalation could approach or sprint toward the player and disappear nearby.
- Observation should remain unsettling and readable without turning every Echo into a chase.
- Add deliberate idle scanning and richer reactive transitions only after dedicated design/playtesting; the beta.2
  correction is limited to preventing accidental motionless/downward-looking manifestations.

## Rare contextual dialogue

- Explore very short, ambiguous lines selected from context such as darkness, caves, home, recent actions,
  observation, or contradictory memories.
- The tester's examples are concept references only, not final copy.
- Avoid random chat spam and do not turn Echoes into conversational chatbots.

## Event pacing

- Investigate a more consistent rhythm while preserving quiet stretches.
- Consider adaptive pacing based on events the player actually perceived, rather than events merely selected by the
  director.
- Balance changes require beta.3 playtesting and must not be folded into a beta.2 compatibility hotfix.

## Diagnostics and test UX

- Replace raw counts such as `16 locations` with named concepts, short explanations, and actionable status.
- Separate player-facing diagnostics from administrator/developer detail.
- Provide clearer tools for testing natural scheduling without making command-forced events the primary QA path.
