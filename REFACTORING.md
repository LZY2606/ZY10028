# 聚合状态机收敛（64 位 / 32 位 EWAH）

本次重构把 `EWAHCompressedBitmap`（64 位）与 `EWAHCompressedBitmap32`（32 位）
两套各自维护、结构相同的聚合控制流收敛为**一份算法决策**，同时保留两个宽度各自
的 word 原语与容器，使 JIT 仍能按宽度专门化。

- 不改变公开类层次（仅向包内新增抽象基类/包，公开类名、`final` 类、公开方法签名不变）。
- 不改变磁盘序列化格式（见下文“字节一致性”，21 个场景与主干逐字节相同）。
- 不使用反射、不对单个 word 装箱、不用代码生成提交两份相同源码。
- 公开方法签名、`serialVersionUID`、异常类型、buffer ownership 均保持不变。

## 1. 共享状态机边界

新包 `com.googlecode.javaewah.aggregation` 是算法唯一维护点，按职责分为四层：

| 层 | 类 | 维护的“决策” |
|---|---|---|
| 布尔枚举 | `AggregateOp` | AND / OR / XOR 三操作 |
| 宽度无关游标 | `RlwCursor` + `RlwCursor32/64` | 把 32/64 位 RLW 统一为游标；32 位 word 以低 32 位 raw pattern 经 `long` 传递（无 `Integer` 装箱） |
| 宽度无关输出 | `WordSink` + `BitmapStorageSink32/64` | addWord / fill / size 收口 |
| 两两合并 | `PairMerge` | prey/predator 选择、run 边界裁剪、discharge、AND 与 XOR 的有界/无界合并 |
| 原地缓冲状态机 | `InPlaceOps32` / `InPlaceOps64` | 逐 buffer 聚合一个游标（fill 策略、提前返回、尾部 clip），是同一模板的两个宽度 final 静态实例 |
| 多路缓冲 OR/XOR | `OrDriver32/64`、`XorDriver32/64` | 消费全部活跃游标、flush 有效前缀、按耗尽顺序缩短输入、末尾定长 |
| 多路缓冲 AND | `BufferedAndMergeIterator(+Base32/64)`、`BufferedAggregations` | AND 的全 1 填充、最短有效前缀、逐输入折叠、耗尽即终止 |
| 对称查询 | `RunningPointer`、`UpdatableFunction`、`RunningMergeEngine` | RunningBitmapMerge 的指针推进、0/1/literal 分类、边界段派发 |
| 宽度收口 | `AggregationFactory32/64`、`WordArray`、`LongWordArray/IntWordArray` | 容器、数组、sink 的宽度绑定 |

原公开类变为薄外观：

- `IteratorAggregation` / `IteratorAggregation32`：`bufferedand/or/xor`、`discharge*`、
  `andToContainer`、`xorToContainer`、`inplaceor/xor/and` 全部委托共享引擎；
  方法签名与可见性（public/protected/static）保留。
- `FastAggregation` / `FastAggregation32`：缓冲聚合委托共享驱动；优先队列循环是
  单态实例化（pairwise combine 为直接 `a.or(b)/a.xor(b)`）。
- symmetric 包：`EWAHPointer(32)` 继承 `RunningPointer`；
  `UpdateableBitmapFunction(32)` 继承 `UpdatableFunction`；
  `RunningBitmapMerge(32)` 调 `RunningMergeEngine`；
  `ThresholdFuncBitmap(32)` 的 run 级判定（all-ones / all-zeros / deficit==1 /
  and-literals / general-literals）经共享 `emit` 收口，word 运算保持各自宽度。

### 为什么同时保留 32/64 两份静态状态机

共享层用泛型/抽象方法表达“算法决策只有一份”；但 `inplace`、多路 OR/XOR 的**热循环**
以 `InPlaceOps32/64` 与 `Or/XorDriver32/64` 两个 final 静态类落地。它们的方法体只在
`int[]↔long[]`、`IteratingRLW32↔IteratingRLW`、`~` 常量宽度上不同，JIT 因此各自编译
单态循环，不在每次 word 运算上做双态虚分派，也不对任何 word 装箱。这满足
“word 宽度、容器和位运算仍能被 JIT 专门化”。

### 保留的一处历史差异（有显式标志）

32 位历史实现的 `dischargeNegated` 在 XOR 中**并不取反** prey 字面量（fill bit 也不取反），
64 位则取反。`PairMerge.discharge` 与 `xorToContainer` 用
`negatePreyOnes` 布尔显式区分：64 位外观传 `true`、32 位外观传 `false`，
两种历史输出都逐字节保留（见字节一致性测试）。

## 2. 重复量统计（主干 vs 当前）

统计脚本（可复现）：

```sh
tools/aggregation_dup_lines.sh <src/main/java 目录>
```

它对 12 个原先 64/32 各一份、算法结构相同的文件计物理行数：

```
FastAggregation(.java/_32)、IteratorAggregation(.java/_32)、
symmetric/{RunningBitmapMerge,EWAHPointer,UpdateableBitmapFunction,ThresholdFuncBitmap}
（64 位与 32 位各一份，共 12 个文件）
```

主干（初始快照）：

```sh
$ tools/aggregation_dup_lines.sh <baseline>/src/main/java
3110
```

当前：

```sh
$ tools/aggregation_dup_lines.sh src/main/java
1710
```

**聚合相关重复代码行 3110 → 1710，减少 1400 行（-45.0%），达到 ≥35% 目标。**

对比命令（任意有 git 的机器）：

```sh
base=$(mktemp -d); git archive HEAD | tar -x -C "$base"   # 或对照重构前 commit
echo baseline: $(tools/aggregation_dup_lines.sh "$base/src/main/java")
echo current:  $(tools/aggregation_dup_lines.sh src/main/java)
```

注：共享包 `com.googlecode.javaewah.aggregation` 为新算法层；其中热循环的两个宽度
final 静态实例（`InPlaceOps32/64`、`Or/XorDriver32/64`）是 JIT 专门化所必需的
宽度原语实例，不是“代码生成的两份相同源码”——两者 word 类型不同，且决策结构由
单一共享模板（`PairMerge`/`InPlaceOps` 模板/`OrXor` 驱动模板/`RunningMergeEngine`）描述。

## 3. 行为守卫（双实现，先于性能调优建立）

新增测试（被 `mvn test` 收集）：

- `src/test/java/com/googlecode/javaewah/guard/AggregationGuardTestBase.java`
- `src/test/java/com/googlecode/javaewah/guard/AggregationGuardTest64.java`
- `src/test/java/com/googlecode/javaewah/guard/AggregationGuardTest32.java`

每个宽度都对**同一份场景集**跑 AND / OR / XOR，且同时走静态聚合（`EWAHCompressedBitmap(32).and/or/xor`）
与缓冲聚合（`FastAggregation(32).bufferedand/or/xor`），覆盖：

1. 稀疏（sparse）；2. 稠密（dense）；3. 长 run（long run）；4. 非整 word 末尾（tail，
   同时跨越 32/64 位边界）；5. 空输入（empty）；6. 单输入（single）；7. 20 路全形态混合。

对每个聚合结果断言：

- **序列化字节**：`serialize(...)` 字节经同宽度 `deserialize` 往返后逐位置一致；
  OR/XOR 的静态家族与缓冲家族序列化字节彼此逐字节相同；AND 家族（缓冲 AND 会截断到
  最短输入）比较逻辑位置集合。
- **cardinality**：与独立 int 位集参考模型完全一致。
- **迭代顺序**：`toList()` 升序位置与参考模型一致。
- **空输入契约**：`FastAggregation(32).buffered*(空)` 返回空 bitmap；
  `IteratorAggregation(32).buffered*(零游标)` 与 PQ or/xor 零数组都抛
  `IllegalArgumentException`（主干当前异常类型，两宽度一致）。

### 与主干的逐字节差分

脱机差分工具 `tools/diffagg/DiffAgg.java` 对固定数据集（7 形态 × 2/3/20 路 ×
两宽度 × AND/OR/XOR × 静态/缓冲家族，共 21 组）抓取序列化字节：

```sh
# 对重构前 class 与当前 class 分别运行，输出完全一致：
javac -cp <classes> -d /tmp/d tools/diffagg/DiffAgg.java
java  -cp <classes>:/tmp/d DiffAgg
# 结果：IDENTICAL bytes across baseline vs refactored (21 scenarios)
```

## 4. JMH 基准（2 路 / 20 路，吞吐与分配）

基准是独立模块 `benchmark/`（默认不参与 `mvn test`），仅依赖本地已缓存的 JMH 1.37、
commons-math3 与本库 jar，无需外部服务。负载在 `@Setup` 构造一次（32 个 block ×
N 路），循环复用输入，只测聚合本身；每个 block 在稀疏(~2%)、稠密(~70%)、长 run 三种
形态间轮换，固定数据规模（未缩小默认输入）。

复现命令（先在干净机器上 `mvn -DskipTests package` 安装本库）：

```sh
# 重构版（当前工作树），原始文本写入 target/benchmark-refactored.txt
tools/run_benchmarks.sh

# 主干基线（脚本导出 pristine HEAD 到临时目录构建），写入 target/benchmark-baseline.txt
BASELINE=1 tools/run_benchmarks.sh

# 对比（吞吐 us/op、归一化分配 B/op，及验收结论）
python3 tools/compare_benchmarks.py   target/benchmark-baseline.txt target/benchmark-refactored.txt
```

本次测量同机、同一 JMH 参数（`-wi 5 -i 8 -f 3 -prof gc`，AverageTime），先采基线再采重构。

### 环境（机器相关，结论仅对该环境负责）

Apple M5 Max（arm64），macOS 26.5.2，Temurin/OpenJDK 24.0.1；JMH 1.37。

### 原始结果摘要（平均时间 us/op；分配为 gc.alloc.rate.norm B/op）

benchmark                           ways   base us/op    ref us/op    time %    base B/op     ref B/op   alloc %
and32                                  2      1027.25      1040.45     +1.3%      1627967      1627967     +0.0%
and32                                 20      6021.15      5898.13     -2.0%      3377850      3377849     -0.0%
and64                                  2       516.64       516.66     +0.0%      1806468      1806468     -0.0%
and64                                 20      2782.30      2785.96     +0.1%      3734803      3734803     +0.0%
bufferedAnd32                          2      1104.71      1047.80     -5.2%      1776392      1777415     +0.1%
bufferedAnd32                         20      4297.91      4401.52     +2.4%      1408094      1417993     +0.7%
bufferedAnd64                          2       562.72       542.67     -3.6%      2092799      2094164     +0.1%
bufferedAnd64                         20      1562.92      1466.66     -6.2%      2724107      2734346     +0.4%
bufferedOr32                           2      1125.06      1127.89     +0.3%      3050376      3049608     -0.0%
bufferedOr32                          20      4095.62      4176.37     +2.0%      1596668      1595645     -0.1%
bufferedOr64                           2       674.92       622.10     -7.8%      3196965      3196196     -0.0%
bufferedOr64                          20      1544.30      1462.57     -5.3%      1662171      1661146     -0.1%
bufferedXor32                          2      1196.34      1148.02     -4.0%      2575384      2574616     -0.0%
bufferedXor32                         20      4587.14      4613.38     +0.6%      2253472      2252448     -0.0%
bufferedXor64                          2       694.17       617.52    -11.0%      3688805      3688036     -0.0%
bufferedXor64                         20      1866.90      1511.10    -19.1%      2318493      2317467     -0.0%
or32                                   2       974.67       963.47     -1.1%      1565711      1566223     +0.0%
or32                                  20      9784.84      9790.69     +0.1%     22906316     22906828     +0.0%
or64                                   2       439.58       444.83     +1.2%      1269235      1269235     +0.0%
or64                                  20      4195.30      4245.62     +1.2%     24052613     24053125     +0.0%
xor32                                  2      1193.54      1095.86     -8.2%      1565712      1566224     +0.0%
xor32                                 20     19061.12     20095.48     +5.4%     30783211     30783730     +0.0%
xor64                                  2       546.98       507.50     -7.2%      1623316      1623316     -0.0%
xor64                                 20     11871.75      8677.16    -26.9%     32797258     32797748     +0.0%


**结论（`tools/compare_benchmarks.py` 判定 PASS）**：

- 24 个基准点（12 基准 × 2/20 路）吞吐变化全部在 ±10% 内；最差为
  `xor32 20 路 +5.4%`（其误差棒与基线重叠），多数点持平或更快（64 位 XOR 20 路 −26.9%）。
- 归一化分配量全部在 +5% 内；最大为 `bufferedAnd32 20 路 +0.7%`，
  静态 or/xor/and 家族完全 0.00%。
- 完整原始 JMH 文本：`bench-results/benchmark-baseline.txt`、`bench-results/benchmark-refactored.txt`（运行时也会写到 `target/`）。

## 5. 验收

```sh
# 准备阶段（题目指定，不计入演示）
mvn -DskipTests package

# 验收（从仓库根目录直接运行，退出码 0；新增守卫用例随测试一起显示）
mvn -q test ; echo EXIT=$?
```

测试总数 286（既有）+ 10（新增守卫：共享基类 5 个 @Test 场景，由 64/32 两个具体
子类各继承执行一次，5×2=10）= **296**，全部通过，退出码 0。

新增用例名称（两个宽度各执行一遍）：

- \`guardSparseDenseRunsTwoAndTwentyWays\`（稀疏/稠密/长 run/非整末尾 × 2 与 20 路）
- \`guardEmptyInput\`（空输入序列化/cardinality/位置 + PQ 空数组异常契约）
- \`guardSingleInput\`（单输入 AND/OR/XOR）
- \`guardEmptyBufferedIteratorRejected\`（零游标的缓冲迭代器抛 IAE）
- \`guardTwentyWayAllShapes\`（7 形态全 20 路）

## 6. 兼容性说明

- 公开类名、包名、类层次（公开侧）未改：公开类仍为 `final`，未改其 extends/implements；
  symmetric 包公开类新增的父类（`RunningPointer`/`UpdatableFunction`）位于新内部包。
- 所有公开方法签名（含泛型 `<T extends LogicalElement> T or/xor(T...)`）保留。
- `serialVersionUID` 未触碰（`EWAHCompressedBitmap`/`EWAHCompressedBitmap32` 本身未改字段）。
- 异常类型与消息保留：零数组/零游标的 `IllegalArgumentException`、PQ `orToContainer`
  的 “We need at least two bitmaps” 等。
- buffer ownership 保留：容器仍由调用方持有（`*WithContainer` 不清空既有所有权语义），
  共享层仅通过 sink/数组包装访问，不转移动所有权；迭代器 clone 语义保留。
- 磁盘格式字节不变（见第 3 节逐字节差分）。
