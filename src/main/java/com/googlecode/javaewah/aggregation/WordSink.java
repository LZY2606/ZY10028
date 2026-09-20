package com.googlecode.javaewah.aggregation;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Width-agnostic write side of the aggregation state machines. Concrete
 * adapters forward to a 64-bit {@code BitmapStorage} (as {@code long}) or to a
 * 32-bit {@code BitmapStorage32} (taking the low 32 bits as an {@code int}).
 */
public interface WordSink {

    /**
     * Append one word.
     *
     * @param word word value (low 32 bits for the 32-bit variant)
     */
    void addWord(long word);

    /**
     * Append a stream of identical empty (fill) words.
     *
     * @param v      the fill bit
     * @param number number of words
     */
    void addStreamOfEmptyWords(boolean v, long number);

    /**
     * Empty the underlying container.
     */
    void clear();

    /**
     * Set the size in bits within the last word.
     *
     * @param size size in bits
     */
    void setSizeInBitsWithinLastWord(int size);
}
