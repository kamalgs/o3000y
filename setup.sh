#!/usr/bin/env bash
#
# Downloads runtime dependencies into libs/.
# Run once before `gradle run`.
#
set -euo pipefail

LIBS_DIR="libs"
MAVEN_BASE="https://repo1.maven.org/maven2"

JACKSON_VERSION="2.18.2"
DUCKDB_VERSION="1.1.3"

declare -A DEPS=(
  ["jackson-databind-${JACKSON_VERSION}.jar"]="com/fasterxml/jackson/core/jackson-databind/${JACKSON_VERSION}"
  ["jackson-core-${JACKSON_VERSION}.jar"]="com/fasterxml/jackson/core/jackson-core/${JACKSON_VERSION}"
  ["jackson-annotations-${JACKSON_VERSION}.jar"]="com/fasterxml/jackson/core/jackson-annotations/${JACKSON_VERSION}"
  ["duckdb_jdbc-${DUCKDB_VERSION}.jar"]="org/duckdb/duckdb_jdbc/${DUCKDB_VERSION}"
)

mkdir -p "$LIBS_DIR"

for jar in "${!DEPS[@]}"; do
  if [ -f "$LIBS_DIR/$jar" ]; then
    echo "  [skip] $jar (already exists)"
  else
    echo "  [download] $jar ..."
    curl -sL -o "$LIBS_DIR/$jar" "${MAVEN_BASE}/${DEPS[$jar]}/$jar"
  fi
done

echo ""
echo "Done. Run the server with:  gradle run"
