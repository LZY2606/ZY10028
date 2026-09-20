package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.CloneableIterator;
import com.googlecode.javaewah.EWAHIterator;

import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 64-bit typed view over the shared buffered AND state machine.
 */
public final class BufferedAndMergeIterator64
        implements CloneableIterator<EWAHIterator> {

    private final BufferedAndMergeIterator<RlwCursor64> delegate;

    /**
     * @param factory width factory
     * @param cursors live cursors
     * @param bufSize scratch buffer size in words
     */
    public BufferedAndMergeIterator64(final AggregationFactory64 factory,
                                      final LinkedList<RlwCursor64> cursors,
                                      final int bufSize) {
        this.delegate = new BufferedAndMergeIterator<RlwCursor64>(
                factory, cursors, bufSize);
    }

    private BufferedAndMergeIterator64(
            final BufferedAndMergeIterator<RlwCursor64> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return this.delegate.hasNext();
    }

    @Override
    public EWAHIterator next() {
        return (EWAHIterator) this.delegate.next();
    }

    @Override
    public CloneableIterator<EWAHIterator> clone()
            throws CloneNotSupportedException {
        @SuppressWarnings("unchecked")
        final BufferedAndMergeIterator<RlwCursor64> copy =
                (BufferedAndMergeIterator<RlwCursor64>) this.delegate.clone();
        return new BufferedAndMergeIterator64(copy);
    }
}
