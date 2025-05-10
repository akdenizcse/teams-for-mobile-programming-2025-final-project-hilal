// app/src/main/java/com/example/recipes/data/model/Review.kt
package com.example.recipes.data.model

data class Review(
    val userId: String = "",
    val recipeId: Int = 0,
    val rating: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
