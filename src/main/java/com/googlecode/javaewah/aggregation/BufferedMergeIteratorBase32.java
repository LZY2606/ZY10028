package com.googlecode.javaewah.aggregation;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 32-bit specialization of the buffered OR/XOR merge iterator. The in-place
 * step is a direct static, monomorphic call into {@link InPlaceOps32}.
 */
public final class BufferedMergeIteratorBase32
        extends BufferedMergeIterator<RlwCursor32> {

    /**
     * @param factory width factory
     * @param op      OR or XOR
     * @param cursors live cursors
     * @param bufSize scratch buffer size in words
     */
    public BufferedMergeIteratorBase32(final AggregationFactory32 factory,
                                       final AggregateOp op,
                                       final java.util.LinkedList<RlwCursor32> cursors,
                                       final int bufSize) {
        super(factory, op, cursors, bufSize);
    }

    @Override
    protected int inplace(final RlwCursor32 cursor) {
        final int[] bitmap = ((IntWordArray) hardBitmap()).raw();
        switch (op()) {
            case XOR:
                return InPlaceOps32.xor(bitmap, cursor.rlw);
            case AND:
                return InPlaceOps32.and(bitmap, cursor.rlw);
            default:
                return InPlaceOps32.or(bitmap, cursor.rlw);
        }
    }
}
