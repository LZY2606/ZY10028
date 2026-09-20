package com.googlecode.javaewah32.symmetric;

import com.googlecode.javaewah.aggregation.RlwCursor32;
import com.googlecode.javaewah.aggregation.RunningPointer;
import com.googlecode.javaewah.aggregation.UpdatableFunction;
import com.googlecode.javaewah32.IteratingBufferedRunningLengthWord32;

/**
 * Wrapper around an IteratingBufferedRunningLengthWord32 used by the
 * RunningBitmapMerge32 class.
 *
 * The run bookkeeping is the width-agnostic {@link RunningPointer} state
 * machine; this class only keeps the public 32-bit surface (typed iterator,
 * typed callbacks).
 *
 * @author Daniel Lemire
 * @since 0.8.2
 */
public final class EWAHPointer32
        extends RunningPointer<RlwCursor32, EWAHPointer32> {

    /**
     * Underlying iterator
     */
    public final IteratingBufferedRunningLengthWord32 iterator;

    /**
     * Construct a pointer over an IteratingBufferedRunningLengthWord32.
     *
     * @param previousEndRun word where the previous run ended
     * @param rw             the iterator
     * @param pos            current position (in word)
     */
    public EWAHPointer32(final int previousEndRun,
                         final IteratingBufferedRunningLengthWord32 rw,
                         final int pos) {
        super(previousEndRun, new RlwCursor32(rw), pos);
        this.iterator = rw;
    }

    /**
     * @param f call the function with the current information
     */
    public void callbackUpdate(final UpdateableBitmapFunction32 f) {
        super.callbackUpdate((UpdatableFunction) f);
    }
}
