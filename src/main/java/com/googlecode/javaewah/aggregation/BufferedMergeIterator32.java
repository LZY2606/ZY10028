package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.CloneableIterator;
import com.googlecode.javaewah32.EWAHIterator32;

import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 32-bit typed view over the shared buffered OR/XOR state machine.
 */
public final class BufferedMergeIterator32
        implements CloneableIterator<EWAHIterator32> {

    private final BufferedMergeIterator<RlwCursor32> delegate;

    /**
     * @param factory width factory
     * @param op      OR or XOR
     * @param cursors live cursors
     * @param bufSize scratch buffer size in words
     */
    private BufferedMergeIterator32(
            final BufferedMergeIterator<RlwCursor32> delegate) {
        this.delegate = delegate;
    }

    /**
     * @param delegate pre-built width-specialized merge iterator
     */
    public BufferedMergeIterator32(
            final BufferedMergeIteratorBase32 delegate) {
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
        final BufferedMergeIterator<RlwCursor32> copy =
                (BufferedMergeIterator<RlwCursor32>) this.delegate.clone();
        return new BufferedMergeIterator32(copy);
    }
}
