package com.googlecode.javaewah.benchmark;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.FastAggregation;
import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import com.googlecode.javaewah32.FastAggregation32;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Throughput and allocation benchmarks for the multiway AND/OR/XOR
 * aggregations of the 64-bit and 32-bit EWAH implementations.
 *
 * The inputs are generated once per state and reused across iterations; only
 * the aggregation itself and its result allocation are measured.
 *
 * Run with the repository script:
 *   tools/run_benchmarks.sh
 * which installs the current jar and executes this benchmark through JMH with
 * the GC profiler (alloc rate).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 4, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xmx2048m"})
@State(Scope.Benchmark)
public class AggregationBenchmark {

    /** Fan-in: two-way and twenty-way aggregation. */
    @Param({"2", "20"})
    public int ways;

    private EWAHCompressedBitmap[][] inputs64;
    private EWAHCompressedBitmap32[][] inputs32;

    /** Fixed-size blocks, cycling through the three data shapes. */
    private static final int BLOCKS = 32;
    /** Words spanned by each fixture bitmap. */
    private static final int SPAN_WORDS = 4096;

    /**
     * Build a mixed workload: alternating sparse, dense and long-run
     * bitmaps so the buffered aggregations exercise every branch of the
     * shared state machine.
     */
    @Setup
    public void setup() {
        this.inputs64 = new EWAHCompressedBitmap[BLOCKS][ways];
        this.inputs32 = new EWAHCompressedBitmap32[BLOCKS][ways];
        for (int block = 0; block < BLOCKS; ++block) {
            for (int k = 0; k < ways; ++k) {
                final int[] positions = fixture(block, k);
                this.inputs64[block][k] =
                        EWAHCompressedBitmap.bitmapOf(positions);
                this.inputs32[block][k] =
                        EWAHCompressedBitmap32.bitmapOf(positions);
            }
        }
    }

    private int[] fixture(final int block, final int index) {
        final int max = SPAN_WORDS * 64;
        final int pattern = block % 3;
        final java.util.List<Integer> list = new java.util.ArrayList<Integer>();
        if (pattern == 0) {
            // sparse: ~2% density, widely spread
            for (int p = (index * 53 + block) % 97; p < max; p += 47 + index % 5)
                list.add(p);
        } else if (pattern == 1) {
            // dense: ~70% density with small gaps
            for (int p = 0; p < max; ++p)
                if ((p + index) % 10 < 7)
                    list.add(p);
        } else {
            // long runs: long filled stretches separated by gaps
            int p = (index * 7) % 257;
            while (p < max) {
                final int end = Math.min(max, p + 512 + (index % 4) * 64);
                for (int q = p; q < end; ++q)
                    list.add(q);
                p = end + 193 + index;
            }
        }
        final int[] out = new int[list.size()];
        for (int k = 0; k < out.length; ++k)
            out[k] = list.get(k);
        return out;
    }

    private long sink64(final EWAHCompressedBitmap b) {
        return (long) b.cardinality() + b.sizeInBytes();
    }

    private long sink32(final EWAHCompressedBitmap32 b) {
        return (long) b.cardinality() + b.sizeInBytes();
    }

    // ---- 64-bit ----

    /**
     * @return black-hole score
     */
    @Benchmark
    public long or64() {
        long score = 0;
        for (final EWAHCompressedBitmap[] block : this.inputs64)
            score += sink64(EWAHCompressedBitmap.or(block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long xor64() {
        long score = 0;
        for (final EWAHCompressedBitmap[] block : this.inputs64)
            score += sink64(EWAHCompressedBitmap.xor(block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long and64() {
        long score = 0;
        for (final EWAHCompressedBitmap[] block : this.inputs64)
            score += sink64(EWAHCompressedBitmap.and(block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long bufferedOr64() {
        long score = 0;
        for (final EWAHCompressedBitmap[] block : this.inputs64)
            score += sink64(FastAggregation.bufferedor(512, block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long bufferedXor64() {
        long score = 0;
        for (final EWAHCompressedBitmap[] block : this.inputs64)
            score += sink64(FastAggregation.bufferedxor(512, block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long bufferedAnd64() {
        long score = 0;
        for (final EWAHCompressedBitmap[] block : this.inputs64)
            score += sink64(FastAggregation.bufferedand(512, block));
        return score;
    }

    // ---- 32-bit ----

    /**
     * @return black-hole score
     */
    @Benchmark
    public long or32() {
        long score = 0;
        for (final EWAHCompressedBitmap32[] block : this.inputs32)
            score += sink32(EWAHCompressedBitmap32.or(block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long xor32() {
        long score = 0;
        for (final EWAHCompressedBitmap32[] block : this.inputs32)
            score += sink32(EWAHCompressedBitmap32.xor(block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long and32() {
        long score = 0;
        for (final EWAHCompressedBitmap32[] block : this.inputs32)
            score += sink32(EWAHCompressedBitmap32.and(block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long bufferedOr32() {
        long score = 0;
        for (final EWAHCompressedBitmap32[] block : this.inputs32)
            score += sink32(FastAggregation32.bufferedor(512, block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long bufferedXor32() {
        long score = 0;
        for (final EWAHCompressedBitmap32[] block : this.inputs32)
            score += sink32(FastAggregation32.bufferedxor(512, block));
        return score;
    }

    /**
     * @return black-hole score
     */
    @Benchmark
    public long bufferedAnd32() {
        long score = 0;
        for (final EWAHCompressedBitmap32[] block : this.inputs32)
            score += sink32(FastAggregation32.bufferedand(512, block));
        return score;
    }
}
