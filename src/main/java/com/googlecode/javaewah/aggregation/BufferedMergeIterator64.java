package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.CloneableIterator;
import com.googlecode.javaewah.EWAHIterator;

import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 64-bit typed view over the shared buffered OR/XOR state machine.
 */
public final class BufferedMergeIterator64
        implements CloneableIterator<EWAHIterator> {

    private final BufferedMergeIterator<RlwCursor64> delegate;

    /**
     * @param factory width factory
     * @param op      OR or XOR
     * @param cursors live cursors
     * @param bufSize scratch buffer size in words
     */
    /**
     * @param delegate pre-built width-specialized merge iterator
     */
    public BufferedMergeIterator64(
            final BufferedMergeIteratorBase64 delegate) {
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
        final BufferedMergeIterator<RlwCursor64> copy =
                (BufferedMergeIterator<RlwCursor64>) this.delegate.clone();
        final BufferedMergeIterator64 answer =
                new BufferedMergeIterator64(copy);
        return answer;
    }

    private BufferedMergeIterator64(
            final BufferedMergeIterator<RlwCursor64> delegate) {
        this.delegate = delegate;
    }
}
