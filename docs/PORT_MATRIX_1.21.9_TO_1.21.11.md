# Echo Protocol 0.5.0-beta.2 port matrix: Minecraft 1.21.9-1.21.11

Porting baseline: tag `v0.5.0-beta.2`, commit `c4121bc21abc891af83f5fdcb2c17e29cec87d5e`.

This is compatibility batch 3. Gameplay and the Echo Protocol persistent-data schema are unchanged.

| Minecraft | Branch | Port commit | Java | Yarn | Loader | Fabric API | Loom | Gradle | Artifact | SHA-256 |
|---|---|---|---:|---|---|---|---|---|---|---|
| 1.21.9 | `port/1.21.9-1.21.10` | `163168ebd9c0cd0873d787e3f98b7380f5799c2f` | 21 | `1.21.9+build.1` | `0.19.3` | `0.134.1+1.21.9` | `1.17.18` | `9.6.1` | `echo-protocol-0.5.0-beta.2-mc1.21.9-1.21.10.jar` | `448a3bfeae49c30f818b16b9d64b9f40c1416143ed69868aba838ee55ece2226` |
| 1.21.10 | `port/1.21.9-1.21.10` | `163168ebd9c0cd0873d787e3f98b7380f5799c2f` | 21 | runtime uses the shared 1.21.9-compiled JAR; reference mapping `1.21.10+build.3` | `0.19.3` | runtime `0.138.4+1.21.10` | `1.17.18` | `9.6.1` | same exact shared JAR | same exact SHA-256 |
| 1.21.11 | `port/1.21.11` | `7c9ff708cfd0e5ede0fde3c1385b2e07bb6b278d` | 21 | `1.21.11+build.6` | `0.19.3` | `0.141.6+1.21.11` | `1.17.18` | `9.6.1` | `echo-protocol-0.5.0-beta.2-mc1.21.11.jar` | `d38e1bcac0bfad146aaf10d751f53a8bfb4b3b8900b57a45f3ce6bd822e437ad` |

## Verification matrix

| Minecraft | 50 tests | Clean build | Dedicated server | Client/world | Representative gameplay | Renderer | Restart | Two-client privacy | Performance/log | Upload readiness |
|---|---|---|---|---|---|---|---|---|---|---|
| 1.21.9 | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | NOT TESTED | NOT TESTED | PARTIALLY VERIFIED | VERIFIED |
| 1.21.10 | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | NOT TESTED | NOT TESTED | PARTIALLY VERIFIED | VERIFIED |
| 1.21.11 | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED | VERIFIED |

The exact shared JAR with SHA-256 `448a3bfeae49c30f818b16b9d64b9f40c1416143ed69868aba838ee55ece2226` was used without modification on both Minecraft 1.21.9 and 1.21.10. Metadata accepts exactly those two versions.

The 1.21.11 privacy result used two real clients and a temporary client-side entity-receipt probe. With `shared_echoes=false`, private Echo entity IDs were received only by Player A. With `shared_echoes=true`, both clients received the same intentionally shared entity ID. The probe was removed after the test. Private persistent metadata has no client payload path and remains server-side.

Performance status is a short release-blocking inspection, not a benchmark. No repeated exceptions, tick warnings, packet/particle/sound spam, stuck event lock, or retained Echo entity was observed. The only server warnings were expected offline-mode warnings in disposable test environments.
