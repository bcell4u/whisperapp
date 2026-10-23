#!/usr/bin/env python3
"""Report Android Gradle lint failures in the GitHub Actions console.

Source-only diagnostic, no audio/ASR data. It never changes the lint exit code,
changes severity, or makes a failing APK eligible for release.
"""
import argparse
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


def summarize(log: Path, report: Path, limit: int = 30) -> None:
    print(f"Android lint log: {log}")
    print(f"Android lint XML: {report}")
    if report.is_file():
        try:
            root = ET.parse(report).getroot()
            issues = list(root.iter("issue"))
            errors = sum(issue.get("severity") in ("Error", "Fatal") for issue in issues)
            warnings = sum(issue.get("severity") == "Warning" for issue in issues)
            print(f"Lint XML issues: {len(issues)} ({errors} errors, {warnings} warnings)")
            for issue in issues[:limit]:
                loc = issue.find("location")
                location = ""
                if loc is not None:
                    location = f" {loc.get('file', '?')}:{loc.get('line', '?')}"
                detail = re.sub(r"\s+", " ", issue.get("message", "")).strip()[:250]
                print(f"  {issue.get('severity', '?')} [{issue.get('id', '?')}]{location}: {detail}")
            if len(issues) > limit:
                print(f"  ... {len(issues) - limit} more issues in the attached XML")
        except (ET.ParseError, OSError) as exc:
            print(f"Unable to parse lint XML: {type(exc).__name__}: {exc}")
    else:
        print("No lint XML was generated; Gradle may have failed before lint analysis.")
    if log.is_file():
        lines = log.read_text(encoding="utf-8", errors="replace").splitlines()
        print(f"--- Last {min(80, len(lines))} lines of lint Gradle log ---")
        for line in lines[-80:]:
            print(line[:500])
    else:
        print("No Gradle lint build log found.")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--log", type=Path, required=True)
    ap.add_argument("--xml", type=Path, required=True)
    args = ap.parse_args()
    summarize(args.log, args.xml)


if __name__ == "__main__":
    main()
