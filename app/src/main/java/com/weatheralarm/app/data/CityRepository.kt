package com.weatheralarm.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CityRepository(
    private val appContext: Context
) {
    suspend fun searchCities(query: String, limit: Int = 20): List<CitySuggestion> =
        withContext(Dispatchers.Default) {
            ChinaCityCatalog.search(appContext, query, limit)
        }
}
