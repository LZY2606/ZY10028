package com.googlecode.javaewah32.symmetric;

import com.googlecode.javaewah.aggregation.BitmapStorageSink32;
import com.googlecode.javaewah.aggregation.RunningMergeEngine;
import com.googlecode.javaewah32.BitmapStorage32;
import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import com.googlecode.javaewah32.IteratingBufferedRunningLengthWord32;

/**
 * This is an implementation of the RunningBitmapMerge algorithm running on top
 * of JavaEWAH. It is well suited to computing symmetric Boolean queries.
 *
 * The merge state machine is shared with the 64-bit variant in
 * {@link com.googlecode.javaewah.aggregation.RunningMergeEngine}; this class
 * only constructs the 32-bit pointers and output sink.
 *
 * It is a revised version of an algorithm described in the following reference:
 * <ul><li>
 * Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned
 * bitmap indexes. Data &amp; Knowledge Engineering 69 (1), pages 3-28, 2010.
 * </li></ul>
 *
 * @author Daniel Lemire
 * @since 0.8.2
 */
public class RunningBitmapMerge32 implements BitmapSymmetricAlgorithm32 {

    @Override
    public void symmetric(final UpdateableBitmapFunction32 f,
                          final BitmapStorage32 out,
                          final EWAHCompressedBitmap32... set) {
        final EWAHPointer32[] pointers = new EWAHPointer32[set.length];
        for (int k = 0; k < set.length; ++k) {
            pointers[k] = new EWAHPointer32(0,
                    new IteratingBufferedRunningLengthWord32(set[k]), k);
        }
        RunningMergeEngine.run(f, new BitmapStorageSink32(out), pointers);
    }
}
