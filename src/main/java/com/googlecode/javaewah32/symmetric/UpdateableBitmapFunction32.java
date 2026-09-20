package com.googlecode.javaewah32.symmetric;

import com.googlecode.javaewah.aggregation.UpdatableFunction;
import com.googlecode.javaewah32.BitmapStorage32;

import java.util.List;

/**
 * This is a Java specification for an "updatable" Boolean function meant to run
 * over 32-bit EWAH bitmaps.
 *
 * The state bookkeeping is shared in {@link UpdatableFunction}; this class
 * preserves the public 32-bit surface (typed pointer lists and the
 * {@link BitmapStorage32} dispatch method).
 *
 * Reference:
 *
 * Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned
 * bitmap indexes. Data &amp; Knowledge Engineering 69 (1), pages 3-28, 2010.
 *
 * @author Daniel Lemire
 * @since 0.8.2
 */
public abstract class UpdateableBitmapFunction32 extends UpdatableFunction {

    UpdateableBitmapFunction32() {
    }

    /**
     * Goes through the literals.
     *
     * @return an iterator
     */
    public final Iterable<EWAHPointer32> getLiterals() {
        return literalPointers(EWAHPointer32.class);
    }

    /**
     * append to the list the literal words as EWAHPointer32
     *
     * @param container where we write
     */
    public final void fillWithLiterals(final List<EWAHPointer32> container) {
        super.<EWAHPointer32>fillLiteralPointers(container);
    }

    /**
     * Writes out the answer.
     *
     * @param out      output buffer
     * @param runBegin beginning of the run
     * @param runEnd   end of the run
     */
    public abstract void dispatch(BitmapStorage32 out, int runBegin, int runEnd);
}
