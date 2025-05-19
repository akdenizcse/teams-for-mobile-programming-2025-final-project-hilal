package com.example.recipes.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Review(
    val userId: String = "",
    val userName: String = "",
    val recipeId: Int = 0,
    val stars: Int = 0,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
