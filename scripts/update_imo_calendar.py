#!/usr/bin/env python3
"""Build the app's meteor-shower JSON from official IMO annual calendars."""

from __future__ import annotations

import argparse
import io
import json
import re
import urllib.request
from datetime import date
from pathlib import Path

from pypdf import PdfReader


MONTHS = {
    "Jan": 1, "Feb": 2, "Mar": 3, "Apr": 4, "May": 5, "Jun": 6,
    "Jul": 7, "Aug": 8, "Sep": 9, "Oct": 10, "Nov": 11, "Dec": 12,
}
GERMAN_NAMES = {
    "QUA": "Quadrantiden", "LYR": "Lyriden", "ETA": "Eta-Aquariiden",
    "SDA": "Südliche Delta-Aquariiden", "PER": "Perseiden",
    "SPE": "September-Epsilon-Perseiden", "DRA": "Draconiden",
    "ORI": "Orioniden", "LEO": "Leoniden", "GEM": "Geminiden", "URS": "Ursiden",
}
TRACKED_CODES = set(GERMAN_NAMES)


def normalize(text: str) -> str:
    return (text.replace("−", "-").replace("–", "-").replace("ﬁ", "fi")
            .replace("ﬂ", "fl").replace("η", "Eta").replace("δ", "Delta")
            .replace("ε", "Epsilon").replace("γ", "Gamma").replace("κ", "Kappa")
            .replace("α", "Alpha").replace("σ", "Sigma").replace("π", "Pi"))


def calendar_text(year: int) -> str:
    url = f"https://www.imo.net/files/meteor-shower/cal{year}.pdf"
    request = urllib.request.Request(url, headers={"User-Agent": "ProjektAstra calendar updater"})
    with urllib.request.urlopen(request, timeout=30) as response:
        pdf = response.read()
    return normalize("\n".join(page.extract_text() or "" for page in PdfReader(io.BytesIO(pdf)).pages))


def parse_year(year: int, text: str) -> dict:
    rows: list[dict] = []
    for line in text.splitlines():
        code_match = re.search(r"\((\d{3})\s+([A-Z0-9]{3})\)", line)
        if not code_match or code_match.group(2) not in TRACKED_CODES:
            continue
        code = code_match.group(2)
        dates = re.findall(r"\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+(\d{2})\b", line)
        coordinate = re.search(r"(\d{1,3})\s*◦\s*([+-]\s*\d{1,2})\s*◦", line)
        zhr_match = re.search(r"(?:\s|^)(\d+)\+?\s*$", line)
        if len(dates) < 3 or not coordinate or not zhr_match:
            continue
        peak_month, peak_day = dates[2]
        rows.append({
            "name": GERMAN_NAMES[code],
            "code": code,
            "month": MONTHS[peak_month],
            "day": int(peak_day),
            "raDegrees": float(coordinate.group(1)),
            "decDegrees": float(coordinate.group(2).replace(" ", "")),
            "zhr": int(zhr_match.group(1)),
        })
    by_code = {row["code"]: row for row in rows}
    missing = TRACKED_CODES - set(by_code)
    if missing:
        raise RuntimeError(f"IMO {year}: entries not parsed: {sorted(missing)}")
    return {"year": year, "showers": sorted(by_code.values(), key=lambda row: (row["month"], row["day"]))}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--years", nargs="+", type=int, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    payload = {
        "source": "International Meteor Organization annual Meteor Shower Calendar",
        "updated": date.today().isoformat(),
        "years": [parse_year(year, calendar_text(year)) for year in args.years],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
