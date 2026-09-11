package com.weatheralarm.app.data

data class ChinaCity(
    val code: String,
    val name: String,
    val province: String,
    val parent: String,
    val display: String
)

data class CitySuggestion(
    val code: String,
    val name: String,
    val province: String,
    val parent: String,
    val displayName: String
)
