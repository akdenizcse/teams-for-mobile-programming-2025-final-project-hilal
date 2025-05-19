package com.example.recipes.data.model


data class NearbyStore(
    val name: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val distanceMeters: Float = 0f
)
