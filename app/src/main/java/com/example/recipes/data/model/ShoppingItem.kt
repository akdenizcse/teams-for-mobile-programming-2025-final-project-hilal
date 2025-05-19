// app/src/main/java/com/example/recipes/data/model/ShoppingItem.kt
package com.example.recipes.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val recipeId: String? = null,
    val price: Double = 0.0
)

