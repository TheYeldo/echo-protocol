# Legacy client runtime cleanup inventory

Audit date: 2026-08-08. No personal Minecraft or Prism Launcher data was deleted.

| Path | Purpose/content | Ownership assessment | Action | Reason |
|---|---|---|---|---|
| `run/` in the repository | Loom development client data, including saves | Project development data; may contain unique test worlds | PRESERVED | The directory is git-ignored, but its saves were not proven disposable. |
| `/tmp/echo-batch3-0Ny7tJ/` | Earlier Echo Protocol disposable dedicated-server runtimes | Codex-created | PRESERVED | Server runtimes are outside the requested legacy-client deletion scope. |
| `/tmp/echo-privacy-probe/` | Packet/privacy investigation output | Codex-created, disposable | PRESERVED | Small diagnostic evidence; not a Minecraft client installation. |
| `/tmp/echo-xdotool/` | Locally extracted X11 automation binaries | Codex-created, disposable | PRESERVED | Required by the current Prism/Xvfb QA lab. |
| `/tmp/echo-xvfb/` | Xvfb support files | Codex-created, disposable | PRESERVED | Required by the current Prism/Xvfb QA lab. |
| `/tmp/echo-installer-accidental.0TQw4g/` | Recoverably moved accidental Fabric installer output | Codex-created | PRESERVED | Kept outside the repository rather than destructively deleted. |
| Prism instances named `EchoProtocol-Test-*` | Permanent version-specific QA instances | Created for Echo Protocol QA | PRESERVED | These are the new repeatable Prism QA matrix. |
| Prism instance `EchoVerify-1.21.11-B` | Previous Codex two-client QA instance | Codex-created | PRESERVED/REUSED | Used as Player B in the 1.21.11 privacy test. |

No legacy client runtime satisfied all deletion requirements with enough certainty. In particular, the repository `run/` directory contains saves and was therefore preserved. The user's normal Prism instances, account data, screenshots, resource packs, and `~/.minecraft` were not modified.
