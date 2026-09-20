package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.CloneableIterator;
import com.googlecode.javaewah32.EWAHIterator32;

import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 32-bit typed view over the shared buffered AND state machine.
 */
public final class BufferedAndMergeIterator32
        implements CloneableIterator<EWAHIterator32> {

    private final BufferedAndMergeIterator<RlwCursor32> delegate;

    /**
     * @param factory width factory
     * @param cursors live cursors
     * @param bufSize scratch buffer size in words
     */
    public BufferedAndMergeIterator32(final AggregationFactory32 factory,
                                      final LinkedList<RlwCursor32> cursors,
                                      final int bufSize) {
        this.delegate = new BufferedAndMergeIterator<RlwCursor32>(
                factory, cursors, bufSize);
    }

    private BufferedAndMergeIterator32(
            final BufferedAndMergeIterator<RlwCursor32> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return this.delegate.hasNext();
    }

    @Override
    public EWAHIterator32 next() {
        return (EWAHIterator32) this.delegate.next();
    }

    @Override
    public CloneableIterator<EWAHIterator32> clone()
            throws CloneNotSupportedException {
        @SuppressWarnings("unchecked")
        final BufferedAndMergeIterator<RlwCursor32> copy =
                (BufferedAndMergeIterator<RlwCursor32>) this.delegate.clone();
        return new BufferedAndMergeIterator32(copy);
    }
}
