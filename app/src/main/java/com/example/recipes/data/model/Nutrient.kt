package com.example.recipes.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Nutrient @JvmOverloads constructor(
    @SerializedName("name")   val name: String = "",
    @SerializedName("amount") val amount: Double = 0.0
) : Parcelable
