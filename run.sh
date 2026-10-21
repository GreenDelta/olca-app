#!/usr/bin/env bash
#
# run.sh — build openLCA from source and launch it (no Eclipse IDE required).
#
# openLCA is an Eclipse RCP application. This script reproduces, on the command
# line, what the Eclipse IDE normally does: it builds the core modules, the HTML
# views, the reference-data templates, then runs the Tycho/Maven product build
# and launches the resulting application.
#
# Usage:
#   ./run.sh              # build everything (if needed) and launch openLCA
#   ./run.sh build        # build everything, do not launch
#   ./run.sh launch       # launch the already-built product
#   ./run.sh clean        # remove build outputs (libs, html, db_templates, build)
#   ./run.sh modules|libs|html|refdata|product   # run a single step
#
# Supported on macOS and Linux. (Windows would need a different launcher path
# and shell; not handled here.)
#
# Prerequisites (verified below):
#   * the olca-modules repository cloned next to this one (../olca-modules)
#   * Maven 3.9+
#   * Node.js + npm (for the HTML views)
#   * a JDK 21  (see JDK auto-detection below)
#
set -euo pipefail

# --------------------------------------------------------------------------
# Paths & toolchain
# --------------------------------------------------------------------------
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE="$(cd "$ROOT/.." && pwd)"
MODULES_DIR="$WORKSPACE/olca-modules"
TOOLING="$WORKSPACE/.olca-tooling"

OS="$(uname -s)"      # Darwin | Linux

# True on Apple Silicon. We can't trust `uname -m` here: under a Rosetta-
# translated shell it reports x86_64 even on an M-series chip. hw.optional.arm64
# reflects the real hardware. The committed build is x86_64-only, so on Apple
# Silicon we must run it (launcher + JVM) under Rosetta 2.
is_apple_silicon() {
  [[ "$OS" == "Darwin" && "$(sysctl -n hw.optional.arm64 2>/dev/null)" == "1" ]]
}

# In-place sed differs between BSD (macOS) and GNU (Linux).
sed_inplace() { if [[ "$OS" == "Darwin" ]]; then sed -i '' "$@"; else sed -i "$@"; fi; }

# Major Java version of a given JAVA_HOME (prints e.g. 21), or nothing.
java_major() {
  local jh="${1:-}"
  [[ -n "$jh" && -x "$jh/bin/java" ]] || return 1
  "$jh/bin/java" -version 2>&1 \
    | awk -F'"' '/version/{split($2,a,"."); print (a[1]=="1"?a[2]:a[1]); exit}'
}

# JDK used to BUILD. Prefers a JDK 21 dropped in ../.olca-tooling, then the
# platform's standard locations.
detect_build_jdk() {
  local cand
  for cand in "$TOOLING/jdk21/Contents/Home" "$TOOLING/jdk21"; do
    [[ "$(java_major "$cand" 2>/dev/null)" == "21" ]] && { echo "$cand"; return; }
  done
  if [[ "$OS" == "Darwin" ]]; then
    cand="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    [[ -n "$cand" ]] && { echo "$cand"; return; }
  else
    [[ "$(java_major "${JAVA_HOME:-}" 2>/dev/null)" == "21" ]] && { echo "$JAVA_HOME"; return; }
    if command -v java >/dev/null; then
      cand="$(readlink -f "$(command -v java)")"; cand="${cand%/bin/java}"
      [[ "$(java_major "$cand" 2>/dev/null)" == "21" ]] && { echo "$cand"; return; }
    fi
    for cand in /usr/lib/jvm/*21*; do
      [[ "$(java_major "$cand" 2>/dev/null)" == "21" ]] && { echo "$cand"; return; }
    done
  fi
  echo ""  # caller handles empty
}

# JDK used to RUN the product. The committed build targets x86_64. On Apple
# Silicon the launcher (and the JVM it loads) must therefore be x86_64 and runs
# under Rosetta 2 — use the dedicated x86_64 JDK. Everywhere else the build JDK
# (native x86_64) is fine.
detect_run_jdk() {
  local cand
  if is_apple_silicon; then
    for cand in "$TOOLING/jdk21x64/Contents/Home" "$TOOLING/jdk21x64"; do
      [[ "$(java_major "$cand" 2>/dev/null)" == "21" ]] && { echo "$cand"; return; }
    done
  fi
  detect_build_jdk
}

MVN="$(command -v mvn || true)"

# --------------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------------
log()  { printf '\n\033[1;34m==>\033[0m \033[1m%s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[error]\033[0m %s\n' "$*" >&2; exit 1; }

check_prereqs() {
  [[ -d "$MODULES_DIR" ]] || die "olca-modules not found at $MODULES_DIR (clone GreenDelta/olca-modules next to this repo)."
  [[ -n "$MVN" && -x "$MVN" ]] || die "Maven not found. Install it ($([[ "$OS" == Darwin ]] && echo 'brew install maven' || echo 'e.g. apt install maven, or via SDKMAN'))."
  command -v node >/dev/null || die "Node.js not found. Install Node 18+."
  BUILD_JDK="$(detect_build_jdk)"
  [[ -n "$BUILD_JDK" ]] || die "No JDK 21 found for building. Install Temurin 21 (or any JDK 21) or drop one at $TOOLING/jdk21."
  export JAVA_HOME="$BUILD_JDK"
  export PATH="$JAVA_HOME/bin:$PATH"
  log "Toolchain"
  echo "  repo        : $ROOT"
  echo "  olca-modules: $MODULES_DIR"
  echo "  build JDK   : $JAVA_HOME"
  echo "  maven       : $MVN"
}

# --------------------------------------------------------------------------
# Build steps
# --------------------------------------------------------------------------
step_modules() {
  log "1/5  Building olca-modules (core logic) -> ~/.m2"
  ( cd "$MODULES_DIR" && "$MVN" -q clean install -DskipTests )
}

step_libs() {
  log "2/5  Copying module jars into olca-app/libs"
  ( cd "$ROOT/olca-app" && "$MVN" -q -f pom_libs.xml clean package )
}

step_jython() {
  # Generates mod_bindings.py / app_bindings.py from the olca-modules sources.
  # The HTML build's prebuild step (extract_completions.py) reads these.
  log "3/6  Generating Jython bindings from olca-modules sources"
  ( cd "$ROOT" && node gen-jython-bindings.js >/dev/null )
}

step_html() {
  log "4/6  Building the HTML views (olca-app-html)"
  ( cd "$ROOT/olca-app-html"
    [[ -d node_modules ]] || npm install
    npm run build )
}

step_refdata() {
  log "5/6  Building reference database templates (olca-refdata)"
  ( cd "$ROOT/olca-refdata" && "$MVN" -q clean package )
}

step_product() {
  log "6/6  Tycho product build (downloads the Eclipse target platform on first run; this is slow)"
  # The committed platform.target uses includeMode="planner" together with
  # includeAllPlatforms="true". Those two are incompatible, so Tycho ignores
  # includeAllPlatforms and then fails to materialize the native launcher
  # feature (org.eclipse.equinox.executable). Building for the configured
  # x86_64 environments only, with includeAllPlatforms="false", resolves it.
  # We patch the file just for the build and always restore it afterwards so
  # the working tree stays clean.
  local target="$ROOT/olca-app/platform.target"
  local backup; backup="$(mktemp)"
  cp "$target" "$backup"
  sed_inplace 's/includeAllPlatforms="true"/includeAllPlatforms="false"/g' "$target"

  # We run only up to 'package' so Tycho materializes the product under
  # product/target/products/. The 'verify' phase has x86_64-hardcoded copy
  # steps meant for the Python installer packaging, which we don't need here.
  local rc=0
  ( cd "$ROOT/olca-app-build" && "$MVN" clean package ) || rc=$?

  # Always restore the committed file, whether the build passed or failed.
  cp "$backup" "$target"; rm -f "$backup"
  [[ $rc -eq 0 ]] || die "Tycho product build failed (exit $rc)."
}

# --------------------------------------------------------------------------
# Launch
# --------------------------------------------------------------------------
locate_launcher() {
  local products="$ROOT/olca-app-build/product/target/products/openLCA"
  if [[ "$OS" == "Darwin" ]]; then
    local base="$products/macosx/cocoa/x86_64"
    [[ -d "$base" ]] || return 1
    find "$base" -type f -path "*/MacOS/*" -perm -111 2>/dev/null | head -1
  else
    local base="$products/linux/gtk/x86_64"
    [[ -d "$base" ]] || return 1
    # native launcher 'openLCA' sits at the product root
    find "$base" -maxdepth 2 -type f -name openLCA -perm -111 2>/dev/null | head -1
  fi
}

step_launch() {
  local launcher; launcher="$(locate_launcher || true)"
  [[ -n "${launcher:-}" ]] || die "Product not built yet. Run: ./run.sh build"
  local run_jdk; run_jdk="$(detect_run_jdk)"
  [[ -n "$run_jdk" ]] || die "No run-time JDK 21 found."
  log "Launching openLCA"
  echo "  launcher : $launcher"
  echo "  run JDK  : $run_jdk"
  # vmargs (-XstartOnFirstThread, -Xmx, ...) come from the generated .ini next
  # to the launcher; we only point the launcher at a matching JVM.
  # On Apple Silicon the x86_64 launcher must run under Rosetta 2.
  local archprefix=()
  is_apple_silicon && archprefix=(arch -x86_64)
  exec "${archprefix[@]}" "$launcher" -vm "$run_jdk/bin/java" -consoleLog
}

# --------------------------------------------------------------------------
# Composite
# --------------------------------------------------------------------------
step_build() {
  step_modules
  step_libs
  step_jython
  step_html
  step_refdata
  step_product
  log "Build complete. Launch with: ./run.sh launch"
}

step_clean() {
  log "Cleaning build outputs"
  rm -rf "$ROOT/olca-app/libs" \
         "$ROOT/olca-app/html/"*.zip \
         "$ROOT/olca-app/db_templates/"*.zolca \
         "$ROOT/olca-app-html/dist" \
         "$ROOT/olca-app-build/product/target" \
         "$ROOT/olca-app-build/build"
  echo "done."
}

# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
cmd="${1:-all}"
case "$cmd" in
  all)     check_prereqs; step_build; step_launch ;;
  build)   check_prereqs; step_build ;;
  modules) check_prereqs; step_modules ;;
  libs)    check_prereqs; step_libs ;;
  jython)  check_prereqs; step_jython ;;
  html)    check_prereqs; step_jython; step_html ;;
  refdata) check_prereqs; step_refdata ;;
  product) check_prereqs; step_product ;;
  launch)  step_launch ;;
  clean)   step_clean ;;
  *)       die "Unknown command: $cmd (use: all|build|launch|clean|modules|libs|jython|html|refdata|product)" ;;
esac
