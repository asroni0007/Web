#!/usr/bin/env python3
import csv
import json
import time
import statistics
import urllib.request
import urllib.error
import argparse
import os
from datetime import datetime


DEFAULT_BASE_URL = "http://localhost:8080/scheme/vehicle"
DEFAULT_RUNS = 100

PAYLOAD = {
    "id": "",
    "name": "vehicle-01"
}

TESTS = [
    {
        "operation": "Sign-Qiao",
        "method": "POST",
        "path": "/sign-qiao",
        "body": None
    },
    {
        "operation": "Verify-Qiao",
        "method": "POST",
        "path": "/verify-qiao",
        "body": PAYLOAD
    },
    {
        "operation": "Sign-DSH",
        "method": "POST",
        "path": "/sign-dsh",
        "body": None
    },
    {
        "operation": "Verify-DSH",
        "method": "POST",
        "path": "/verify-dsh",
        "body": PAYLOAD
    },

    # Aktifkan bagian ini kalau ingin menguji aggregate endpoint juga.
    # {
    #     "operation": "SignAgg-Qiao",
    #     "method": "POST",
    #     "path": "/signagg-qiao",
    #     "body": None
    # },
    # {
    #     "operation": "AggVerify-Qiao",
    #     "method": "POST",
    #     "path": "/aggverify-qiao",
    #     "body": None
    # },
    # {
    #     "operation": "SignAgg-DSH",
    #     "method": "POST",
    #     "path": "/signagg-dsh",
    #     "body": None
    # },
    # {
    #     "operation": "AggVerify-DSH",
    #     "method": "POST",
    #     "path": "/aggverify-dsh",
    #     "body": None
    # },
]


def send_request(method, endpoint, body=None):
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json"
    }

    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")

    request = urllib.request.Request(
        endpoint,
        data=data,
        headers=headers,
        method=method
    )

    start = time.perf_counter()

    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            elapsed_client_ms = (time.perf_counter() - start) * 1000
            raw = response.read().decode("utf-8")
            parsed = json.loads(raw)

            return {
                "http_status": response.status,
                "raw": raw,
                "json": parsed,
                "clientElapsedMs": elapsed_client_ms,
                "error": ""
            }

    except urllib.error.HTTPError as e:
        elapsed_client_ms = (time.perf_counter() - start) * 1000
        raw = e.read().decode("utf-8", errors="replace")

        return {
            "http_status": e.code,
            "raw": raw,
            "json": None,
            "clientElapsedMs": elapsed_client_ms,
            "error": raw
        }

    except Exception as e:
        elapsed_client_ms = (time.perf_counter() - start) * 1000

        return {
            "http_status": 0,
            "raw": "",
            "json": None,
            "clientElapsedMs": elapsed_client_ms,
            "error": str(e)
        }


def extract_result(operation, run, method, endpoint, response):
    parsed = response["json"]

    row = {
        "timestamp": datetime.now().isoformat(timespec="seconds"),
        "run": run,
        "operation": operation,
        "method": method,
        "endpoint": endpoint,
        "http_status": response["http_status"],
        "code": "",
        "valid": "",
        "result": "",
        "controllerTimeMs": "",
        "clientElapsedMs": round(response["clientElapsedMs"], 3),
        "message": "",
        "error": response["error"]
    }

    if parsed is not None:
        row["code"] = parsed.get("code", "")
        row["message"] = parsed.get("message", "")

        data = parsed.get("data", {})

        if isinstance(data, dict):
            row["valid"] = data.get("valid", "")
            row["result"] = data.get("result", "")
            row["controllerTimeMs"] = data.get("controllerTimeMs", "")
        else:
            row["result"] = data

    return row


def summarize(rows):
    grouped = {}
    failures = []

    for row in rows:
        op = row["operation"]
        grouped.setdefault(op, [])

        is_valid = row["valid"] is True or str(row["valid"]).lower() == "true"
        is_success = str(row["http_status"]) == "200" and is_valid and not row["error"]

        if is_success and row["controllerTimeMs"] != "":
            grouped[op].append(float(row["controllerTimeMs"]))
        else:
            failures.append(row)

    print("\n=== BENCHMARK SUMMARY: controllerTimeMs ===")
    print("-" * 78)
    print(f"Successful rows: {len(rows) - len(failures)} | Failed/invalid rows: {len(failures)}")
    print(f"{'Operation':<18} {'n':>4} {'Mean':>10} {'Median':>10} {'Min':>8} {'Max':>8} {'StdDev':>10}")
    print("-" * 78)

    summary = {}

    for op, values in grouped.items():
        if not values:
            continue

        mean = statistics.mean(values)
        median = statistics.median(values)
        min_v = min(values)
        max_v = max(values)
        std = statistics.stdev(values) if len(values) > 1 else 0.0

        summary[op] = {
            "n": len(values),
            "mean": mean,
            "median": median,
            "min": min_v,
            "max": max_v,
            "std": std
        }

        print(f"{op:<18} {len(values):>4} {mean:>10.2f} {median:>10.2f} {min_v:>8.2f} {max_v:>8.2f} {std:>10.2f}")

    print("-" * 78)

    compare(summary, "Sign-Qiao", "Sign-DSH")
    compare(summary, "Verify-Qiao", "Verify-DSH")
    compare(summary, "SignAgg-Qiao", "SignAgg-DSH")
    compare(summary, "AggVerify-Qiao", "AggVerify-DSH")


def compare(summary, qiao_key, dsh_key):
    if qiao_key not in summary or dsh_key not in summary:
        return

    qiao = summary[qiao_key]["mean"]
    dsh = summary[dsh_key]["mean"]

    diff_ms = dsh - qiao
    diff_percent = (diff_ms / qiao) * 100 if qiao != 0 else 0

    print(f"\nComparison: {qiao_key} vs {dsh_key}")
    print(f"Qiao mean : {qiao:.2f} ms")
    print(f"DSH mean  : {dsh:.2f} ms")
    print(f"Diff      : {diff_ms:+.2f} ms")
    print(f"Overhead  : {diff_percent:+.2f}%")

    if diff_percent < 0:
        print("Result    : CBAS-DSH is faster in this run.")
    elif diff_percent > 0:
        print("Result    : CBAS-DSH has additional measured overhead.")
    else:
        print("Result    : The measured means are equal.")


def parse_args():
    parser = argparse.ArgumentParser(description="Run the CBAS web benchmark.")
    parser.add_argument("--base-url", default=os.getenv("CBAS_BASE_URL", DEFAULT_BASE_URL))
    parser.add_argument("--runs", type=int, default=int(os.getenv("CBAS_RUNS", DEFAULT_RUNS)))
    parser.add_argument("--warmup", type=int, default=int(os.getenv("CBAS_WARMUP", "0")))
    parser.add_argument("--output", help="Output CSV path; defaults to a timestamped filename.")
    parser.add_argument(
        "--analyze",
        metavar="CSV",
        help="Analyze an existing result CSV without sending HTTP requests."
    )
    return parser.parse_args()


def main():
    args = parse_args()
    if args.runs < 1 or args.warmup < 0:
        raise SystemExit("--runs must be at least 1 and --warmup cannot be negative")

    if args.analyze:
        with open(args.analyze, newline="", encoding="utf-8-sig") as csvfile:
            rows = list(csv.DictReader(csvfile))
        if not rows:
            raise SystemExit("The input CSV contains no observations.")
        print(f"Analyzing: {args.analyze}")
        summarize(rows)
        return

    all_rows = []

    print("Starting CBAS benchmark...")
    print(f"Base URL : {args.base_url}")
    print(f"Runs     : {args.runs}")
    print(f"Warm-up  : {args.warmup}")
    print()

    for test in TESTS:
        operation = test["operation"]
        method = test["method"]
        endpoint = args.base_url.rstrip("/") + test["path"]
        body = test["body"]

        print(f"=== {operation} ===")

        for _ in range(args.warmup):
            send_request(method, endpoint, body)

        for run in range(1, args.runs + 1):
            response = send_request(method, endpoint, body)
            row = extract_result(operation, run, method, endpoint, response)
            all_rows.append(row)

            status = row["http_status"]
            valid = row["valid"]
            controller_time = row["controllerTimeMs"]
            client_time = row["clientElapsedMs"]

            print(
                f"Run {run:02d} | HTTP {status} | valid={valid} | "
                f"controller={controller_time} ms | client={client_time} ms"
            )

        print()

    output_file = args.output or f"cbas_benchmark_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"

    with open(output_file, "w", newline="", encoding="utf-8") as csvfile:
        fieldnames = [
            "timestamp",
            "run",
            "operation",
            "method",
            "endpoint",
            "http_status",
            "code",
            "valid",
            "result",
            "controllerTimeMs",
            "clientElapsedMs",
            "message",
            "error"
        ]

        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(all_rows)

    summarize(all_rows)

    print(f"\nCSV saved to: {output_file}")


if __name__ == "__main__":
    main()
