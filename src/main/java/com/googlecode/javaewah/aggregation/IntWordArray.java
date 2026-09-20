package com.googlecode.javaewah.aggregation;

import java.util.Arrays;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * {@code int[]} backed specialization of the buffered aggregation scratch
 * array. Its in-place state machine is a final, monomorphic template:
 * all callees ({@link RlwCursor32}) inline to direct {@code int[]} accesses.
 */
public final class IntWordArray extends WordArray<RlwCursor32> {

    private int[] words;

    /**
     * @param size capacity in words
     */
    public IntWordArray(final int size) {
        this.words = new int[size];
    }

    private IntWordArray(final int[] words) {
        this.words = words;
    }

    /**
     * @return the backing array (no copy)
     */
    public int[] raw() {
        return this.words;
    }

    /**
     * @return an independent copy
     */
    public IntWordArray copy() {
        return new IntWordArray(this.words.clone());
    }

    @Override
    public void clear(final int from, final int to) {
        Arrays.fill(this.words, from, to, 0);
    }

    @Override
    public void flushTo(final AggregationFactory<RlwCursor32> factory,
                        final Object buffer, final int length) {
        final com.googlecode.javaewah32.EWAHCompressedBitmap32 out =
                (com.googlecode.javaewah32.EWAHCompressedBitmap32) buffer;
        for (int k = 0; k < length; ++k)
            out.addWord(this.words[k]);
        java.util.Arrays.fill(this.words, 0, length, 0);
    }

    @Override
    public void flushToSink(final WordSink sink, final int length) {
        for (int k = 0; k < length; ++k)
            sink.addWord(this.words[k]);
    }

    @Override
    public void fillAll() {
        Arrays.fill(this.words, ~0);
    }

    @Override
    public int length() {
        return this.words.length;
    }


}
