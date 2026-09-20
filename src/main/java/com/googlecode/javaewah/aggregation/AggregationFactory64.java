package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.BufferedIterator;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.EWAHIterator;
import com.googlecode.javaewah.IteratingBufferedRunningLengthWord;
import com.googlecode.javaewah.IteratingRLW;

import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 64-bit specialization seam used by all shared aggregation state machines.
 * The instance is stateless and monomorphic.
 */
public final class AggregationFactory64
        implements AggregationFactory<RlwCursor64> {

    /** Singleton. */
    public static final AggregationFactory64 INSTANCE =
            new AggregationFactory64();

    private AggregationFactory64() {
    }

    @Override
    public LongWordArray newWordArray(final int size) {
        return new LongWordArray(size);
    }

    @Override
    public LongWordArray copyWordArray(final WordArray<RlwCursor64> array) {
        return ((LongWordArray) array).copy();
    }

    @Override
    public EWAHCompressedBitmap newBuffer() {
        return new EWAHCompressedBitmap();
    }

    @Override
    public EWAHCompressedBitmap resetBuffer(final Object buffer) {
        final EWAHCompressedBitmap bitmap = (EWAHCompressedBitmap) buffer;
        bitmap.clear();
        return bitmap;
    }

    @Override
    public EWAHCompressedBitmap copyBuffer(final Object buffer)
            throws CloneNotSupportedException {
        return ((EWAHCompressedBitmap) buffer).clone();
    }

    @Override
    public BitmapStorageSink64 asSink(final Object buffer) {
        return new BitmapStorageSink64((EWAHCompressedBitmap) buffer);
    }

    @Override
    public EWAHIterator iteratorOf(final Object buffer) {
        return ((EWAHCompressedBitmap) buffer).getEWAHIterator();
    }

    @Override
    public void andToContainer(final WordSink sink, final long maxWords,
                               final RlwCursor64 first,
                               final RlwCursor64 second) {
        PairMerge.andToContainer(sink, maxWords, first, second);
    }

    @Override
    public void andToContainer(final WordSink sink, final RlwCursor64 first,
                               final RlwCursor64 second) {
        PairMerge.andToContainer(sink, first, second);
    }

    @Override
    public RlwCursor64 cursorOfBuffer(final Object buffer) {
        return new RlwCursor64(
                ((EWAHCompressedBitmap) buffer).getIteratingRLW());
    }

    @Override
    public void swapBuffers(final Object a, final Object b) {
        ((EWAHCompressedBitmap) a).swap((EWAHCompressedBitmap) b);
    }

    @Override
    public int sizeInBytes(final Object buffer) {
        return ((EWAHCompressedBitmap) buffer).sizeInBytes();
    }

    @Override
    public RlwCursor64 cursorOfBitmap(final Object bitmap) {
        return new RlwCursor64(
                new IteratingBufferedRunningLengthWord(
                        (EWAHCompressedBitmap) bitmap));
    }

    @Override
    public int sizeInBits(final Object bitmap) {
        return ((EWAHCompressedBitmap) bitmap).sizeInBits();
    }

    @Override
    public void fillAll(final WordArray<RlwCursor64> array) {
        ((LongWordArray) array).fillAll();
    }

    @Override
    public Object bitmapOfSink(final WordSink sink) {
        if (sink instanceof BitmapStorageSink64) {
            return (EWAHCompressedBitmap) ((BitmapStorageSink64) sink).storage();
        }
        throw new IllegalArgumentException("unexpected sink type");
    }

    @Override
    public void runBufferedOrXor(final WordSink sink, final AggregateOp op,
                                 final int bufSize,
                                 final Object[] bitmaps, final int range) {
        final EWAHCompressedBitmap[] typed =
                new EWAHCompressedBitmap[bitmaps.length];
        System.arraycopy(bitmaps, 0, typed, 0, bitmaps.length);
        final EWAHCompressedBitmap container =
                (EWAHCompressedBitmap) bitmapOfSink(sink);
        if (op == AggregateOp.XOR)
            XorDriver64.run(container, bufSize, typed, range);
        else
            OrDriver64.run(container, bufSize, typed, range);
    }

    /**
     * Build the public buffered OR iterator.
     *
     * @param cursors input cursors
     * @param bufSize scratch size in words
     * @return buffered iterator
     */
    public BufferedIterator newBufferedOr(
            final LinkedList<IteratingRLW> cursors, final int bufSize) {
        final LinkedList<RlwCursor64> wrapped = wrap(cursors);
        return new BufferedIterator(new BufferedMergeIterator64(
                new BufferedMergeIteratorBase64(
                        this, AggregateOp.OR, wrapped, bufSize)));
    }

    /**
     * Build the public buffered XOR iterator.
     *
     * @param cursors input cursors
     * @param bufSize scratch size in words
     * @return buffered iterator
     */
    public BufferedIterator newBufferedXor(
            final LinkedList<IteratingRLW> cursors, final int bufSize) {
        final LinkedList<RlwCursor64> wrapped = wrap(cursors);
        return new BufferedIterator(new BufferedMergeIterator64(
                new BufferedMergeIteratorBase64(
                        this, AggregateOp.XOR, wrapped, bufSize)));
    }

    /**
     * Build the public buffered AND iterator.
     *
     * @param cursors input cursors
     * @param bufSize scratch size in words
     * @return buffered iterator
     */
    public BufferedIterator newBufferedAnd(
            final LinkedList<IteratingRLW> cursors, final int bufSize) {
        final LinkedList<RlwCursor64> wrapped = wrap(cursors);
        return new BufferedIterator(new BufferedAndMergeIterator64(
                this, wrapped, bufSize));
    }

    private static LinkedList<RlwCursor64> wrap(
            final LinkedList<IteratingRLW> cursors) {
        final LinkedList<RlwCursor64> wrapped =
                new LinkedList<RlwCursor64>();
        for (final IteratingRLW cursor : cursors)
            wrapped.add(new RlwCursor64(cursor));
        return wrapped;
    }
}
