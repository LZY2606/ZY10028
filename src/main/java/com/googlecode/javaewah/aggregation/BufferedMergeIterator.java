package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.CloneableIterator;

import java.util.Iterator;
import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Shared state machine for the buffered multiway OR/XOR iterators.
 *
 * Each {@code next()} aggregates one buffer-full of words from every live
 * cursor into the scratch {@link WordArray}, flushes the effective prefix to a
 * width-specific compressed buffer and removes exhausted cursors. The only
 * width-specific pieces are the scratch array and the output buffer, supplied
 * by {@link AggregationFactory}.
 *
 * @param <C> concrete cursor type
 */
public abstract class BufferedMergeIterator<C extends RlwCursor<C>>
        implements CloneableIterator<Object> {

    private final AggregationFactory<C> factory;
    private final AggregateOp op;

    /**
     * @return the width factory
     */
    protected final AggregationFactory<C> factory() {
        return this.factory;
    }

    /**
     * @return the boolean operation
     */
    protected final AggregateOp op() {
        return this.op;
    }

    /**
     * @return the scratch array
     */
    protected final WordArray<C> hardBitmap() {
        return this.hardBitmap;
    }
    private LinkedList<C> cursors;
    private WordArray<C> hardBitmap;
    private Object buffer;

    /**
     * @param factory width-specific factory
     * @param op      OR or XOR
     * @param cursors live input cursors
     * @param bufSize scratch buffer size in words
     */
    public BufferedMergeIterator(final AggregationFactory<C> factory,
                                 final AggregateOp op,
                                 final LinkedList<C> cursors,
                                 final int bufSize) {
        this.factory = factory;
        this.op = op;
        this.cursors = cursors;
        this.hardBitmap = factory.newWordArray(bufSize);
        this.buffer = factory.newBuffer();
    }

    @Override
    public boolean hasNext() {
        return !this.cursors.isEmpty();
    }

    /**
     * Width-specialized in-place step.
     *
     * @param cursor cursor to consume
     * @return index of the first unwritten word
     */
    protected abstract int inplace(C cursor);

    @Override
    public Object next() {
        this.buffer = this.factory.resetBuffer(this.buffer);
        long effective = 0;
        final Iterator<C> iterator = this.cursors.iterator();
        while (iterator.hasNext()) {
            final C cursor = iterator.next();
            if (cursor.size() > 0) {
                final int eff = inplace(cursor);
                if (eff > effective)
                    effective = eff;
            } else {
                iterator.remove();
            }
        }
        this.hardBitmap.flushTo(this.factory, this.buffer, (int) effective);
        return this.factory.iteratorOf(this.buffer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CloneableIterator<Object> clone()
            throws CloneNotSupportedException {
        final BufferedMergeIterator<C> answer =
                (BufferedMergeIterator<C>) super.clone();
        answer.cursors = (LinkedList<C>) this.cursors.clone();
        answer.hardBitmap = this.factory.copyWordArray(this.hardBitmap);
        answer.buffer = this.factory.copyBuffer(this.buffer);
        return answer;
    }
}
