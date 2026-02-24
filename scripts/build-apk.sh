#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() { echo "[build-apk] $*"; }
warn() { echo "[build-apk][warn] $*"; }

if [[ "${KEEP_PROXY:-0}" != "1" ]]; then
  unset http_proxy HTTP_PROXY https_proxy HTTPS_PROXY all_proxy ALL_PROXY
  export GRADLE_OPTS="${GRADLE_OPTS:-} -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort="
  export JAVA_OPTS="${JAVA_OPTS:-} -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort="
  log "Proxy environment cleared (set KEEP_PROXY=1 to disable this behavior)."
fi

if command -v mise >/dev/null 2>&1 && mise where java@17 >/dev/null 2>&1; then
  export JAVA_HOME="$(mise where java@17)"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if ! command -v java >/dev/null 2>&1; then
  warn "Java not found. Install JDK 17 and retry."
  exit 1
fi

JAVA_VERSION="$(java -version 2>&1 | head -n1)"
log "Using Java: ${JAVA_VERSION}"

if [[ "$JAVA_VERSION" != *'"17.'* && "$JAVA_VERSION" != *'"17"'* ]]; then
  warn "Recommended Java is 17 for this project."
fi

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  warn "ANDROID_HOME/ANDROID_SDK_ROOT is not set. Build may fail if SDK is not discoverable."
fi

chmod +x ./gradlew

TASKS=(assembleDebug)
if [[ "${SKIP_CLEAN:-0}" != "1" ]]; then
  TASKS=(clean assembleDebug)
fi

WRAPPER_LOG="$(mktemp)"
set +e
./gradlew --no-daemon "${TASKS[@]}" >"$WRAPPER_LOG" 2>&1
WRAPPER_EXIT=$?
set -e

if [[ $WRAPPER_EXIT -eq 0 ]]; then
  cat "$WRAPPER_LOG"
  log "Debug APK: app/build/outputs/apk/debug/app-debug.apk"
  exit 0
fi

if grep -Eq 'Unable to tunnel through proxy|403 Forbidden|Network is unreachable|Could not resolve host|Read timed out|Connection timed out' "$WRAPPER_LOG"; then
  log "Gradle wrapper failed due to network/proxy issue. Trying local Gradle fallback..."
  if command -v gradle >/dev/null 2>&1; then
    set +e
    gradle --no-daemon -p "$ROOT_DIR" "${TASKS[@]}"
    FALLBACK_EXIT=$?
    set -e
    if [[ $FALLBACK_EXIT -eq 0 ]]; then
      log "Fallback build succeeded. Debug APK: app/build/outputs/apk/debug/app-debug.apk"
      exit 0
    fi
    warn "Fallback build failed. Most likely repository access is blocked (Google/Maven/Plugin Portal)."
    warn "If you are behind a proxy, re-run with KEEP_PROXY=1 and valid proxy settings."
    warn "If dependencies are already cached, re-run with SKIP_CLEAN=1."
    log "Wrapper failure output:"
    cat "$WRAPPER_LOG"
    exit $FALLBACK_EXIT
  fi
  warn "Local gradle is not installed, and wrapper download failed due to network/proxy."
  cat "$WRAPPER_LOG"
  exit $WRAPPER_EXIT
fi

cat "$WRAPPER_LOG"
exit $WRAPPER_EXIT
