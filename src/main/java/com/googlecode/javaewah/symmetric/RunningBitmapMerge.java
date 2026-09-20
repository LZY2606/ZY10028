package com.googlecode.javaewah.symmetric;

import com.googlecode.javaewah.BitmapStorage;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IteratingBufferedRunningLengthWord;
import com.googlecode.javaewah.aggregation.BitmapStorageSink64;
import com.googlecode.javaewah.aggregation.RunningMergeEngine;

/**
 * This is an implementation of the RunningBitmapMerge algorithm running on top
 * of JavaEWAH. It is well suited to computing symmetric Boolean queries.
 *
 * The merge state machine is shared with the 32-bit variant in
 * {@link com.googlecode.javaewah.aggregation.RunningMergeEngine}; this class
 * only constructs the 64-bit pointers and output sink.
 *
 * It is a revised version of an algorithm described in the following reference:
 * <ul><li>
 * Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned
 * bitmap indexes. Data &amp; Knowledge Engineering 69 (1), pages 3-28, 2010.
 * </li></ul>
 *
 * @author Daniel Lemire
 * @since 0.8.0
 */
public class RunningBitmapMerge implements BitmapSymmetricAlgorithm {

    @Override
    public void symmetric(final UpdateableBitmapFunction f,
                          final BitmapStorage out,
                          final EWAHCompressedBitmap... set) {
        final EWAHPointer[] pointers = new EWAHPointer[set.length];
        for (int k = 0; k < set.length; ++k) {
            pointers[k] = new EWAHPointer(0,
                    new IteratingBufferedRunningLengthWord(set[k]), k);
        }
        RunningMergeEngine.run(f, new BitmapStorageSink64(out), pointers);
    }
}
