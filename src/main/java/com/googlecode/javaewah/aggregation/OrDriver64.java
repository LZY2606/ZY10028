package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IteratingBufferedRunningLengthWord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Width-specialized buffered OR driver. The scheduling is the shared
 * template; there is a distinct static entry point per boolean operation and
 * per width so the JIT sees a branch-free, monomorphic hot loop with the same
 * call shape as the original monomorphic implementation.
 */
public final class OrDriver64 {

    private OrDriver64() {
    }

    /**
     * @param container output container
     * @param bufSize   scratch size in words
     * @param bitmaps   inputs sorted by descending size in bits
     * @param range     maximum size in bits over the inputs
     */
    public static void run(final EWAHCompressedBitmap container, final int bufSize,
                          final EWAHCompressedBitmap[] bitmaps, final int range) {
        final List<IteratingBufferedRunningLengthWord> cursors = new ArrayList<IteratingBufferedRunningLengthWord>(bitmaps.length);
        for (final EWAHCompressedBitmap b : bitmaps) {
            cursors.add(new IteratingBufferedRunningLengthWord(b));
        }
        final long[] hardBitmap = new long[bufSize];
        int maxRange = cursors.size();
        while (maxRange > 0) {
            int effective = 0;
            for (int k = 0; k < maxRange; ++k) {
                final IteratingBufferedRunningLengthWord cursor = cursors.get(k);
                if (cursor.size() > 0) {
                    final int eff = InPlaceOps64.or(hardBitmap, cursor);
                    if (eff > effective)
                        effective = eff;
                } else {
                    maxRange = k;
                }
            }
            for (int k = 0; k < effective; ++k)
                container.addWord(hardBitmap[k]);
            Arrays.fill(hardBitmap, 0);
        }
        container.setSizeInBitsWithinLastWord(range);
    }
}
