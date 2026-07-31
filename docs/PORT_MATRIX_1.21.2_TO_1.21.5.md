# Echo Protocol 0.5.0-beta.2 port matrix

This batch is a compatibility port of **Echo Protocol 0.5.0-beta.2 — The House Remembers**. It does not add gameplay content.

Baseline: tag `v0.5.0-beta.2`, commit `c4121bc21abc891af83f5fdcb2c17e29cec87d5e`, originally released for Minecraft 1.21.1. The published and locally rebuilt original JAR both have SHA-256 `0bb35cdd01d90d0d2a441541e41aa599e412c1d711f98494833823c4b5be011a`.

| Minecraft | Java | Yarn | Loader | Fabric API | Loom | Gradle | Branch | Port commit | Artifact | SHA-256 |
|---|---:|---|---|---|---|---|---|---|---|---|
| 1.21.2 | 21 | 1.21.2+build.1 | 0.16.14 | 0.106.1+1.21.2 | 1.17.14 | 9.6.1 | `port/1.21.2-1.21.3` | `3ee366c48c83be96e3f27ece34f3839f3de9a505` | `echo-protocol-0.5.0-beta.2-mc1.21.2-1.21.3.jar` | `ecee744cd5326d177918a27acb037e0408480b16a8aad7084be12479edc383a1` |
| 1.21.3 | 21 | 1.21.2+build.1 (compiled artifact) | 0.16.14 | 0.114.1+1.21.3 (runtime) | 1.17.14 | 9.6.1 | `port/1.21.2-1.21.3` | `3ee366c48c83be96e3f27ece34f3839f3de9a505` | same unchanged JAR | `ecee744cd5326d177918a27acb037e0408480b16a8aad7084be12479edc383a1` |
| 1.21.4 | 21 | 1.21.4+build.8 | 0.16.14 | 0.119.4+1.21.4 | 1.17.14 | 9.6.1 | `port/1.21.4` | `0c577bd977fa5bce14b21087447e6c7700d0565c` | `echo-protocol-0.5.0-beta.2-mc1.21.4.jar` | `833665dc011e17fc8ffea10a97b1326027f9e5a5572f2f1e0be919fc69bbf865` |
| 1.21.5 | 21 | 1.21.5+build.1 | 0.16.14 | 0.128.2+1.21.5 | 1.17.14 | 9.6.1 | `port/1.21.5` | `51091462de5f8b2203e32140c9a1bea805a5fa08` | `echo-protocol-0.5.0-beta.2-mc1.21.5.jar` | `188286f6a7dbae143db0857a61a73acdecae87443bcdf38ef0fd524ba17ab858` |

The shared artifact is one unchanged compiled JAR. That exact SHA-256 was run on both Minecraft 1.21.2 and 1.21.3. Its metadata contains the exact alternatives `1.21.2` and `1.21.3`, not an open-ended range.

## Verification status

| Target | Automated tests | Build | Dedicated server | Client/menu/world | Gameplay smoke | Persistence restart | Real two-client privacy | Overall |
|---|---|---|---|---|---|---|---|---|
| 1.21.2 | VERIFIED (50) | VERIFIED | VERIFIED | VERIFIED | PARTIALLY VERIFIED | PARTIALLY VERIFIED | NOT TESTED | PARTIALLY VERIFIED |
| 1.21.3 | VERIFIED (50) | VERIFIED | VERIFIED | VERIFIED | PARTIALLY VERIFIED | NOT TESTED | NOT TESTED | PARTIALLY VERIFIED |
| 1.21.4 | VERIFIED (50) | VERIFIED | VERIFIED | VERIFIED | PARTIALLY VERIFIED | PARTIALLY VERIFIED | NOT TESTED | PARTIALLY VERIFIED |
| 1.21.5 | VERIFIED (50) | VERIFIED | VERIFIED | VERIFIED | PARTIALLY VERIFIED | VERIFIED | NOT TESTED | PARTIALLY VERIFIED |

Every server reached `Done`, registered Echo Protocol commands, loaded advancements/resources, applied the critical server entity-tracking mixin, and stopped cleanly. Every real Fabric client reached the menu and loaded a world. The 1.21.5 dedicated test also connected a real client, spawned tracked Echo entities, and verified persistent state after a clean restart.

`PARTIALLY VERIFIED` means representative live behavior was exercised, but the complete manual visual/gameplay and real two-client recipient audit requested for release acceptance was not performed. These artifacts are therefore build candidates, not yet approved for public upload.
