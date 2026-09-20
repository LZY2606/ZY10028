package com.googlecode.javaewah;

import com.googlecode.javaewah.aggregation.AggregateOp;
import com.googlecode.javaewah.aggregation.AggregationFactory64;
import com.googlecode.javaewah.aggregation.BitmapStorageSink64;
import com.googlecode.javaewah.aggregation.LongWordArray;
import com.googlecode.javaewah.aggregation.PairMerge;
import com.googlecode.javaewah.aggregation.RlwCursor64;

import java.util.Collections;
import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Set of helper functions to aggregate bitmaps.
 *
 * The aggregation state machines now live once in
 * {@link com.googlecode.javaewah.aggregation}; this class is the width-specific
 * 64-bit facade that wires the shared engine to {@link IteratingRLW} and
 * {@link BitmapStorage}.
 */
public final class IteratorAggregation {

    /** Private constructor to prevent instantiation */
    private IteratorAggregation() {}

    /**
     * @param x iterator to negate
     * @return negated version of the iterator
     */
    public static IteratingRLW not(final IteratingRLW x) {
        return new IteratingRLW() {

            @Override
            public boolean next() {
                return x.next();
            }

            @Override
            public long getLiteralWordAt(int index) {
                return ~x.getLiteralWordAt(index);
            }

            @Override
            public int getNumberOfLiteralWords() {
                return x.getNumberOfLiteralWords();
            }

            @Override
            public boolean getRunningBit() {
                return !x.getRunningBit();
            }

            @Override
            public long size() {
                return x.size();
            }

            @Override
            public long getRunningLength() {
                return x.getRunningLength();
            }

            @Override
            public void discardFirstWords(long y) {
                x.discardFirstWords(y);
            }

            @Override
            public void discardRunningWords() {
                x.discardRunningWords();
            }

            @Override
            public IteratingRLW clone()
                    throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public void discardLiteralWords(long y) {
                x.discardLiteralWords(y);
            }
        };
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al set of iterators to aggregate
     * @return and aggregate
     */
    public static IteratingRLW bufferedand(final IteratingRLW... al) {
        return bufferedand(DEFAULT_MAX_BUF_SIZE, al);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al      set of iterators to aggregate
     * @param bufSize size of the internal buffer used by the iterator in
     *                64-bit words (per input iterator)
     * @return and aggregate
     */
    public static IteratingRLW bufferedand(final int bufSize, final IteratingRLW... al) {
        if (al.length == 0)
            throw new IllegalArgumentException("Need at least one iterator");
        if (al.length == 1)
            return al[0];
        final LinkedList<IteratingRLW> basell = new LinkedList<IteratingRLW>();
        Collections.addAll(basell, al);
        return AggregationFactory64.INSTANCE.newBufferedAnd(basell, bufSize);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al set of iterators to aggregate
     * @return or aggregate
     */
    public static IteratingRLW bufferedor(final IteratingRLW... al) {
        return bufferedor(DEFAULT_MAX_BUF_SIZE, al);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al      iterators to aggregate
     * @param bufSize size of the internal buffer used by the iterator in
     *                64-bit words
     * @return or aggregate
     */
    public static IteratingRLW bufferedor(final int bufSize,
                                          final IteratingRLW... al) {
        if (al.length == 0)
            throw new IllegalArgumentException("Need at least one iterator");
        if (al.length == 1)
            return al[0];

        final LinkedList<IteratingRLW> basell = new LinkedList<IteratingRLW>();
        Collections.addAll(basell, al);
        return AggregationFactory64.INSTANCE.newBufferedOr(basell, bufSize);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al set of iterators to aggregate
     * @return xor aggregate
     */
    public static IteratingRLW bufferedxor(final IteratingRLW... al) {
        return bufferedxor(DEFAULT_MAX_BUF_SIZE, al);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al      iterators to aggregate
     * @param bufSize size of the internal buffer used by the iterator in 64-bit words
     * @return xor aggregate
     */
    public static IteratingRLW bufferedxor(final int bufSize, final IteratingRLW... al) {
        if (al.length == 0)
            throw new IllegalArgumentException("Need at least one iterator");
        if (al.length == 1)
            return al[0];

        final LinkedList<IteratingRLW> basell = new LinkedList<IteratingRLW>();
        Collections.addAll(basell, al);

        return AggregationFactory64.INSTANCE.newBufferedXor(basell, bufSize);
    }

    /**
     * Write out the content of the iterator, but as if it were all zeros.
     *
     * @param container where we write
     * @param i         the iterator
     */
    protected static void dischargeAsEmpty(final BitmapStorage container,
                                           final IteratingRLW i) {
        PairMerge.dischargeAsEmpty(new BitmapStorageSink64(container),
                new RlwCursor64(i));
    }

    /**
     * Write out up to max words, returns how many were written
     *
     * @param container target for writes
     * @param i         source of data
     * @param max       maximal number of writes
     * @return how many written
     */
    protected static long discharge(final BitmapStorage container, IteratingRLW i, long max) {
        return PairMerge.discharge(new BitmapStorageSink64(container),
                new RlwCursor64(i), max, false);
    }

    /**
     * Write out up to max negated words, returns how many were written
     *
     * @param container target for writes
     * @param i         source of data
     * @param max       maximal number of writes
     * @return how many written
     */
    protected static long dischargeNegated(final BitmapStorage container, IteratingRLW i, long max) {
        return PairMerge.discharge(new BitmapStorageSink64(container),
                new RlwCursor64(i), max, true);
    }

    static void andToContainer(final BitmapStorage container,
                               int desiredrlwcount, final IteratingRLW rlwi, IteratingRLW rlwj) {
        PairMerge.andToContainer(new BitmapStorageSink64(container),
                desiredrlwcount, new RlwCursor64(rlwi), new RlwCursor64(rlwj));
    }

    static void andToContainer(final BitmapStorage container,
                               final IteratingRLW rlwi, IteratingRLW rlwj) {
        PairMerge.andToContainer(new BitmapStorageSink64(container),
                new RlwCursor64(rlwi), new RlwCursor64(rlwj));
    }

    /**
     * Compute the first few words of the XOR aggregate between two
     * iterators.
     *
     * @param container       where to write
     * @param desiredrlwcount number of words to be written (max)
     * @param rlwi            first iterator to aggregate
     * @param rlwj            second iterator to aggregate
     */
    public static void xorToContainer(final BitmapStorage container,
                                      int desiredrlwcount, final IteratingRLW rlwi,
                                      final IteratingRLW rlwj) {
        PairMerge.xorToContainer(new BitmapStorageSink64(container),
                desiredrlwcount, new RlwCursor64(rlwi), new RlwCursor64(rlwj),
                true);
    }

    protected static int inplaceor(long[] bitmap, IteratingRLW i) {
        return inplace(bitmap, i, AggregateOp.OR);
    }

    protected static int inplacexor(long[] bitmap, IteratingRLW i) {
        return inplace(bitmap, i, AggregateOp.XOR);
    }

    protected static int inplaceand(long[] bitmap, IteratingRLW i) {
        return inplace(bitmap, i, AggregateOp.AND);
    }

    private static int inplace(final long[] bitmap, final IteratingRLW i,
                               final AggregateOp op) {
        switch (op) {
            case OR:
                return com.googlecode.javaewah.aggregation.InPlaceOps64.or(
                        bitmap, i);
            case XOR:
                return com.googlecode.javaewah.aggregation.InPlaceOps64.xor(
                        bitmap, i);
            default:
                return com.googlecode.javaewah.aggregation.InPlaceOps64.and(
                        bitmap, i);
        }
    }

    /**
     * An optimization option. Larger values may improve speed, but at the
     * expense of memory.
     */
    public static final int DEFAULT_MAX_BUF_SIZE = 65536;

}
