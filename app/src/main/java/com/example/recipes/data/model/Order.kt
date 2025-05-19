// app/src/main/java/com/example/recipes/data/model/Order.kt
package com.example.recipes.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Order(
    val id: String = "",              //from firestore
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Grand-total is computed from items */
    val total: Double
        get() = items.sumOf { it.price }

    /** yyyy-MM-dd HH:mm (existing helper) */
    fun dateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /** Friendly title for dialogs or lists */
    fun readableTitle(): String {
        val fmt = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
        return "Order • ${fmt.format(Date(timestamp))}"
    }
}
