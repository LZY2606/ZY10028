package com.googlecode.javaewah.aggregation;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Backing scratch array used by the buffered aggregation state machines.
 *
 * The word-touching aggregation steps are the width-specific static machines
 * {@link InPlaceOps64} and {@link InPlaceOps32}; they operate on the concrete
 * backing array ({@code long[]} / {@code int[]}) obtained from
 * {@code raw()}. This class only owns the array lifecycle and the output
 * flush. No individual word is ever boxed.
 *
 * @param <C> concrete cursor type
 */
public abstract class WordArray<C extends RlwCursor<C>> {

    /**
     * Clear a range of words with zeroes.
     *
     * @param from inclusive index
     * @param to   exclusive index
     */
    public abstract void clear(int from, int to);

    /**
     * Append the first {@code length} words of this array to a width
     * buffer and clear the flushed prefix.
     *
     * @param factory width factory
     * @param buffer  output buffer
     * @param length  number of words to flush
     */
    public abstract void flushTo(AggregationFactory<C> factory,
                                 Object buffer, int length);

    /**
     * Append the first {@code length} words of this array to a generic
     * sink (no extra buffering assumption).
     *
     * @param sink   output sink
     * @param length number of words to flush
     */
    public abstract void flushToSink(WordSink sink, int length);

    /**
     * @return capacity in words
     */
    public abstract int length();

    /**
     * Fill the whole array with all-ones.
     */
    public abstract void fillAll();
}
