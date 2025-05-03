package com.example.recipes.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@IgnoreExtraProperties
@Parcelize
data class Nutrition @JvmOverloads constructor(
    @SerializedName("nutrients")
    val nutrients: @RawValue List<Nutrient>? = null
) : Parcelable
