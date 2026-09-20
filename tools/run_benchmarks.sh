#!/bin/sh
# Reproduce the aggregation throughput/allocation benchmark numbers reported in
# REFACTORING.md. No external services are required; Maven needs access to the
# configured repositories only to fetch JMH (already cached in .m2).
#
# Usage:
#   tools/run_benchmarks.sh            # refactored (current working tree)
#   BASELINE=1 tools/run_benchmarks.sh # pristine HEAD for comparison
set -e
cd "$(dirname "$0")/.."
ROOT=$(pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT INT TERM

if [ -n "$BASELINE" ]; then
  echo "[bench] exporting pristine HEAD into $WORK ..."
  git archive HEAD | tar -x -C "$WORK"
  SRC="$WORK"
  LABEL="baseline"
else
  SRC="$ROOT"
  LABEL="refactored"
fi

echo "[bench] ($LABEL) installing JavaEWAH jar ..."
( cd "$SRC" && mvn -q -DskipTests -Dgpg.skip=true install )

echo "[bench] ($LABEL) building JMH benchmark ..."
mkdir -p "$WORK/bench"
cp -R "$ROOT/benchmark/." "$WORK/bench/"
( cd "$WORK/bench" && mvn -q -DskipTests package )
JAR=$(find "$WORK/bench/target" -name '*benchmarks*.jar' | head -1)
CP="$JAR:$(find ~/.m2/repository -name 'jmh-core-1.37.jar'):$(find ~/.m2/repository -name 'jopt-simple-5.0.4.jar'):$(find ~/.m2/repository -name 'commons-math3-3.6.1.jar' | head -1):$(find ~/.m2/repository/com/googlecode/javaewah/JavaEWAH -name 'JavaEWAH-1.2.4-SNAPSHOT.jar' | head -1)"

echo "[bench] ($LABEL) running JMH (throughput + alloc rate) ..."
OUT="$ROOT/target/benchmark-$LABEL.txt"
mkdir -p "$ROOT/target"
# shellcheck disable=SC2086
java -cp "$CP" org.openjdk.jmh.Main AggregationBenchmark \
  -prof gc -rf text -rff "$OUT" "$@"
echo "[bench] raw summary saved to $OUT"
