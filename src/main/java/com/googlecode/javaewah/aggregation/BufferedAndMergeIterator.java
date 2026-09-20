package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.CloneableIterator;

import java.util.Iterator;
import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Shared state machine for the buffered multiway AND iterator.
 *
 * The first two cursors are merged up to {@code bufSize * n} words; every
 * remaining cursor is folded into the running result with the bounded pairwise
 * AND merge. As soon as any input cursor is exhausted the whole iterator is
 * finished. The width-specific steps are the factory hooks.
 *
 * @param <C> concrete cursor type
 */
public final class BufferedAndMergeIterator<C extends RlwCursor<C>>
        implements CloneableIterator<Object> {

    private final AggregationFactory<C> factory;
    private LinkedList<C> cursors;
    private final int bufferSize;
    private Object buffer;

    /**
     * @param factory width-specific factory
     * @param cursors live input cursors
     * @param bufSize scratch buffer size in words
     */
    public BufferedAndMergeIterator(final AggregationFactory<C> factory,
                                    final LinkedList<C> cursors,
                                    final int bufSize) {
        this.factory = factory;
        this.cursors = cursors;
        this.bufferSize = bufSize;
        this.buffer = factory.newBuffer();
    }

    @Override
    public boolean hasNext() {
        return !this.cursors.isEmpty();
    }

    @Override
    public Object next() {
        this.buffer = this.factory.resetBuffer(this.buffer);
        final WordSink sink = this.factory.asSink(this.buffer);
        this.factory.andToContainer(sink,
                (long) this.bufferSize * this.cursors.size(),
                this.cursors.get(0), this.cursors.get(1));
        if (this.cursors.size() > 2) {
            final Iterator<C> iterator = this.cursors.iterator();
            iterator.next();
            iterator.next();
            Object tmpBuffer = this.factory.newBuffer();
            while (iterator.hasNext()
                    && this.factory.sizeInBytes(this.buffer) > 0) {
                final WordSink tmpSink = this.factory.asSink(tmpBuffer);
                this.factory.andToContainer(tmpSink,
                        this.factory.cursorOfBuffer(this.buffer),
                        iterator.next());
                this.factory.swapBuffers(this.buffer, tmpBuffer);
                tmpBuffer = this.factory.resetBuffer(tmpBuffer);
            }
        }
        for (final C cursor : this.cursors) {
            if (cursor.size() == 0) {
                this.cursors.clear();
                break;
            }
        }
        return this.factory.iteratorOf(this.buffer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CloneableIterator<Object> clone()
            throws CloneNotSupportedException {
        final BufferedAndMergeIterator<C> answer =
                (BufferedAndMergeIterator<C>) super.clone();
        answer.cursors = (LinkedList<C>) this.cursors.clone();
        answer.buffer = this.factory.copyBuffer(this.buffer);
        return answer;
    }
}
