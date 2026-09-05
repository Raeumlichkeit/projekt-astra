package de.projektastra.app

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class SecurityPolicyTest {
    @Test fun onlyExactHttpsSourcesAndPathsArePermitted() {
        val allowed = listOf(
            "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41",
            "https://www.imo.net/files/meteor-shower/cal2027.pdf",
            "https://tile.openstreetmap.org/8/137/83.png",
            "https://tilecache.rainviewer.com/v2/radar/1788647400/256/7/67/42/2/1_1.png"
        )
        allowed.forEach { assertTrue(it, NetworkPolicy.permits(it)) }
        val blocked = listOf(
            "http://api.open-meteo.com/v1/forecast", "file:///etc/passwd",
            "https://api.open-meteo.com.evil.test/v1/forecast",
            "https://api.open-meteo.com@evil.test/v1/forecast",
            "https://evil.test@api.open-meteo.com/v1/forecast",
            "https://api.open-meteo.com:444/v1/forecast",
            "https://api.open-meteo.com/v1/forecast#fragment",
            "https://www.imo.net/files/meteor-shower/../../other.pdf",
            "https://www.imo.net/files/meteor-shower/%63al2027.pdf",
            "https://www.imo.net/redirect?to=https://evil.test", "not a URL"
        )
        blocked.forEach { assertFalse(it, NetworkPolicy.permits(it)) }
    }

    @Test fun onlineRequiresConsentAndForegroundAndTerrainIsSeparate() {
        try {
            SecureNetwork.configure(PrivacyOptions())
            SecureNetwork.setForeground(true)
            assertFalse(SecureNetwork.available)
            assertThrows(IOException::class.java) { SecureNetwork.get("https://api.open-meteo.com/v1/forecast") }
            SecureNetwork.configure(PrivacyOptions(online = true))
            assertTrue(SecureNetwork.available)
            assertThrows(IOException::class.java) { SecureNetwork.get("https://api.open-meteo.com/v1/elevation") }
            SecureNetwork.setForeground(false)
            assertFalse(SecureNetwork.available)
            SecureNetwork.configure(PrivacyOptions(terrain = true))
            assertFalse(SecureNetwork.options.terrain)
        } finally {
            SecureNetwork.configure(PrivacyOptions())
            SecureNetwork.setForeground(false)
        }
    }

    @Test fun locationPrecisionIsReducedAndAltitudeRemoved() {
        val rounded = NetworkPolicy.roundedLocation(GeoPoint(52.523456, 13.406789, 71.4))
        assertEquals(GeoPoint(52.52, 13.41, 0.0), rounded)
        assertEquals(GeoPoint(-52.52, -13.41, 0.0), NetworkPolicy.roundedLocation(GeoPoint(-52.523456, -13.406789, 0.0)))
    }

    @Test fun downloadsAcceptTheLimitButRejectAnExtraByte() {
        assertEquals(8192, NetworkPolicy.readBounded(ByteArrayInputStream(ByteArray(8192)), 8192).size)
        assertThrows(IOException::class.java) {
            NetworkPolicy.readBounded(ByteArrayInputStream(ByteArray(8193)), 8192)
        }
    }

    @Test fun pdfTextOutputIsBounded() {
        val writer = LimitedTextWriter()
        writer.write(CharArray(PdfLimits.TEXT_CHARS))
        assertEquals(PdfLimits.TEXT_CHARS, writer.toString().length)
        assertThrows(IOException::class.java) { writer.write(charArrayOf('x')) }
    }

    @Test fun calendarRejectsInvalidDatesAndCoordinates() {
        val valid = java.io.File("src/main/assets/imo_meteor_showers.json").readText()
        val json = org.json.JSONObject(valid)
        val shower = json.getJSONArray("years").getJSONObject(0).getJSONArray("showers").getJSONObject(0)
        shower.put("raDegrees", 999)
        assertThrows(IllegalArgumentException::class.java) { MeteorCalendarRepository.parse(json.toString(), true) }
        shower.put("raDegrees", 48).put("month", 2).put("day", 31)
        assertThrows(java.time.DateTimeException::class.java) { MeteorCalendarRepository.parse(json.toString(), true) }
    }
}
