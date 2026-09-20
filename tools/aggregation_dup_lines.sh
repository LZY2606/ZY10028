#!/bin/sh
# Count physical lines in the aggregation-related files that used to be
# maintained twice (64-bit variant and 32-bit variant).
#
# Usage: tools/aggregation_dup_lines.sh SRC_ROOT
#   SRC_ROOT is a directory containing src/main/java/...
SRC="$1"
FILES="
com/googlecode/javaewah/FastAggregation.java
com/googlecode/javaewah/IteratorAggregation.java
com/googlecode/javaewah/symmetric/RunningBitmapMerge.java
com/googlecode/javaewah/symmetric/EWAHPointer.java
com/googlecode/javaewah/symmetric/UpdateableBitmapFunction.java
com/googlecode/javaewah/symmetric/ThresholdFuncBitmap.java
com/googlecode/javaewah32/FastAggregation32.java
com/googlecode/javaewah32/IteratorAggregation32.java
com/googlecode/javaewah32/symmetric/RunningBitmapMerge32.java
com/googlecode/javaewah32/symmetric/EWAHPointer32.java
com/googlecode/javaewah32/symmetric/UpdateableBitmapFunction32.java
com/googlecode/javaewah32/symmetric/ThresholdFuncBitmap32.java
"
total=0
for f in $FILES; do
  if [ -f "$SRC/$f" ]; then
    n=$(wc -l < "$SRC/$f")
    total=$((total+n))
  fi
done
echo "$total"
