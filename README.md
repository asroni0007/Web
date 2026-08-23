# CBAS-DSH Software Simulation and Reproducibility Package

This repository contains the complete software simulation accompanying the
manuscript currently under review. It provides the Java implementation,
Spring MVC web application, benchmark runner, and raw observations used to
evaluate the controller-time cost of the baseline CBAS construction attributed
to Qiao et al. and the proposed **CBAS-DSH** construction.

The repository is organized so that reviewers can:

1. inspect the implemented equations and additional security mechanisms;
2. build and run the same web-based simulation environment;
3. repeat the four individual sign/verify benchmarks; and
4. independently recalculate the descriptive statistics reported in Table 6.

## Quick guide for reviewers

| Review objective | File or location |
|---|---|
| Inspect Qiao and CBAS-DSH algorithms | `src/main/java/com/abc/service/impl/VehicleServiceImpl.java` |
| Inspect REST timing instrumentation | `src/main/java/com/abc/controller/VehicleController.java` |
| Inspect web benchmark interface | `src/main/webapp/pages/element.html` |
| Inspect correctness and tamper tests | `src/test/java/com/abc/service/impl/VehicleServiceImplTest.java` |
| Repeat the experiment | `benchmark_cbas.py` |
| Recalculate Table 6 | `data/raw/table6_100_runs.csv` |
| Check data provenance | `data/README.md` |
| Check dependencies and build settings | `pom.xml` |
| Check automated clean build | `.github/workflows/build.yml` |

For a rapid verification of the reported results, reviewers may proceed
directly to [Reproducing Table 6](#reproducing-table-6).

## Implemented functionality

### Baseline CBAS (Qiao et al.)

The implementation includes:

- system setup and public-parameter generation;
- user secret/public key generation;
- certificate generation and certificate validation;
- individual signing and verification; and
- aggregate signing and aggregate verification.

The primary verification relation is implemented as:

```text
delta_i P = h2_i(R_i + h1_i P_pub) + h3_i U_i + h4_i X_i
```

### Proposed CBAS-DSH

CBAS-DSH implements the same principal sign/verify relation together with the
three security extensions discussed in the manuscript:

1. **Domain-separated hashing**
   - certificate and signature domains use distinct tags;
   - the implementation uses separate labels for H1, H2, H3, H4, HN, and PoP;
   - every hash component is length-prefixed before hashing to prevent
     concatenation ambiguity.

2. **Deterministic nonce derivation**

   ```text
   u_i = H_N(tag_sig || ID_i || M_i || x_i)
   U_i = u_i P
   ```

3. **Proof of Possession (PoP)**

   ```text
   c_i = H(tag_cert || ID_i || X_i || W_i)
   z_i = w_i + c_i x_i
   z_i P = W_i + c_i X_i
   ```

Hashing uses SHA-256, and the JPBC Type A pairing parameters are generated with
`rBits = 160` and `qBits = 512`, matching the supplied simulation source.

## Software architecture

```text
HTTP request
    -> Spring MVC VehicleController
    -> VehicleService interface
    -> VehicleServiceImpl
       -> JPBC setup and group operations
       -> Qiao or CBAS-DSH simulation
    -> JSON response containing controllerTimeMs
```

The controller measures the service execution interval using
`System.nanoTime()` and returns the result as `controllerTimeMs`. The web client
and Python runner additionally measure client-observed elapsed time. The paper's
Table 6 uses `controllerTimeMs` only.

## Repository structure

```text
.
├── .github/workflows/build.yml
├── benchmark_cbas.py
├── data/
│   ├── README.md
│   └── raw/table6_100_runs.csv
├── pom.xml
└── src/main/
    ├── java/com/abc/
    │   ├── config/
    │   ├── controller/
    │   ├── dao/
    │   ├── domian/
    │   └── service/
    │       ├── VehicleService.java
    │       └── impl/VehicleServiceImpl.java
    ├── resources/jdbc.properties
    └── webapp/
        ├── WEB-INF/web.xml
        ├── index.jsp
        └── pages/element.html
```

Generated build output, IDE metadata, local credentials, and operating-system
metadata are excluded from the reviewed source package through `.gitignore`.

## Requirements

- JDK 17
- Maven 3.9 or newer
- Apache Tomcat 9 or another Servlet 4-compatible container
- Python 3.8 or newer for the automated benchmark
- Network access to the Maven repositories declared in `pom.xml`

The application uses Spring MVC 5 and `javax.servlet`; therefore, Tomcat 9 is
recommended. Tomcat 10+ uses the `jakarta.servlet` namespace and requires a
migration that is outside the configuration evaluated in the manuscript.

## Original experimental environment

The measurements reported in Table 6 were collected using the following
software-simulation environment:

| Component | Configuration |
|---|---|
| Hardware | MacBook Air with Apple M1 |
| Memory | 8 GB RAM |
| Operating system | macOS 13.0 |
| Java | JDK 17 |
| Web framework | Spring MVC 5.3.39 |
| Persistence framework | MyBatis 3.5.16 |
| Pairing library | JPBC 2.0.0 |
| Pairing parameters | Type A, `rBits = 160`, `qBits = 512` |
| Application packaging | Maven WAR deployed with context path `/scheme` |
| Measured operations | Sign-Qiao, Verify-Qiao, Sign-DSH, Verify-DSH |
| Independent runs | 100 per operation |
| Reported metric | Backend `controllerTimeMs` |
| Warm-up policy | No separate unrecorded warm-up configured in the supplied runner |

Exact latency can vary across machines because pairing-parameter generation,
JVM behavior, garbage collection, entropy generation, and host load are part of
the evaluated full controller execution path. Reproduction on different
hardware should therefore prioritize the relative comparison and retain the
new raw CSV for inspection.

## Build instructions

From a fresh clone, run:

```bash
mvn clean verify
```

The build produces:

```text
target/scheme.war
```

Deploy `scheme.war` to Tomcat 9 and start the server. The benchmark interface
is then available at:

```text
http://localhost:8080/scheme/
```

The included GitHub Actions workflow performs the same clean Maven verification
and uploads the resulting WAR as a workflow artifact.

## Automated correctness tests

Running `mvn clean verify` executes the JUnit test suite in addition to building
the WAR. The tests exercise both successful and deliberately modified inputs:

| Test category | Expected result |
|---|---|
| Qiao individual sign and verify | Accepted |
| CBAS-DSH individual sign and verify | Accepted |
| Qiao and CBAS-DSH aggregate operations | Accepted |
| Qiao message modified after signing | Rejected |
| CBAS-DSH message modified after signing | Rejected |
| CBAS-DSH signature scalar modified | Rejected |
| Aggregate member removed without updating aggregate | Rejected |
| Identical hash inputs | Identical field element |
| Different hash-function role or domain | Different field element |
| Deterministic nonce formula recomputation | Matches stored nonce |
| Valid PoP equation | Accepted |
| Modified PoP response | Rejected |

The negative tests call the same internal sign/verify helpers used by the web
simulation. Reflection is limited to the test source because the simulation's
internal signature structures are intentionally not exposed as public API.

## REST endpoints

| Scheme | Operation | Method | Endpoint |
|---|---|---|---|
| Qiao et al. | Sign | POST | `/scheme/vehicle/sign-qiao` |
| Qiao et al. | Verify | POST | `/scheme/vehicle/verify-qiao` |
| CBAS-DSH | Sign | POST | `/scheme/vehicle/sign-dsh` |
| CBAS-DSH | Verify | POST | `/scheme/vehicle/verify-dsh` |
| Qiao et al. | Aggregate sign | POST | `/scheme/vehicle/signagg-qiao` |
| Qiao et al. | Aggregate verify | POST | `/scheme/vehicle/aggverify-qiao` |
| CBAS-DSH | Aggregate sign | POST | `/scheme/vehicle/signagg-dsh` |
| CBAS-DSH | Aggregate verify | POST | `/scheme/vehicle/aggverify-dsh` |

Example request:

```bash
curl -X POST http://localhost:8080/scheme/vehicle/verify-dsh \
  -H 'Content-Type: application/json' \
  -d '{"name":"vehicle-01"}'
```

Representative response structure:

```json
{
  "data": {
    "scheme": "CBAS-DSH",
    "operation": "Verify-DSH",
    "result": 1,
    "valid": true,
    "controllerTimeMs": 119
  },
  "code": 20041,
  "message": "CBAS-DSH Verify success"
}
```

## Running the automated experiment

The runner uses only Python's standard library. With Tomcat running, execute:

```bash
python3 benchmark_cbas.py \
  --base-url http://localhost:8080/scheme/vehicle \
  --runs 100 \
  --output results.csv
```

Available options:

```text
--base-url URL   REST base URL
--runs N         measured requests per operation (default: 100)
--warmup N       unrecorded warm-up requests per operation (default: 0)
--output FILE    destination CSV filename
--analyze CSV    analyze an existing CSV without contacting the server
```

The runner executes the four operations sequentially, records one row per
request, distinguishes successful and failed/invalid rows, writes the raw CSV,
and prints n, mean, median, minimum, maximum, and sample standard deviation.

The protocol associated with Table 6 uses exactly 100 independent measured
runs per operation. The runner accepts another positive value for exploratory
experiments, but such runs are not part of the reported Table 6 dataset.

## Reproducing Table 6

The supplied file `data/raw/table6_100_runs.csv` contains 100 successful
observations for each of the four individual operations. Independent
recalculation of `controllerTimeMs` gives:

Reviewers can reproduce the summary immediately, without running Tomcat:

```bash
python3 benchmark_cbas.py --analyze data/raw/table6_100_runs.csv
```

| Operation | n | Mean (ms) | Median (ms) | Min (ms) | Max (ms) | StdDev |
|---|---:|---:|---:|---:|---:|---:|
| Sign-Qiao | 100 | 92.24 | 84.50 | 66.00 | 216.00 | 27.74 |
| Verify-Qiao | 100 | 113.08 | 104.00 | 90.00 | 183.00 | 21.56 |
| **Sign-DSH** | **100** | **100.94** | **95.00** | **81.00** | **165.00** | **18.00** |
| **Verify-DSH** | **100** | **126.25** | **119.00** | **105.00** | **275.00** | **24.61** |

These values reproduce Table 6 exactly. Relative to Qiao et al., the measured
mean overhead is:

- Sign: `(100.94 - 92.24) / 92.24 = 9.43%` or `+8.70 ms`
- Verify: `(126.25 - 113.08) / 113.08 = 11.65%` or `+13.17 ms`

Python's `statistics.stdev` is used, corresponding to sample standard deviation.

## Measurement scope

For transparent interpretation, `controllerTimeMs` encloses the complete Java
service call. In the supplied implementation, each request creates a fresh
simulation context. The measured path therefore includes:

- JPBC Type A parameter generation;
- pairing and public-parameter setup;
- user key and certificate generation;
- signing; and
- for verify endpoints, creation and verification of a valid test signature.

This scope is the **full controller execution time for the simulated
operation** evaluated by the web application. It should not be interpreted as
an isolated microbenchmark of a single cryptographic primitive.
Browser/client elapsed time additionally includes HTTP dispatch, JSON
processing, networking, and client scheduling and is not used in Table 6.

## Correctness and reproducibility notes

- Both schemes are executed under the same controller, JPBC parameters, client,
  and request sequence.
- Hash inputs are length-prefixed before SHA-256 processing.
- CBAS-DSH uses distinct labels and domain tags for independent hash roles.
- Deterministic nonce derivation depends on identity, message, domain tag, and
  the user's secret scalar.
- The CBAS-DSH scenario validates PoP before continuing to certificate and
  signature processing.
- The verify endpoints construct valid test instances internally. They are
  simulation endpoints, not public verification APIs accepting serialized
  third-party signatures.
- JVM JIT compilation, garbage collection, entropy generation, and host load can
  influence repeated measurements. Reviewers should retain the generated raw
  CSV and report any different environment or warm-up configuration.

## Database configuration

The cryptographic benchmark service does not query the legacy DAO during the
four reported operations. Database configuration is retained for compatibility
with the original Spring/MyBatis application and uses environment variables:

```bash
export DB_URL='jdbc:mysql://localhost:3306/Text1?useSSL=false&serverTimezone=UTC'
export DB_USERNAME='cbas_app'
export DB_PASSWORD='local-password'
```

No database password is stored in this reviewed repository.

## Scope of this release

This package is intended to support manuscript evaluation and reproduction of
the reported software-simulation results. It includes the implementation and
raw dataset needed for that purpose. It is not intended as a production-ready
cryptographic API, a standardized protocol implementation, or a replacement
for independent cryptographic security analysis.

## Contact and citation

Because the associated manuscript remains under review, complete bibliographic
metadata can be added after the review process. Questions about the simulation
or reproduction procedure should be directed through the manuscript's
corresponding-author contact channel.
