#!/usr/bin/env python3
"""Compare two JMH text summaries: throughput (us/op) and normalized alloc (B/op)."""
import re, sys

def parse(path):
    rows = {}
    for line in open(path):
        m = re.match(r'^(AggregationBenchmark\.[A-Za-z0-9]+(?::gc\.alloc\.rate\.norm)?)\s+(\d+)\s+avgt\s+\d+\s+([\d.]+)\s+', line)
        if m:
            rows[(m.group(1), int(m.group(2)))] = float(m.group(3))
    return rows

base = parse(sys.argv[1])
ref = parse(sys.argv[2])
keys = sorted(set(k[0] for k in base) | set(k[0] for k in ref))
print("%-34s %5s %12s %12s %9s %12s %12s %9s" % (
    "benchmark", "ways", "base us/op", "ref us/op", "time %",
    "base B/op", "ref B/op", "alloc %"))
worst_t = worst_a = 0.0
for name in keys:
    for ways in (2, 20):
        bt = base.get((name, ways)); rt = ref.get((name, ways))
        an = name + ":gc.alloc.rate.norm"
        ba = base.get((an, ways)); ra = ref.get((an, ways))
        if bt and rt and ':gc.' not in name:
            dt = (rt / bt - 1) * 100
            da = (ra / ba - 1) * 100 if (ba and ra) else float('nan')
            if not (ba and ra):
                # time rows carry no allocation; read the paired norm row
                an2 = name + ':gc.alloc.rate.norm'
                ba = base.get((an2, ways)); ra = ref.get((an2, ways))
                da = (ra / ba - 1) * 100 if (ba and ra) else float('nan')
            worst_t = max(worst_t, dt); worst_a = max(worst_a, da)
            print("%-34s %5d %12.2f %12.2f %+8.1f%% %12.0f %12.0f %+8.1f%%" % (
                name.replace("AggregationBenchmark.", ""), ways,
                bt, rt, dt, ba or 0, ra or 0, da))
print("\nworst throughput regression: %+.1f%%   worst alloc regression: %+.1f%%" % (worst_t, worst_a))
print("acceptance: time <= +10%% and alloc <= +5%% -> %s" % (
    "PASS" if worst_t <= 10.0 and worst_a <= 5.0 else "FAIL"))
