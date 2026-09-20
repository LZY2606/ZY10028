package com.googlecode.javaewah.aggregation;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Width-agnostic run pointer used by the RunningBitmapMerge state machine.
 *
 * A pointer walks the runs of one input bitmap. The state (end of run,
 * literal/run flag, current fill bit, dead flag) and the run-parsing control
 * flow live here once; the only width-specific operation is discarding the
 * words of the underlying cursor and reading its run descriptors.
 *
 * @param <C> concrete cursor type
 * @param <P> concrete pointer type
 */
public abstract class RunningPointer<C extends RlwCursor<C>,
        P extends RunningPointer<C, P>> implements Comparable<P> {

    /**
     * Underlying cursor.
     */
    protected final C iterator;

    private int endRun;
    private final int pos;
    private boolean literal;
    private boolean value;
    private boolean dead = false;

    /**
     * @param previousEndRun word where the previous run ended
     * @param cursor         the cursor
     * @param position       position (input index) of this pointer
     */
    protected RunningPointer(final int previousEndRun, final C cursor,
                             final int position) {
        this.pos = position;
        this.iterator = cursor;
        if (this.iterator.getRunningLength() > 0) {
            this.endRun = previousEndRun
                    + (int) this.iterator.getRunningLength();
            this.literal = false;
            this.value = this.iterator.getRunningBit();
        } else if (this.iterator.getNumberOfLiteralWords() > 0) {
            this.literal = true;
            this.endRun = previousEndRun
                    + this.iterator.getNumberOfLiteralWords();
        } else {
            this.endRun = previousEndRun;
            this.dead = true;
        }
    }

    /**
     * @return the end of the current run
     */
    public final int endOfRun() {
        return this.endRun;
    }

    /**
     * @return the beginning of the current run
     */
    public final int beginOfRun() {
        if (this.literal)
            return this.endRun
                    - this.iterator.getNumberOfLiteralWords();
        return (int) (this.endRun - this.iterator.getRunningLength());
    }

    /**
     * Process the next run.
     */
    public final void parseNextRun() {
        if (this.literal
                || this.iterator.getNumberOfLiteralWords() == 0) {
            this.iterator.discardFirstWords(this.iterator.size());
            if (this.iterator.getRunningLength() > 0) {
                this.endRun += (int) this.iterator.getRunningLength();
                this.literal = false;
                this.value = this.iterator.getRunningBit();
            } else if (this.iterator.getNumberOfLiteralWords() > 0) {
                this.literal = true;
                this.endRun += this.iterator.getNumberOfLiteralWords();
            } else {
                this.dead = true;
            }
        } else {
            this.literal = true;
            this.endRun += this.iterator.getNumberOfLiteralWords();
        }
    }

    /**
     * @return true if there is no more data
     */
    public final boolean hasNoData() {
        return this.dead;
    }

    /**
     * Update the symmetric function with the current run state.
     *
     * @param function function to update
     */
    public final void callbackUpdate(final UpdatableFunction function) {
        if (this.dead)
            function.setZero(this.pos);
        else if (this.literal)
            function.setLiteral(this.pos);
        else if (this.value)
            function.setOne(this.pos);
        else
            function.setZero(this.pos);
    }

    @Override
    public final int compareTo(final P other) {
        return this.endRun - other.endOfRun();
    }
}
