"""Offline regression tests; run with python -m unittest discover -s scripts -p 'test_*.py'."""
import io
import json
import ssl
import tempfile
import unittest
import urllib.error
from pathlib import Path
from unittest.mock import MagicMock, patch
from pypdf import PdfWriter
from update_imo_calendar import CalendarSourceUnavailable, calendar_text, extract_pdf, main, MAX_PDF_BYTES
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

    def response_opener(self, content, content_type):
        response = MagicMock()
        response.read.return_value = content
        response.headers = {"Content-Type": content_type}
        opener = MagicMock()
        opener.open.return_value.__enter__.return_value = response
        return opener

    def test_http_200_maintenance_is_unavailable(self):
        for notice in (b"Website maintenance", b"We will be back soon"):
            with self.subTest(notice=notice):
                opener = self.response_opener(b"<!doctype html><title>" + notice + b"</title>", "text/html; charset=utf-8")
                with patch("update_imo_calendar.urllib.request.build_opener", return_value=opener):
                    with self.assertRaises(CalendarSourceUnavailable):
                        calendar_text(2026)
                response = opener.open.return_value.__enter__.return_value
                response.read.assert_called_once_with(MAX_PDF_BYTES + 1)
                self.assertEqual(15, opener.open.call_args.kwargs["timeout"])

    def test_known_unavailable_http_statuses(self):
        for status in (404, 408, 429, 500, 502, 503, 504):
            with self.subTest(status=status):
                opener = MagicMock()
                opener.open.side_effect = urllib.error.HTTPError("https://www.imo.net/", status, "unavailable", {}, None)
                with patch("update_imo_calendar.urllib.request.build_opener", return_value=opener):
                    with self.assertRaises(CalendarSourceUnavailable):
                        calendar_text(2027)

    def test_unexpected_http_and_tls_errors_are_not_skipped(self):
        errors = (
            urllib.error.HTTPError("https://www.imo.net/", 403, "forbidden", {}, None),
            urllib.error.URLError(ssl.SSLCertVerificationError("certificate verify failed")),
        )
        for error in errors:
            with self.subTest(error=type(error).__name__):
                opener = MagicMock()
                opener.open.side_effect = error
                with patch("update_imo_calendar.urllib.request.build_opener", return_value=opener):
                    with self.assertRaises(type(error)):
                        calendar_text(2026)

    def test_unexpected_or_oversized_responses_remain_errors(self):
        cases = (
            (b"<!doctype html><title>Unexpected page</title>", "text/html"),
            (b"Website maintenance", "application/pdf"),
            (b"<!doctype html>Website maintenance" + b"x" * MAX_PDF_BYTES, "text/html"),
            (b"%PDF-invalid", "application/pdf"),
        )
        for content, content_type in cases:
            with self.subTest(content_type=content_type, size=len(content)):
                opener = self.response_opener(content, content_type)
                with patch("update_imo_calendar.urllib.request.build_opener", return_value=opener):
                    with self.assertRaises(ValueError):
                        calendar_text(2026)

    def test_valid_download_still_extracts_pdf(self):
        writer = PdfWriter()
        writer.add_blank_page(width=72, height=72)
        stream = io.BytesIO()
        writer.write(stream)
        opener = self.response_opener(stream.getvalue(), "application/pdf")
        with patch("update_imo_calendar.urllib.request.build_opener", return_value=opener):
            self.assertEqual("", calendar_text(2026))

    def test_unavailable_source_keeps_existing_output_and_exits_75(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "calendar.json"
            original = b'{"existing": "calendar data"}\n'
            output.write_bytes(original)
            with patch("sys.argv", ["update_imo_calendar.py", "--years", "2026", "2027", "--output", str(output)]):
                with patch("update_imo_calendar.calendar_text", side_effect=CalendarSourceUnavailable("IMO 2026: website maintenance")):
                    with patch("sys.stderr", new_callable=io.StringIO) as stderr:
                        with self.assertRaises(SystemExit) as result:
                            main()
            self.assertEqual(75, result.exception.code)
            self.assertIn("no calendar data written", stderr.getvalue())
            self.assertEqual(original, output.read_bytes())


if __name__ == "__main__":
    unittest.main()
