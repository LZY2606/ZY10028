package com.googlecode.javaewah.aggregation;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Width-agnostic view over a running-length-word cursor.
 *
 * This is the narrow primitive boundary of the shared aggregation state
 * machines: all word values cross as {@code long} (an {@code int} word of the
 * 32-bit variant is a bit pattern carried in the low 32 bits, without boxing),
 * while lengths are uniformly expressed as {@code long} word counts.
 *
 * @param <C> concrete cursor type
 */
public abstract class RlwCursor<C extends RlwCursor<C>> {

    /**
     * Advance to the next running length word.
     *
     * @return whether there is more data
     */
    public abstract boolean next();

    /**
     * @param index literal word index
     * @return literal word at the index (32-bit values use the low 32 bits)
     */
    public abstract long getLiteralWordAt(int index);

    /**
     * @return number of literal words in the current running length word
     */
    public abstract int getNumberOfLiteralWords();

    /**
     * @return bit used by the fill words
     */
    public abstract boolean getRunningBit();

    /**
     * @return running length plus number of literal words
     */
    public abstract long size();

    /**
     * @return length of the current run of fill words
     */
    public abstract long getRunningLength();

    /**
     * Discard the first {@code x} words.
     *
     * @param x number of words to discard
     */
    public abstract void discardFirstWords(long x);

    /**
     * Discard all running words.
     */
    public abstract void discardRunningWords();

    /**
     * Discard {@code x} literal words (assumes there is no running word).
     *
     * @param x number of words to discard
     */
    public abstract void discardLiteralWords(long x);
}
