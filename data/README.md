# Benchmark Data

## `raw/table6_100_runs.csv`

This file is the source artifact named
`cbas_benchmark_20260426_065450.csv` in the supplemental upload. It contains
400 successful observations collected on 2026-04-26:

- 100 Sign-Qiao
- 100 Verify-Qiao
- 100 Sign-DSH
- 100 Verify-DSH

The file is retained byte-for-byte except for its descriptive filename. It used
GET for signing under the historical API; the reviewed API and runner use POST.

Recalculated `controllerTimeMs` statistics:

| Operation | n | Mean | Median | Min | Max | Sample StdDev |
|---|---:|---:|---:|---:|---:|---:|
| Sign-Qiao | 100 | 92.24 | 84.50 | 66 | 216 | 27.74 |
| Verify-Qiao | 100 | 113.08 | 104.00 | 90 | 183 | 21.56 |
| Sign-DSH | 100 | 100.94 | 95.00 | 81 | 165 | 18.00 |
| Verify-DSH | 100 | 126.25 | 119.00 | 105 | 275 | 24.61 |

These values exactly reproduce the supplied manuscript Table 6.

The reported dataset corresponds to 100 independent measured runs per
operation using `controllerTimeMs`. The original environment was a MacBook Air
with Apple M1, 8 GB RAM, macOS 13.0, JDK 17, Spring MVC 5.3.39, MyBatis 3.5.16,
and JPBC 2.0.0 with Type A 160/512-bit parameters. No separate unrecorded
warm-up was configured in the supplied benchmark runner.

Other CSV files in the supplemental archive were not integrated: one was a
30-run UI/demo dataset, one was a second 100-run experiment not used by Table 6,
and three contained 400 connection-refused failures each. Excluding them avoids
mistaking failed or unrelated sessions for the reported experiment; their
existence and disposition are documented here for provenance.
