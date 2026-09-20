package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.IteratingRLW;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 64-bit specialization of {@link RlwCursor}: a thin, final wrapper over an
 * {@link IteratingRLW}. Every method is monomorphic at this type so the JIT
 * inlines it into the shared state-machine loops.
 */
public final class RlwCursor64 extends RlwCursor<RlwCursor64> {

    /** Wrapped cursor. */
    final IteratingRLW rlw;

    /**
     * @param rlw cursor to wrap
     */
    public RlwCursor64(final IteratingRLW rlw) {
        this.rlw = rlw;
    }

    @Override
    public boolean next() {
        return this.rlw.next();
    }

    @Override
    public long getLiteralWordAt(final int index) {
        return this.rlw.getLiteralWordAt(index);
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
        this.rlw.discardFirstWords(x);
    }

    @Override
    public void discardRunningWords() {
        this.rlw.discardRunningWords();
    }

    @Override
    public void discardLiteralWords(final long x) {
        this.rlw.discardLiteralWords(x);
    }
}
