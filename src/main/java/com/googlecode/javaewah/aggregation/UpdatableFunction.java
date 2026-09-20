package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.datastructure.BitSet;

import java.util.Arrays;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Width-agnostic bookkeeping of an updatable symmetric Boolean function.
 *
 * The classification of each input word (zero / one / literal), the counters
 * ({@code hammingWeight}, {@code litWeight}) and the literal set are maintained
 * once here. Width-specific public subclasses supply only the output decision
 * through {@link #emit(WordSink, int, int)}.
 */
public abstract class UpdatableFunction {

    /** Active run pointers, indexed by input position. */
    protected RunningPointer<?, ?>[] rw = new RunningPointer[0];
    /** Number of inputs currently contributing an all-ones word. */
    protected int hammingWeight = 0;
    /** Number of inputs currently contributing a literal word. */
    protected int litWeight = 0;
    /** Whether an input is currently an all-ones word. */
    protected boolean[] b = new boolean[0];
    /** Set of inputs currently contributing literal words. */
    protected final BitSet litwlist = new BitSet(0);

    /**
     * @return the current number of literal words
     */
    public final int getNumberOfLiterals() {
        return this.litwlist.cardinality();
    }

    /**
     * Iterate over the literal pointers of a given type.
     *
     * @param type concrete pointer class (for the cast)
     * @param <P>  concrete pointer type
     * @return iterable over pointers currently in the literal state
     */
    @SuppressWarnings("unchecked")
    protected final <P extends RunningPointer<?, P>> Iterable<P> literalPointers(
            final Class<P> type) {
        final BitSet set = this.litwlist;
        final RunningPointer<?, ?>[] pointers = this.rw;
        return new Iterable<P>() {
            @Override
            public java.util.Iterator<P> iterator() {
                return new java.util.Iterator<P>() {
                    int k = set.nextSetBit(0);

                    @Override
                    public boolean hasNext() {
                        return this.k >= 0;
                    }

                    @Override
                    public P next() {
                        final P answer = (P) pointers[this.k];
                        this.k = set.nextSetBit(this.k + 1);
                        return answer;
                    }

                    @Override
                    public void remove() {
                        throw new RuntimeException("N/A");
                    }
                };
            }
        };
    }

    /**
     * Append the literal pointers to a container.
     *
     * @param container list to fill
     * @param <P>       concrete pointer type
     */
    @SuppressWarnings("unchecked")
    public final <P extends RunningPointer<?, P>> void fillLiteralPointers(
            final java.util.List<P> container) {
        for (int k = this.litwlist.nextSetBit(0); k >= 0;
             k = this.litwlist.nextSetBit(k + 1)) {
            container.add((P) this.rw[k]);
        }
    }

    /**
     * @param newSize the number of inputs
     */
    public final void resize(final int newSize) {
        this.rw = Arrays.copyOf(this.rw, newSize);
        this.litwlist.resize(newSize);
        this.b = Arrays.copyOf(this.b, newSize);
    }

    /**
     * @param pos position of a literal
     */
    public final void setLiteral(final int pos) {
        if (!this.litwlist.get(pos)) {
            this.litwlist.set(pos);
            this.litWeight++;
            if (this.b[pos]) {
                this.b[pos] = false;
                --this.hammingWeight;
            }
        }
    }

    /**
     * @param pos position where a literal was removed
     */
    public final void clearLiteral(final int pos) {
        if (this.litwlist.get(pos)) {
            this.litwlist.set(pos, false);
            this.litWeight--;
        }
    }

    /**
     * @param pos position where a zero word was added
     */
    public final void setZero(final int pos) {
        if (this.b[pos]) {
            this.b[pos] = false;
            --this.hammingWeight;
        } else {
            clearLiteral(pos);
        }
    }

    /**
     * @param pos position where a 11...1 word was added
     */
    public final void setOne(final int pos) {
        if (!this.b[pos]) {
            clearLiteral(pos);
            this.b[pos] = true;
            ++this.hammingWeight;
        }
    }

    /**
     * Writes out the answer for a run.
     *
     * @param sink     output sink
     * @param runBegin beginning of the run (in words)
     * @param runEnd   end of the run (in words)
     */
    public abstract void emit(WordSink sink, int runBegin, int runEnd);
}
