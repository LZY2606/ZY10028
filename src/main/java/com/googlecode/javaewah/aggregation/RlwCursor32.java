package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah32.IteratingRLW32;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 32-bit specialization of {@link RlwCursor}: a thin, final wrapper over an
 * {@link IteratingRLW32}. {@code int} words cross the shared state-machine
 * boundary as raw 32-bit patterns stored in a {@code long}; they never cross
 * boxed as {@link Integer}.
 */
public final class RlwCursor32 extends RlwCursor<RlwCursor32> {

    /** Wrapped cursor. */
    final IteratingRLW32 rlw;

    /**
     * @param rlw cursor to wrap
     */
    public RlwCursor32(final IteratingRLW32 rlw) {
        this.rlw = rlw;
    }

    @Override
    public boolean next() {
        return this.rlw.next();
    }

    @Override
    public long getLiteralWordAt(final int index) {
        return this.rlw.getLiteralWordAt(index) & 0xffffffffL;
    }

    @Override
    public int getNumberOfLiteralWords() {
        return this.rlw.getNumberOfLiteralWords();
    }

    @Override
    public boolean getRunningBit() {
        return this.rlw.getRunningBit();
    }

    @Override
    public long size() {
        return this.rlw.size();
    }

    @Override
    public long getRunningLength() {
        return this.rlw.getRunningLength();
    }

    @Override
    public void discardFirstWords(final long x) {
        this.rlw.discardFirstWords((int) x);
    }

    @Override
    public void discardRunningWords() {
        this.rlw.discardRunningWords();
    }

    @Override
    public void discardLiteralWords(final long x) {
        this.rlw.discardLiteralWords((int) x);
    }
}
