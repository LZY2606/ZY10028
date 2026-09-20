package com.googlecode.javaewah;

import com.googlecode.javaewah.aggregation.AggregateOp;
import com.googlecode.javaewah.aggregation.AggregationFactory64;
import com.googlecode.javaewah.aggregation.BitmapStorageSink64;
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
 * EWAHCompressedBitmap class.
 *
 * The buffered scheduling state machines are shared with the 32-bit
 * implementation in {@link com.googlecode.javaewah.aggregation}. The
 * priority-queue scheduling below is the 64-bit monomorphic instantiation of
 * the single shared scheduling rule (repeatedly combine the two smallest
 * bitmaps): the pairwise combine is a direct {@code or}/{@code xor} call (as
 * in the original code) so the JIT can inline it while merging large
 * intermediate bitmaps.
 *
 * @author Daniel Lemire
 */
public final class FastAggregation {

    /** Private constructor to prevent instantiation */
    private FastAggregation() {}

    /**
     * Compute the and aggregate using a temporary uncompressed bitmap.
     *
     * @param bitmaps the source bitmaps
     * @param bufSize buffer size used during the computation in 64-bit
     *                words (per input bitmap)
     * @return the or aggregate.
     */
    public static EWAHCompressedBitmap bufferedand(final int bufSize,
                                                   final EWAHCompressedBitmap... bitmaps) {
        EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
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
            final BitmapStorage container, final int bufSize,
            final EWAHCompressedBitmap... bitmaps) {
        BufferedAggregations.bufferedAnd(AggregationFactory64.INSTANCE,
                new BitmapStorageSink64(container), bufSize, bitmaps);
    }

    /**
     * Compute the or aggregate using a temporary uncompressed bitmap.
     *
     * @param bitmaps the source bitmaps
     * @param bufSize buffer size used during the computation in 64-bit
     *                words
     * @return the or aggregate.
     */
    public static EWAHCompressedBitmap bufferedor(final int bufSize,
                                                  final EWAHCompressedBitmap... bitmaps) {
        EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
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
            final BitmapStorage container, final int bufSize,
            final EWAHCompressedBitmap... bitmaps) {
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
    public static EWAHCompressedBitmap bufferedxor(final int bufSize,
                                                   final EWAHCompressedBitmap... bitmaps) {
        EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
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
            final BitmapStorage container, final int bufSize,
            final EWAHCompressedBitmap... bitmaps) {
        sortedBuffered(container, bufSize, bitmaps, AggregateOp.XOR);
    }

    private static void sortedBuffered(final BitmapStorage container,
                                       final int bufSize,
                                       final EWAHCompressedBitmap[] bitmaps,
                                       final AggregateOp op) {
        int range = 0;
        final EWAHCompressedBitmap[] sorted = bitmaps.clone();
        Arrays.sort(sorted, new Comparator<EWAHCompressedBitmap>() {
            @Override
            public int compare(EWAHCompressedBitmap a,
                               EWAHCompressedBitmap b) {
                return b.sizeInBits() - a.sizeInBits();
            }
        });
        for (final EWAHCompressedBitmap bitmap : sorted) {
            if (bitmap.sizeInBits() > range)
                range = bitmap.sizeInBits();
        }
        BufferedAggregations.bufferedOrXor(AggregationFactory64.INSTANCE,
                new BitmapStorageSink64(container), op, bufSize, sorted, range);
    }

    /**
     * Uses a priority queue to compute the or aggregate.
     *
     * @param <T>     a class extending LogicalElement (like a compressed
     *                bitmap)
     * @param bitmaps bitmaps to be aggregated
     * @return the or aggregate
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends LogicalElement> T or(T... bitmaps) {
        return PriorityAggregations.orArray(bitmaps, null);
    }

    /**
     * Uses a priority queue to compute the or aggregate.
     *
     * @param container where we write the result
     * @param bitmaps   to be aggregated
     */
    public static void orToContainer(final BitmapStorage container,
                                     final EWAHCompressedBitmap... bitmaps) {
        PriorityAggregations.orToContainer(container, bitmaps,
                new PriorityAggregations.ToContainer<EWAHCompressedBitmap>() {
                    @Override
                    public void combine(final EWAHCompressedBitmap a,
                                        final EWAHCompressedBitmap b,
                                        final Object out) {
                        a.orToContainer(b, (BitmapStorage) out);
                    }
                });
    }

    /**
     * Simple algorithm that computes the OR aggregate.
     *
     * @param bitmaps input bitmaps
     * @return new bitmap containing the aggregate
     */
    public static EWAHCompressedBitmap or(
            final EWAHCompressedBitmap... bitmaps) {
        return PriorityAggregations.orArray(bitmaps,
                new java.util.function.Supplier<EWAHCompressedBitmap>() {
                    @Override
                    public EWAHCompressedBitmap get() {
                        return new EWAHCompressedBitmap();
                    }
                });
    }

    /**
     * Simple algorithm that computes the XOR aggregate.
     *
     * @param bitmaps input bitmaps
     * @return new bitmap containing the aggregate
     */
    public static EWAHCompressedBitmap xor(
            final EWAHCompressedBitmap... bitmaps) {
        return PriorityAggregations.xorArray(bitmaps,
                new java.util.function.Supplier<EWAHCompressedBitmap>() {
                    @Override
                    public EWAHCompressedBitmap get() {
                        return new EWAHCompressedBitmap();
                    }
                });
    }

    /**
     * Simple algorithm that computes the OR aggregate.
     *
     * @param bitmaps input bitmaps
     * @return new bitmap containing the aggregate
     */
    public static EWAHCompressedBitmap or(
            final Iterator<EWAHCompressedBitmap> bitmaps) {
        return PriorityAggregations.orIterator(bitmaps,
                new java.util.function.Supplier<EWAHCompressedBitmap>() {
                    @Override
                    public EWAHCompressedBitmap get() {
                        return new EWAHCompressedBitmap();
                    }
                });
    }

    /**
     * Simple algorithm that computes the XOR aggregate.
     *
     * @param bitmaps input bitmaps
     * @return new bitmap containing the aggregate
     */
    public static EWAHCompressedBitmap xor(
            final Iterator<EWAHCompressedBitmap> bitmaps) {
        return PriorityAggregations.xorIterator(bitmaps,
                new java.util.function.Supplier<EWAHCompressedBitmap>() {
                    @Override
                    public EWAHCompressedBitmap get() {
                        return new EWAHCompressedBitmap();
                    }
                });
    }

    /**
     * Uses a priority queue to compute the xor aggregate.
     *
     * @param <T>     a class extending LogicalElement (like a compressed
     *                bitmap)
     * @param bitmaps bitmaps to be aggregated
     * @return the xor aggregate
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends LogicalElement> T xor(T... bitmaps) {
        return PriorityAggregations.xorArray(bitmaps, null);
    }

    /**
     * Uses a priority queue to compute the xor aggregate.
     *
     * @param container where we write the result
     * @param bitmaps   to be aggregated
     */
    public static void xorToContainer(final BitmapStorage container,
                                      final EWAHCompressedBitmap... bitmaps) {
        PriorityAggregations.xorToContainer(container, bitmaps,
                new PriorityAggregations.ToContainer<EWAHCompressedBitmap>() {
                    @Override
                    public void combine(final EWAHCompressedBitmap a,
                                        final EWAHCompressedBitmap b,
                                        final Object out) {
                        a.xorToContainer(b, (BitmapStorage) out);
                    }
                });
    }

}
