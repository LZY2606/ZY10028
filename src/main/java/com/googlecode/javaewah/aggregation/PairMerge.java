package com.googlecode.javaewah.aggregation;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Shared control flow for the pairwise AND/XOR merges between two running
 * length word cursors.
 *
 * The algorithm (which input is prey/predator for a run, how words are
 * discharged and clipped at run boundaries) is maintained once here. The word
 * width only enters through {@link RlwCursor} and {@link WordSink}, which the
 * JIT specializes per concrete cursor/sink pair.
 *
 * The 64-bit and 32-bit historical implementations differed in one detail:
 * when the predator has a run of ones during an XOR, the 64-bit path negates
 * the words it copies from the prey whereas the 32-bit path copies them
 * unchanged. Both behaviors are preserved exactly via {@code negatePreyOnes};
 * serialization-identical output is asserted by the behavior guard tests.
 */
public final class PairMerge {

    private PairMerge() {
    }

    /**
     * Write out up to {@code max} words of a cursor.
     *
     * @param sink    target for writes
     * @param cur     source of data
     * @param max     maximal number of words to write
     * @param negate  whether copied words must be negated
     * @param <C>     concrete cursor type
     * @return number of words written
     */
    public static <C extends RlwCursor<C>> long discharge(final WordSink sink,
                                                          final C cur, final long max,
                                                          final boolean negate) {
        long counter = 0;
        while (cur.size() > 0 && counter < max) {
            long runningLength = cur.getRunningLength();
            if (runningLength > 0) {
                if (runningLength + counter > max)
                    runningLength = max - counter;
                sink.addStreamOfEmptyWords(negate != cur.getRunningBit(),
                        runningLength);
                counter += runningLength;
            }
            long literalCount = cur.getNumberOfLiteralWords();
            if (literalCount + counter > max)
                literalCount = max - counter;
            for (int k = 0; k < literalCount; ++k) {
                final long word = cur.getLiteralWordAt(k);
                sink.addWord(negate ? ~word : word);
            }
            counter += literalCount;
            cur.discardFirstWords(literalCount + runningLength);
        }
        return counter;
    }

    /**
     * Write out the content of a cursor as if it were all zeros.
     *
     * @param sink target for writes
     * @param cur  the cursor
     * @param <C>  concrete cursor type
     */
    public static <C extends RlwCursor<C>> void dischargeAsEmpty(
            final WordSink sink, final C cur) {
        while (cur.size() > 0) {
            sink.addStreamOfEmptyWords(false, cur.size());
            cur.next();
        }
    }

    /**
     * Pairwise AND merge, limited to a number of output words.
     *
     * @param sink             where to write
     * @param desiredRlwCount  maximum number of words to write
     * @param first            first cursor
     * @param second           second cursor
     * @param <C>              concrete cursor type
     */
    public static <C extends RlwCursor<C>> void andToContainer(
            final WordSink sink, long desiredRlwCount,
            final C first, final C second) {
        while (first.size() > 0 && second.size() > 0
                && desiredRlwCount-- > 0) {
            andRuns(sink, first, second);
            final int literalCount = Math.min(first.getNumberOfLiteralWords(),
                    second.getNumberOfLiteralWords());
            if (literalCount > 0) {
                desiredRlwCount -= literalCount;
                for (int k = 0; k < literalCount; ++k)
                    sink.addWord(first.getLiteralWordAt(k)
                            & second.getLiteralWordAt(k));
                first.discardFirstWords(literalCount);
                second.discardFirstWords(literalCount);
            }
        }
    }

    /**
     * Pairwise AND merge.
     *
     * @param sink   where to write
     * @param first  first cursor
     * @param second second cursor
     * @param <C>    concrete cursor type
     */
    public static <C extends RlwCursor<C>> void andToContainer(
            final WordSink sink, final C first, final C second) {
        while (first.size() > 0 && second.size() > 0) {
            andRuns(sink, first, second);
            final int literalCount = Math.min(first.getNumberOfLiteralWords(),
                    second.getNumberOfLiteralWords());
            if (literalCount > 0) {
                for (int k = 0; k < literalCount; ++k)
                    sink.addWord(first.getLiteralWordAt(k)
                            & second.getLiteralWordAt(k));
                first.discardFirstWords(literalCount);
                second.discardFirstWords(literalCount);
            }
        }
    }

    /**
     * Pairwise XOR merge, limited to a number of output words.
     *
     * @param sink             where to write
     * @param desiredRlwCount  maximum number of words to write
     * @param first            first cursor
     * @param second           second cursor
     * @param negatePreyOnes   true for the 64-bit variant (negate prey words
     *                         under a predator run of ones), false for the
     *                         32-bit variant
     * @param <C>              concrete cursor type
     */
    public static <C extends RlwCursor<C>> void xorToContainer(
            final WordSink sink, long desiredRlwCount,
            final C first, final C second, final boolean negatePreyOnes) {
        while (first.size() > 0 && second.size() > 0
                && desiredRlwCount-- > 0) {
            xorRuns(sink, first, second, negatePreyOnes);
            final int literalCount = Math.min(first.getNumberOfLiteralWords(),
                    second.getNumberOfLiteralWords());
            if (literalCount > 0) {
                desiredRlwCount -= literalCount;
                for (int k = 0; k < literalCount; ++k)
                    sink.addWord(first.getLiteralWordAt(k)
                            ^ second.getLiteralWordAt(k));
                first.discardFirstWords(literalCount);
                second.discardFirstWords(literalCount);
            }
        }
    }

    private static <C extends RlwCursor<C>> void andRuns(
            final WordSink sink, final C first, final C second) {
        while (first.getRunningLength() > 0 || second.getRunningLength() > 0) {
            final boolean firstIsPrey =
                    first.getRunningLength() < second.getRunningLength();
            final C prey = firstIsPrey ? first : second;
            final C predator = firstIsPrey ? second : first;
            final long predatorRun = predator.getRunningLength();
            if (!predator.getRunningBit()) {
                sink.addStreamOfEmptyWords(false, predatorRun);
                prey.discardFirstWords(predatorRun);
                predator.discardFirstWords(predatorRun);
            } else {
                final long written = discharge(sink, prey, predatorRun, false);
                sink.addStreamOfEmptyWords(false, predatorRun - written);
                predator.discardFirstWords(predatorRun);
            }
        }
    }

    private static <C extends RlwCursor<C>> void xorRuns(
            final WordSink sink, final C first, final C second,
            final boolean negatePreyOnes) {
        while (first.getRunningLength() > 0 || second.getRunningLength() > 0) {
            final boolean firstIsPrey =
                    first.getRunningLength() < second.getRunningLength();
            final C prey = firstIsPrey ? first : second;
            final C predator = firstIsPrey ? second : first;
            final long predatorRun = predator.getRunningLength();
            if (!predator.getRunningBit()) {
                final long written = discharge(sink, prey, predatorRun, false);
                sink.addStreamOfEmptyWords(false, predatorRun - written);
                prey.discardFirstWords(predatorRun - written);
                predator.discardFirstWords(predatorRun);
            } else {
                final long written = discharge(sink, prey, predatorRun,
                        negatePreyOnes);
                sink.addStreamOfEmptyWords(true, predatorRun - written);
                prey.discardFirstWords(predatorRun - written);
                predator.discardFirstWords(predatorRun);
            }
        }
    }
}
