package de.projektastra.app

import org.junit.Assert.*
import org.junit.Test

class ExternalLightMapPolicyTest {
    @Test fun officialEmbedContainsOnlyRoundedCoordinates() {
        val observer = GeoPoint(52.523456, 13.406789, 88.9)
        assertEquals("https://lightpollutionmap.app/de/embed/?lat=52.52&lng=13.41&zoom=8", ExternalLightMapPolicy.embedUrl(observer))
        val html = ExternalLightMapPolicy.document(observer)
        assertFalse(html.contains("52.523456"))
        assertFalse(html.contains("13.406789"))
        assertFalse(html.contains("88.9"))
        assertTrue(html.contains("sandbox=\"allow-scripts allow-same-origin\""))
        assertTrue(html.contains("referrerpolicy=\"no-referrer\""))
        assertThrows(IllegalArgumentException::class.java) { ExternalLightMapPolicy.embedUrl(GeoPoint(Double.NaN, 0.0, 0.0)) }
    }

    @Test fun permitsOfficialEmbedDependenciesAndCorsPreflightOnly() {
        listOf(
            ExternalLightMapPolicy.embedUrl(GeoPoint(52.52, 13.41, 0.0)),
            "https://lightpollutionmap.app/_astro/Map.astro_astro_type_script_index_0_lang.D9grwfdV.js",
            "https://lightpollutionmap.app/_astro/Map.DgDFqbFk.css",
            "https://lightpollutionmap.app/sky/virtualsky.js",
            "https://lightpollutionmap.app/sky/lang/de.json",
            "https://lightpollutionmap.app/layer-previews/classic.png",
            "https://lightpollutionmap.app/stargazinghub.webp",
            "https://api.lightpollutionmap.app/api/lightpollution/all?lat=52.52&lng=13.41",
            "https://api.lightpollutionmap.app/api/lightpollution/image-tiles/2025/8/137/83.png",
            "https://api.lightpollutionmap.app/api/lightpollution/style-tiles/classic/2025/8/137/83.png",
            "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js",
            "https://tile.openstreetmap.org/8/137/83.png",
            "https://d.basemaps.cartocdn.com/dark_all/8/137/83@2x.png",
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/8/83/137"
        ).forEach { assertTrue(it, ExternalLightMapPolicy.permits(it)) }
        val endpoint = "https://api.lightpollutionmap.app/api/lightpollution/sky-profile?lat=52.52&lng=13.41"
        assertTrue(ExternalLightMapPolicy.permits(endpoint, "OPTIONS"))
        listOf("POST", "PUT", "PATCH", "DELETE", "HEAD").forEach { assertFalse(ExternalLightMapPolicy.permits(endpoint, it)) }
        assertFalse(ExternalLightMapPolicy.permits("https://lightpollutionmap.app/de/embed/", "OPTIONS"))
    }

    @Test fun blocksTrackersGeolocationNavigationAndOriginBypasses() {
        listOf(
            "https://matomotech.com/matomo.php",
            "https://lightpollutionmap.app/api/ip-location",
            "https://api.lightpollutionmap.app/api/ip-location",
            "https://lightpollutionmap.app/de/",
            "https://lightpollutionmap.app/go/app?download=1",
            "https://clouds.lightpollutionmap.app/clouds-ifs/VIS/8/1/1.png",
            "https://tile.openweathermap.org/map/precipitation_new/8/1/1.png",
            "https://cdn.jsdelivr.net/npm/unapproved/index.js",
            "https://lightpollutionmap.app.evil.test/de/embed/",
            "https://evil.test@lightpollutionmap.app/de/embed/",
            "https://lightpollutionmap.app:444/de/embed/",
            "http://lightpollutionmap.app/de/embed/",
            "https://lightpollutionmap.app/de/embed/#fragment",
            "https://lightpollutionmap.app/_astro/../secret.js",
            "https://lightpollutionmap.app/_astro/%2e%2e/secret.js",
            "file:///sdcard/location.txt", "content://contacts", "javascript:alert(1)", "not a URL"
        ).forEach { assertFalse(it, ExternalLightMapPolicy.permits(it)) }
        assertFalse(ExternalLightMapPolicy.isEmbedDocument("https://lightpollutionmap.app/_astro/example.js"))
    }

    @Test fun externalOptInDoesNotWidenAstrasNormalNetworkPolicy() {
        assertTrue(ExternalLightMapPolicy.permits("https://lightpollutionmap.app/de/embed/"))
        assertFalse(NetworkPolicy.permits("https://lightpollutionmap.app/de/embed/"))
        assertFalse(NetworkPolicy.permits("https://api.lightpollutionmap.app/api/lightpollution/all"))
        assertTrue(ExternalLightMapPolicy.EMBED_CSP.contains("worker-src 'none'"))
        assertTrue(ExternalLightMapPolicy.EMBED_CSP.contains("frame-src 'none'"))
    }
}
