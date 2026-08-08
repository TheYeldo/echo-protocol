#!/usr/bin/env bash
set -euo pipefail

readonly APP_ID="org.prismlauncher.PrismLauncher"
readonly INSTANCE_PREFIX="EchoProtocol-Test-"
readonly SUPPORTED_VERSIONS=(
  1.21.1 1.21.2 1.21.3 1.21.4 1.21.5 1.21.6
  1.21.7 1.21.8 1.21.9 1.21.10 1.21.11
)

if command -v prismlauncher >/dev/null 2>&1; then
  readonly LAUNCHER=(prismlauncher)
  readonly PRISM_ROOT="${XDG_DATA_HOME:-$HOME/.local/share}/PrismLauncher"
elif command -v flatpak >/dev/null 2>&1 \
    && flatpak info "$APP_ID" >/dev/null 2>&1; then
  readonly LAUNCHER=(flatpak run "$APP_ID")
  readonly PRISM_ROOT="$HOME/.var/app/$APP_ID/data/PrismLauncher"
else
  echo "Prism Launcher was not found as a native executable or Flatpak." >&2
  exit 1
fi

is_supported() {
  local requested="$1"
  local version
  for version in "${SUPPORTED_VERSIONS[@]}"; do
    [[ "$version" == "$requested" ]] && return 0
  done
  return 1
}

list_instances() {
  local version instance state
  printf 'Prism root: %s\n' "$PRISM_ROOT"
  for version in "${SUPPORTED_VERSIONS[@]}"; do
    instance="${INSTANCE_PREFIX}${version}"
    state="MISSING"
    [[ -d "$PRISM_ROOT/instances/$instance" ]] && state="READY"
    printf '%-8s %-31s %s\n' "$version" "$instance" "$state"
  done
}

case "${1:-}" in
  list)
    list_instances
    ;;
  launch)
    version="${2:-}"
    if [[ -z "$version" ]] || ! is_supported "$version"; then
      echo "Usage: $0 launch <1.21.1 ... 1.21.11>" >&2
      exit 2
    fi
    instance="${INSTANCE_PREFIX}${version}"
    if [[ ! -d "$PRISM_ROOT/instances/$instance" ]]; then
      echo "Prism instance does not exist: $instance" >&2
      exit 1
    fi
    exec "${LAUNCHER[@]}" --launch "$instance"
    ;;
  *)
    echo "Usage: $0 {list|launch <version>}" >&2
    exit 2
    ;;
esac
