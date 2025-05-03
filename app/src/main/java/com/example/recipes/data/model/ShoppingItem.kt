package com.example.recipes.data.model

/**
 * Represents a shopping list entry in Firestore.
 * The 'id' field holds the Firestore document ID so you can delete/edit it later.
 */
data class ShoppingItem(
    val id: String = "",       // Firestore document ID (must be set when you read from / write to Firestore)
    val name: String = "",     // e.g. "2 cups flour"
    val purchased: Boolean = false
)
