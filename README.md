# Experiments for a JVM Mison implementation

See Mison paper at: https://www.microsoft.com/en-us/research/wp-content/uploads/2017/05/mison-vldb17.pdf

## Implementation detail

It follows exactly the design described in the Mison paper. Because there is no way to avx2 extensions from the JVM this part is written in rust and wrapped in a native call.

## Benchmarks

```
Benchmark            (entry)  Mode  Cnt       Score       Error  Units

Benchmarks.fastJson    empty  avgt  200     244.944 ±     25.159  ns/op
Benchmarks.fastJson    short  avgt  200    1171.627 ±     51.561  ns/op
Benchmarks.fastJson   medium  avgt  200    2967.922 ±    156.079  ns/op
Benchmarks.fastJson     long  avgt  200   12229.239 ±    407.896  ns/op
Benchmarks.fastJson     huge  avgt  200  118164.704 ±   3499.133  ns/op
Benchmarks.fastJson    giant  avgt  200  438202.179 ±  19309.856  ns/op

Benchmarks.jackson     empty  avgt  200    4872.127 ±   2271.468  ns/op
Benchmarks.jackson     short  avgt  200    9131.281 ±   7338.789  ns/op
Benchmarks.jackson    medium  avgt  200    5470.434 ±    534.308  ns/op
Benchmarks.jackson      long  avgt  200   11512.887 ±    889.058  ns/op
Benchmarks.jackson      huge  avgt  200  569606.131 ± 997983.165  ns/op
Benchmarks.jackson     giant  avgt  200  517020.023 ± 742322.271  ns/op

Benchmarks.mison       empty  avgt  200    2033.237 ±     85.180  ns/op
Benchmarks.mison       short  avgt  200    3312.618 ±    141.704  ns/op
Benchmarks.mison      medium  avgt  200    5707.338 ±    264.357  ns/op
Benchmarks.mison        long  avgt  200    8287.681 ±    562.832  ns/op
Benchmarks.mison        huge  avgt  200   38999.464 ±   3946.958  ns/op
Benchmarks.mison       giant  avgt  200  137388.974 ±  13729.804  ns/op
```

## Conclusion

__fastJson__ is super fast :) This mison implementation starts being more performant at the `long` test case (around 1kb). The overhead of the native call makes it less interesting for small json.