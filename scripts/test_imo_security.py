"""Offline regression tests; run with python -m unittest discover -s scripts -p 'test_*.py'."""
import io
import json
import unittest
from pathlib import Path
from pypdf import PdfWriter
from update_imo_calendar import extract_pdf, MAX_PDF_BYTES
from validate_imo_calendar import validate


class CalendarSecurityTest(unittest.TestCase):
    def test_bundled_schema(self):
        validate(json.loads(Path("app/src/main/assets/imo_meteor_showers.json").read_text(encoding="utf-8")))

    def test_rejects_invalid_coordinate(self):
        payload = json.loads(Path("app/src/main/assets/imo_meteor_showers.json").read_text(encoding="utf-8"))
        payload["years"][0]["showers"][0]["raDegrees"] = 999
        with self.assertRaises(ValueError):
            validate(payload)

    def test_size_and_magic_limits(self):
        for content in (b"not a PDF", b"%PDF-" + b"x" * MAX_PDF_BYTES):
            with self.assertRaises(ValueError):
                extract_pdf(content)

    def test_malformed_pdf_is_rejected(self):
        with self.assertRaises(ValueError):
            extract_pdf(b"%PDF-invalid")

    def test_valid_blank_pdf(self):
        writer = PdfWriter()
        writer.add_blank_page(width=72, height=72)
        stream = io.BytesIO()
        writer.write(stream)
        self.assertEqual("", extract_pdf(stream.getvalue()))


if __name__ == "__main__":
    unittest.main()
