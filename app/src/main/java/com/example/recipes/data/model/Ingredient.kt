// app/src/main/java/com/example/recipes/data/model/Ingredient.kt
package com.example.recipes.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Ingredient @JvmOverloads constructor(
    @SerializedName("id")           val id: Int = 0,
    @SerializedName("name")         val name: String = "",
    @SerializedName("original")     val original: String = "",
    @SerializedName("amount")       val amount: Double = 0.0,    // how many units
    @SerializedName("unit")         val unit: String = ""       // e.g. "g", "kg", "cup"
) : Parcelable
