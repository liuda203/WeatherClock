package com.weatheralarm.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val province: String? = null,
    val city: String? = null,
    val district: String? = null
)

object LocationHelper {
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun resolveLocation(context: Context): DeviceLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var best: Location? = null
        for (provider in providers) {
            if (!manager.isProviderEnabled(provider) && provider != LocationManager.PASSIVE_PROVIDER) {
                continue
            }
            val location = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            if (location != null && (best == null || location.accuracy < best.accuracy)) {
                best = location
            }
        }
        val location = best ?: return@withContext null
        reverseGeocodeDetail(context, location.latitude, location.longitude)
            ?: DeviceLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                label = "当前位置 (${"%.3f".format(location.latitude)}, ${"%.3f".format(location.longitude)})"
            )
    }

    private fun reverseGeocodeDetail(context: Context, lat: Double, lon: Double): DeviceLocation? {
        return runCatching {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, Locale.CHINA)
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(lat, lon, 1)
            val address = results?.firstOrNull() ?: return null
            val province = address.adminArea
            val city = address.locality ?: address.subAdminArea
            val district = address.subLocality ?: address.thoroughfare
            val label = listOfNotNull(province, city, district)
                .distinct()
                .joinToString(" · ")
                .ifBlank { address.getAddressLine(0) }
            DeviceLocation(
                latitude = lat,
                longitude = lon,
                label = label ?: "当前位置",
                province = province,
                city = city,
                district = district
            )
        }.getOrNull()
    }
}
