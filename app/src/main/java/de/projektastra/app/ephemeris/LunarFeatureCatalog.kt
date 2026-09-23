package de.projektastra.app.ephemeris

internal object LunarFeatureCatalog {

    val allFeatures: List<LunarFeature> = listOf(
        // SCARPS & FAULTS
        LunarFeature(
            id = "rupes_recta",
            name = "Rupes Recta",
            type = LunarFeatureType.SCARP,
            selenographicLat = -22.1,
            selenographicLon = -7.8,
            diameterKm = 110.0,
            description = "Die berühmte 'Lange Wand': Eine markante tektonische Geländestufe im östlichen Mare Nubium.",
            observationTip = "Wirft bei Sonnenaufgang (Colongitude 1°–3°) einen messerscharfen, tiefschwarzen Schatten nach Westen."
        ),
        LunarFeature(
            id = "rupes_altai",
            name = "Rupes Altai",
            type = LunarFeatureType.SCARP,
            selenographicLat = -24.3,
            selenographicLon = 22.6,
            diameterKm = 480.0,
            description = "Gewaltige Geländestufe des Nectaris-Einschlagsbeckens mit bis zu 1.000 m Steilabfall.",
            observationTip = "Hervorragend sichtbar bei zunehmendem Mond (Tag 4–5) entlang des Morgenterminators."
        ),
        LunarFeature(
            id = "rupes_cauchy",
            name = "Rupes Cauchy",
            type = LunarFeatureType.SCARP,
            selenographicLat = 9.0,
            selenographicLon = 37.0,
            diameterKm = 120.0,
            description = "Geländestufe im Mare Tranquillitatis parallel zur Rima Cauchy.",
            observationTip = "Schattenfall bei Sonnenaufgang hebt den Höhenunterschied von 300 m deutlich hervor."
        ),
        LunarFeature(
            id = "rupes_kelvin",
            name = "Rupes Kelvin",
            type = LunarFeatureType.SCARP,
            selenographicLat = -27.3,
            selenographicLon = -33.1,
            diameterKm = 78.0,
            description = "Verwerfungslinie am Rand des Mare Humorum nahe Promontorium Kelvin.",
            observationTip = "Tritt kurz vor Vollmond bei Sonnenaufgang an Tag 10 plastisch hervor."
        ),

        // RILLES & VALLEYS
        LunarFeature(
            id = "vallis_alpes",
            name = "Vallis Alpes",
            type = LunarFeatureType.RILLE,
            selenographicLat = 48.5,
            selenographicLon = -3.2,
            diameterKm = 166.0,
            description = "Das Alpental: Ein geradliniger tektonischer Graben durch die Mondalpen.",
            observationTip = "Die extrem schmale Zentralrille (ca. 700 m) ist ein Härtetest für Fernrohre ab 150 mm Öffnung."
        ),
        LunarFeature(
            id = "hadley_rille",
            name = "Hadley-Rille (Rima Hadley)",
            type = LunarFeatureType.RILLE,
            selenographicLat = 25.0,
            selenographicLon = 3.0,
            diameterKm = 80.0,
            description = "Mäandrierende Lavaröhre am Fuß der Apenninen; historische Landestelle von Apollo 15.",
            observationTip = "Am besten bei niedrigem Sonnenstand am Morgenterminator sichtbar (Colongitude 0°–2°)."
        ),
        LunarFeature(
            id = "vallis_schroeteri",
            name = "Vallis Schröteri",
            type = LunarFeatureType.RILLE,
            selenographicLat = 26.2,
            selenographicLon = -50.8,
            diameterKm = 160.0,
            description = "Größte gewundene Lavarille des Mondes auf dem vulkanischen Aristarchus-Plateau.",
            observationTip = "Der charakteristische 'Schlangenkopf' (Kraterursprung) ist ein spektakuläres Detail bei Tag 11–12."
        ),
        LunarFeature(
            id = "rima_ariadaeus",
            name = "Rima Ariadaeus",
            type = LunarFeatureType.RILLE,
            selenographicLat = 6.4,
            selenographicLon = 14.0,
            diameterKm = 220.0,
            description = "Breiter tektonischer Grabenbruch, der Gebirgsrücken wie ein gerader Schnitt durchtrennt.",
            observationTip = "Schon im kleinen Teleskop ab 70 mm bei Colongitude 358°–5° leicht erkennbar."
        ),
        LunarFeature(
            id = "rima_hyginus",
            name = "Rima Hyginus",
            type = LunarFeatureType.RILLE,
            selenographicLat = 7.8,
            selenographicLon = 6.3,
            diameterKm = 220.0,
            description = "Verzweigter Graben mit der zentralen vulkanischen Einsturzkaldera Hyginus.",
            observationTip = "Enthält mehrere aneinandergereihte Vulkankrater (Pits) entlang der Bruchlinie."
        ),
        LunarFeature(
            id = "rimae_hippalus",
            name = "Rimae Hippalus",
            type = LunarFeatureType.RILLE,
            selenographicLat = -25.5,
            selenographicLon = -29.2,
            diameterKm = 240.0,
            description = "Konzentrisches System dreier geschwungener Rillen am Ostrand des Mare Humorum.",
            observationTip = "Durchschneidet den halb überfluteten Krater Hippalus."
        ),
        LunarFeature(
            id = "rimae_petavius",
            name = "Rima Petavius",
            type = LunarFeatureType.RILLE,
            selenographicLat = -25.3,
            selenographicLon = 60.4,
            diameterKm = 80.0,
            description = "Tiefe Bruchspalte vom Zentralberg des Kraters Petavius bis zum südwestlichen Kraterrand.",
            observationTip = "Hervorragend 3 Tage nach Neumond am Morgenterminator."
        ),
        LunarFeature(
            id = "rimae_triesnecker",
            name = "Rimae Triesnecker",
            type = LunarFeatureType.RILLE,
            selenographicLat = 4.3,
            selenographicLon = 4.6,
            diameterKm = 200.0,
            description = "Komplexes Netzwerk filigraner Rillen östlich des Kraters Triesnecker.",
            observationTip = "Erfordert ruhiges Seeing und mindestens 100 mm Öffnung bei Sonnenaufgang."
        ),

        // MOUNTAINS & PEAKS
        LunarFeature(
            id = "montes_apenninus",
            name = "Montes Apenninus",
            type = LunarFeatureType.MOUNTAIN,
            selenographicLat = 18.9,
            selenographicLon = -3.7,
            diameterKm = 600.0,
            description = "Die Mond-Apenninen: Bis zu 5.000 m hohe Steilwand am Südostrand des Mare Imbrium.",
            observationTip = "Spektakuläres Hochgebirgspanorama bei Halbmond mit kilometerlangen Schattenwürfen."
        ),
        LunarFeature(
            id = "mons_piton",
            name = "Mons Piton",
            type = LunarFeatureType.MOUNTAIN,
            selenographicLat = 40.6,
            selenographicLon = -1.1,
            diameterKm = 25.0,
            description = "Isolierter, 2.250 m hoher Pyramidenberg im Mare Imbrium.",
            observationTip = "Wirft bei Sonnenaufgang einen riesigen, messerscharfen Schlagschatten über den Basaltboden."
        ),
        LunarFeature(
            id = "mons_pico",
            name = "Mons Pico",
            type = LunarFeatureType.MOUNTAIN,
            selenographicLat = 45.7,
            selenographicLon = -8.9,
            diameterKm = 25.0,
            description = "Solitärberg südlich von Plato mit einer Gipfelhöhe von ca. 2.400 m.",
            observationTip = "Tritt bei Colongitude 10°–14° spektakulär aus dem Morgenschatten hervor."
        ),
        LunarFeature(
            id = "promontorium_laplace",
            name = "Promontorium Laplace",
            type = LunarFeatureType.MOUNTAIN,
            selenographicLat = 46.0,
            selenographicLon = -25.8,
            diameterKm = 50.0,
            description = "Gewaltiges Steilkap am Osteingang des Sinus Iridum mit bis zu 3.000 m Wandhöhe.",
            observationTip = "Leuchtet bei aufsteigender Sonne auf, während der Sinus Iridum noch im Schatten liegt."
        ),
        LunarFeature(
            id = "promontorium_heraclides",
            name = "Promontorium Heraclides",
            type = LunarFeatureType.MOUNTAIN,
            selenographicLat = 40.3,
            selenographicLon = -33.2,
            diameterKm = 50.0,
            description = "Das 'Mondmädchen': Westliches Steilkap der Regenbogenbucht.",
            observationTip = "Schattenprofile erzeugen bei streifendem Licht das Profil eines Frauenkopfes mit wehendem Haar."
        ),
        LunarFeature(
            id = "montes_carpatus",
            name = "Montes Carpatus",
            type = LunarFeatureType.MOUNTAIN,
            selenographicLat = 14.5,
            selenographicLon = -25.5,
            diameterKm = 360.0,
            description = "Die Mondkarpaten: Gebirgszug südlich des Mare Imbrium nördlich von Copernicus.",
            observationTip = "Zahlreiche zerklüftete Täler und vulkanische Kuppen bei Tag 9 sichtbar."
        ),
        LunarFeature(
            id = "montes_jura",
            name = "Montes Jura",
            type = LunarFeatureType.MOUNTAIN,
            selenographicLat = 47.0,
            selenographicLon = -34.0,
            diameterKm = 420.0,
            description = "Halbkreisförmiger Gebirgsbogen, der den Sinus Iridum umrahmt.",
            observationTip = "Gipfel fangen das erste Sonnenlicht ein, während das Becken noch in finsterer Nacht liegt ('Golden Handle')."
        ),
        LunarFeature(
            id = "mons_huygens",
            name = "Mons Huygens",
            type = LunarFeatureType.MOUNTAIN,
            selenographicLat = 20.0,
            selenographicLon = -2.9,
            diameterKm = 40.0,
            description = "Höchster Bergmassiv-Gipfel der Mondapenninen mit rund 5.500 m Höhe.",
            observationTip = "Zeigt extreme Reliefwirkung am Terminator kurz nach Erstes Viertel."
        ),

        // CRATERS: CENTRAL REGION
        LunarFeature(
            id = "ptolemaeus",
            name = "Ptolemaeus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -9.2,
            selenographicLon = -1.8,
            diameterKm = 153.0,
            description = "Riesige Wallebene mit glattem, basaltgefülltem Boden im Zentrum der Mondvorderseite.",
            observationTip = "Enthält flache Geisterkrater ('Ghost Craters'), die nur bei Sonnenhöhe unter 3° erkennbar sind."
        ),
        LunarFeature(
            id = "alphonsus",
            name = "Alphonsus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -13.4,
            selenographicLon = -2.8,
            diameterKm = 119.0,
            description = "Wallebene mit zentralem Bergmassiv und dunklen vulkanischen Aschedepots (Pyroklastika).",
            observationTip = "Historischer Ort von transienten Mondphänomenen (TLP); Rillensystem auf dem Kraterboden."
        ),
        LunarFeature(
            id = "arzachel",
            name = "Arzachel",
            type = LunarFeatureType.CRATER,
            selenographicLat = -18.2,
            selenographicLon = -1.9,
            diameterKm = 97.0,
            description = "Tief eingesenkter Ringkrater mit 1.500 m hohem Zentralgipfel und gestuften Terrassen.",
            observationTip = "Prächtiges Trio mit Ptolemaeus und Alphonsus am Terminator bei Colongitude 0°–2°."
        ),
        LunarFeature(
            id = "werner",
            name = "Werner",
            type = LunarFeatureType.CRATER,
            selenographicLat = -28.0,
            selenographicLon = 3.3,
            diameterKm = 70.0,
            description = "Sehr scharf konturierter Krater mit hellem Fleck am Innenhang und hohem Zentralberg.",
            observationTip = "Beteiligt an der bekannten 'Lunar X'-Lichtformation."
        ),
        LunarFeature(
            id = "aliacensis",
            name = "Aliacensis",
            type = LunarFeatureType.CRATER,
            selenographicLat = -30.6,
            selenographicLon = 5.2,
            diameterKm = 80.0,
            description = "Südlicher Nachbar von Werner mit steilen Rändern und flachem Zentralberg.",
            observationTip = "Sehr kontrastreich bei zunehmendem Halbmond."
        ),
        LunarFeature(
            id = "walther",
            name = "Walther",
            type = LunarFeatureType.CRATER,
            selenographicLat = -33.0,
            selenographicLon = 1.0,
            diameterKm = 135.0,
            description = "Große unregelmäßige Wallebene mit asymmetrisch verschobenem Zentralgipfel.",
            observationTip = "Zahlreiche Nebenkrater auf dem terrassierten Rand."
        ),
        LunarFeature(
            id = "purbach",
            name = "Purbach",
            type = LunarFeatureType.CRATER,
            selenographicLat = -25.5,
            selenographicLon = -2.0,
            diameterKm = 118.0,
            description = "Erodierte Wallebene; der Nordostrand bildet zusammen mit Blanchinus das 'Lunar X'.",
            observationTip = "Ca. 6 Stunden vor dem Ersten Viertel tritt die X-Illumination hervor."
        ),
        LunarFeature(
            id = "blanchinus",
            name = "Blanchinus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -25.4,
            selenographicLon = 2.5,
            diameterKm = 68.0,
            description = "Stark abgetragener Krater östlich von Purbach.",
            observationTip = "Teil der Werner/Purbach-Hochlandgruppe."
        ),

        // CRATERS: NORTH & NORTH-WEST
        LunarFeature(
            id = "plato",
            name = "Plato",
            type = LunarFeatureType.CRATER,
            selenographicLat = 51.6,
            selenographicLon = -9.3,
            diameterKm = 101.0,
            description = "'Das schwarze Meer': Dunkelster Kraterboden der Mondvorderseite, eingerahmt von steilen Zacken.",
            observationTip = "Östliche Kraterzinnen werfen bei Sonnenaufgang spektakuläre spitze Nadelschatten über den Boden."
        ),
        LunarFeature(
            id = "archimedes",
            name = "Archimedes",
            type = LunarFeatureType.CRATER,
            selenographicLat = 29.7,
            selenographicLon = -4.0,
            diameterKm = 83.0,
            description = "Große Wallebene mit glattem lavagefülltem Boden ohne Zentralberg im Palus Putredinis.",
            observationTip = "Bildet mit Aristillus und Autolycus ein markantes Dreigestirn im Mare Imbrium."
        ),
        LunarFeature(
            id = "aristillus",
            name = "Aristillus",
            type = LunarFeatureType.CRATER,
            selenographicLat = 33.9,
            selenographicLon = 1.2,
            diameterKm = 55.0,
            description = "Kompakter Terrassenkrater mit auffälliger Dreifach-Zentralberggruppe.",
            observationTip = "Zeigt bei Vollmond ein ausgedehntes helles Strahlensystem."
        ),
        LunarFeature(
            id = "autolycus",
            name = "Autolycus",
            type = LunarFeatureType.CRATER,
            selenographicLat = 30.7,
            selenographicLon = 1.5,
            diameterKm = 39.0,
            description = "Südlicher Nachbarkrater von Aristillus mit scharfen Konturen.",
            observationTip = "Historische Absturzstelle von Luna 2 (1959), der ersten irdischen Sonde auf dem Mond."
        ),
        LunarFeature(
            id = "cassini",
            name = "Cassini",
            type = LunarFeatureType.CRATER,
            selenographicLat = 39.8,
            selenographicLon = 4.6,
            diameterKm = 57.0,
            description = "Mit Lava überfluteter Kraterboden mit den beiden auffälligen Binnenkratern Cassini A und B.",
            observationTip = "Ideal beobachtbar bei Colongitude 358°–4°."
        ),
        LunarFeature(
            id = "eratosthenes",
            name = "Eratosthenes",
            type = LunarFeatureType.CRATER,
            selenographicLat = 14.5,
            selenographicLon = -11.3,
            diameterKm = 58.0,
            description = "Markanter Abschlusskrater der Apenninenkette mit steilem Zentralberg.",
            observationTip = "Beeindruckende Terrassenschatten am Terminator; verschwindet bei hohem Sonnenstand fast völlig."
        ),
        LunarFeature(
            id = "copernicus",
            name = "Copernicus",
            type = LunarFeatureType.CRATER,
            selenographicLat = 9.6,
            selenographicLon = -20.1,
            diameterKm = 93.0,
            description = "'Das Auge des Mondes': Einer der spektakulärsten Ringkrater mit gewaltigen Terrassen und Zentralbergen.",
            observationTip = "Am Terminator (Tag 9–10) wirft der Kraterrand kilometerlange Schatten; bei Vollmond riesiges Strahlensystem."
        ),
        LunarFeature(
            id = "aristarchus",
            name = "Aristarchus",
            type = LunarFeatureType.CRATER,
            selenographicLat = 23.7,
            selenographicLon = -47.4,
            diameterKm = 40.0,
            description = "Hellste Formation der Mondvorderseite (Albedo 0.2); leuchtet selbst im Erdschein sichtbar.",
            observationTip = "Im Kraterinneren und auf dem Zentralberg sind dunkle Bänder an den Steilhängen erkennbar."
        ),
        LunarFeature(
            id = "herodotus",
            name = "Herodotus",
            type = LunarFeatureType.CRATER,
            selenographicLat = 23.2,
            selenographicLon = -49.7,
            diameterKm = 35.0,
            description = "Flacher lavagefüllter Begleiter von Aristarchus; direkter Ausgangspunkt des Schrötertals.",
            observationTip = "Interessanter Helligkeitskontrast: dunkel gegenüber dem blendend hellen Aristarchus."
        ),
        LunarFeature(
            id = "kepler",
            name = "Kepler",
            type = LunarFeatureType.CRATER,
            selenographicLat = 8.1,
            selenographicLon = -38.0,
            diameterKm = 31.0,
            description = "Kompakter junger Einschlagkrater im Oceanus Procellarum mit starkem Strahlensystem.",
            observationTip = "Sehr kontrastreich bei Tag 10–11 am Morgenterminator."
        ),

        // CRATERS: EAST & NORTH-EAST
        LunarFeature(
            id = "theophilus",
            name = "Theophilus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -11.4,
            selenographicLon = 26.4,
            diameterKm = 100.0,
            description = "Prachtvoller Ringkrater mit 1.400 m hohem Dreifach-Zentralberg und steil gestuften Wänden.",
            observationTip = "Überlagert den älteren Nachbarkrater Cyrillus; herrlicher Anblick an Tag 5."
        ),
        LunarFeature(
            id = "cyrillus",
            name = "Cyrillus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -13.2,
            selenographicLon = 24.0,
            diameterKm = 98.0,
            description = "Stark erodierter Nachbarkrater von Theophilus mit geschwungenem Innenrücken.",
            observationTip = "Demonstriert anschaulich die zeitliche Abfolge lunarer Einschläge."
        ),
        LunarFeature(
            id = "catharina",
            name = "Catharina",
            type = LunarFeatureType.CRATER,
            selenographicLat = -18.0,
            selenographicLon = 23.6,
            diameterKm = 100.0,
            description = "Ältester Krater des Theophilus-Trios mit tief zerfurchter Wallebene.",
            observationTip = "Sehr schöner Schattenwurf bei Sonnenaufgang (Colongitude 330°–335°)."
        ),
        LunarFeature(
            id = "posidonius",
            name = "Posidonius",
            type = LunarFeatureType.CRATER,
            selenographicLat = 31.8,
            selenographicLon = 29.9,
            diameterKm = 95.0,
            description = "Großer Bruchbodenkrater (FFC) am Nordostrand des Mare Serenitatis.",
            observationTip = "Enthält einen inneren Kraterring, Rillen und einen dezentralen Nebenkrater (Posidonius A)."
        ),
        LunarFeature(
            id = "cleomedes",
            name = "Cleomedes",
            type = LunarFeatureType.CRATER,
            selenographicLat = 27.7,
            selenographicLon = 56.0,
            diameterKm = 126.0,
            description = "Ausgedehnte Wallebene nördlich des Mare Crisium mit dunklem, glattem Boden.",
            observationTip = "Schon 3 Tage nach Neumond am Terminator gut zu erfassen."
        ),
        LunarFeature(
            id = "petavius",
            name = "Petavius",
            type = LunarFeatureType.CRATER,
            selenographicLat = -25.3,
            selenographicLon = 60.4,
            diameterKm = 177.0,
            description = "Majestätischer Ostrand-Krater mit markantem Zentralgebirge und mächtiger Radialrille (Rima Petavius).",
            observationTip = "Ein Highlight der frühen Mondsichel (Tag 3)."
        ),
        LunarFeature(
            id = "langrenus",
            name = "Langrenus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -8.9,
            selenographicLon = 61.0,
            diameterKm = 132.0,
            description = "Wallebene am Ostrand des Mare Fecunditatis mit hellem Doppelzentralberg.",
            observationTip = "Helle Terrassen leuchten schon bei sehr flachem Sonnenwinkel auf."
        ),
        LunarFeature(
            id = "messier",
            name = "Messier & Messier A",
            type = LunarFeatureType.CRATER,
            selenographicLat = -1.9,
            selenographicLon = 47.6,
            diameterKm = 14.0,
            description = "Berühmtes Kraterduo mit Kometenschweif-Doppelstrahl durch extrem flachen Einschlagswinkel.",
            observationTip = "Der Zwillingsstrahl erstreckt sich über mehr als 100 km nach Westen über das Mare Fecunditatis."
        ),
        LunarFeature(
            id = "fracastorius",
            name = "Fracastorius",
            type = LunarFeatureType.CRATER,
            selenographicLat = -21.2,
            selenographicLon = 33.0,
            diameterKm = 124.0,
            description = "Zur Nectaris-Bucht hin überfluteter Geisterkrater, dessen Nordrand komplett in Basalt versunken ist.",
            observationTip = "Filigrane Rillen auf dem Boden treten bei Sonnenhöhe 1°–2° hervor."
        ),

        // CRATERS: SOUTH & SOUTH-WEST
        LunarFeature(
            id = "tycho",
            name = "Tycho",
            type = LunarFeatureType.CRATER,
            selenographicLat = -43.3,
            selenographicLon = -11.2,
            diameterKm = 86.0,
            description = "Geologisch junger Prachtkrater mit 1.600 m hohem Zentralberg und 1.500 km weitem Strahlensystem.",
            observationTip = "Am Terminator tiefe Schlagschatten der 4.800 m tiefen Wände; bei Vollmond dominierendes Merkmal des Südens."
        ),
        LunarFeature(
            id = "clavius",
            name = "Clavius",
            type = LunarFeatureType.CRATER,
            selenographicLat = -58.4,
            selenographicLon = -14.4,
            diameterKm = 225.0,
            description = "Gewaltige südliche Wallebene mit einer bogenförmigen Kette abnehmender Krater auf dem Boden.",
            observationTip = "Die Kraterkette (Rutherfurd, Clavius D, C, N, J) dient Beobachtern als klassischer Teleskop-Auflösungstest."
        ),
        LunarFeature(
            id = "maginus",
            name = "Maginus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -50.0,
            selenographicLon = -5.9,
            diameterKm = 163.0,
            description = "Stark erodierte Wallebene nordöstlich von Clavius mit komplex zerfallenen Rändern.",
            observationTip = "Schattenfall bei tiefem Sonnenstand lässt die alte Struktur dramatisch hervortreten."
        ),
        LunarFeature(
            id = "longomontanus",
            name = "Longomontanus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -49.5,
            selenographicLon = -21.7,
            diameterKm = 145.0,
            description = "Tiefe kreisrunde Wallebene westlich von Tycho mit glattem Boden.",
            observationTip = "Schon im kleinen Feldstecher als markante dunkle Senke am Terminator sichtbar."
        ),
        LunarFeature(
            id = "moretus",
            name = "Moretus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -70.6,
            selenographicLon = -5.5,
            diameterKm = 114.0,
            description = "Tiefer Südpolkrater mit einem der imposantesten Zentralberge des Mondes (2.100 m Höhe).",
            observationTip = "Durch die perspektivische Verkürzung nahe dem Südpol stark elliptisch verzerrt."
        ),
        LunarFeature(
            id = "bullialdus",
            name = "Bullialdus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -20.7,
            selenographicLon = -22.2,
            diameterKm = 61.0,
            description = "Isolierter Prachtkrater im Mare Nubium mit steilen Terrassen und markantem Doppelzentralberg.",
            observationTip = "Erinnert im Aufbau an eine verkleinerte Version von Copernicus."
        ),
        LunarFeature(
            id = "gassendi",
            name = "Gassendi",
            type = LunarFeatureType.CRATER,
            selenographicLat = -17.5,
            selenographicLon = -39.9,
            diameterKm = 110.0,
            description = "Prachtvoller Bruchbodenkrater (FFC) am Nordrand des Mare Humorum mit filigranen Rillen (Rimae Gassendi).",
            observationTip = "Mehrere Zentralberggipfel und das reiche Rillennetz sind ideale Beobachtungsziele an Tag 10."
        ),
        LunarFeature(
            id = "schickard",
            name = "Schickard",
            type = LunarFeatureType.CRATER,
            selenographicLat = -44.4,
            selenographicLon = -55.1,
            diameterKm = 227.0,
            description = "Riesige südwestliche Wallebene mit charakteristisch gestreiftem Hell-Dunkel-Muster auf dem Boden.",
            observationTip = "Boden zeigt deutliche Farb- und Albedounterschiede durch verschiedene Lavaströme."
        ),
        LunarFeature(
            id = "hainzel",
            name = "Hainzel",
            type = LunarFeatureType.CRATER,
            selenographicLat = -41.2,
            selenographicLon = -33.5,
            diameterKm = 70.0,
            description = "Kurioses Kratergebilde aus drei ineinander verschmolzenen Kratern.",
            observationTip = "Sehr plastischer Anblick bei schrägem Lichteinfall an Tag 10."
        ),
        LunarFeature(
            id = "schiller",
            name = "Schiller",
            type = LunarFeatureType.CRATER,
            selenographicLat = -51.8,
            selenographicLon = -40.0,
            diameterKm = 179.0,
            description = "Einzigartige langgestreckte Wallebene (Schuhsohlenform), vermutlich durch Schrägeinschlag entstanden.",
            observationTip = "Länge 179 km bei nur 71 km Breite; Längsgrat im nordwestlichen Teil."
        ),
        LunarFeature(
            id = "grimaldi",
            name = "Grimaldi",
            type = LunarFeatureType.CRATER,
            selenographicLat = -5.2,
            selenographicLon = -68.6,
            diameterKm = 173.0,
            description = "Dunkelste Wallebene auf der Mondvorderseite (Albedo nur 0.07) nahe dem Westrand.",
            observationTip = "Wirkt wie ein kleines isoliertes Mondmeer; gut für Librationstests geeignet."
        ),
        LunarFeature(
            id = "hevelius",
            name = "Hevelius",
            type = LunarFeatureType.CRATER,
            selenographicLat = 2.2,
            selenographicLon = -67.6,
            diameterKm = 106.0,
            description = "Zerbrochene Wallebene am Westrand mit kreuzenden Rillen (Rimae Hevelius).",
            observationTip = "Am Terminator kurz vor Neumond oder bei abnehmender Mondsichel faszinierend zerklüftet."
        ),
        LunarFeature(
            id = "bailly",
            name = "Bailly",
            type = LunarFeatureType.CRATER,
            selenographicLat = -66.8,
            selenographicLon = -68.9,
            diameterKm = 287.0,
            description = "Größter Krater der Mondvorderseite; fast schon ein kleines Einschlagsbecken.",
            observationTip = "Nur bei günstiger Südwest-Libration gut über den Mondrand hinweg zu beobachten."
        ),
        LunarFeature(
            id = "maurolycus",
            name = "Maurolycus",
            type = LunarFeatureType.CRATER,
            selenographicLat = -41.8,
            selenographicLon = 14.0,
            diameterKm = 114.0,
            description = "Gewaltiger südlicher Hochlandkrater mit ineinandergreifenden Wällen und Nebenkratern.",
            observationTip = "Sehr dramatische Schattenkaskaden bei Sonnenaufgang an Tag 5."
        ),

        // MARIA & BASINS
        LunarFeature(
            id = "mare_crisium",
            name = "Mare Crisium",
            type = LunarFeatureType.MARE,
            selenographicLat = 17.0,
            selenographicLon = 59.1,
            diameterKm = 555.0,
            description = "Kreisrundes, isoliertes Randmeer; erstes großes Oberflächendetail bei junger Mondsichel.",
            observationTip = "Veranschaulicht die Mondlibration: wandert scheinbar mal näher, mal weiter vom Mondrand weg."
        ),
        LunarFeature(
            id = "mare_serenitatis",
            name = "Mare Serenitatis",
            type = LunarFeatureType.MARE,
            selenographicLat = 28.0,
            selenographicLon = 17.5,
            diameterKm = 707.0,
            description = "'Meer der Heiterkeit': Großes Basaltbecken mit dunklem Randbereich und markanten Meeresrücken.",
            observationTip = "Dorsa Bessel zieht sich als kilometerlange gewundene Falte quer durch das Becken."
        ),
        LunarFeature(
            id = "mare_tranquillitatis",
            name = "Mare Tranquillitatis",
            type = LunarFeatureType.MARE,
            selenographicLat = 8.5,
            selenographicLon = 31.4,
            diameterKm = 873.0,
            description = "'Meer der Ruhe': Historische Landestelle von Apollo 11 (Statio Tranquillitatis, 1969).",
            observationTip = "Bläulicher Titan-Basalt kontrastiert deutlich mit dem rötlicheren Mare Serenitatis."
        ),
        LunarFeature(
            id = "mare_imbrium",
            name = "Mare Imbrium",
            type = LunarFeatureType.MARE,
            selenographicLat = 32.8,
            selenographicLon = -15.6,
            diameterKm = 1123.0,
            description = "'Regenmeer': Gewaltiges Einschlagbecken, umgeben von Alpen, Kaukasus, Apenninen und Karpaten.",
            observationTip = "Beherrscht die Nordwesthälfte des Mondes; reich an Rillen, Faltenrücken und Solitärbergen."
        ),
        LunarFeature(
            id = "sinus_iridum",
            name = "Sinus Iridum",
            type = LunarFeatureType.MARE,
            selenographicLat = 44.1,
            selenographicLon = -31.5,
            diameterKm = 236.0,
            description = "'Regenbogenbucht': Malerische halbkreisförmige Senke, eingerahmt von den Montes Jura.",
            observationTip = "Das Phänomen des 'Goldenen Henkels': Spitzen der Jura-Berge fangen Licht, während die Bucht noch dunkel ist."
        ),
        LunarFeature(
            id = "mare_humorum",
            name = "Mare Humorum",
            type = LunarFeatureType.MARE,
            selenographicLat = -24.4,
            selenographicLon = -38.4,
            diameterKm = 389.0,
            description = "'Meer der Feuchtigkeit': Rundes Lavabecken mit konzentrischen Rillensystemen am Rand.",
            observationTip = "Perfektes Lehrbeispiel für Beckenabsenkung und konzentrische Grabensysteme."
        ),
        LunarFeature(
            id = "mare_nubium",
            name = "Mare Nubium",
            type = LunarFeatureType.MARE,
            selenographicLat = -21.3,
            selenographicLon = -16.6,
            diameterKm = 715.0,
            description = "'Wolkenmeer': Ausgedehnte Tiefebene der südlichen Mondmitte, Heimat von Rupes Recta und Bullialdus.",
            observationTip = "Helle Tycho-Strahlen durchqueren das gesamte Mare von Süden nach Norden."
        ),
        LunarFeature(
            id = "oceanus_procellarum",
            name = "Oceanus Procellarum",
            type = LunarFeatureType.MARE,
            selenographicLat = 18.4,
            selenographicLon = -57.4,
            diameterKm = 2568.0,
            description = "'Ozean der Stürme': Die mit Abstand größte Basaltfläche des Mondes auf der Westhälfte.",
            observationTip = "Landegebiete von Luna 9, Surveyor 1 & 3 sowie Apollo 12."
        ),
        LunarFeature(
            id = "mare_nectaris",
            name = "Mare Nectaris",
            type = LunarFeatureType.MARE,
            selenographicLat = -15.2,
            selenographicLon = 35.5,
            diameterKm = 333.0,
            description = "'Nektarmeer': Kleines, tief eingesenktes kreisförmiges Becken im Südosten.",
            observationTip = "Umrahmt von Rupes Altai und Fracastorius; sehr reich an Kontrasten."
        ),

        // SPECIAL ILLUMINATION & TRANSIENT EFFECTS
        LunarFeature(
            id = "lunar_x",
            name = "Lunar X (Werner X)",
            type = LunarFeatureType.SPECIAL,
            selenographicLat = -25.3,
            selenographicLon = 1.1,
            diameterKm = 25.0,
            description = "Berühmter Licht- und Schatteneffekt an den Kraterrändern Blanchinus, La Caille und Purbach.",
            observationTip = "Erscheint nur für ca. 4 Stunden kurz vor dem Ersten Viertel bei Colongitude ~358° als leuchtendes 'X'."
        ),
        LunarFeature(
            id = "lunar_v",
            name = "Lunar V (Ukert V)",
            type = LunarFeatureType.SPECIAL,
            selenographicLat = 9.9,
            selenographicLon = 1.6,
            diameterKm = 30.0,
            description = "Scharfe V-förmige Lichtformation nahe dem Krater Ukert zur gleichen Zeit wie das Lunar X.",
            observationTip = "Gemeinsam mit dem Lunar X ein beliebtes Beobachtungsziel für Amateurteleskope."
        ),
        LunarFeature(
            id = "marius_hills",
            name = "Marius Hills",
            type = LunarFeatureType.SPECIAL,
            selenographicLat = 12.5,
            selenographicLon = -56.0,
            diameterKm = 150.0,
            description = "Dichtestes Kuppelfeld (vulkanische Domes) auf dem Mond im Oceanus Procellarum.",
            observationTip = "Dutzende flache Lavadome mit winzigen Gipfelkratern; treten nur bei ganz flachem Licht hervor."
        ),
        LunarFeature(
            id = "reiner_gamma",
            name = "Reiner Gamma",
            type = LunarFeatureType.SPECIAL,
            selenographicLat = 7.5,
            selenographicLon = -59.0,
            diameterKm = 70.0,
            description = "Geheimnisvoller lunarer Swirl: Helles, fischförmiges Albedomuster mit starkem lokalen Magnetfeld.",
            observationTip = "Besitzt keinerlei topographische Erhebung; bleibt selbst bei senkrechtem Sonnenstand blendend hell sichtbar."
        ),
        LunarFeature(
            id = "mons_ruemker",
            name = "Mons Rümker",
            type = LunarFeatureType.SPECIAL,
            selenographicLat = 40.8,
            selenographicLon = -58.1,
            diameterKm = 70.0,
            description = "Großer vulkanischer Schildhügelkomplex im nördlichen Oceanus Procellarum; Landestelle von Chang'e 5.",
            observationTip = "Besteht aus über 30 zusammengewachsenen Lavadomen mit extrem sanften Hängen."
        )
    )

    fun findById(id: String): LunarFeature? = allFeatures.firstOrNull { it.id == id }

    fun featuresByType(type: LunarFeatureType): List<LunarFeature> =
        allFeatures.filter { it.type == type }
}
