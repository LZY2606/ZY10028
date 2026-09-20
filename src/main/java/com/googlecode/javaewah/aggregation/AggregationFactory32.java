package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah32.BufferedIterator32;
import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import com.googlecode.javaewah32.EWAHIterator32;
import com.googlecode.javaewah32.IteratingBufferedRunningLengthWord32;
import com.googlecode.javaewah32.IteratingRLW32;

import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 32-bit specialization seam used by all shared aggregation state machines.
 * The instance is stateless, final and monomorphic.
 */
public final class AggregationFactory32
        implements AggregationFactory<RlwCursor32> {

    /** Singleton. */
    public static final AggregationFactory32 INSTANCE =
            new AggregationFactory32();

    private AggregationFactory32() {
    }

    @Override
    public IntWordArray newWordArray(final int size) {
        return new IntWordArray(size);
    }

    @Override
    public IntWordArray copyWordArray(final WordArray<RlwCursor32> array) {
        return ((IntWordArray) array).copy();
    }

    @Override
    public EWAHCompressedBitmap32 newBuffer() {
        return new EWAHCompressedBitmap32();
    }

    @Override
    public EWAHCompressedBitmap32 resetBuffer(final Object buffer) {
        final EWAHCompressedBitmap32 bitmap =
                (EWAHCompressedBitmap32) buffer;
        bitmap.clear();
        return bitmap;
    }

    @Override
    public EWAHCompressedBitmap32 copyBuffer(final Object buffer)
            throws CloneNotSupportedException {
        return ((EWAHCompressedBitmap32) buffer).clone();
    }

    @Override
    public BitmapStorageSink32 asSink(final Object buffer) {
        return new BitmapStorageSink32((EWAHCompressedBitmap32) buffer);
    }

    @Override
    public EWAHIterator32 iteratorOf(final Object buffer) {
        return ((EWAHCompressedBitmap32) buffer).getEWAHIterator();
    }

    @Override
    public void andToContainer(final WordSink sink, final long maxWords,
                               final RlwCursor32 first,
                               final RlwCursor32 second) {
        PairMerge.andToContainer(sink, maxWords, first, second);
    }

    @Override
    public void andToContainer(final WordSink sink, final RlwCursor32 first,
                               final RlwCursor32 second) {
        PairMerge.andToContainer(sink, first, second);
    }

    @Override
    public RlwCursor32 cursorOfBuffer(final Object buffer) {
        return new RlwCursor32(
                ((EWAHCompressedBitmap32) buffer).getIteratingRLW());
    }

    @Override
    public void swapBuffers(final Object a, final Object b) {
        ((EWAHCompressedBitmap32) a).swap((EWAHCompressedBitmap32) b);
    }

    @Override
    public int sizeInBytes(final Object buffer) {
        return ((EWAHCompressedBitmap32) buffer).sizeInBytes();
    }

    @Override
    public RlwCursor32 cursorOfBitmap(final Object bitmap) {
        return new RlwCursor32(
                new IteratingBufferedRunningLengthWord32(
                        (EWAHCompressedBitmap32) bitmap));
    }

    @Override
    public int sizeInBits(final Object bitmap) {
        return ((EWAHCompressedBitmap32) bitmap).sizeInBits();
    }

    @Override
    public void fillAll(final WordArray<RlwCursor32> array) {
        ((IntWordArray) array).fillAll();
    }

    @Override
    public Object bitmapOfSink(final WordSink sink) {
        if (sink instanceof BitmapStorageSink32) {
            return bitmapStorage32(sink);
        }
        throw new IllegalArgumentException("unexpected sink type");
    }

    private static EWAHCompressedBitmap32 bitmapStorage32(final WordSink sink) {
        return (EWAHCompressedBitmap32) ((BitmapStorageSink32) sink).storage();
    }

    @Override
    public void runBufferedOrXor(final WordSink sink, final AggregateOp op,
                                 final int bufSize,
                                 final Object[] bitmaps, final int range) {
        final EWAHCompressedBitmap32[] typed =
                new EWAHCompressedBitmap32[bitmaps.length];
        System.arraycopy(bitmaps, 0, typed, 0, bitmaps.length);
        final EWAHCompressedBitmap32 container =
                (EWAHCompressedBitmap32) bitmapOfSink(sink);
        if (op == AggregateOp.XOR)
            XorDriver32.run(container, bufSize, typed, range);
        else
            OrDriver32.run(container, bufSize, typed, range);
    }

    /**
     * Build the public buffered OR iterator.
     *
     * @param cursors input cursors
     * @param bufSize scratch size in words
     * @return buffered iterator
     */
    public BufferedIterator32 newBufferedOr(
            final LinkedList<IteratingRLW32> cursors, final int bufSize) {
        final LinkedList<RlwCursor32> wrapped = wrap(cursors);
        return new BufferedIterator32(new BufferedMergeIterator32(
                new BufferedMergeIteratorBase32(
                        this, AggregateOp.OR, wrapped, bufSize)));
    }

    /**
     * Build the public buffered XOR iterator.
     *
     * @param cursors input cursors
     * @param bufSize scratch size in words
     * @return buffered iterator
     */
    public BufferedIterator32 newBufferedXor(
            final LinkedList<IteratingRLW32> cursors, final int bufSize) {
        final LinkedList<RlwCursor32> wrapped = wrap(cursors);
        return new BufferedIterator32(new BufferedMergeIterator32(
                new BufferedMergeIteratorBase32(
                        this, AggregateOp.XOR, wrapped, bufSize)));
    }

    /**
     * Build the public buffered AND iterator.
     *
     * @param cursors input cursors
     * @param bufSize scratch size in words
     * @return buffered iterator
     */
    public BufferedIterator32 newBufferedAnd(
            final LinkedList<IteratingRLW32> cursors, final int bufSize) {
        final LinkedList<RlwCursor32> wrapped = wrap(cursors);
        return new BufferedIterator32(new BufferedAndMergeIterator32(
                this, wrapped, bufSize));
    }

    private static LinkedList<RlwCursor32> wrap(
            final LinkedList<IteratingRLW32> cursors) {
        final LinkedList<RlwCursor32> wrapped =
                new LinkedList<RlwCursor32>();
        for (final IteratingRLW32 cursor : cursors)
            wrapped.add(new RlwCursor32(cursor));
        return wrapped;
    }
}
