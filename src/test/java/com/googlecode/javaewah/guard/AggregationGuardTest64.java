package com.googlecode.javaewah.guard;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.FastAggregation;
import com.googlecode.javaewah.IteratorAggregation;
import com.googlecode.javaewah.IteratorUtil;
import com.googlecode.javaewah.IteratingRLW;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 64-bit behavior guard for the refactored aggregation state machines.
 */
public class AggregationGuardTest64 extends AggregationGuardTestBase {

    @Override
    protected Variant variant() {
        return VARIANT;
    }

    @Override
    protected Object deserialize(final byte[] bytes) {
        try {
            final EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
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
            return EWAHCompressedBitmap.bitmapOf(positions);
        }

        @Override
        public Object and(final Object... bitmaps) {
            return EWAHCompressedBitmap.and(cast(bitmaps));
        }

        @Override
        public Object or(final Object... bitmaps) {
            if (bitmaps.length == 0)
                return new EWAHCompressedBitmap();
            return EWAHCompressedBitmap.or(cast(bitmaps));
        }

        @Override
        public Object xor(final Object... bitmaps) {
            if (bitmaps.length == 0)
                return new EWAHCompressedBitmap();
            return EWAHCompressedBitmap.xor(cast(bitmaps));
        }

        @Override
        public Object fastOrEmpty() {
            return FastAggregation.or(new EWAHCompressedBitmap[0]);
        }

        @Override
        public Object fastXorEmpty() {
            return FastAggregation.xor(new EWAHCompressedBitmap[0]);
        }

        @Override
        public Object bufferedAnd(final Object... bitmaps) {
            return FastAggregation.bufferedand(512, cast(bitmaps));
        }

        @Override
        public Object bufferedOr(final Object... bitmaps) {
            return FastAggregation.bufferedor(512, cast(bitmaps));
        }

        @Override
        public Object bufferedXor(final Object... bitmaps) {
            return FastAggregation.bufferedxor(512, cast(bitmaps));
        }

        @Override
        public byte[] serialize(final Object bitmap) throws IOException {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutput out = new DataOutputStream(baos);
            ((EWAHCompressedBitmap) bitmap).serialize(out);
            return baos.toByteArray();
        }

        @Override
        public int cardinality(final Object bitmap) {
            return ((EWAHCompressedBitmap) bitmap).cardinality();
        }

        @Override
        public List<Integer> positions(final Object bitmap) {
            return ((EWAHCompressedBitmap) bitmap).toList();
        }

        @Override
        public void iteratorBufferedOrZero() {
            IteratorUtil.materialize(IteratorAggregation.bufferedor(512,
                    new IteratingRLW[0]));
        }

        @Override
        public void iteratorBufferedXorZero() {
            IteratorUtil.materialize(IteratorAggregation.bufferedxor(512,
                    new IteratingRLW[0]));
        }

        @Override
        public void iteratorBufferedAndZero() {
            IteratorUtil.materialize(IteratorAggregation.bufferedand(512,
                    new IteratingRLW[0]));
        }

        private EWAHCompressedBitmap[] cast(final Object[] bitmaps) {
            final EWAHCompressedBitmap[] out =
                    new EWAHCompressedBitmap[bitmaps.length];
            System.arraycopy(bitmaps, 0, out, 0, bitmaps.length);
            return out;
        }
    };
}
