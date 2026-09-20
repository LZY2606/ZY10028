package com.googlecode.javaewah32;

import com.googlecode.javaewah.aggregation.AggregateOp;
import com.googlecode.javaewah.aggregation.AggregationFactory32;
import com.googlecode.javaewah.aggregation.BitmapStorageSink32;
import com.googlecode.javaewah.aggregation.BufferedAggregations;
import com.googlecode.javaewah.aggregation.PriorityAggregations;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Fast algorithms to aggregate many bitmaps. These algorithms are just given as
 * reference. They may not be faster than the corresponding methods in the
 * EWAHCompressedBitmap32 class.
 *
 * The buffered scheduling state machines are shared with the 64-bit
 * implementation in {@link com.googlecode.javaewah.aggregation}. The
 * priority-queue scheduling below is the 32-bit monomorphic instantiation of
 * the single shared scheduling rule (repeatedly combine the two smallest
 * bitmaps): the pairwise combine is a direct {@code or}/{@code xor} call (as
 * in the original code) so the JIT can inline it while merging large
 * intermediate bitmaps.
 *
 * @author Daniel Lemire
 */
public final class FastAggregation32 {

    /** Private constructor to prevent instantiation */
    private FastAggregation32() {}

    /**
     * Compute the and aggregate using a temporary uncompressed bitmap.
     *
     * @param bitmaps the source bitmaps
     * @param bufSize buffer size used during the computation in 64-bit
     *                words (per input bitmap)
     * @return the or aggregate.
     */
    public static EWAHCompressedBitmap32 bufferedand(final int bufSize,
                                                     final EWAHCompressedBitmap32... bitmaps) {
        EWAHCompressedBitmap32 answer = new EWAHCompressedBitmap32();
        bufferedandWithContainer(answer, bufSize, bitmaps);
        return answer;
    }

    /**
     * Compute the and aggregate using a temporary uncompressed bitmap.
     *
     * @param container where the aggregate is written
     * @param bufSize   buffer size used during the computation in 64-bit
     *                  words (per input bitmap)
     * @param bitmaps   the source bitmaps
     */
    public static void bufferedandWithContainer(
            final BitmapStorage32 container, final int bufSize,
            final EWAHCompressedBitmap32... bitmaps) {
        BufferedAggregations.bufferedAnd(AggregationFactory32.INSTANCE,
                new BitmapStorageSink32(container), bufSize, bitmaps);
    }

    /**
     * Compute the or aggregate using a temporary uncompressed bitmap.
     *
     * @param bitmaps the source bitmaps
     * @param bufSize buffer size used during the computation in 64-bit
     *                words
     * @return the or aggregate.
     */
    public static EWAHCompressedBitmap32 bufferedor(final int bufSize,
                                                    final EWAHCompressedBitmap32... bitmaps) {
        EWAHCompressedBitmap32 answer = new EWAHCompressedBitmap32();
        bufferedorWithContainer(answer, bufSize, bitmaps);
        return answer;
    }

    /**
     * Compute the or aggregate using a temporary uncompressed bitmap.
     *
     * @param container where the aggregate is written
     * @param bufSize   buffer size used during the computation in 64-bit
     *                  words
     * @param bitmaps   the source bitmaps
     */
    public static void bufferedorWithContainer(
            final BitmapStorage32 container, final int bufSize,
            final EWAHCompressedBitmap32... bitmaps) {
        sortedBuffered(container, bufSize, bitmaps, AggregateOp.OR);
    }

    /**
     * Compute the xor aggregate using a temporary uncompressed bitmap.
     *
     * @param bitmaps the source bitmaps
     * @param bufSize buffer size used during the computation in 64-bit
     *                words
     * @return the xor aggregate.
     */
    public static EWAHCompressedBitmap32 bufferedxor(final int bufSize,
                                                     final EWAHCompressedBitmap32... bitmaps) {
        EWAHCompressedBitmap32 answer = new EWAHCompressedBitmap32();
        bufferedxorWithContainer(answer, bufSize, bitmaps);
        return answer;
    }

    /**
     * Compute the xor aggregate using a temporary uncompressed bitmap.
     *
     * @param container where the aggregate is written
     * @param bufSize   buffer size used during the computation in 64-bit
     *                  words
     * @param bitmaps   the source bitmaps
     */
    public static void bufferedxorWithContainer(
            final BitmapStorage32 container, final int bufSize,
            final EWAHCompressedBitmap32... bitmaps) {
        sortedBuffered(container, bufSize, bitmaps, AggregateOp.XOR);
    }

    private static void sortedBuffered(final BitmapStorage32 container,
                                       final int bufSize,
                                       final EWAHCompressedBitmap32[] bitmaps,
                                       final AggregateOp op) {
        int range = 0;
        final EWAHCompressedBitmap32[] sorted = bitmaps.clone();
        Arrays.sort(sorted, new Comparator<EWAHCompressedBitmap32>() {
            @Override
            public int compare(EWAHCompressedBitmap32 a,
                               EWAHCompressedBitmap32 b) {
                return b.sizeInBits() - a.sizeInBits();
            }
        });
        for (final EWAHCompressedBitmap32 bitmap : sorted) {
            if (bitmap.sizeInBits() > range)
                range = bitmap.sizeInBits();
        }
        BufferedAggregations.bufferedOrXor(AggregationFactory32.INSTANCE,
                new BitmapStorageSink32(container), op, bufSize, sorted, range);
    }

    /**
     * Uses a priority queue to compute the or aggregate.
     *
     * @param bitmaps bitmaps to be aggregated
     * @return the or aggregate
     */
    public static EWAHCompressedBitmap32 or(
            final EWAHCompressedBitmap32... bitmaps) {
        return PriorityAggregations.orArray(bitmaps,
                new java.util.function.Supplier<EWAHCompressedBitmap32>() {
                    @Override
                    public EWAHCompressedBitmap32 get() {
                        return new EWAHCompressedBitmap32();
                    }
                });
    }

    /**
     * Uses a priority queue to compute the or aggregate.
     *
     * @param container where we write the result
     * @param bitmaps   to be aggregated
     */
    public static void orToContainer(final BitmapStorage32 container,
                                     final EWAHCompressedBitmap32... bitmaps) {
        PriorityAggregations.orToContainer(container, bitmaps,
                new PriorityAggregations.ToContainer<EWAHCompressedBitmap32>() {
                    @Override
                    public void combine(final EWAHCompressedBitmap32 a,
                                        final EWAHCompressedBitmap32 b,
                                        final Object out) {
                        a.orToContainer(b, (BitmapStorage32) out);
                    }
                });
    }

    /**
     * Simple algorithm that computes the XOR aggregate.
     *
     * @param bitmaps input bitmaps
     * @return new bitmap containing the aggregate
     */
    public static EWAHCompressedBitmap32 xor(
            final EWAHCompressedBitmap32... bitmaps) {
        return PriorityAggregations.xorArray(bitmaps,
                new java.util.function.Supplier<EWAHCompressedBitmap32>() {
                    @Override
                    public EWAHCompressedBitmap32 get() {
                        return new EWAHCompressedBitmap32();
                    }
                });
    }

    /**
     * Simple algorithm that computes the OR aggregate.
     *
     * @param bitmaps input bitmaps
     * @return new bitmap containing the aggregate
     */
    public static EWAHCompressedBitmap32 or(
            final Iterator<EWAHCompressedBitmap32> bitmaps) {
        return PriorityAggregations.orIterator(bitmaps,
                new java.util.function.Supplier<EWAHCompressedBitmap32>() {
                    @Override
                    public EWAHCompressedBitmap32 get() {
                        return new EWAHCompressedBitmap32();
                    }
                });
    }

    /**
     * Simple algorithm that computes the XOR aggregate.
     *
     * @param bitmaps input bitmaps
     * @return new bitmap containing the aggregate
     */
    public static EWAHCompressedBitmap32 xor(
            final Iterator<EWAHCompressedBitmap32> bitmaps) {
        return PriorityAggregations.xorIterator(bitmaps,
                new java.util.function.Supplier<EWAHCompressedBitmap32>() {
                    @Override
                    public EWAHCompressedBitmap32 get() {
                        return new EWAHCompressedBitmap32();
                    }
                });
    }

    /**
     * Uses a priority queue to compute the xor aggregate.
     *
     * @param container where we write the result
     * @param bitmaps   to be aggregated
     */
    public static void xorToContainer(final BitmapStorage32 container,
                                      final EWAHCompressedBitmap32... bitmaps) {
        PriorityAggregations.xorToContainer(container, bitmaps,
                new PriorityAggregations.ToContainer<EWAHCompressedBitmap32>() {
                    @Override
                    public void combine(final EWAHCompressedBitmap32 a,
                                        final EWAHCompressedBitmap32 b,
                                        final Object out) {
                        a.xorToContainer(b, (BitmapStorage32) out);
                    }
                });
    }

}
