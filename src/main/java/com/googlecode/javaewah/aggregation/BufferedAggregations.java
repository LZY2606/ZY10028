package com.googlecode.javaewah.aggregation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Shared control flow of the {@code bufferedand/bufferedor/bufferedxor}
 * reference aggregations in the 64-bit and 32-bit {@code FastAggregation}
 * classes.
 *
 * The scheduling of the scratch buffer (fill policy per operation, when the
 * effective prefix is flushed, when inputs are considered exhausted, the
 * final size adjustment) lives here once; the width-specific word work is the
 * static, JIT-specialized {@link InPlaceOps64}/{@link InPlaceOps32} machines.
 */
public final class BufferedAggregations {

    private BufferedAggregations() {
    }

    /**
     * Buffered AND aggregate into a sink.
     *
     * @param factory width factory
     * @param sink    output sink
     * @param bufSize scratch size in words (per input)
     * @param bitmaps concrete input bitmaps
     * @param <C>     cursor type
     */
    public static <C extends RlwCursor<C>> void bufferedAnd(
            final AggregationFactory<C> factory, final WordSink sink,
            final int bufSize, final Object[] bitmaps) {
        final LinkedList<C> cursors = new LinkedList<C>();
        for (final Object bitmap : bitmaps) {
            cursors.add(factory.cursorOfBitmap(bitmap));
        }
        final WordArray<C> hardBitmap =
                factory.newWordArray(bufSize * bitmaps.length);

        for (final C cursor : cursors) {
            if (cursor.size() == 0) {
                cursors.clear();
                break;
            }
        }

        while (!cursors.isEmpty()) {
            factory.fillAll(hardBitmap);
            long effective = Integer.MAX_VALUE;
            for (final C cursor : cursors) {
                final int eff = inplaceAnd(factory, hardBitmap, cursor);
                if (eff < effective)
                    effective = eff;
            }
            hardBitmap.flushToSink(sink, (int) effective);
            for (final C cursor : cursors) {
                if (cursor.size() == 0) {
                    cursors.clear();
                    break;
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int inplaceAnd(final AggregationFactory<?> factory,
                                  final WordArray<?> array,
                                  final RlwCursor<?> cursor) {
        if (factory == AggregationFactory32.INSTANCE) {
            return InPlaceOps32.and(((IntWordArray) array).raw(),
                    ((RlwCursor32) cursor).rlw);
        }
        return InPlaceOps64.and(((LongWordArray) array).raw(),
                ((RlwCursor64) cursor).rlw);
    }

    /**
     * Buffered OR or XOR aggregate into a sink.
     *
     * @param factory width factory
     * @param sink    output sink
     * @param op      OR or XOR
     * @param bufSize scratch size in words
     * @param bitmaps concrete input bitmaps already sorted by descending
     *                size in bits
     * @param range   maximum size in bits over the inputs
     * @param <C>     cursor type
     */
    public static <C extends RlwCursor<C>> void bufferedOrXor(
            final AggregationFactory<C> factory, final WordSink sink,
            final AggregateOp op, final int bufSize,
            final Object[] bitmaps, final int range) {
        factory.runBufferedOrXor(sink, op, bufSize, bitmaps, range);
    }
}
