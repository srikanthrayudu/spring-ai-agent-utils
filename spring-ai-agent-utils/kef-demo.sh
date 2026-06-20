#!/bin/bash
# ──────────────────────────────────────────────────────────────
# KEF Demo Launcher
# Usage:  ./kef-demo.sh
# ──────────────────────────────────────────────────────────────

set -e
cd "$(dirname "$0")"

echo "Building classpath..."
mvn compile -q 2>/dev/null
mvn dependency:build-classpath -DincludeScope=provided \
    -Dmdep.outputFile=/tmp/kef-cp.txt -q 2>/dev/null

CP="target/classes:$(cat /tmp/kef-cp.txt)"

exec java -cp "$CP" org.springaicommunity.agent.memory.KefDemo "$@"
