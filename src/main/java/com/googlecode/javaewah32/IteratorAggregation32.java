package com.googlecode.javaewah32;

import com.googlecode.javaewah.CloneableIterator;
import com.googlecode.javaewah.aggregation.AggregateOp;
import com.googlecode.javaewah.aggregation.AggregationFactory32;
import com.googlecode.javaewah.aggregation.BitmapStorageSink32;
import com.googlecode.javaewah.aggregation.IntWordArray;
import com.googlecode.javaewah.aggregation.PairMerge;
import com.googlecode.javaewah.aggregation.RlwCursor32;

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
 * 32-bit facade that wires the shared engine to {@link IteratingRLW32} and
 * {@link BitmapStorage32}.
 */
public final class IteratorAggregation32 {

    /** Private constructor to prevent instantiation */
    private IteratorAggregation32() {}

    /**
     * @param x iterator to negate
     * @return negated version of the iterator
     */
    public static IteratingRLW32 not(final IteratingRLW32 x) {
        return new IteratingRLW32() {

            @Override
            public boolean next() {
                return x.next();
            }

            @Override
            public int getLiteralWordAt(int index) {
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
            public int size() {
                return x.size();
            }

            @Override
            public int getRunningLength() {
                return x.getRunningLength();
            }

            @Override
            public void discardFirstWords(int y) {
                x.discardFirstWords(y);
            }

            @Override
            public void discardRunningWords() {
                x.discardRunningWords();
            }

            @Override
            public IteratingRLW32 clone()
                    throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public void discardLiteralWords(int y) {
                x.discardLiteralWords(y);
            }
        };
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al iterators to aggregate
     * @return and aggregate
     */
    public static IteratingRLW32 bufferedand(final IteratingRLW32... al) {
        return bufferedand(DEFAULT_MAX_BUF_SIZE, al);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al      iterators to aggregate
     * @param bufSize size of the internal buffer used by the iterator in
     *                64-bit words
     * @return and aggregate
     */
    public static IteratingRLW32 bufferedand(final int bufSize,
                                             final IteratingRLW32... al) {
        if (al.length == 0)
            throw new IllegalArgumentException(
                    "Need at least one iterator");
        if (al.length == 1)
            return al[0];
        final LinkedList<IteratingRLW32> basell = new LinkedList<IteratingRLW32>();
        Collections.addAll(basell, al);
        return AggregationFactory32.INSTANCE.newBufferedAnd(basell, bufSize);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al iterators to aggregate
     * @return or aggregate
     */
    public static IteratingRLW32 bufferedor(final IteratingRLW32... al) {
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
    public static IteratingRLW32 bufferedor(final int bufSize,
                                            final IteratingRLW32... al) {
        if (al.length == 0)
            throw new IllegalArgumentException(
                    "Need at least one iterator");
        if (al.length == 1)
            return al[0];

        final LinkedList<IteratingRLW32> basell = new LinkedList<IteratingRLW32>();
        Collections.addAll(basell, al);
        return AggregationFactory32.INSTANCE.newBufferedOr(basell, bufSize);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al iterators to aggregate
     * @return xor aggregate
     */
    public static IteratingRLW32 bufferedxor(final IteratingRLW32... al) {
        return bufferedxor(DEFAULT_MAX_BUF_SIZE, al);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al      iterators to aggregate
     * @param bufSize size of the internal buffer used by the iterator in
     *                64-bit words
     * @return xor aggregate
     */
    public static IteratingRLW32 bufferedxor(final int bufSize,
                                             final IteratingRLW32... al) {
        if (al.length == 0)
            throw new IllegalArgumentException(
                    "Need at least one iterator");
        if (al.length == 1)
            return al[0];

        final LinkedList<IteratingRLW32> basell = new LinkedList<IteratingRLW32>();
        Collections.addAll(basell, al);
        return AggregationFactory32.INSTANCE.newBufferedXor(basell, bufSize);
    }

    /**
     * Write out the content of the iterator, but as if it were all zeros.
     *
     * @param container where we write
     * @param i         the iterator
     */
    protected static void dischargeAsEmpty(final BitmapStorage32 container,
                                           final IteratingRLW32 i) {
        PairMerge.dischargeAsEmpty(new BitmapStorageSink32(container),
                new RlwCursor32(i));
    }

    /**
     * Write out up to max words, returns how many were written
     *
     * @param container target for writes
     * @param i         source of data
     * @param max       maximal number of writes
     * @return how many written
     */
    protected static int discharge(final BitmapStorage32 container,
                                   IteratingRLW32 i, int max) {
        return (int) PairMerge.discharge(new BitmapStorageSink32(container),
                new RlwCursor32(i), max, false);
    }

    /**
     * Write out up to max negated words, returns how many were written.
     *
     * Historically the 32-bit implementation copied the words unchanged
     * (and reported the fill bit unchanged); the shared state machine keeps
     * that exact behavior with {@code negate=false}.
     *
     * @param container target for writes
     * @param i         source of data
     * @param max       maximal number of writes
     * @return how many written
     */
    protected static int dischargeNegated(final BitmapStorage32 container,
                                          IteratingRLW32 i, int max) {
        return (int) PairMerge.discharge(new BitmapStorageSink32(container),
                new RlwCursor32(i), max, false);
    }

    static void andToContainer(final BitmapStorage32 container,
                               int desiredrlwcount, final IteratingRLW32 rlwi,
                               IteratingRLW32 rlwj) {
        PairMerge.andToContainer(new BitmapStorageSink32(container),
                desiredrlwcount, new RlwCursor32(rlwi),
                new RlwCursor32(rlwj));
    }

    static void andToContainer(final BitmapStorage32 container,
                               final IteratingRLW32 rlwi, IteratingRLW32 rlwj) {
        PairMerge.andToContainer(new BitmapStorageSink32(container),
                new RlwCursor32(rlwi), new RlwCursor32(rlwj));
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
    public static void xorToContainer(final BitmapStorage32 container,
                                      int desiredrlwcount, final IteratingRLW32 rlwi,
                                      IteratingRLW32 rlwj) {
        PairMerge.xorToContainer(new BitmapStorageSink32(container),
                desiredrlwcount, new RlwCursor32(rlwi),
                new RlwCursor32(rlwj), false);
    }

    protected static int inplaceor(int[] bitmap, IteratingRLW32 i) {
        return inplace(bitmap, i, AggregateOp.OR);
    }

    protected static int inplacexor(int[] bitmap, IteratingRLW32 i) {
        return inplace(bitmap, i, AggregateOp.XOR);
    }

    protected static int inplaceand(int[] bitmap, IteratingRLW32 i) {
        return inplace(bitmap, i, AggregateOp.AND);
    }

    private static int inplace(final int[] bitmap, final IteratingRLW32 i,
                               final AggregateOp op) {
        switch (op) {
            case OR:
                return com.googlecode.javaewah.aggregation.InPlaceOps32.or(
                        bitmap, i);
            case XOR:
                return com.googlecode.javaewah.aggregation.InPlaceOps32.xor(
                        bitmap, i);
            default:
                return com.googlecode.javaewah.aggregation.InPlaceOps32.and(
                        bitmap, i);
        }
    }

    /**
     * An optimization option. Larger values may improve speed, but at the
     * expense of memory.
     */
    public static final int DEFAULT_MAX_BUF_SIZE = 65536;

}
