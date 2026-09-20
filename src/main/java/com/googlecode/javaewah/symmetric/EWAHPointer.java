package com.googlecode.javaewah.symmetric;

import com.googlecode.javaewah.IteratingBufferedRunningLengthWord;
import com.googlecode.javaewah.aggregation.RlwCursor64;
import com.googlecode.javaewah.aggregation.RunningPointer;
import com.googlecode.javaewah.aggregation.UpdatableFunction;

/**
 * Wrapper around an IteratingBufferedRunningLengthWord used by the
 * RunningBitmapMerge class.
 *
 * The run bookkeeping is the width-agnostic
 * {@link RunningPointer} state machine; this class only keeps the public
 * 64-bit surface (typed iterator, typed callbacks).
 *
 * @author Daniel Lemire
 * @since 0.8.0
 */
public final class EWAHPointer
        extends RunningPointer<RlwCursor64, EWAHPointer> {

    /**
     * Underlying iterator
     */
    public final IteratingBufferedRunningLengthWord iterator;

    /**
     * Construct a pointer over an IteratingBufferedRunningLengthWord.
     *
     * @param previousEndRun word where the previous run ended
     * @param rw             the iterator
     * @param pos            current position (in word)
     */
    public EWAHPointer(final int previousEndRun,
                       final IteratingBufferedRunningLengthWord rw,
                       final int pos) {
        super(previousEndRun, new RlwCursor64(rw), pos);
        this.iterator = rw;
    }

    /**
     * @param f call the function with the current information
     */
    public void callbackUpdate(final UpdateableBitmapFunction f) {
        super.callbackUpdate((UpdatableFunction) f);
    }
}
