#!/usr/bin/env python3
"""Parse Karma console log into structured JSON with detailed assertion failures."""

import argparse
import datetime as dt
import json
import os
import re
import sys
from typing import List, Optional, Tuple

ANSI_RE = re.compile(r"\x1b\[[0-9;]*[mK]")
SUMMARY_RE = re.compile(
    r"Summary:\s*(?P<total>\d+)\s+tests,\s*(?P<passed>\d+)\s+passed,\s*(?P<failed>\d+)\s+failed(?:,\s*(?P<rate>\d+)%)?",
    re.IGNORECASE,
)
LOCATION_RE = re.compile(r"(.+\.(?:clj|cljs|cljc|js)):(\d+)(?::(\d+))?")


def strip_ansi(value: str) -> str:
    return ANSI_RE.sub("", value)


def normalise_suite(path: str) -> str:
    if not path:
        return "Unknown suite"
    base = re.sub(r"\.(clj|cljs|cljc|js)$", "", path)
    base = base.replace("/", ".")
    base = base.replace("_", "-")
    return base or "Unknown suite"


class FailureCollector:
    def __init__(self) -> None:
        self.current_test: Optional[str] = None
        self.current_header: Optional[str] = None
        self.block_lines: List[str] = []
        self.failures: List[dict] = []
        self.unique_tests: set[str] = set()
        self.summary_line: Optional[str] = None
        self.seen_headers: set[str] = set()
        self.total_headers: int = 0

    def _flush(self) -> None:
        if not self.current_header:
            return
        header = self.current_header.strip()
        # Deduplicate identical assertion headers (some reporters print FAIL blocks twice)
        self.total_headers += 1
        if header in self.seen_headers:
            self.current_header = None
            self.block_lines = []
            return
        match = re.match(r"\s*FAIL in\s+\(([^)]+)\)\s+\(([^)]+)\)", header)
        if match:
            test_name = match.group(1).strip()
            location = match.group(2).strip()
        else:
            test_name = (self.current_test or header).strip()
            location = ""

        file_path = ""
        line_no: Optional[int] = None
        if location:
            loc_match = LOCATION_RE.match(location)
            if loc_match:
                file_path = loc_match.group(1)
                try:
                    line_no = int(loc_match.group(2))
                except ValueError:
                    line_no = None
            else:
                file_path = location

        message = ""
        log_lines: List[str] = []
        for raw_line in self.block_lines:
            cleaned = strip_ansi(raw_line).strip()
            cleaned = cleaned.rstrip("'")
            cleaned = cleaned.strip()
            if not cleaned:
                continue
            log_lines.append(cleaned)
            if not message:
                if cleaned.startswith('"') and cleaned.endswith('"') and len(cleaned) > 1:
                    message = cleaned.strip('"')
                elif cleaned.lower().startswith("expected"):
                    message = cleaned

        if not message:
            message = header

        suite = normalise_suite(file_path)
        entry = {
            "name": test_name,
            "suite": suite,
            "success": False,
            "message": message,
            "log": [header] + log_lines,
            "file": file_path,
        }
        if line_no is not None:
            entry["line"] = line_no

        self.failures.append(entry)
        self.unique_tests.add(test_name)
        self.seen_headers.add(header)
        self.current_header = None
        self.block_lines = []

    def handle_payload(self, payload: str) -> None:
        payload = payload.strip()
        if payload.startswith("'") and payload.endswith("'") and len(payload) >= 2:
            payload = payload[1:-1]
        payload = payload.strip().rstrip("'").strip()
        if payload.startswith("'"):
            payload = payload[1:].strip()
        if payload.startswith("[KARMA-ADAPTER]"):
            payload = payload[len("[KARMA-ADAPTER]") :].strip()
        if not payload:
            return
        if payload.startswith("Summary:"):
            # Flush any open block before capturing summary
            self._flush()
            self.summary_line = payload
            return
        if payload.startswith("FAILED TEST:"):
            self._flush()
            self.current_test = payload[len("FAILED TEST:") :].strip()
            return
        if payload.startswith("Test result received:"):
            # A new result means any pending assertion is complete
            self._flush()
            return
        # Assertion header lines may start with optional spaces
        if payload.lstrip().startswith("FAIL in"):
            self._flush()
            self.current_header = payload.lstrip()
            self.block_lines = []
            return
        if self.current_header:
            self.block_lines.append(payload)

    def handle_continuation(self, raw_line: str) -> None:
        if not self.current_header:
            return
        cleaned = strip_ansi(raw_line).strip()
        if cleaned == "" or cleaned == "'":
            return
        self.block_lines.append(cleaned)

    def finalise(self) -> None:
        self._flush()


def parse_log(path: str) -> Tuple[List[dict], set[str], Optional[str], int]:
    collector = FailureCollector()
    marker = "'[KARMA-ADAPTER]', "
    with open(path, "r", encoding="utf-8", errors="ignore") as handle:
        for raw in handle:
            clean = strip_ansi(raw.rstrip("\n"))
            if marker in clean:
                payload = clean.split(marker, 1)[1]
                collector.handle_payload(payload)
            else:
                collector.handle_continuation(raw)
    collector.finalise()
    summary_line = getattr(collector, "summary_line", None)
    return collector.failures, collector.unique_tests, summary_line, collector.total_headers


def build_summary(summary_line: Optional[str], unique_tests: set[str], failure_count: int, raw_headers: int) -> dict:
    total_tests = len(unique_tests) if unique_tests else failure_count
    passed_tests = None
    failed_tests = len(unique_tests)
    success_rate = None

    if summary_line:
        summary_line = summary_line.rstrip("'")
        match = SUMMARY_RE.search(summary_line)
        if match:
            total_tests = int(match.group("total"))
            passed_tests = int(match.group("passed"))
            failed_tests = int(match.group("failed"))
            rate = match.group("rate")
            if rate is not None:
                try:
                    success_rate = int(rate)
                except ValueError:
                    success_rate = None

    if passed_tests is None:
        passed_tests = max(total_tests - failed_tests, 0)

    summary = {
        "timestamp": dt.datetime.now().isoformat(timespec="seconds"),
        "total": total_tests,
        "passed": passed_tests,
        "failed": failure_count,
        "failedTests": failed_tests,
        "failedAssertions": failure_count,
        "rawFailedAssertions": raw_headers,
        "source": "python-log-parser",
    }
    if success_rate is not None:
        summary["successRate"] = success_rate
    return summary


def write_json(path: str, data: dict) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def main(argv: List[str]) -> int:
    parser = argparse.ArgumentParser(description="Convert Karma log to JSON")
    parser.add_argument("--log", required=True, help="Path to Karma log file")
    parser.add_argument("--summary", required=True, help="Output JSON with summary + results")
    parser.add_argument("--failures", required=True, help="Output JSON with failures only")
    args = parser.parse_args(argv)

    if not os.path.isfile(args.log):
        print(f"Karma log not found: {args.log}", file=sys.stderr)
        return 1

    failures, unique_tests, summary_line, raw_headers = parse_log(args.log)

    failure_count = len(failures)
    summary = build_summary(summary_line, unique_tests, failure_count, raw_headers)
    result_payload = {
        "summary": summary,
        "results": failures,
        "extractionMethod": "python-log-parser"
    }
    write_json(args.summary, result_payload)

    failures_payload = {
        "timestamp": summary["timestamp"],
        "failures": failures,
        "failedAssertions": failure_count,
        "failedTests": summary.get("failedTests", failure_count)
    }
    write_json(args.failures, failures_payload)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
