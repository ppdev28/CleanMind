#!/bin/sh
# Reuse Android Studio's formatter and the shared CustomTopBar code style.
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
formatter=${STUDIO_FORMATTER:-/Applications/Android Studio.app/Contents/bin/format.sh}
if [ ! -x "$formatter" ]; then
    echo 'Set STUDIO_FORMATTER to Android Studio Contents/bin/format.sh' >&2
    exit 1
fi
format_dir=$(mktemp -d "${TMPDIR:-/tmp}/cleanmind-format.XXXXXX")
trap 'rm -rf "$format_dir"' EXIT HUP INT TERM
cat > "$format_dir/idea.properties" <<PROPERTIES
idea.config.path=$format_dir/config
idea.system.path=$format_dir/system
idea.log.path=$format_dir/log
PROPERTIES
cd "$project_dir"
if [ -n "${KTFMT_JAR:-}" ]; then
    find app/src -name '*.kt' -exec java -jar "$KTFMT_JAR" --kotlinlang-style {} +
    java -jar "$KTFMT_JAR" --kotlinlang-style build.gradle.kts settings.gradle.kts app/build.gradle.kts
fi
STUDIO_PROPERTIES="$format_dir/idea.properties" "$formatter" "$@" \
    -s .idea/codeStyles/Project.xml -r -m '*.kt,*.kts,*.xml,*.yml,*.yaml,*.toml' \
    app/src app/build.gradle.kts build.gradle.kts settings.gradle.kts gradle/libs.versions.toml .github
