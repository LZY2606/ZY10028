package com.googlecode.javaewah.aggregation;

import java.util.Arrays;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * {@code long[]} backed specialization of the buffered aggregation scratch
 * array. Its in-place state machine is a final, monomorphic template:
 * all callees ({@link RlwCursor64}) inline to direct {@code long[]} accesses.
 */
public final class LongWordArray extends WordArray<RlwCursor64> {

    private long[] words;

    /**
     * @param size capacity in words
     */
    public LongWordArray(final int size) {
        this.words = new long[size];
    }

    private LongWordArray(final long[] words) {
        this.words = words;
    }

    /**
     * @return the backing array (no copy)
     */
    public long[] raw() {
        return this.words;
    }

    /**
     * @return an independent copy
     */
    public LongWordArray copy() {
        return new LongWordArray(this.words.clone());
    }

    @Override
    public void clear(final int from, final int to) {
        Arrays.fill(this.words, from, to, 0L);
    }

    @Override
    public void flushTo(final AggregationFactory<RlwCursor64> factory,
                        final Object buffer, final int length) {
        final com.googlecode.javaewah.EWAHCompressedBitmap out =
                (com.googlecode.javaewah.EWAHCompressedBitmap) buffer;
        for (int k = 0; k < length; ++k)
            out.addWord(this.words[k]);
        java.util.Arrays.fill(this.words, 0, length, 0L);
    }

    @Override
    public void flushToSink(final WordSink sink, final int length) {
        for (int k = 0; k < length; ++k)
            sink.addWord(this.words[k]);
    }

    @Override
    public void fillAll() {
        Arrays.fill(this.words, ~0L);
    }

    @Override
    public int length() {
        return this.words.length;
    }


}
