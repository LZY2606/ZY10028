package com.googlecode.javaewah.aggregation;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * The specialization seam of the shared aggregation state machines.
 *
 * One implementation per word width. Every method returns concrete, final
 * types internally, so the JIT can inline and scalarize them; no method of the
 * shared state machine boxes an individual word.
 *
 * @param <C> concrete cursor type produced by this width
 */
public interface AggregationFactory<C extends RlwCursor<C>> {

    /**
     * Allocate a scratch array of {@code size} words.
     *
     * @param size capacity in words
     * @return a new scratch array
     */
    WordArray<C> newWordArray(int size);

    /**
     * Copy a scratch array (used by iterator cloning).
     *
     * @param array array to copy
     * @return an independent copy
     */
    WordArray<C> copyWordArray(WordArray<C> array);

    /**
     * Allocate a fresh compressed output buffer.
     *
     * @return new buffer
     */
    Object newBuffer();

    /**
     * Reset (empty) an existing output buffer.
     *
     * @param buffer buffer to reset
     * @return the same buffer, emptied
     */
    Object resetBuffer(Object buffer);

    /**
     * Copy an output buffer (used by iterator cloning).
     *
     * @param buffer buffer to copy
     * @return an independent copy
     */
    Object copyBuffer(Object buffer) throws CloneNotSupportedException;

    /**
     * View an output buffer as a {@link WordSink}.
     *
     * @param buffer output buffer
     * @return sink writing into the buffer
     */
    WordSink asSink(Object buffer);

    /**
     * Build the width-specific EWAH iterator over a buffer filled by
     * {@link #asSink(Object)}.
     *
     * @param buffer filled output buffer
     * @return EWAH iterator
     */
    Object iteratorOf(Object buffer);
    /**
     * Pairwise AND merge limited to {@code maxWords} output words.
     *
     * @param sink     output sink
     * @param maxWords maximum number of words written
     * @param first    first cursor
     * @param second   second cursor
     */
    void andToContainer(WordSink sink, long maxWords, C first, C second);

    /**
     * Unbounded pairwise AND merge.
     *
     * @param sink   output sink
     * @param first  first cursor
     * @param second second cursor
     */
    void andToContainer(WordSink sink, C first, C second);

    /**
     * Cursor over the current content of an output buffer.
     *
     * @param buffer output buffer
     * @return a new cursor
     */
    C cursorOfBuffer(Object buffer);

    /**
     * Swap the contents of two output buffers.
     *
     * @param a first buffer
     * @param b second buffer
     */
    void swapBuffers(Object a, Object b);

    /**
     * @param buffer output buffer
     * @return serialized size in bytes
     */
    int sizeInBytes(Object buffer);
    /**
     * Cursor over a concrete input bitmap.
     *
     * @param bitmap input bitmap
     * @return a new cursor
     */
    C cursorOfBitmap(Object bitmap);

    /**
     * @param bitmap concrete bitmap
     * @return size in bits
     */
    int sizeInBits(Object bitmap);

    /**
     * Fill the whole scratch array with all-ones.
     *
     * @param array scratch array
     */
    void fillAll(WordArray<C> array);

    /**
     * Width-specialized buffered OR/XOR driver over sorted bitmaps.
     *
     * @param sink    output sink
     * @param op      OR or XOR
     * @param bufSize scratch size in words
     * @param bitmaps inputs sorted by descending size in bits
     * @param range   maximum size in bits over the inputs
     */
    void runBufferedOrXor(WordSink sink, AggregateOp op, int bufSize,
                          Object[] bitmaps, int range);

    /**
     * @param sink a sink known to wrap a concrete width bitmap
     * @return the unwrapped concrete output bitmap
     */
    Object bitmapOfSink(WordSink sink);
}
