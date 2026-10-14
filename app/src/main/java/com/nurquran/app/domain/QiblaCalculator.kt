package com.nurquran.app.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.PI

object QiblaCalculator {
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    /** Initial great-circle bearing clockwise from true north, normalized to [0, 360). */
    fun bearing(latitude: Double, longitude: Double): Double {
        require(latitude in -90.0..90.0) { "Latitude di luar rentang" }
        require(longitude in -180.0..180.0) { "Longitude di luar rentang" }
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLon = Math.toRadians(KAABA_LONGITUDE - longitude)
        val y = sin(deltaLon)
        val x = cos(lat1) * tan(lat2) - sin(lat1) * cos(deltaLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}
