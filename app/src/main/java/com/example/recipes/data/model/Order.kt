package com.example.recipes.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val address: String = "",           // ← new
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Optional convenience: a human‐readable title for dialogs */
    fun readableTitle(): String {
        val dt = java.text.DateFormat.getDateTimeInstance().format(timestamp)
        return "Order on $dt"
    }

    /** Compute total price on the fly */
    val total: Double
        get() = items.sumOf { it.price }
}
