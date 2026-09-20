package com.googlecode.javaewah.aggregation;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 64-bit specialization of the buffered OR/XOR merge iterator. The in-place
 * step is a direct static, monomorphic call into {@link InPlaceOps64}.
 */
public final class BufferedMergeIteratorBase64
        extends BufferedMergeIterator<RlwCursor64> {

    /**
     * @param factory width factory
     * @param op      OR or XOR
     * @param cursors live cursors
     * @param bufSize scratch buffer size in words
     */
    public BufferedMergeIteratorBase64(final AggregationFactory64 factory,
                                       final AggregateOp op,
                                       final java.util.LinkedList<RlwCursor64> cursors,
                                       final int bufSize) {
        super(factory, op, cursors, bufSize);
    }

    @Override
    protected int inplace(final RlwCursor64 cursor) {
        final long[] bitmap = ((LongWordArray) hardBitmap()).raw();
        switch (op()) {
            case XOR:
                return InPlaceOps64.xor(bitmap, cursor.rlw);
            case AND:
                return InPlaceOps64.and(bitmap, cursor.rlw);
            default:
                return InPlaceOps64.or(bitmap, cursor.rlw);
        }
    }
}
