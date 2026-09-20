package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah32.IteratingRLW32;

import java.util.Arrays;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 32-bit in-place aggregation state machines.
 *
 * These static methods are the JIT-specialized form of the single shared
 * algorithm template: each takes a concrete {@code int[]} scratch array and a
 * concrete {@link IteratingRLW32} cursor, and the bodies differ from the
 * 64-bit form ({@link InPlaceOps64}) only in the word primitive. Static entry
 * points let C2 inline the whole machine directly into the aggregation
 * driver, preserving the baseline throughput on wide fan-ins.
 */
public final class InPlaceOps32 {

    private InPlaceOps32() {
    }

    /**
     * @param bitmap scratch array
     * @param cur    source cursor
     * @return index of the first unwritten word
     */
    public static int or(final int[] bitmap, final IteratingRLW32 cur) {
        int pos = 0;
        int s;
        while ((s = cur.size()) > 0) {
            if (pos + s < bitmap.length) {
                final int runningLength = cur.getRunningLength();
                if (cur.getRunningBit())
                    Arrays.fill(bitmap, pos, pos + runningLength, ~0);
                pos += runningLength;
                final int literalCount = cur.getNumberOfLiteralWords();
                for (int k = 0; k < literalCount; ++k)
                    bitmap[pos++] |= cur.getLiteralWordAt(k);
                if (!cur.next())
                    return pos;
            } else {
                final int howMany = bitmap.length - pos;
                final int runningLength = cur.getRunningLength();
                if (pos + runningLength > bitmap.length) {
                    if (cur.getRunningBit())
                        Arrays.fill(bitmap, pos, bitmap.length, ~0);
                    cur.discardFirstWords(howMany);
                    return bitmap.length;
                }
                if (cur.getRunningBit())
                    Arrays.fill(bitmap, pos, pos + runningLength, ~0);
                pos += runningLength;
                for (int k = 0; pos < bitmap.length; ++k)
                    bitmap[pos++] |= cur.getLiteralWordAt(k);
                cur.discardFirstWords(howMany);
                return pos;
            }
        }
        return pos;
    }

    /**
     * @param bitmap scratch array
     * @param cur    source cursor
     * @return index of the first unwritten word
     */
    public static int xor(final int[] bitmap, final IteratingRLW32 cur) {
        int pos = 0;
        int s;
        while ((s = cur.size()) > 0) {
            if (pos + s < bitmap.length) {
                final int runningLength = cur.getRunningLength();
                if (cur.getRunningBit())
                    for (int k = pos; k < pos + runningLength; ++k)
                        bitmap[k] = ~bitmap[k];
                pos += runningLength;
                final int literalCount = cur.getNumberOfLiteralWords();
                for (int k = 0; k < literalCount; ++k)
                    bitmap[pos++] ^= cur.getLiteralWordAt(k);
                if (!cur.next())
                    return pos;
            } else {
                final int howMany = bitmap.length - pos;
                final int runningLength = cur.getRunningLength();
                if (pos + runningLength > bitmap.length) {
                    if (cur.getRunningBit())
                        for (int k = pos; k < bitmap.length; ++k)
                            bitmap[k] = ~bitmap[k];
                    cur.discardFirstWords(howMany);
                    return bitmap.length;
                }
                if (cur.getRunningBit())
                    for (int k = pos; k < pos + runningLength; ++k)
                        bitmap[k] = ~bitmap[k];
                pos += runningLength;
                for (int k = 0; pos < bitmap.length; ++k)
                    bitmap[pos++] ^= cur.getLiteralWordAt(k);
                cur.discardFirstWords(howMany);
                return pos;
            }
        }
        return pos;
    }

    /**
     * @param bitmap scratch array
     * @param cur    source cursor
     * @return index of the first unwritten word
     */
    public static int and(final int[] bitmap, final IteratingRLW32 cur) {
        int pos = 0;
        int s;
        while ((s = cur.size()) > 0) {
            if (pos + s < bitmap.length) {
                final int runningLength = cur.getRunningLength();
                if (!cur.getRunningBit())
                    for (int k = pos; k < pos + runningLength; ++k)
                        bitmap[k] = 0;
                pos += runningLength;
                final int literalCount = cur.getNumberOfLiteralWords();
                for (int k = 0; k < literalCount; ++k)
                    bitmap[pos++] &= cur.getLiteralWordAt(k);
                if (!cur.next())
                    return pos;
            } else {
                final int howMany = bitmap.length - pos;
                final int runningLength = cur.getRunningLength();
                if (pos + runningLength > bitmap.length) {
                    if (!cur.getRunningBit())
                        for (int k = pos; k < bitmap.length; ++k)
                            bitmap[k] = 0;
                    cur.discardFirstWords(howMany);
                    return bitmap.length;
                }
                if (!cur.getRunningBit())
                    for (int k = pos; k < pos + runningLength; ++k)
                        bitmap[k] = 0;
                pos += runningLength;
                for (int k = 0; pos < bitmap.length; ++k)
                    bitmap[pos++] &= cur.getLiteralWordAt(k);
                cur.discardFirstWords(howMany);
                return pos;
            }
        }
        return pos;
    }
}
