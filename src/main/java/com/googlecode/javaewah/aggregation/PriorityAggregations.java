package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.LogicalElement;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Shared control flow of the priority-queue based pairwise aggregations.
 *
 * The two smallest bitmaps are repeatedly combined with the requested boolean
 * operation until one bitmap remains (array/iterator variants), or until two
 * remain when a container receives the final combination. The scheduling lives
 * here once for the 64-bit and 32-bit elements. Each boolean operation is a
 * separate static entry point, so the combine call inside the hot loop is a
 * direct, single-target {@code or}/{@code xor} on a {@link LogicalElement}
 * and is inlinable while merging large intermediate bitmaps (no boxing, no
 * generic functional-interface indirection).
 */
public final class PriorityAggregations {

    private PriorityAggregations() {
    }

    private static <T extends LogicalElement> PriorityQueue<T> newQueue(
            final int initialCapacity) {
        return new PriorityQueue<T>(initialCapacity,
                new Comparator<T>() {
                    @Override
                    public int compare(final T a, final T b) {
                        return a.sizeInBytes() - b.sizeInBytes();
                    }
                });
    }

    /**
     * OR-aggregate an array.
     *
     * @param bitmaps inputs (may be empty)
     * @param empty   supplier of an empty result for zero inputs
     * @param <T>     element type
     * @return the aggregate
     */
    @SuppressWarnings("unchecked")
    public static <T extends LogicalElement> T orArray(
            final T[] bitmaps,
            final java.util.function.Supplier<T> empty) {
        final PriorityQueue<T> queue = newQueue(bitmaps.length);
        for (final T bitmap : bitmaps)
            queue.add(bitmap);
        if (empty != null && queue.isEmpty())
            return empty.get();
        while (queue.size() > 1) {
            final T first = queue.poll();
            final T second = queue.poll();
            queue.add((T) first.or(second));
        }
        return queue.poll();
    }

    /**
     * XOR-aggregate an array.
     *
     * @param bitmaps inputs (may be empty)
     * @param empty   supplier of an empty result for zero inputs
     * @param <T>     element type
     * @return the aggregate
     */
    @SuppressWarnings("unchecked")
    public static <T extends LogicalElement> T xorArray(
            final T[] bitmaps,
            final java.util.function.Supplier<T> empty) {
        final PriorityQueue<T> queue = newQueue(bitmaps.length);
        for (final T bitmap : bitmaps)
            queue.add(bitmap);
        if (empty != null && queue.isEmpty())
            return empty.get();
        while (queue.size() > 1) {
            final T first = queue.poll();
            final T second = queue.poll();
            queue.add((T) first.xor(second));
        }
        return queue.poll();
    }

    /**
     * OR-aggregate an iterator.
     *
     * @param bitmaps inputs
     * @param empty   supplier of an empty result for zero inputs
     * @param <T>     element type
     * @return the aggregate
     */
    @SuppressWarnings("unchecked")
    public static <T extends LogicalElement> T orIterator(
            final Iterator<T> bitmaps,
            final java.util.function.Supplier<T> empty) {
        final PriorityQueue<T> queue = newQueue(32);
        while (bitmaps.hasNext())
            queue.add(bitmaps.next());
        if (empty != null && queue.isEmpty())
            return empty.get();
        while (queue.size() > 1) {
            final T first = queue.poll();
            final T second = queue.poll();
            queue.add((T) first.or(second));
        }
        return queue.poll();
    }

    /**
     * XOR-aggregate an iterator.
     *
     * @param bitmaps inputs
     * @param empty   supplier of an empty result for zero inputs
     * @param <T>     element type
     * @return the aggregate
     */
    @SuppressWarnings("unchecked")
    public static <T extends LogicalElement> T xorIterator(
            final Iterator<T> bitmaps,
            final java.util.function.Supplier<T> empty) {
        final PriorityQueue<T> queue = newQueue(32);
        while (bitmaps.hasNext())
            queue.add(bitmaps.next());
        if (empty != null && queue.isEmpty())
            return empty.get();
        while (queue.size() > 1) {
            final T first = queue.poll();
            final T second = queue.poll();
            queue.add((T) first.xor(second));
        }
        return queue.poll();
    }

    /**
     * OR-aggregate until two inputs remain, then write to a container.
     *
     * @param container output container
     * @param bitmaps   at least two inputs
     * @param toContainer width-specific final combination
     * @param <T>       element type
     */
    public static <T extends LogicalElement> void orToContainer(
            final Object container, final T[] bitmaps,
            final ToContainer<T> toContainer) {
        toContainerImpl(container, bitmaps, toContainer, true);
    }

    /**
     * XOR-aggregate until two inputs remain, then write to a container.
     *
     * @param container output container
     * @param bitmaps   at least two inputs
     * @param toContainer width-specific final combination
     * @param <T>       element type
     */
    public static <T extends LogicalElement> void xorToContainer(
            final Object container, final T[] bitmaps,
            final ToContainer<T> toContainer) {
        toContainerImpl(container, bitmaps, toContainer, false);
    }

    /** Final combination written to a container. */
    public interface ToContainer<T> {
        /**
         * @param first     first operand
         * @param second    second operand
         * @param container output container
         */
        void combine(T first, T second, Object container);
    }

    @SuppressWarnings("unchecked")
    private static <T extends LogicalElement> void toContainerImpl(
            final Object container, final T[] bitmaps,
            final ToContainer<T> toContainer, final boolean or) {
        if (bitmaps.length < 2)
            throw new IllegalArgumentException("We need at least two bitmaps");
        final PriorityQueue<T> queue = newQueue(bitmaps.length);
        for (final T bitmap : bitmaps)
            queue.add(bitmap);
        while (queue.size() > 2) {
            final T first = queue.poll();
            final T second = queue.poll();
            queue.add((T) (or ? first.or(second) : first.xor(second)));
        }
        toContainer.combine(queue.poll(), queue.poll(), container);
    }
}
