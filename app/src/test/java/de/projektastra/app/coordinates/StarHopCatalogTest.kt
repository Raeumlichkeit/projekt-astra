package de.projektastra.app.coordinates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarHopCatalogTest {

    @Test
    fun testCatalogCompletenessAndIntegrity() {
        val routes = StarHopCatalog.getAllRoutes()
        assertEquals("Expected 7 curated star-hop routes", 7, routes.size)

        val expectedIds = setOf(
            "vega_to_m57",
            "merak_dubhe_to_m81_m82",
            "alpheratz_mirach_to_m31",
            "albireo_to_m27",
            "keystone_to_m13",
            "orion_belt_to_m42",
            "deneb_to_ngc7000"
        )
        val actualIds = routes.map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)

        routes.forEach { route ->
            assertTrue(route.targetName.isNotBlank())
            assertTrue(route.targetCatalogId.isNotBlank())
            assertTrue(route.guideStar.isNotBlank())
            assertTrue(route.constellation.isNotBlank())
            assertTrue("Route ${route.id} must have >= 2 steps", route.steps.size >= 2)

            route.steps.forEachIndexed { index, step ->
                assertEquals("Step indices must be consecutive from 0", index, step.stepIndex)
                assertTrue(step.title.isNotBlank())
                assertTrue(step.instruction.isNotBlank())
            }
        }
    }

    @Test
    fun testCoordinateSanityAndBounds() {
        val routes = StarHopCatalog.getAllRoutes()
        routes.forEach { route ->
            route.steps.forEach { step ->
                val coords = step.fieldCenterCoords
                assertTrue("RA must be in [0, 24), was ${coords.raHours}", coords.raHours in 0.0..24.0)
                assertTrue("Dec must be in [-90, 90], was ${coords.decDegrees}", coords.decDegrees in -90.0..90.0)
                assertTrue("Recommended FOV must be positive, was ${step.recommendedFovDegrees}", step.recommendedFovDegrees > 0.0)
                assertTrue("Recommended FOV should not exceed 10° for hopping", step.recommendedFovDegrees <= 10.0)
            }
        }
    }

    @Test
    fun testRealisticHopDistances() {
        val routes = StarHopCatalog.getAllRoutes()
        routes.forEach { route ->
            for (i in 0 until route.steps.size - 1) {
                val step1 = route.steps[i]
                val step2 = route.steps[i + 1]
                val dist = step1.fieldCenterCoords.angularDistanceTo(step2.fieldCenterCoords)
                assertTrue(
                    "Hop distance between step $i and ${i + 1} in '${route.id}' was $dist° (should be <= 15.0°)",
                    dist <= 15.0
                )
            }
        }
    }

    @Test
    fun testQueryUtilities() {
        // Query by ID
        val vegaRoute = StarHopCatalog.getRouteById("vega_to_m57")
        assertNotNull(vegaRoute)
        assertEquals("Ringnebel (M 57)", vegaRoute!!.targetName)
        assertNull(StarHopCatalog.getRouteById("non_existent_route"))

        // Query by Target
        val m57Routes = StarHopCatalog.findRoutesForTarget("M 57")
        assertEquals(1, m57Routes.size)
        assertEquals("vega_to_m57", m57Routes.first().id)

        val ngc6853Routes = StarHopCatalog.findRoutesForTarget("NGC 6853")
        assertEquals(1, ngc6853Routes.size)
        assertEquals("albireo_to_m27", ngc6853Routes.first().id)

        // Query by Guide Star
        val wegaRoutes = StarHopCatalog.findRoutesForGuideStar("Wega")
        assertEquals(1, wegaRoutes.size)

        // Query by Difficulty
        val easyRoutes = StarHopCatalog.findRoutesByDifficulty(StarHopDifficulty.EASY)
        assertTrue(easyRoutes.isNotEmpty())
        easyRoutes.forEach { assertEquals(StarHopDifficulty.EASY, it.difficulty) }

        val mediumRoutes = StarHopCatalog.findRoutesByDifficulty(StarHopDifficulty.MEDIUM)
        assertTrue(mediumRoutes.isNotEmpty())
        mediumRoutes.forEach { assertEquals(StarHopDifficulty.MEDIUM, it.difficulty) }

        // Query by Constellation
        val lyraRoutes = StarHopCatalog.findRoutesInConstellation("Leier")
        assertEquals(1, lyraRoutes.size)
        assertEquals("vega_to_m57", lyraRoutes.first().id)
    }

    @Test
    fun testSessionStateProgression() {
        val route = StarHopCatalog.ROUTE_KEYSTONE_TO_M13 // 3 steps: 0, 1, 2
        var session = StarHopSessionState(activeRoute = route, activeStepIndex = 0)

        assertEquals(0, session.activeStepIndex)
        assertEquals(route.steps[0], session.currentStep)
        assertFalse(session.isFinished)

        // Advance to step 1
        session = session.nextStep()
        assertEquals(1, session.activeStepIndex)
        val routeAfterStep1 = session.activeRoute
        assertNotNull(routeAfterStep1)
        assertTrue(routeAfterStep1!!.steps[0].isCompleted)
        assertFalse(routeAfterStep1.steps[1].isCompleted)
        assertFalse(session.isFinished)

        // Advance to step 2 (final step)
        session = session.nextStep()
        assertEquals(2, session.activeStepIndex)
        val routeAfterStep2 = session.activeRoute
        assertNotNull(routeAfterStep2)
        assertTrue(routeAfterStep2!!.steps[1].isCompleted)
        assertFalse(session.isFinished)

        // Complete final step
        session = session.nextStep()
        assertTrue(session.isFinished)
        val finishedRoute = session.activeRoute
        assertNotNull(finishedRoute)
        assertTrue(finishedRoute!!.steps.all { it.isCompleted })

        // Previous step
        session = session.previousStep()
        assertEquals(1, session.activeStepIndex)
        assertFalse(session.isFinished)

        // Jump to step
        session = session.jumpToStep(0)
        assertEquals(0, session.activeStepIndex)

        // Toggle step completed
        session = session.toggleStepCompleted(0)
        val toggledRoute = session.activeRoute
        assertNotNull(toggledRoute)
        assertFalse(toggledRoute!!.steps[0].isCompleted)

        // Reset session
        session = session.reset()
        assertEquals(0, session.activeStepIndex)
        assertFalse(session.isFinished)
        val resetRoute = session.activeRoute
        assertNotNull(resetRoute)
        assertTrue(resetRoute!!.steps.none { it.isCompleted })
    }
}
