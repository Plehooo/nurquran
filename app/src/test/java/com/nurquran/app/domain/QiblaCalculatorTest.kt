package com.nurquran.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {
    @Test fun jakartaBearingMatchesPublishedCoordinateCalculation() {
        assertEquals(295.15, QiblaCalculator.bearing(-6.2088, 106.8456), 0.2)
    }

    @Test fun resultIsNormalizedToCompassRange() {
        val result = QiblaCalculator.bearing(0.0, 0.0)
        assertTrue(result >= 0.0 && result < 360.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeLatitude() {
        QiblaCalculator.bearing(91.0, 0.0)
    }
}
