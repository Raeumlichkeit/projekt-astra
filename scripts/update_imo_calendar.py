#!/usr/bin/env python3
"""Build the app's meteor-shower JSON from official IMO annual calendars."""

from __future__ import annotations

import argparse
import io
import json
import logging
import multiprocessing
import re
import urllib.request
from datetime import date
from pathlib import Path

from validate_imo_calendar import validate

MAX_PDF_BYTES = 4 * 1024 * 1024
MAX_TEXT_CHARS = 131072
PARSE_SECONDS = 20


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


class NoRedirects(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError("Calendar redirects are not permitted")


def _extract_worker(pdf: bytes, output) -> None:
    try:
        # Linux CI has a hard address-space/CPU budget and a parent watchdog.
        try:
            import resource
            resource.setrlimit(resource.RLIMIT_AS, (1024**3, 1024**3))
            resource.setrlimit(resource.RLIMIT_CPU, (PARSE_SECONDS, PARSE_SECONDS))
        except ImportError:
            pass  # Windows has size checks and timeout, but no RLIMIT_AS.
        from pypdf import PdfReader
        # Never copy arbitrary PDF contents into CI logs; validation errors are reported by the parent.
        logging.getLogger("pypdf").disabled = True
        logging.getLogger("pypdf").setLevel(logging.CRITICAL)
        reader = PdfReader(io.BytesIO(pdf))
        if reader.is_encrypted or not 1 <= len(reader.pages) <= 64:
            raise ValueError("Calendar page/encryption limit")
        pieces = []
        size = 0
        for page in reader.pages:
            text = page.extract_text() or ""
            size += len(text) + 1
            if size > MAX_TEXT_CHARS:
                raise ValueError("Calendar text limit")
            pieces.append(text)
        output.send((True, "\n".join(pieces)))
    except Exception:
        output.send((False, "Calendar PDF rejected"))
    finally:
        output.close()


def extract_pdf(pdf: bytes) -> str:
    if len(pdf) > MAX_PDF_BYTES or not pdf.startswith(b"%PDF-"):
        raise ValueError("Invalid or oversized PDF")
    ctx = multiprocessing.get_context("spawn")
    receive, send = ctx.Pipe(duplex=False)
    process = ctx.Process(target=_extract_worker, args=(pdf, send), daemon=True)
    process.start()
    send.close()
    try:
        if not receive.poll(PARSE_SECONDS):
            raise TimeoutError("Calendar PDF parsing timed out")
        success, text = receive.recv()
        if not success or len(text) > MAX_TEXT_CHARS:
            raise ValueError("Calendar PDF rejected")
        return normalize(text)
    finally:
        receive.close()
        if process.is_alive():
            process.terminate()
        process.join(timeout=3)
        if process.is_alive():
            process.kill()
            process.join(timeout=3)


def calendar_text(year: int) -> str:
    if not 2020 <= year <= 2099:
        raise ValueError("Unsupported year")
    url = f"https://www.imo.net/files/meteor-shower/cal{year}.pdf"
    request = urllib.request.Request(url, headers={"User-Agent": "ProjektAstra calendar updater"})
    with urllib.request.build_opener(NoRedirects).open(request, timeout=15) as response:
        pdf = response.read(MAX_PDF_BYTES + 1)
    return extract_pdf(pdf)


def parse_year(year: int, text: str) -> dict:
    if len(text) > MAX_TEXT_CHARS:
        raise ValueError("Calendar text limit")
    rows: list[dict] = []
    for line in text.splitlines():
        code_match = re.search(r"\((\d{3})\s+([A-Z0-9]{3})\)", line)
        if not code_match or code_match.group(2) not in TRACKED_CODES:
            continue
        code = code_match.group(2)
        dates = re.findall(r"\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+(\d{2})\b", line)
        coordinate = re.search(r"(\d{1,3})\s*[°◦]\s*([+-]\s*\d{1,2})\s*[°◦]", line)
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
    validate(payload)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
