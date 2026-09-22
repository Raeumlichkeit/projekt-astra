package de.projektastra.app.coordinates

/**
 * Difficulty level for star-hopping routes.
 */
enum class StarHopDifficulty(val label: String) {
    EASY("Leicht"),
    MEDIUM("Mittel"),
    ADVANCED("Fortgeschritten")
}

/**
 * Display mode for reticle rings during star-hopping.
 */
enum class StarHopReticleMode(val label: String) {
    TELRAD_ONLY("Telrad (0.5° / 2° / 4°)"),
    FOV_ONLY("Okular-FOV"),
    TELRAD_AND_FOV("Telrad + Okular-FOV")
}

/**
 * An individual hop or waypoint along a star-hopping route.
 */
data class StarHopStep(
    val stepIndex: Int,
    val title: String,
    val instruction: String,
    val fieldCenterCoords: EquatorialCoordinates,
    val recommendedFovDegrees: Double,
    val landmarkObjects: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val hint: String? = null
) {
    // Compatibility aliases with PROJECT.md
    val description: String get() = instruction
    val centerCoords: EquatorialCoordinates get() = fieldCenterCoords
    val fovDegrees: Double get() = recommendedFovDegrees
}

/**
 * A curated star-hop route guiding the observer from a prominent guide star to a deep-sky target.
 */
data class StarHopRoute(
    val id: String,
    val targetName: String,
    val targetCatalogId: String,
    val guideStar: String,
    val guideStarCatalogId: String? = null,
    val constellation: String,
    val difficulty: StarHopDifficulty = StarHopDifficulty.EASY,
    val estimatedHops: Int = 3,
    val description: String = "",
    val steps: List<StarHopStep>
) {
    val totalSteps: Int get() = steps.size
    val completedStepsCount: Int get() = steps.count { it.isCompleted }
    val isFinished: Boolean get() = steps.isNotEmpty() && steps.all { it.isCompleted }
}

/**
 * Mutable session state during an active star-hopping observation.
 */
data class StarHopSessionState(
    val activeRoute: StarHopRoute? = null,
    val activeStepIndex: Int = 0,
    val reticleMode: StarHopReticleMode = StarHopReticleMode.TELRAD_AND_FOV,
    val autoCenterOnHop: Boolean = true,
    val isFinished: Boolean = false
) {
    val currentStep: StarHopStep? get() = activeRoute?.steps?.getOrNull(activeStepIndex)

    fun nextStep(): StarHopSessionState {
        val route = activeRoute ?: return this
        val nextIndex = activeStepIndex + 1
        return if (nextIndex < route.steps.size) {
            val updatedSteps = route.steps.mapIndexed { idx, s ->
                if (idx == activeStepIndex) s.copy(isCompleted = true) else s
            }
            copy(activeRoute = route.copy(steps = updatedSteps), activeStepIndex = nextIndex)
        } else {
            val updatedSteps = route.steps.map { it.copy(isCompleted = true) }
            copy(activeRoute = route.copy(steps = updatedSteps), isFinished = true)
        }
    }

    fun previousStep(): StarHopSessionState {
        if (activeStepIndex <= 0) return this
        return copy(activeStepIndex = activeStepIndex - 1, isFinished = false)
    }

    fun jumpToStep(stepIndex: Int): StarHopSessionState {
        val route = activeRoute ?: return this
        if (stepIndex in 0 until route.steps.size) {
            return copy(activeStepIndex = stepIndex)
        }
        return this
    }

    fun toggleStepCompleted(stepIndex: Int): StarHopSessionState {
        val route = activeRoute ?: return this
        val updatedSteps = route.steps.mapIndexed { idx, s ->
            if (idx == stepIndex) s.copy(isCompleted = !s.isCompleted) else s
        }
        val allDone = updatedSteps.all { it.isCompleted }
        return copy(
            activeRoute = route.copy(steps = updatedSteps),
            activeStepIndex = if (!allDone && stepIndex == activeStepIndex && updatedSteps[stepIndex].isCompleted) {
                (stepIndex + 1).coerceAtMost(route.steps.size - 1)
            } else activeStepIndex,
            isFinished = allDone
        )
    }

    fun reset(): StarHopSessionState = copy(
        activeStepIndex = 0,
        isFinished = false,
        activeRoute = activeRoute?.let { route ->
            route.copy(steps = route.steps.map { it.copy(isCompleted = false) })
        }
    )
}

/**
 * Curated astronomical star-hop catalog.
 */
object StarHopCatalog {

    val ROUTE_VEGA_TO_M57 = StarHopRoute(
        id = "vega_to_m57",
        targetName = "Ringnebel (M 57)",
        targetCatalogId = "M 57 · NGC 6720",
        guideStar = "Wega (Alpha Lyrae)",
        guideStarCatalogId = "HIP 91262",
        constellation = "Leier",
        difficulty = StarHopDifficulty.EASY,
        estimatedHops = 3,
        description = "Klassischer Star-Hop vom hellen Sommerdreiecks-Stern Wega durch das Parallelogramm der Leier direkt zum berühmten Ringnebel M 57.",
        steps = listOf(
            StarHopStep(
                stepIndex = 0,
                title = "Startstern Wega zentrieren",
                instruction = "Zentriere Wega (Alpha Lyrae, 0.03 mag) im Telrad-Sucher. Wega ist der hellste Stern der Leier und die nordwestliche Spitze des Sommerdreiecks.",
                fieldCenterCoords = EquatorialCoordinates(18.61564, 38.783692),
                recommendedFovDegrees = 4.0,
                landmarkObjects = listOf("Wega (HIP 91262)"),
                hint = "Im Telrad den 4°-Kreis exakt auf Wega ausrichten."
            ),
            StarHopStep(
                stepIndex = 1,
                title = "Südlich zum Parallelogramm: Sulafat & Sheliak",
                instruction = "Bewege das Teleskop ca. 6° nach Südsüdost zum unteren Rand des Leier-Parallelogramms. Zentriere zwischen Sulafat (Gamma Lyr, 3.25 mag) und Sheliak (Beta Lyr, 3.52 mag).",
                fieldCenterCoords = EquatorialCoordinates(18.90853, 33.02611),
                recommendedFovDegrees = 2.0,
                landmarkObjects = listOf("Sulafat (Gamma Lyr)", "Sheliak (Beta Lyr)"),
                hint = "Sulafat und Sheliak haben einen Abstand von ca. 2.1°. Im 2°-Telradring passen beide Sterne fast gleichzeitig an den Rand."
            ),
            StarHopStep(
                stepIndex = 2,
                title = "Halbwegspunkt zwischen Sheliak & Sulafat einstellen",
                instruction = "Platziere das Gesichtsfeld auf etwa 40 % des Weges von Sheliak in Richtung Sulafat (leicht näher an Sheliak). Der Ringnebel liegt fast exakt auf der Verbindungslinie.",
                fieldCenterCoords = EquatorialCoordinates(18.893058, 33.028583),
                recommendedFovDegrees = 1.0,
                landmarkObjects = listOf("M 57", "Sheliak", "Sulafat"),
                hint = "Im Aufsuchokular (1° FOV) erscheint M 57 bereits als winziges, diffuses Rauchbällchen (8.8 mag)."
            ),
            StarHopStep(
                stepIndex = 3,
                title = "Ziel erreicht: M 57 mit hoher Vergrößerung auflösen",
                instruction = "Wechsle zu mittlerer/hoher Vergrößerung (Okular-FOV <= 0.5°). Die markante ovale Ringform („Rauchring“) und das dunklere Zentrum treten deutlich hervor.",
                fieldCenterCoords = EquatorialCoordinates(18.893058, 33.028583),
                recommendedFovDegrees = 0.5,
                landmarkObjects = listOf("Ringnebel (M 57)"),
                hint = "Bei indirektem Sehen wird der Ring noch kontrastreicher sichtbar."
            )
        )
    )

    val ROUTE_MERAK_DUBHE_TO_M81_M82 = StarHopRoute(
        id = "merak_dubhe_to_m81_m82",
        targetName = "Bode's & Zigarren-Galaxie (M 81 / M 82)",
        targetCatalogId = "M 81 · NGC 3031",
        guideStar = "Merak & Dubhe (Großer Bär)",
        guideStarCatalogId = "HIP 53910",
        constellation = "Großer Bär",
        difficulty = StarHopDifficulty.MEDIUM,
        estimatedHops = 3,
        description = "Eleganter Diagonalsprung über die Kastensterne des Großen Wagens in den Norden zu den beiden Vorzeige-Galaxien M 81 und M 82.",
        steps = listOf(
            StarHopStep(
                stepIndex = 0,
                title = "Start bei Merak (Beta UMa)",
                instruction = "Zentriere Merak (Beta Ursae Majoris, 2.34 mag), die südwestliche Kastenecke des Großen Wagens.",
                fieldCenterCoords = EquatorialCoordinates(11.030677, 56.382427),
                recommendedFovDegrees = 4.0,
                landmarkObjects = listOf("Merak (HIP 53910)"),
                hint = "Merak ist der untere der beiden bekannten Polarstern-Zeigersterne."
            ),
            StarHopStep(
                stepIndex = 1,
                title = "Diagonale über Dubhe verlängern",
                instruction = "Ziehe eine gedankliche Linie von Merak durch Dubhe (Alpha UMa, 1.81 mag, Abstand ~5.4°) und verlängere sie um denselben Abstand nach Nordwesten.",
                fieldCenterCoords = EquatorialCoordinates(11.062155, 61.751033),
                recommendedFovDegrees = 4.0,
                landmarkObjects = listOf("Dubhe (HIP 54061)"),
                hint = "Dubhe bildet die obere Kastenecke."
            ),
            StarHopStep(
                stepIndex = 2,
                title = "Zwischenstation: Dreieck um 24 Ursae Majoris",
                instruction = "Schwenke weiter entlang der Verlängerung bis zur Sterngruppe um 24 UMa (4.5 mag). Hier findest du ein markantes kleines Sterndreieck.",
                fieldCenterCoords = EquatorialCoordinates(9.56667, 69.83333),
                recommendedFovDegrees = 2.0,
                landmarkObjects = listOf("24 UMa (HIP 47053)"),
                hint = "Von diesem Dreieck sind es nur noch ca. 2° nach Südosten zum Galaxienpaar."
            ),
            StarHopStep(
                stepIndex = 3,
                title = "Ziel erreicht: M 81 & M 82 gemeinsam im Okular",
                instruction = "Zentriere auf RA 9h 55m, Dec +69° 04'. Bei 1° bis 1.5° Gesichtsfeld stehen die Spiralgalaxie M 81 und die Starburst-Galaxie M 82 gemeinsam im Bildfeld!",
                fieldCenterCoords = EquatorialCoordinates(9.9286, 69.3723),
                recommendedFovDegrees = 1.2,
                landmarkObjects = listOf("Bode's Galaxy (M 81)", "Cigar Galaxy (M 82)"),
                hint = "M 81 zeigt einen hellen Kern; M 82 erscheint als feine nadelförmige Lichtspindel im 90°-Winkel dazu."
            )
        )
    )

    val ROUTE_ALPHERATZ_MIRACH_TO_M31 = StarHopRoute(
        id = "alpheratz_mirach_to_m31",
        targetName = "Andromedagalaxie (M 31)",
        targetCatalogId = "M 31 · NGC 224",
        guideStar = "Mirach / Alpheratz (Andromeda)",
        guideStarCatalogId = "HIP 5447",
        constellation = "Andromeda",
        difficulty = StarHopDifficulty.EASY,
        estimatedHops = 3,
        description = "Der berühmteste aller Star-Hops: Entlang der Sternenkette der Andromeda von Mirach aus über Mu und Nu Andromedae zum nächsten galaktischen Nachbarn.",
        steps = listOf(
            StarHopStep(
                stepIndex = 0,
                title = "Start bei Mirach (Beta Andromedae)",
                instruction = "Finde das Herbstviereck (Pegasus) und folge der Sternenkette nach Nordosten bis zum auffälligen, rötlichen Mirach (2.07 mag).",
                fieldCenterCoords = EquatorialCoordinates(1.162194, 35.620558),
                recommendedFovDegrees = 4.0,
                landmarkObjects = listOf("Mirach (HIP 5447)"),
                hint = "Mirach ist leicht an seiner markanten warm-orangefarbenen Tönung erkennbar."
            ),
            StarHopStep(
                stepIndex = 1,
                title = "Hopp 1 nach Nordwesten: Mu Andromedae",
                instruction = "Schwenke rechtwinklig zur Hauptkette ca. 3.5° nach Nordwesten zum Stern Mu Andromedae (3.86 mag).",
                fieldCenterCoords = EquatorialCoordinates(0.94553, 38.49917),
                recommendedFovDegrees = 2.0,
                landmarkObjects = listOf("Mu And (HIP 4436)"),
                hint = "Im 4°-Telradkreis liegt Mu And knapp innerhalb des mittleren 2°-Rings, wenn Mirach noch am Rand steht."
            ),
            StarHopStep(
                stepIndex = 2,
                title = "Hopp 2 weiter nach Nordwesten: Nu Andromedae",
                instruction = "Verlängere den Sprung in genau derselben Richtung um weitere 3.5° nach Nordwesten zu Nu Andromedae (4.53 mag).",
                fieldCenterCoords = EquatorialCoordinates(0.82856, 41.08056),
                recommendedFovDegrees = 2.0,
                landmarkObjects = listOf("Nu And (HIP 3881)"),
                hint = "Nu Andromedae ist die letzte Sprosse der Himmelsleiter vor der Galaxie."
            ),
            StarHopStep(
                stepIndex = 3,
                title = "Ziel erreicht: Galaxienzentrum M 31",
                instruction = "Verschiebe das Sichtfeld nur ca. 1.2° westlich von Nu Andromedae. Das ovale, milchige Leuchten des riesigen Andromedakerns erstreckt sich über mehrere Gesichtsfelder!",
                fieldCenterCoords = EquatorialCoordinates(0.712319, 41.269056),
                recommendedFovDegrees = 2.5,
                landmarkObjects = listOf("Andromedagalaxie (M 31)", "M 32", "M 110"),
                hint = "Am Rand des Gesichtsfelds sind oft die Zwergbegleitgalaxien M 32 und M 110 zu erkennen."
            )
        )
    )

    val ROUTE_ALBIREO_TO_M27 = StarHopRoute(
        id = "albireo_to_m27",
        targetName = "Hantelnebel (M 27)",
        targetCatalogId = "M 27 · NGC 6853",
        guideStar = "Albireo (Beta Cygni)",
        guideStarCatalogId = "HIP 95947",
        constellation = "Fuchs / Pfeil",
        difficulty = StarHopDifficulty.MEDIUM,
        estimatedHops = 3,
        description = "Vom farbenprächtigen Doppelstern Albireo an der Schwanenspitze über die markante Pfeilspitze (Sagitta) zum hellen Hantelnebel M 27 im Füchschen.",
        steps = listOf(
            StarHopStep(
                stepIndex = 0,
                title = "Start bei Albireo (Beta Cygni)",
                instruction = "Zentriere Albireo (3.05 mag), den Kopfstern des Schwans. Im Okular offenbart sich einer der schönsten Doppelsterne (goldgelber Riese und saphirblauer Begleiter).",
                fieldCenterCoords = EquatorialCoordinates(19.512023, 27.959681),
                recommendedFovDegrees = 4.0,
                landmarkObjects = listOf("Albireo (HIP 95947)"),
                hint = "Albireo liegt genau in der Mitte des Sommerdreiecks."
            ),
            StarHopStep(
                stepIndex = 1,
                title = "Südlich zum Sternbild Pfeil (Sagitta): Gamma Sge",
                instruction = "Bewege das Teleskop ca. 8.5° nach Südosten zur Spitze des Pfeils (Stern Gamma Sagittae, 3.51 mag).",
                fieldCenterCoords = EquatorialCoordinates(19.97889, 19.49139),
                recommendedFovDegrees = 2.5,
                landmarkObjects = listOf("Gamma Sge (HIP 98337)", "Delta Sge"),
                hint = "Das Sternbild Pfeil hat die unverwechselbare, kompakte Form eines echten Pfeils."
            ),
            StarHopStep(
                stepIndex = 2,
                title = "Hopp nach Norden zu 14 Vulpeculae",
                instruction = "Schwenke von Gamma Sagittae genau 3.2° nach Norden in das Sternbild Füchschen, leicht westlich an 14 Vulpeculae (5.8 mag) vorbei.",
                fieldCenterCoords = EquatorialCoordinates(20.00333, 22.10000),
                recommendedFovDegrees = 1.5,
                landmarkObjects = listOf("14 Vul (HIP 98575)"),
                hint = "Im 2°-Sucherkreis taucht M 27 unmittelbar nördlich auf."
            ),
            StarHopStep(
                stepIndex = 3,
                title = "Ziel erreicht: Hantelnebel M 27 zentrieren",
                instruction = "Zentriere auf RA 19h 59m 36s, Dec +22° 43'. Der Hantelnebel erscheint als auffällige sanduhrförmige Nebelscheibe (7.4 mag).",
                fieldCenterCoords = EquatorialCoordinates(19.993439, 22.721028),
                recommendedFovDegrees = 1.0,
                landmarkObjects = listOf("Hantelnebel (M 27)"),
                hint = "Ein OIII- oder UHC-Nebelfilter steigert den Kontrast der beiden Hantelbacken dramatisch."
            )
        )
    )

    val ROUTE_KEYSTONE_TO_M13 = StarHopRoute(
        id = "keystone_to_m13",
        targetName = "Herkuleshaufen (M 13)",
        targetCatalogId = "M 13 · NGC 6205",
        guideStar = "Eta / Zeta Herculis (Keystone)",
        guideStarCatalogId = "HIP 81693",
        constellation = "Herkules",
        difficulty = StarHopDifficulty.EASY,
        estimatedHops = 2,
        description = "Schneller und zielsicherer Hopp an der westlichen Kante des Herkules-Vierecks („Keystone“) zum prächtigsten Kugelsternhaufen des Nordhimmels.",
        steps = listOf(
            StarHopStep(
                stepIndex = 0,
                title = "Start bei Eta Herculis (Nordwest-Ecke des Keystone)",
                instruction = "Finde das markante Trapez des Herkules und zentriere Eta Herculis (3.48 mag) an der oberen westlichen Ecke.",
                fieldCenterCoords = EquatorialCoordinates(16.71458, 38.92056),
                recommendedFovDegrees = 4.0,
                landmarkObjects = listOf("Eta Her (HIP 81833)"),
                hint = "Eta Her bildet zusammen mit Zeta, Pi und Epsilon den Rumpf des Herkules."
            ),
            StarHopStep(
                stepIndex = 1,
                title = "Kante nach Süden entlangpeilen",
                instruction = "Bewege das Teleskop entlang der Kante in Richtung Zeta Herculis (Süden). Nach ca. 2.5° (etwa ein Drittel des Weges zu Zeta) erscheint M 13.",
                fieldCenterCoords = EquatorialCoordinates(16.6950, 36.4600),
                recommendedFovDegrees = 2.0,
                landmarkObjects = listOf("Eta Her", "Zeta Her", "M 13"),
                hint = "Im Telrad: Den 2°-Kreis so anlegen, dass Eta Her knapp außerhalb des äußeren 4°-Rings liegt."
            ),
            StarHopStep(
                stepIndex = 2,
                title = "Ziel erreicht: Kugelsternhaufen M 13 im Okular",
                instruction = "Zentriere M 13 (5.8 mag). Bei mittlerer bis hoher Vergrößerung lösen sich zahllose Einzelsterne bis in das funkelnde Zentrum hinein auf.",
                fieldCenterCoords = EquatorialCoordinates(16.6950, 36.4600),
                recommendedFovDegrees = 0.8,
                landmarkObjects = listOf("Herkuleshaufen (M 13)"),
                hint = "Achte auf die dunklen Staubspuren („Propeller-Muster“) im südöstlichen Quadranten."
            )
        )
    )

    val ROUTE_ORION_BELT_TO_M42 = StarHopRoute(
        id = "orion_belt_to_m42",
        targetName = "Großer Orionnebel (M 42)",
        targetCatalogId = "M 42 · NGC 1976",
        guideStar = "Alnitak (Gürtel des Orion)",
        guideStarCatalogId = "HIP 26727",
        constellation = "Orion",
        difficulty = StarHopDifficulty.EASY,
        estimatedHops = 2,
        description = "Vom östlichen Gürtelstern Alnitak hinab zum Schwertgehänge des Orion in die bekannteste Sternenwiege der Galaxie.",
        steps = listOf(
            StarHopStep(
                stepIndex = 0,
                title = "Start bei Alnitak (Zeta Orionis)",
                instruction = "Zentriere Alnitak (1.74 mag), den linken/östlichen der drei berühmten Gürtelsterne des Orion.",
                fieldCenterCoords = EquatorialCoordinates(5.67931, -1.94250),
                recommendedFovDegrees = 4.0,
                landmarkObjects = listOf("Alnitak (HIP 26727)", "Alnilam", "Mintaka"),
                hint = "Direkt bei Alnitak liegt auch der Pferdekopf- und Flammennebel (visuell extrem anspruchsvoll)."
            ),
            StarHopStep(
                stepIndex = 1,
                title = "Südlich zum Schwertgehänge schwenken",
                instruction = "Schwenke ca. 3.5° senkrecht nach Süden in das Schwertgehänge des Orion bis zur auffälligen Sterngruppe um Theta 1 & 2 Orionis.",
                fieldCenterCoords = EquatorialCoordinates(5.5900, -5.3900),
                recommendedFovDegrees = 2.0,
                landmarkObjects = listOf("Trapez-Sterne (Theta 1 Ori)", "Iota Ori"),
                hint = "Bereits im Sucher ist ein helles, schwingenförmiges Gasleuchten zu erkennen."
            ),
            StarHopStep(
                stepIndex = 2,
                title = "Ziel erreicht: Trapez und Gasschwingen von M 42",
                instruction = "Zentriere das Trapez. Der leuchtende Kern von M 42 und M 43 überwältigt das Bildfeld mit feinsten Gasfilamenten und Dunkelwolken.",
                fieldCenterCoords = EquatorialCoordinates(5.5880, -5.3910),
                recommendedFovDegrees = 1.0,
                landmarkObjects = listOf("Orionnebel (M 42)", "De Mairans Nebel (M 43)", "Trapez"),
                hint = "Selbst bei Mondschein oder Stadtrandhimmel zeigt M 42 bereits prächtige Details."
            )
        )
    )

    val ROUTE_DENEB_TO_NGC7000 = StarHopRoute(
        id = "deneb_to_ngc7000",
        targetName = "Nordamerikanebel (NGC 7000)",
        targetCatalogId = "NGC 7000",
        guideStar = "Deneb (Alpha Cygni)",
        guideStarCatalogId = "HIP 102098",
        constellation = "Schwan",
        difficulty = StarHopDifficulty.MEDIUM,
        estimatedHops = 2,
        description = "Vom hellen Hauptstern Deneb nach Osten zur markanten Umrisslinie des Nordamerikanebels am Sommermilchstraßen-Band.",
        steps = listOf(
            StarHopStep(
                stepIndex = 0,
                title = "Start bei Deneb (Alpha Cygni)",
                instruction = "Zentriere Deneb (1.25 mag), den hellsten Stern im Schwan und nördlichsten Stern des Sommerdreiecks.",
                fieldCenterCoords = EquatorialCoordinates(20.690532, 45.280338),
                recommendedFovDegrees = 4.0,
                landmarkObjects = listOf("Deneb (HIP 102098)"),
                hint = "Deneb ist ein weißer Überriese von gewaltiger Leuchtkraft."
            ),
            StarHopStep(
                stepIndex = 1,
                title = "Ost-Südost zu Xi Cygni schwenken",
                instruction = "Bewege das Teleskop ca. 3° nach Ost-Südost zum Stern Xi Cygni (3.72 mag).",
                fieldCenterCoords = EquatorialCoordinates(21.0819, 43.9272),
                recommendedFovDegrees = 3.0,
                landmarkObjects = listOf("Xi Cygni (HIP 104043)"),
                hint = "Xi Cygni markiert den westlichen Rand des Nebels nahe der „Karibik-Bucht“."
            ),
            StarHopStep(
                stepIndex = 2,
                title = "Ziel erreicht: Nordamerika-Wand („Mexiko & Florida“)",
                instruction = "Platziere das Gesichtsfeld zwischen Deneb und Xi Cygni auf RA 20h 59m, Dec +44° 31'. Bei großem Gesichtsfeld (>= 2.5°) und dunklem Himmel zeichnet sich der Kontinent ab.",
                fieldCenterCoords = EquatorialCoordinates(20.9833, 44.5300),
                recommendedFovDegrees = 3.0,
                landmarkObjects = listOf("Nordamerikanebel (NGC 7000)", "Pelikannebel (IC 5070)"),
                hint = "Ein H-Beta- oder OIII-Filter und weites Gesichtsfeld (Fernglas/Richfield) sind hier ideal."
            )
        )
    )

    private val allRoutes = listOf(
        ROUTE_VEGA_TO_M57,
        ROUTE_MERAK_DUBHE_TO_M81_M82,
        ROUTE_ALPHERATZ_MIRACH_TO_M31,
        ROUTE_ALBIREO_TO_M27,
        ROUTE_KEYSTONE_TO_M13,
        ROUTE_ORION_BELT_TO_M42,
        ROUTE_DENEB_TO_NGC7000
    )

    fun getAllRoutes(): List<StarHopRoute> = allRoutes

    fun getRouteById(id: String): StarHopRoute? = allRoutes.firstOrNull { it.id == id }

    fun findRoutesForTarget(targetCatalogId: String): List<StarHopRoute> {
        val clean = targetCatalogId.trim().lowercase()
        return allRoutes.filter { route ->
            route.targetCatalogId.lowercase().contains(clean) ||
            route.targetName.lowercase().contains(clean) ||
            clean.contains(route.targetName.lowercase())
        }
    }

    fun findRoutesForGuideStar(guideStarName: String): List<StarHopRoute> {
        val clean = guideStarName.trim().lowercase()
        return allRoutes.filter { it.guideStar.lowercase().contains(clean) }
    }

    fun findRoutesByDifficulty(difficulty: StarHopDifficulty): List<StarHopRoute> {
        return allRoutes.filter { it.difficulty == difficulty }
    }

    fun findRoutesInConstellation(constellation: String): List<StarHopRoute> {
        val clean = constellation.trim().lowercase()
        return allRoutes.filter { it.constellation.lowercase().contains(clean) }
    }
}
