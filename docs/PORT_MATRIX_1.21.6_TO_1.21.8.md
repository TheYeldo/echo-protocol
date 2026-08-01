# Echo Protocol 0.5.0-beta.2 port matrix — batch 2

This is a compatibility port of **Echo Protocol 0.5.0-beta.2 — The House Remembers**. Gameplay content is unchanged from the Minecraft 1.21.1 release.

Baseline: tag `v0.5.0-beta.2`, commit `c4121bc21abc891af83f5fdcb2c17e29cec87d5e`. The reviewed Minecraft 1.21.5 reference head was `9ead66b552606d12ac7c14c829e87475c91e5db5`.

| Minecraft | Java | Compile/runtime mappings | Loader | Fabric API | Loom | Gradle | Branch | Implementation commit |
|---|---:|---|---|---|---|---|---|---|
| 1.21.6 | 21 | Yarn 1.21.6+build.1 | 0.19.3 | 0.128.2+1.21.6 | 1.17.17 | 9.6.1 | `port/1.21.6-1.21.8` | `8534d32` |
| 1.21.7 | 21 | compiled with 1.21.6+build.1; tested with 1.21.7+build.8 | 0.19.3 | 0.129.0+1.21.7 | 1.17.17 | 9.6.1 | `port/1.21.6-1.21.8` | `8534d32` |
| 1.21.8 | 21 | compiled with 1.21.6+build.1; tested with 1.21.8+build.1 | 0.19.3 | 0.136.1+1.21.8 | 1.17.17 | 9.6.1 | `port/1.21.6-1.21.8` | `8534d32` |

One unchanged production JAR was tested on all three versions. Its metadata lists exactly `1.21.6`, `1.21.7`, and `1.21.8` and does not claim 1.21.9 or newer.

- Artifact: `echo-protocol-0.5.0-beta.2-mc1.21.6-1.21.8.jar`
- SHA-256: `7a48227b95b6f172ee7d2abd1f6997baa00cd76fc48d794bacbc96f37756c672`

## Verification status

| Target | Tests | Build | Server | Client/world | Gameplay | Renderer | Restart | Two-client privacy | Performance/log | Upload readiness |
|---|---|---|---|---|---|---|---|---|---|---|
| 1.21.6 | VERIFIED (50) | VERIFIED | VERIFIED | VERIFIED | VERIFIED | PARTIALLY VERIFIED | NOT TESTED | NOT TESTED | NOT TESTED | VERIFIED |
| 1.21.7 | VERIFIED (50) | VERIFIED | VERIFIED | VERIFIED | VERIFIED | PARTIALLY VERIFIED | NOT TESTED | NOT TESTED | NOT TESTED | VERIFIED |
| 1.21.8 | VERIFIED (50) | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED |

All three dedicated servers reached `Done`, applied `ServerEntityTrackingMixin`, loaded resources and commands, and stopped cleanly. Real clients connected and loaded worlds on all three versions. Minecraft 1.21.7 received direct Memory, Corrupted, Peripheral, Audio Residue playback, Original, and cleanup coverage against the final shared SHA-256. Audio Residue was also directly verified on 1.21.8.

Minecraft 1.21.8 received the full representative pipeline, persistence restart, packet-instrumented two-client privacy test, visual translucent renderer check, and a short performance/log inspection. A real End Portal transition with an active Memory Echo was repeated against the final SHA-256; the target changed to `minecraft:the_end`, no Echo remained, and the event lock was clear.
