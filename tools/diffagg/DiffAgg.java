import com.googlecode.javaewah.*;
import com.googlecode.javaewah32.*;
import java.io.*;
import java.util.*;

/**
 * Differential byte check: aggregate fixed fixtures with AND/OR/XOR and print
 * the serialized bytes of every result. Run against the baseline class files
 * and the refactored class files; the outputs must be identical.
 */
public class DiffAgg {
    static int[] gen(String shape, int idx) {
        switch (shape) {
            case "empty": return new int[0];
            case "single": return new int[]{idx*7+3};
            case "sparse": {
                int[] o = new int[40];
                for (int k=0;k<o.length;++k) o[k]=k*1009+idx*37+5;
                return o;
            }
            case "dense": {
                int[] o = new int[2000]; int p=0;
                for (int k=0;k<o.length;++k){ p+=1+((k*13+idx)%3); o[k]=p; }
                return o;
            }
            case "run": {
                int[] o = new int[3000]; int p=idx*5;
                for (int k=0;k<o.length;++k) o[k]=p++;
                return o;
            }
            case "tail": {
                int[] o = new int[130];
                for (int k=0;k<o.length;++k) o[k]=k*2+(idx%2);
                int[] e = Arrays.copyOf(o, o.length+3);
                e[o.length]=5000+idx; e[o.length+1]=5001+idx; e[o.length+2]=5050+idx;
                Arrays.sort(e); return e;
            }
            case "mix":
            default: {
                int[] o = new int[500];
                for (int k=0;k<250;++k) o[k]=k*31+idx;
                int p=10000+idx*17;
                for (int k=250;k<o.length;++k) o[k]=p++;
                return o;
            }
        }
    }

    static byte[] ser64(EWAHCompressedBitmap b) throws IOException {
        ByteArrayOutputStream x = new ByteArrayOutputStream();
        b.serialize(new DataOutputStream(x)); return x.toByteArray();
    }
    static byte[] ser32(EWAHCompressedBitmap32 b) throws IOException {
        ByteArrayOutputStream x = new ByteArrayOutputStream();
        b.serialize(new DataOutputStream(x)); return x.toByteArray();
    }
    static String h(byte[] b){ StringBuilder s=new StringBuilder();
        for(byte z:b) s.append(String.format("%02x", z)); return s.toString(); }

    public static void main(String[] args) throws Exception {
        String[] shapes = {"sparse","dense","run","tail","mix","empty","single"};
        StringBuilder out = new StringBuilder();
        for (String shape: shapes) {
            for (int n : new int[]{2,3,20}) {
                EWAHCompressedBitmap[] a64 = new EWAHCompressedBitmap[n];
                EWAHCompressedBitmap32[] a32 = new EWAHCompressedBitmap32[n];
                for (int k=0;k<n;++k){
                    int[] pos = gen(shape,k);
                    a64[k] = EWAHCompressedBitmap.bitmapOf(pos);
                    a32[k] = EWAHCompressedBitmap32.bitmapOf(pos);
                }
                out.append(shape).append('/').append(n);
                if (n >= 2) {
                    out.append(" 64and-static=").append(h(ser64(EWAHCompressedBitmap.and(a64))));
                    out.append(" 64and-buf=").append(h(ser64(FastAggregation.bufferedand(512,a64))));
                    out.append(" 32and-static=").append(h(ser32(EWAHCompressedBitmap32.and(a32))));
                    out.append(" 32and-buf=").append(h(ser32(FastAggregation32.bufferedand(512,a32))));
                }
                out.append(" 64or-static=").append(h(ser64(EWAHCompressedBitmap.or(a64))));
                out.append(" 64or-buf=").append(h(ser64(FastAggregation.bufferedor(512,a64))));
                out.append(" 32or-static=").append(h(ser32(EWAHCompressedBitmap32.or(a32))));
                out.append(" 32or-buf=").append(h(ser32(FastAggregation32.bufferedor(512,a32))));
                out.append(" 64xor-static=").append(h(ser64(EWAHCompressedBitmap.xor(a64))));
                out.append(" 64xor-buf=").append(h(ser64(FastAggregation.bufferedxor(512,a64))));
                out.append(" 32xor-static=").append(h(ser32(EWAHCompressedBitmap32.xor(a32))));
                out.append(" 32xor-buf=").append(h(ser32(FastAggregation32.bufferedxor(512,a32))));
                out.append('\n');
            }
        }
        System.out.print(out);
    }
}
