# Echo Protocol 0.5.0-beta.2 port matrix: Minecraft 1.21.9-1.21.11

Porting baseline: tag `v0.5.0-beta.2`, commit `c4121bc21abc891af83f5fdcb2c17e29cec87d5e`.

This is compatibility batch 3. Gameplay and the Echo Protocol persistent-data schema are unchanged.

| Minecraft | Branch | Port commit | Java | Yarn | Loader | Fabric API | Loom | Gradle | Artifact | SHA-256 |
|---|---|---|---:|---|---|---|---|---|---|---|
| 1.21.9 | `port/1.21.9-1.21.10` | `ca289f77c20cc25d14f32e77dc8d6b9b06f3abcf` | 21 | `1.21.9+build.1` | `0.19.3` | `0.134.1+1.21.9` | `1.17.18` | `9.6.1` | `echo-protocol-0.5.0-beta.2-mc1.21.9-1.21.10.jar` | `5944747243d59411f9efa237607a64ae70f9a4108cc2ab8ece638a2a8986710e` |
| 1.21.10 | `port/1.21.9-1.21.10` | `ca289f77c20cc25d14f32e77dc8d6b9b06f3abcf` | 21 | runtime uses the shared 1.21.9-compiled JAR; reference mapping `1.21.10+build.3` | `0.19.3` | runtime `0.138.4+1.21.10` | `1.17.18` | `9.6.1` | same exact shared JAR | same exact SHA-256 |
| 1.21.11 | `port/1.21.11` | `fb3d91ad972b8508bad908a498d7c25ccf967c38` | 21 | `1.21.11+build.6` | `0.19.3` | `0.141.6+1.21.11` | `1.17.18` | `9.6.1` | `echo-protocol-0.5.0-beta.2-mc1.21.11.jar` | `bb9767566548c87cd1a3f2da8149b47916d5d3c8bf0b046f25f8c651accfc0ca` |

## Verification matrix

| Minecraft | 50 tests | Clean build | Dedicated server | Client/world | Representative gameplay | Renderer | Restart | Two-client privacy | Performance/log | Upload readiness |
|---|---|---|---|---|---|---|---|---|---|---|
| 1.21.9 | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | NOT TESTED | NOT TESTED | PARTIALLY VERIFIED | VERIFIED |
| 1.21.10 | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | NOT TESTED | NOT TESTED | PARTIALLY VERIFIED | VERIFIED |
| 1.21.11 | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED |

The exact shared JAR with SHA-256 `5944747243d59411f9efa237607a64ae70f9a4108cc2ab8ece638a2a8986710e` was used without modification on both Minecraft 1.21.9 and 1.21.10. Metadata accepts exactly those two versions.

The 1.21.11 privacy result used two real clients and a temporary client-side entity-receipt probe. With `shared_echoes=false`, private Echo entity IDs were received only by Player A. With `shared_echoes=true`, both clients received the same intentionally shared entity ID. The probe was removed after the test. Private persistent metadata has no client payload path and remains server-side.

Performance status is a short release-blocking inspection, not a benchmark. No repeated exceptions, tick warnings, packet/particle/sound spam, stuck event lock, or retained Echo entity was observed. The only server warnings were expected offline-mode warnings in disposable test environments.
