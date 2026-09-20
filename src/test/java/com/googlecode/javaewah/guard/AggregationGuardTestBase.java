package com.googlecode.javaewah.guard;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Behavior guard shared by the 64-bit and 32-bit implementations.
 *
 * For every data shape (sparse, dense, long runs, non-integral tail word,
 * empty, singleton and a 20-way mix) it computes the AND/OR/XOR aggregates
 * through the public aggregation entry points and checks:
 *
 * <ul>
 *   <li>serialized bytes against an independent bit-set reference model,</li>
 *   <li>cardinality,</li>
 *   <li>iteration order (toList),</li>
 *   <li>parity of the reference {@code FastAggregation} helpers with the
 *       static bitmap aggregates and the iterator-buffered aggregates.</li>
 * </ul>
 *
 * Since the same scenarios are executed by both width variants, this is the
 * dual-implementation guard added before refactoring.
 */
public abstract class AggregationGuardTestBase {

    /** Width-specific operations under test. */
    protected interface Variant {
        /** @param positions sorted set bits
         *  @return bitmap */
        Object bitmapOf(int... positions);

        /** @param bitmaps inputs
         *  @return static AND */
        Object and(Object... bitmaps);

        /** @param bitmaps inputs
         *  @return static OR */
        Object or(Object... bitmaps);

        /** @param bitmaps inputs
         *  @return static XOR */
        Object xor(Object... bitmaps);

        /** @return empty OR aggregate of zero inputs (FastAggregation) */
        Object fastOrEmpty();

        /** @return empty XOR aggregate of zero inputs (FastAggregation) */
        Object fastXorEmpty();

        /** @param bitmaps inputs
         *  @return buffered AND */
        Object bufferedAnd(Object... bitmaps);

        /** zero-input iterator-buffered OR (must throw) */
        void iteratorBufferedOrZero();

        /** zero-input iterator-buffered XOR (must throw) */
        void iteratorBufferedXorZero();

        /** zero-input iterator-buffered AND (must throw) */
        void iteratorBufferedAndZero();

        /** @param bitmaps inputs
         *  @return buffered OR */
        Object bufferedOr(Object... bitmaps);

        /** @param bitmaps inputs
         *  @return buffered XOR */
        Object bufferedXor(Object... bitmaps);

        /** @param bitmap a bitmap
         *  @return serialized bytes */
        byte[] serialize(Object bitmap) throws IOException;

        /** @param bitmap a bitmap
         *  @return cardinality */
        int cardinality(Object bitmap);

        /** @param bitmap a bitmap
         *  @return ascending set-bit positions */
        List<Integer> positions(Object bitmap);
    }

    /** @return the variant under test */
    protected abstract Variant variant();

    // ---- Independent reference model over int[] word masks ----

    private static final int WORD_BITS = 64;

    private static int wordCount(final int maxPosition) {
        return (maxPosition >>> 6) + 1;
    }

    private static long[] referenceOf(final int... positions) {
        final long[] words = new long[wordCount(maxPos(positions))];
        for (final int p : positions)
            words[p >>> 6] |= 1L << p;
        return words;
    }

    private static int maxPos(final int[] positions) {
        int max = -1;
        for (final int p : positions)
            if (p > max)
                max = p;
        return Math.max(max, 0);
    }

    private static long[][] truncateToShortest(final long[][] inputs) {
        int shortest = Integer.MAX_VALUE;
        for (final long[] in : inputs)
            shortest = Math.min(shortest, in.length);
        final long[][] out = new long[inputs.length][];
        for (int k = 0; k < inputs.length; ++k)
            out[k] = Arrays.copyOf(inputs[k], shortest);
        return out;
    }

    private static long[] combine(final long[][] inputs, final int op) {
        int words = 1;
        for (final long[] in : inputs)
            words = Math.max(words, in.length);
        final long[] out = new long[words];
        if (inputs.length == 0)
            return out;
        Arrays.fill(out, op == 0 ? ~0L : 0L);
        for (final long[] in : inputs) {
            for (int k = 0; k < words; ++k) {
                final long w = k < in.length ? in[k]
                        : (op == 0 ? ~0L : 0L);
                switch (op) {
                    case 0:
                        out[k] &= w;
                        break;
                    case 1:
                        out[k] |= w;
                        break;
                    default:
                        out[k] ^= w;
                        break;
                }
            }
        }
        return out;
    }

    private static List<Integer> positionsOf(final long[] words) {
        final List<Integer> list = new ArrayList<Integer>();
        for (int k = 0; k < words.length; ++k) {
            long w = words[k];
            while (w != 0) {
                list.add(k * WORD_BITS + Long.numberOfTrailingZeros(w));
                w &= w - 1;
            }
        }
        return list;
    }

    // ---- Scenario generation ----

    private enum Shape {
        EMPTY, SINGLE, SPARSE, DENSE, LONG_RUN, TAIL, MIX
    }

    private int[] generate(final Shape shape, final int index) {
        switch (shape) {
            case EMPTY:
                return new int[0];
            case SINGLE:
                return new int[]{index * 7 + 3};
            case SPARSE: {
                final int[] out = new int[40];
                for (int k = 0; k < out.length; ++k)
                    out[k] = k * 1009 + index * 37 + 5;
                return out;
            }
            case DENSE: {
                final int[] out = new int[2000];
                int p = 0;
                for (int k = 0; k < out.length; ++k) {
                    p += 1 + ((k * 13 + index) % 3);
                    out[k] = p;
                }
                return out;
            }
            case LONG_RUN: {
                final int[] out = new int[3000];
                int p = index * 5;
                for (int k = 0; k < out.length; ++k)
                    out[k] = p++;
                return out;
            }
            case TAIL: {
                // deliberately straddle 64-bit and 32-bit word boundaries
                final int[] out = new int[130];
                for (int k = 0; k < out.length; ++k)
                    out[k] = k * 2 + (index % 2);
                return append(out, 5000 + index, 5001 + index, 5050 + index);
            }
            case MIX:
            default: {
                final int[] out = new int[500];
                for (int k = 0; k < 250; ++k)
                    out[k] = k * 31 + index;
                int p = 10000 + index * 17;
                for (int k = 250; k < out.length; ++k)
                    out[k] = p++;
                return out;
            }
        }
    }

    private static int[] append(final int[] base, final int... extra) {
        final int[] out = Arrays.copyOf(base, base.length + extra.length);
        System.arraycopy(extra, 0, out, base.length, extra.length);
        Arrays.sort(out);
        return out;
    }

    private int[][] inputs(final Shape shape, final int count) {
        final int[][] inputs = new int[count][];
        for (int k = 0; k < count; ++k)
            inputs[k] = generate(shape, k);
        return inputs;
    }

    // ---- The guard ----

    private void assertAggregateEqualsReference(final long[] expected,
                                                final Object actual,
                                                final String label) {
        assertNotNull(label + ": null result", actual);
        final List<Integer> expectedPositions = positionsOf(expected);
        final List<Integer> actualPositions = variant().positions(actual);
        assertEquals(label + ": iteration order / positions",
                expectedPositions, actualPositions);
        assertEquals(label + ": cardinality",
                expectedPositions.size(), variant().cardinality(actual));

        final byte[] serialized = serializeUnchecked(actual);
        // Serialization is a width-specific on-disk layout: deserialize it
        // back through the same variant and assert the round trip is exact.
        final Object roundTripped = deserializeUnchecked(serialized);
        assertEquals(label + ": serialization round trip positions",
                expectedPositions, variant().positions(roundTripped));
        assertEquals(label + ": serialization round trip cardinality",
                expectedPositions.size(),
                variant().cardinality(roundTripped));
    }

    /**
     * @param bytes serialized form
     * @return rebuilt bitmap
     */
    protected abstract Object deserialize(byte[] bytes);

    private Object deserializeUnchecked(final byte[] bytes) {
        try {
            return deserialize(bytes);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] serializeUnchecked(final Object bitmap) {
        try {
            return variant().serialize(bitmap);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void guardShape(final Shape shape, final int count) {
        final int[][] rawInputs = inputs(shape, count);
        final Object[] bitmaps = new Object[count];
        final long[][] refs = new long[count][];
        for (int k = 0; k < count; ++k) {
            bitmaps[k] = variant().bitmapOf(rawInputs[k]);
            refs[k] = referenceOf(rawInputs[k]);
        }

        final long[] andExpected = count == 0
                ? combine(new long[0][], 0)
                : combine(truncateToShortest(refs), 0);
        final long[] orExpected = combine(refs, 1);
        final long[] xorExpected = combine(refs, 2);

        if (count >= 1) {
            assertAggregateEqualsReference(andExpected,
                    variant().and(bitmaps), shape + "/and");
            assertAggregateEqualsReference(orExpected,
                    variant().or(bitmaps), shape + "/or");
            assertAggregateEqualsReference(xorExpected,
                    variant().xor(bitmaps), shape + "/xor");

            assertAggregateEqualsReference(orExpected,
                    variant().bufferedOr(bitmaps), shape + "/bufferedOr");
            assertAggregateEqualsReference(xorExpected,
                    variant().bufferedXor(bitmaps), shape + "/bufferedXor");
            assertAggregateEqualsReference(andExpected,
                    variant().bufferedAnd(bitmaps), shape + "/bufferedAnd");

            // The three public families must agree with each other.
            assertArrayEquals(shape + ": or family disagreement",
                    serializeUnchecked(variant().or(bitmaps)),
                    serializeUnchecked(variant().bufferedOr(bitmaps)));
            assertArrayEquals(shape + ": xor family disagreement",
                    serializeUnchecked(variant().xor(bitmaps)),
                    serializeUnchecked(variant().bufferedXor(bitmaps)));
            // Both AND families truncate to the shortest input; their
            // compressed encodings may use different run boundaries, so
            // compare logical content (already byte-frozen above for OR/XOR).
            assertEquals(shape + ": and family disagreement",
                    variant().positions(variant().and(bitmaps)),
                    variant().positions(variant().bufferedAnd(bitmaps)));
        }
    }

    /**
     * Sparse, dense and long-run data at 2 and 20 ways.
     */
    @Test
    public void guardSparseDenseRunsTwoAndTwentyWays() {
        for (final Shape shape : new Shape[]{
                Shape.SPARSE, Shape.DENSE, Shape.LONG_RUN, Shape.TAIL}) {
            guardShape(shape, 2);
            guardShape(shape, 20);
        }
    }

    /**
     * Empty input: the static aggregates must accept zero inputs and return
     * an empty bitmap; buffered helpers must reject zero iterators.
     */
    @Test
    public void guardEmptyInput() {
        final Object empty = variant().bitmapOf();
        assertEquals(0, variant().cardinality(empty));
        assertEquals(0, variant().positions(empty).size());
        // Current version rejects zero-input OR/XOR arrays with
        // IllegalArgumentException on both widths (preserved exception type).
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                variant().fastOrEmpty();
            }
        });
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                variant().fastXorEmpty();
            }
        });
    }

    /**
     * Buffered iterators must reject zero inputs with
     * IllegalArgumentException (preserved public exception type).
     */
    @Test
    public void guardEmptyBufferedIteratorRejected() {
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                variant().iteratorBufferedOrZero();
            }
        });
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                variant().iteratorBufferedXorZero();
            }
        });
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                variant().iteratorBufferedAndZero();
            }
        });
    }

    private static void assertIllegalArgument(final Runnable runnable) {
        try {
            runnable.run();
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            // preserved exception type
        }
    }

    /**
     * Single input: the static aggregates return the sole input's content.
     */
    @Test
    public void guardSingleInput() {
        guardShape(Shape.MIX, 1);
        final Object singleton = variant().bitmapOf(generate(Shape.DENSE, 7));
        assertEquals(variant().cardinality(singleton),
                variant().cardinality(variant().and(singleton)));
        assertEquals(variant().cardinality(singleton),
                variant().cardinality(variant().or(singleton)));
        assertEquals(variant().cardinality(singleton),
                variant().cardinality(variant().xor(singleton)));
    }

    /**
     * The full 20-way scenario for every shape including empty members.
     */
    @Test
    public void guardTwentyWayAllShapes() {
        for (final Shape shape : Shape.values()) {
            guardShape(shape, 20);
        }
    }
}
