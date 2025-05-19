package com.example.recipes.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
@Parcelize
data class CartItem(
    val id: String = "",          // Firestore doc-id  (auto / copy)
    val recipeId: Int = 0,
    val title: String = "",
    val price: Double = 0.0,      // fake price
    val image: String = ""
) : Parcelable
