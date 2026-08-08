# Prism Launcher QA workflow

Echo Protocol final client verification uses Prism Launcher only. Loom `runClient` is not accepted as release evidence.

## Local installation

- Installation: Flatpak `org.prismlauncher.PrismLauncher`
- Version tested: Prism Launcher 11.0.3
- Root: `/home/theyeldo/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher`
- Java: Prism-managed Java 21 runtime
- Launch form: `flatpak run org.prismlauncher.PrismLauncher --launch <instance-id>`
- Helper: `./tools/prism-qa.sh list` and `./tools/prism-qa.sh launch <version>`

The helper never launches Minecraft directly and contains no account data. Authentication remains owned by Prism Launcher.

## Permanent instance matrix

| Instance ID | Minecraft | Loader | Fabric API | Branch | Production JAR |
|---|---:|---:|---:|---|---|
| EchoProtocol-Test-1.21.1 | 1.21.1 | 0.16.14 | 0.116.13+1.21.1 | fix/1.21.1-beta.2-runtime | echo-protocol-0.5.0-beta.2-mc1.21.1.jar |
| EchoProtocol-Test-1.21.2 | 1.21.2 | 0.16.14 | 0.106.1+1.21.2 | port/1.21.2-1.21.3 | echo-protocol-0.5.0-beta.2-mc1.21.2-1.21.3.jar |
| EchoProtocol-Test-1.21.3 | 1.21.3 | 0.16.14 | 0.114.1+1.21.3 | port/1.21.2-1.21.3 | echo-protocol-0.5.0-beta.2-mc1.21.2-1.21.3.jar |
| EchoProtocol-Test-1.21.4 | 1.21.4 | 0.16.14 | 0.119.4+1.21.4 | port/1.21.4 | echo-protocol-0.5.0-beta.2-mc1.21.4.jar |
| EchoProtocol-Test-1.21.5 | 1.21.5 | 0.16.14 | 0.128.2+1.21.5 | port/1.21.5 | echo-protocol-0.5.0-beta.2-mc1.21.5.jar |
| EchoProtocol-Test-1.21.6 | 1.21.6 | 0.19.3 | 0.128.2+1.21.6 | port/1.21.6-1.21.8 | echo-protocol-0.5.0-beta.2-mc1.21.6-1.21.8.jar |
| EchoProtocol-Test-1.21.7 | 1.21.7 | 0.19.3 | 0.129.0+1.21.7 | port/1.21.6-1.21.8 | echo-protocol-0.5.0-beta.2-mc1.21.6-1.21.8.jar |
| EchoProtocol-Test-1.21.8 | 1.21.8 | 0.19.3 | 0.136.1+1.21.8 | port/1.21.6-1.21.8 | echo-protocol-0.5.0-beta.2-mc1.21.6-1.21.8.jar |
| EchoProtocol-Test-1.21.9 | 1.21.9 | 0.19.3 | 0.134.1+1.21.9 | port/1.21.9-1.21.10 | echo-protocol-0.5.0-beta.2-mc1.21.9-1.21.10.jar |
| EchoProtocol-Test-1.21.10 | 1.21.10 | 0.19.3 | 0.138.4+1.21.10 | port/1.21.9-1.21.10 | echo-protocol-0.5.0-beta.2-mc1.21.9-1.21.10.jar |
| EchoProtocol-Test-1.21.11 | 1.21.11 | 0.19.3 | 0.141.6+1.21.11 | port/1.21.11 | echo-protocol-0.5.0-beta.2-mc1.21.11.jar |

Each baseline instance contains exactly Fabric API and one Echo Protocol production JAR. No Sodium, Iris, OptiFine, shader, skin, renderer, or performance mod is part of the baseline.

## Final-JAR procedure

1. Run `clean test` and `clean build` on the target branch.
2. Copy the remapped production JAR to a stable QA location and calculate SHA-256.
3. Stop every client using that shared artifact.
4. Replace the single `echo-protocol*.jar` in each relevant instance.
5. Remove only that instance's generated `.fabric/processedMods` and `.fabric/remappedJars` cache. This is necessary when the filename stays constant; otherwise Fabric may execute a stale processed JAR.
6. Verify the instance has exactly one Echo Protocol JAR and the expected Fabric API.
7. Launch with `./tools/prism-qa.sh launch <version>`.
8. Load the QA world, join the matching dedicated server, and perform the smoke test below.
9. Check `minecraft/logs/latest.log`, close normally, and confirm the installed JAR still has the recorded SHA-256.

## Minimal client smoke test

- Confirm the authenticated player has the expected non-default skin before evaluating Echo skin behavior.
- Spawn Memory Echo and The Original; confirm manifestation, target skin, model selection, translucency, fade, movement, cleanup, and held item where applicable.
- Replay a recorded route against a newly placed solid wall; the Echo must stop/truncate and fade rather than cross.
- Confirm a stage change alone does not grant a manifestation advancement.
- Join the matching dedicated server and disconnect cleanly.
- Treat only actual visible/audible behavior as gameplay evidence; command success is insufficient.

QA worlds and instances are intentionally retained for future beta.3 testing. Credentials and Prism account files must never be copied into the repository.
