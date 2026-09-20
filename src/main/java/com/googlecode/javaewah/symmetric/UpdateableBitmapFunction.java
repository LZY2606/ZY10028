package com.googlecode.javaewah.symmetric;

import com.googlecode.javaewah.BitmapStorage;
import com.googlecode.javaewah.aggregation.RunningPointer;
import com.googlecode.javaewah.aggregation.UpdatableFunction;
import com.googlecode.javaewah.aggregation.WordSink;

import java.util.Iterator;
import java.util.List;

/**
 * This is a Java specification for an "updatable" Boolean function meant to run
 * over EWAH bitmaps.
 *
 * The state bookkeeping is shared in {@link UpdatableFunction}; this class
 * preserves the public 64-bit surface (typed pointer lists and the
 * {@link BitmapStorage} dispatch method).
 *
 * Reference:
 *
 * Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned
 * bitmap indexes. Data &amp; Knowledge Engineering 69 (1), pages 3-28, 2010.
 *
 * @author Daniel Lemire
 * @since 0.8.0
 */
public abstract class UpdateableBitmapFunction extends UpdatableFunction {

    UpdateableBitmapFunction() {
    }

    /**
     * Goes through the literals.
     *
     * @return an iterator
     */
    public final Iterable<EWAHPointer> getLiterals() {
        return literalPointers(EWAHPointer.class);
    }

    /**
     * append to the list the literal words as EWAHPointer
     *
     * @param container where we write
     */
    public final void fillWithLiterals(final List<EWAHPointer> container) {
        super.<EWAHPointer>fillLiteralPointers(container);
    }

    /**
     * Writes out the answer.
     *
     * @param out      output buffer
     * @param runBegin beginning of the run
     * @param runEnd   end of the run
     */
    public abstract void dispatch(BitmapStorage out, int runBegin, int runEnd);
}
