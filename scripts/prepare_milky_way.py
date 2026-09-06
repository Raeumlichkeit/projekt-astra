#!/usr/bin/env python3
"""Fetch and validate Astra's pinned, already tone-mapped Milky Way JPEG.

No pixel modification is performed. This is an optional development-time
asset preparation step, never an Android runtime download or a Gradle task.
"""

from __future__ import annotations

import argparse
import hashlib
import shutil
import tempfile
import urllib.parse
import urllib.request
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "app/src/main/assets/milkyway_gaia_2020.jpg"
SOURCE = ROOT / "app/build/milkyway-source.jpg"
SOURCE_URL = (
    "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/3f/"
    "Deep_Star_Maps_2020_%E2%80%93_Milkyway_2020_64k.jpg/"
    "3840px-Deep_Star_Maps_2020_%E2%80%93_Milkyway_2020_64k.jpg"
)
EXPECTED_SHA256 = "6eb620fff7e4742323b5e291a45844cdaff131c7755b3c8a386574db3febb6dc"
EXPECTED_BYTES = 1_465_425
EXPECTED_SIZE = (3840, 1920)
MAX_DOWNLOAD_BYTES = 5_000_000


class AssetRedirectHandler(urllib.request.HTTPRedirectHandler):
    """Keep the optional source fetch on HTTPS Wikimedia image hosts."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        parsed = urllib.parse.urlparse(newurl)
        if (
            parsed.scheme != "https"
            or parsed.hostname not in {"thumb.wikimedia.org", "upload.wikimedia.org"}
            or parsed.port not in {None, 443}
            or parsed.username is not None
            or parsed.password is not None
        ):
            raise ValueError("Rejected an unexpected asset redirect")
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def validate_asset(path: Path) -> None:
    if path.stat().st_size != EXPECTED_BYTES:
        raise ValueError("Unexpected asset size; source was not accepted")
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest != EXPECTED_SHA256:
        raise ValueError("SHA-256 mismatch; source was not accepted")
    with Image.open(path) as image:
        if image.format != "JPEG" or image.size != EXPECTED_SIZE or image.mode != "RGB":
            raise ValueError("Unexpected image format, dimensions, or color mode")
        image.verify()


def download_source(destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = None
    try:
        with tempfile.NamedTemporaryFile(
            dir=destination.parent, prefix="milkyway-", suffix=".download", delete=False
        ) as temporary:
            temporary_path = Path(temporary.name)
            request = urllib.request.Request(
                SOURCE_URL, headers={"User-Agent": "ProjektAstra-AssetPreparation/1.0"}
            )
            opener = urllib.request.build_opener(AssetRedirectHandler())
            with opener.open(request, timeout=30) as response:
                total = 0
                while chunk := response.read(64 * 1024):
                    total += len(chunk)
                    if total > MAX_DOWNLOAD_BYTES:
                        raise ValueError("Asset response exceeds the download limit")
                    temporary.write(chunk)
        validate_asset(temporary_path)
        temporary_path.replace(destination)
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--download", action="store_true", help="Fetch the pinned source over HTTPS")
    mode.add_argument("--check", action="store_true", help="Verify the bundled asset without network access")
    parser.add_argument("--source", type=Path, default=SOURCE, help="Existing local source JPEG")
    args = parser.parse_args()

    if args.check:
        validate_asset(ASSET)
    else:
        if args.download:
            download_source(args.source)
        validate_asset(args.source)
        ASSET.parent.mkdir(parents=True, exist_ok=True)
        if ASSET.exists():
            # Never silently overwrite an independently changed asset.
            validate_asset(ASSET)
        elif args.source.resolve() != ASSET.resolve():
            shutil.copyfile(args.source, ASSET)
        validate_asset(ASSET)
    print(f"OK: {ASSET.name}, {EXPECTED_SIZE[0]}x{EXPECTED_SIZE[1]}, {EXPECTED_BYTES} bytes")
    print(f"SHA-256: {EXPECTED_SHA256}")


if __name__ == "__main__":
    main()
