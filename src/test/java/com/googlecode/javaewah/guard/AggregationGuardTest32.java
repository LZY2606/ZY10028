package com.googlecode.javaewah.guard;

import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import com.googlecode.javaewah32.FastAggregation32;
import com.googlecode.javaewah32.IteratorAggregation32;
import com.googlecode.javaewah32.IteratorUtil32;
import com.googlecode.javaewah32.IteratingRLW32;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 32-bit behavior guard for the refactored aggregation state machines.
 */
public class AggregationGuardTest32 extends AggregationGuardTestBase {

    @Override
    protected Variant variant() {
        return VARIANT;
    }

    @Override
    protected Object deserialize(final byte[] bytes) {
        try {
            final EWAHCompressedBitmap32 bitmap =
                    new EWAHCompressedBitmap32();
            bitmap.deserialize(new DataInputStream(
                    new ByteArrayInputStream(bytes)));
            return bitmap;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Variant VARIANT = new Variant() {
        @Override
        public Object bitmapOf(final int... positions) {
            return EWAHCompressedBitmap32.bitmapOf(positions);
        }

        @Override
        public Object and(final Object... bitmaps) {
            return EWAHCompressedBitmap32.and(cast(bitmaps));
        }

        @Override
        public Object or(final Object... bitmaps) {
            if (bitmaps.length == 0)
                return new EWAHCompressedBitmap32();
            return EWAHCompressedBitmap32.or(cast(bitmaps));
        }

        @Override
        public Object xor(final Object... bitmaps) {
            if (bitmaps.length == 0)
                return new EWAHCompressedBitmap32();
            return EWAHCompressedBitmap32.xor(cast(bitmaps));
        }

        @Override
        public Object fastOrEmpty() {
            return FastAggregation32.or(new EWAHCompressedBitmap32[0]);
        }

        @Override
        public Object fastXorEmpty() {
            return FastAggregation32.xor(new EWAHCompressedBitmap32[0]);
        }

        @Override
        public Object bufferedAnd(final Object... bitmaps) {
            return FastAggregation32.bufferedand(512, cast(bitmaps));
        }

        @Override
        public Object bufferedOr(final Object... bitmaps) {
            return FastAggregation32.bufferedor(512, cast(bitmaps));
        }

        @Override
        public Object bufferedXor(final Object... bitmaps) {
            return FastAggregation32.bufferedxor(512, cast(bitmaps));
        }

        @Override
        public byte[] serialize(final Object bitmap) throws IOException {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutput out = new DataOutputStream(baos);
            ((EWAHCompressedBitmap32) bitmap).serialize(out);
            return baos.toByteArray();
        }

        @Override
        public int cardinality(final Object bitmap) {
            return ((EWAHCompressedBitmap32) bitmap).cardinality();
        }

        @Override
        public List<Integer> positions(final Object bitmap) {
            return ((EWAHCompressedBitmap32) bitmap).toList();
        }

        @Override
        public void iteratorBufferedOrZero() {
            IteratorUtil32.materialize(IteratorAggregation32.bufferedor(512,
                    new IteratingRLW32[0]));
        }

        @Override
        public void iteratorBufferedXorZero() {
            IteratorUtil32.materialize(IteratorAggregation32.bufferedxor(512,
                    new IteratingRLW32[0]));
        }

        @Override
        public void iteratorBufferedAndZero() {
            IteratorUtil32.materialize(IteratorAggregation32.bufferedand(512,
                    new IteratingRLW32[0]));
        }

        private EWAHCompressedBitmap32[] cast(final Object[] bitmaps) {
            final EWAHCompressedBitmap32[] out =
                    new EWAHCompressedBitmap32[bitmaps.length];
            System.arraycopy(bitmaps, 0, out, 0, bitmaps.length);
            return out;
        }
    };
}
