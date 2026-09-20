package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.datastructure.PriorityQ;

import java.util.Comparator;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Shared RunningBitmapMerge state machine.
 *
 * All inputs are wrapped in run pointers and put into a bounded
 * {@link PriorityQ}; the earliest run boundary defines the length of a
 * constant segment, the updatable function is asked to emit that segment, and
 * every pointer ending at the boundary advances. The scheduling is identical
 * for the 64-bit and 32-bit variants and lives here once.
 */
public final class RunningMergeEngine {

    private RunningMergeEngine() {
    }

    /**
     * Run the merge.
     *
     * @param function   updatable symmetric function
     * @param sink       output sink
     * @param pointers   one pointer per input (some may be dead)
     * @param <P>        pointer type
     */
    public static <P extends RunningPointer<?, P>> void run(
            final UpdatableFunction function, final WordSink sink,
            final P[] pointers) {
        sink.clear();
        final PriorityQ<P> heap = new PriorityQ<P>(pointers.length,
                new Comparator<P>() {
                    @Override
                    public int compare(final P a, final P b) {
                        return a.compareTo(b);
                    }
                });
        function.resize(pointers.length);

        for (int k = 0; k < pointers.length; ++k) {
            final P pointer = pointers[k];
            if (pointer.hasNoData())
                continue;
            function.rw[k] = pointer;
            pointer.callbackUpdate(function);
            heap.toss(pointer);
        }
        heap.buildHeap();

        int last = 0;
        if (heap.isEmpty())
            return;
        mainLoop:
        while (true) {
            final int end = heap.peek().endOfRun();
            function.emit(sink, last, end);
            last = end;

            while (heap.peek().endOfRun() == end) {
                final P pointer = heap.peek();
                pointer.parseNextRun();
                pointer.callbackUpdate(function);
                if (pointer.hasNoData()) {
                    heap.poll();
                    if (heap.isEmpty())
                        break mainLoop;
                } else {
                    heap.percolateDown();
                }
            }
        }
    }
}
