"""Standard-library-only schema gate for the write-enabled CI job."""
import json
import math
import sys
from datetime import date
from pathlib import Path

CODES = {"QUA", "LYR", "ETA", "SDA", "PER", "SPE", "DRA", "ORI", "LEO", "GEM", "URS"}


def validate(payload):
    if set(payload) != {"source", "updated", "years"}:
        raise ValueError("Unexpected root fields")
    if payload["source"] != "International Meteor Organization annual Meteor Shower Calendar":
        raise ValueError("Unexpected source")
    date.fromisoformat(payload["updated"])
    if not isinstance(payload["years"], list) or not 1 <= len(payload["years"]) <= 3:
        raise ValueError("Invalid year count")
    seen = set()
    for item in payload["years"]:
        if set(item) != {"year", "showers"} or type(item["year"]) is not int or not 2020 <= item["year"] <= 2099:
            raise ValueError("Invalid year")
        if item["year"] in seen:
            raise ValueError("Duplicate year")
        seen.add(item["year"])
        if len(item["showers"]) != len(CODES) or {s["code"] for s in item["showers"]} != CODES:
            raise ValueError("Incomplete showers")
        for shower in item["showers"]:
            if set(shower) != {"name", "code", "month", "day", "raDegrees", "decDegrees", "zhr"}:
                raise ValueError("Unexpected shower fields")
            if not isinstance(shower["name"], str) or not 1 <= len(shower["name"]) <= 80 or any(ord(c) < 32 for c in shower["name"]):
                raise ValueError("Invalid name")
            date(item["year"], shower["month"], shower["day"])
            for field, low, high in (("raDegrees", 0, 360), ("decDegrees", -90, 90), ("zhr", 0, 100000)):
                value = shower[field]
                if type(value) not in (int, float) or not math.isfinite(value) or not low <= value <= high:
                    raise ValueError("Invalid numeric field")


if __name__ == "__main__":
    path = Path(sys.argv[1])
    if path.stat().st_size > 131072:
        raise ValueError("Calendar JSON too large")
    validate(json.loads(path.read_text(encoding="utf-8")))
    print("Calendar schema valid")
